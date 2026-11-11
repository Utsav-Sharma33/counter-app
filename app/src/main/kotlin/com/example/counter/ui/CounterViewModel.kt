package com.example.counter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.counter.data.CounterEntity
import com.example.counter.domain.CounterFailure
import com.example.counter.domain.CounterRepository
import com.example.counter.domain.CounterResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CounterViewModel(private val repository: CounterRepository) : ViewModel() {

    private val _counters = MutableStateFlow<List<CounterEntity>>(emptyList())
    val counters: StateFlow<List<CounterEntity>> = _counters.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedCounterId = MutableStateFlow<Int?>(null)
    val selectedCounterId: StateFlow<Int?> = _selectedCounterId.asStateFlow()

    init {
        loadCounters()
    }

    private fun loadCounters() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getAllCounters().collect { countersList ->
                    _counters.value = countersList
                    _isLoading.value = false
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
                    if (_selectedCounterId.value == counter.id) {
                        _selectedCounterId.value = null
                    }
                    clearError()
                }
                is CounterResult.Failure -> handleFailure(result.reason)
            }
        }
    }

    fun incrementCounter(counterId: Int) = runMutation {
        repository.incrementCounter(counterId)
    }

    fun decrementCounter(counterId: Int) = runMutation {
        repository.decrementCounter(counterId)
    }

    fun resetCounter(counterId: Int) = runMutation {
        repository.resetCounter(counterId)
    }

    fun setCounterValue(counterId: Int, value: Int) = runMutation {
        repository.setCounterValue(counterId, value)
    }

    fun renameCounter(counterId: Int, newName: String) = runMutation {
        repository.renameCounter(counterId, newName)
    }

    fun selectCounter(counterId: Int?) {
        _selectedCounterId.value = counterId
    }

    fun deleteAllCounters() {
        viewModelScope.launch {
            when (val result = repository.deleteAllCounters()) {
                is CounterResult.Success -> {
                    _selectedCounterId.value = null
                    clearError()
                }
                is CounterResult.Failure -> handleFailure(result.reason)
            }
        }
    }

    fun getCounterById(id: Int): CounterEntity? = _counters.value.find { it.id == id }

    private fun runMutation(operation: suspend () -> CounterResult<Unit>) {
        viewModelScope.launch {
            when (val result = operation()) {
                is CounterResult.Success -> clearError()
                is CounterResult.Failure -> handleFailure(result.reason)
            }
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

    fun clearError() {
        _errorMessage.value = null
    }
}

class CounterViewModelFactory(private val repository: CounterRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CounterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CounterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
