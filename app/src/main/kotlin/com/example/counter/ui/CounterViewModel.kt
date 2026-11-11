package com.example.counter.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.counter.data.CounterEntity
import com.example.counter.domain.CounterFailure
import com.example.counter.domain.CounterRepository
import com.example.counter.domain.CounterResult
import com.example.counter.notification.CounterNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CounterViewModel(private val repository: CounterRepository, context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val _counters = MutableStateFlow<List<CounterEntity>>(emptyList())
    val counters: StateFlow<List<CounterEntity>> = _counters.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    private val _selectedCounterId = MutableStateFlow<Int?>(null)
    val selectedCounterId: StateFlow<Int?> = _selectedCounterId.asStateFlow()
    private val _trackedCounterIds = MutableStateFlow(
        CounterNotificationManager.getTrackedCounterIds(appContext)
    )
    val trackedCounterIds: StateFlow<Set<Int>> = _trackedCounterIds.asStateFlow()

    init { loadCounters() }

    private fun loadCounters() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getAllCounters().collect { list ->
                    _counters.value = list
                    _isLoading.value = false
                    CounterNotificationManager.syncTrackedCounters(appContext, list)
                    _trackedCounterIds.value = CounterNotificationManager.getTrackedCounterIds(appContext)
                }
            } catch (exception: Exception) {
                handleError("Failed to load counters")
            }
        }
    }

    fun createCounter(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.createCounter(name)) {
                is CounterResult.Success -> {
                    _selectedCounterId.value = result.value.toInt()
                    _isLoading.value = false
                    clearError()
                }
                is CounterResult.Failure -> handleFailure(result.reason)
            }
        }
    }

    fun deleteCounter(counter: CounterEntity) {
        viewModelScope.launch {
            when (val result = repository.deleteCounter(counter)) {
                is CounterResult.Success -> {
                    CounterNotificationManager.removeTrackedCounter(appContext, counter.id)
                    _trackedCounterIds.value = CounterNotificationManager.getTrackedCounterIds(appContext)
                    if (_selectedCounterId.value == counter.id) _selectedCounterId.value = null
                    clearError()
                }
                is CounterResult.Failure -> handleFailure(result.reason)
            }
        }
    }

    fun incrementCounter(id: Int) = runMutation(id) { repository.incrementCounter(id) }
    fun decrementCounter(id: Int) = runMutation(id) { repository.decrementCounter(id) }
    fun resetCounter(id: Int) = runMutation(id) { repository.resetCounter(id) }
    fun setCounterValue(id: Int, value: Int) = runMutation(id) { repository.setCounterValue(id, value) }
    fun renameCounter(id: Int, newName: String) = runMutation(id) { repository.renameCounter(id, newName) }

    fun setTrackingEnabled(id: Int, enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val counter = try {
                    repository.getCounterByIdOnce(id)
                } catch (exception: Exception) {
                    handleError("Unable to enable notification tracking")
                    return@launch
                }
                if (counter == null) {
                    handleFailure(CounterFailure.COUNTER_NOT_FOUND)
                    return@launch
                }
                CounterNotificationManager.enableTracking(appContext, id)
                CounterNotificationManager.syncCounterIfTracked(appContext, counter)
            } else {
                CounterNotificationManager.removeTrackedCounter(appContext, id)
            }
            _trackedCounterIds.value = CounterNotificationManager.getTrackedCounterIds(appContext)
            clearError()
        }
    }

    fun selectCounter(id: Int?) { _selectedCounterId.value = id }

    fun deleteAllCounters() {
        viewModelScope.launch {
            when (val result = repository.deleteAllCounters()) {
                is CounterResult.Success -> {
                    CounterNotificationManager.clearTrackedCounters(appContext)
                    _trackedCounterIds.value = emptySet()
                    _selectedCounterId.value = null
                    clearError()
                }
                is CounterResult.Failure -> handleFailure(result.reason)
            }
        }
    }

    fun getCounterById(id: Int): CounterEntity? = _counters.value.find { it.id == id }

    private fun runMutation(id: Int, operation: suspend () -> CounterResult<Unit>) {
        viewModelScope.launch {
            when (val result = operation()) {
                is CounterResult.Success -> {
                    syncTrackedCounter(id)
                    clearError()
                }
                is CounterResult.Failure -> {
                    syncTrackedCounter(id)
                    handleFailure(result.reason)
                }
            }
        }
    }

    private suspend fun syncTrackedCounter(id: Int) {
        val counter = try { repository.getCounterByIdOnce(id) } catch (exception: Exception) { return }
        if (counter == null) {
            CounterNotificationManager.removeTrackedCounter(appContext, id)
            _trackedCounterIds.value = CounterNotificationManager.getTrackedCounterIds(appContext)
        } else {
            CounterNotificationManager.syncCounterIfTracked(appContext, counter)
        }
    }

    private fun handleFailure(failure: CounterFailure) {
        _errorMessage.value = when (failure) {
            CounterFailure.INVALID_NAME -> "Counter name cannot be empty"
            CounterFailure.NAME_TOO_LONG -> "Counter name cannot exceed 50 characters"
            CounterFailure.DUPLICATE_NAME -> "Counter with this name already exists"
            CounterFailure.COUNTER_NOT_FOUND -> "Counter no longer exists"
            CounterFailure.COUNTER_AT_MINIMUM -> "Counter is already zero"
            CounterFailure.COUNTER_AT_MAXIMUM -> "Counter has reached its maximum value"
            CounterFailure.INVALID_COUNT -> "Counter value cannot be negative"
            CounterFailure.STORAGE_ERROR -> "Unable to save the counter. Please try again"
        }
        _isLoading.value = false
    }

    private fun handleError(message: String) {
        _errorMessage.value = message
        _isLoading.value = false
    }

    fun clearError() { _errorMessage.value = null }
}

class CounterViewModelFactory(
    private val repository: CounterRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CounterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CounterViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
