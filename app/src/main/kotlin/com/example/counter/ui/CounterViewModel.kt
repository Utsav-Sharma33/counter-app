package com.example.counter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.counter.data.CounterEntity
import com.example.counter.domain.CounterDataSource
import com.example.counter.domain.CounterFailure
import com.example.counter.domain.CounterResult
import com.example.counter.notification.CounterNotificationController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CounterUiState(
    val counters: List<CounterEntity> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isCreating: Boolean = false,
    val isDeletingAll: Boolean = false,
    val busyCounterIds: Set<Int> = emptySet(),
    val selectedCounterId: Int? = null,
    val trackedCounterIds: Set<Int> = emptySet(),
    val errorMessage: String? = null
)

class CounterViewModel(
    private val dataSource: CounterDataSource,
    private val notifications: CounterNotificationController
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CounterUiState(trackedCounterIds = notifications.trackedCounterIds())
    )
    val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

    init {
        observeCounters()
    }

    private fun observeCounters() {
        viewModelScope.launch {
            dataSource.getAllCounters()
                .onEach { counters ->
                    _uiState.update { state ->
                        state.copy(
                            counters = counters,
                            isInitialLoading = false,
                            selectedCounterId = state.selectedCounterId
                                ?.takeIf { selectedId -> counters.any { it.id == selectedId } }
                        )
                    }
                    notifications.syncTrackedCounters(counters)
                    _uiState.update {
                        it.copy(trackedCounterIds = notifications.trackedCounterIds())
                    }
                }
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update {
                        it.copy(
                            isInitialLoading = false,
                            errorMessage = "Unable to load counters. Please try again."
                        )
                    }
                }
                .collect { }
        }
    }

    fun createCounter(name: String) {
        if (_uiState.value.isCreating || _uiState.value.isDeletingAll) return
        _uiState.update { it.copy(isCreating = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                when (val result = dataSource.createCounter(name)) {
                    is CounterResult.Success -> _uiState.update {
                        it.copy(selectedCounterId = result.value.toInt(), errorMessage = null)
                    }
                    is CounterResult.Failure -> showFailure(result.reason)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                showMessage("Unable to create the counter. Please try again.")
            } finally {
                _uiState.update { it.copy(isCreating = false) }
            }
        }
    }

    fun deleteCounter(counterId: Int) {
        runCounterOperation(counterId) {
            when (val result = dataSource.deleteCounter(counterId)) {
                is CounterResult.Success -> {
                    notifications.removeTrackedCounter(counterId)
                    _uiState.update {
                        it.copy(
                            selectedCounterId = it.selectedCounterId.takeUnless { id -> id == counterId },
                            trackedCounterIds = notifications.trackedCounterIds(),
                            errorMessage = null
                        )
                    }
                    CounterResult.Success(Unit)
                }
                is CounterResult.Failure -> result
            }
        }
    }

    fun incrementCounter(id: Int) = runCounterOperation(id) { dataSource.incrementCounter(id) }
    fun decrementCounter(id: Int) = runCounterOperation(id) { dataSource.decrementCounter(id) }
    fun resetCounter(id: Int) = runCounterOperation(id) { dataSource.resetCounter(id) }
    fun setCounterValue(id: Int, value: Int) =
        runCounterOperation(id) { dataSource.setCounterValue(id, value) }
    fun renameCounter(id: Int, newName: String) =
        runCounterOperation(id) { dataSource.renameCounter(id, newName) }

    fun setTrackingEnabled(id: Int, enabled: Boolean) {
        if (id in _uiState.value.busyCounterIds || _uiState.value.isDeletingAll) return
        markCounterBusy(id, true)
        viewModelScope.launch {
            try {
                if (enabled) {
                    val counter = dataSource.getCounterByIdOnce(id)
                    if (counter == null) {
                        showFailure(CounterFailure.COUNTER_NOT_FOUND)
                        return@launch
                    }
                    notifications.enableTracking(id)
                    notifications.syncCounterIfTracked(counter)
                } else {
                    notifications.removeTrackedCounter(id)
                }
                _uiState.update {
                    it.copy(trackedCounterIds = notifications.trackedCounterIds(), errorMessage = null)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                showMessage("Unable to update notification tracking. Please try again.")
            } finally {
                markCounterBusy(id, false)
            }
        }
    }

    fun selectCounter(id: Int?) {
        _uiState.update { state ->
            state.copy(
                selectedCounterId = id?.takeIf { selectedId ->
                    state.counters.any { it.id == selectedId }
                }
            )
        }
    }

    fun deleteAllCounters() {
        if (_uiState.value.isDeletingAll || _uiState.value.isCreating ||
            _uiState.value.busyCounterIds.isNotEmpty()
        ) return

        _uiState.update { it.copy(isDeletingAll = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                when (val result = dataSource.deleteAllCounters()) {
                    is CounterResult.Success -> {
                        notifications.clearTrackedCounters()
                        _uiState.update {
                            it.copy(
                                selectedCounterId = null,
                                trackedCounterIds = emptySet(),
                                errorMessage = null
                            )
                        }
                    }
                    is CounterResult.Failure -> showFailure(result.reason)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                showMessage("Unable to delete counters. Please try again.")
            } finally {
                _uiState.update { it.copy(isDeletingAll = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun runCounterOperation(
        id: Int,
        operation: suspend () -> CounterResult<Unit>
    ) {
        if (id in _uiState.value.busyCounterIds || _uiState.value.isDeletingAll) return
        markCounterBusy(id, true)
        viewModelScope.launch {
            try {
                when (val result = operation()) {
                    is CounterResult.Success -> {
                        syncTrackedCounter(id)
                        _uiState.update { it.copy(errorMessage = null) }
                    }
                    is CounterResult.Failure -> {
                        syncTrackedCounter(id)
                        showFailure(result.reason)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                showMessage("Unable to save the counter. Please try again.")
            } finally {
                markCounterBusy(id, false)
            }
        }
    }

    private suspend fun syncTrackedCounter(id: Int) {
        val counter = dataSource.getCounterByIdOnce(id)
        if (counter == null) {
            notifications.removeTrackedCounter(id)
            _uiState.update { it.copy(trackedCounterIds = notifications.trackedCounterIds()) }
        } else {
            notifications.syncCounterIfTracked(counter)
        }
    }

    private fun markCounterBusy(id: Int, busy: Boolean) {
        _uiState.update { state ->
            state.copy(
                busyCounterIds = if (busy) state.busyCounterIds + id
                else state.busyCounterIds - id
            )
        }
    }

    private fun showFailure(failure: CounterFailure) {
        val message = when (failure) {
            CounterFailure.INVALID_NAME -> "Counter name cannot be empty."
            CounterFailure.NAME_TOO_LONG -> "Counter name cannot exceed 50 characters."
            CounterFailure.DUPLICATE_NAME -> "A counter with this name already exists."
            CounterFailure.COUNTER_NOT_FOUND -> "This counter no longer exists."
            CounterFailure.COUNTER_AT_MINIMUM -> "The counter is already zero."
            CounterFailure.COUNTER_AT_MAXIMUM -> "The counter has reached its maximum value."
            CounterFailure.INVALID_COUNT -> "Counter value cannot be negative."
            CounterFailure.STORAGE_ERROR -> "Unable to save the counter. Please try again."
        }
        showMessage(message)
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }
}

class CounterViewModelFactory(
    private val dataSource: CounterDataSource,
    private val notifications: CounterNotificationController
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CounterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CounterViewModel(dataSource, notifications) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
