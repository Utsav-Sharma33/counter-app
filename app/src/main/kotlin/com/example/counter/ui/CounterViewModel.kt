package com.example.counter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.counter.data.CounterEntity
import com.example.counter.domain.CounterRepository
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
            } catch (e: Exception) {
                handleError("Failed to load counters: ${e.message}")
            }
        }
    }
    
    fun createCounter(name: String) {
        if (name.trim().isEmpty()) {
            _errorMessage.value = "Counter name cannot be empty"
            return
        }
        
        // Check for duplicate names
        if (_counters.value.any { it.name.equals(name.trim(), ignoreCase = true) }) {
            _errorMessage.value = "Counter with this name already exists"
            return
        }
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val id = repository.createCounter(name)
                _selectedCounterId.value = id.toInt()
                _isLoading.value = false
                clearError()
            } catch (e: Exception) {
                handleError("Failed to create counter: ${e.message}")
            }
        }
    }
    
    fun deleteCounter(counter: CounterEntity) {
        viewModelScope.launch {
            try {
                repository.deleteCounter(counter)
                if (_selectedCounterId.value == counter.id) {
                    _selectedCounterId.value = null
                }
                clearError()
            } catch (e: Exception) {
                handleError("Failed to delete counter: ${e.message}")
            }
        }
    }
    
    fun incrementCounter(counterId: Int) {
        viewModelScope.launch {
            try {
                repository.incrementCounter(counterId)
                clearError()
            } catch (e: Exception) {
                handleError("Failed to increment counter: ${e.message}")
            }
        }
    }
    
    fun decrementCounter(counterId: Int) {
        viewModelScope.launch {
            try {
                repository.decrementCounter(counterId)
                clearError()
            } catch (e: Exception) {
                handleError("Failed to decrement counter: ${e.message}")
            }
        }
    }
    
    fun resetCounter(counterId: Int) {
        viewModelScope.launch {
            try {
                repository.resetCounter(counterId)
                clearError()
            } catch (e: Exception) {
                handleError("Failed to reset counter: ${e.message}")
            }
        }
    }
    
    fun setCounterValue(counterId: Int, value: Int) {
        if (value < 0) {
            _errorMessage.value = "Counter value cannot be negative"
            return
        }
        
        viewModelScope.launch {
            try {
                repository.setCounterValue(counterId, value)
                clearError()
            } catch (e: Exception) {
                handleError("Failed to set counter value: ${e.message}")
            }
        }
    }
    
    fun renameCounter(counterId: Int, newName: String) {
        if (newName.trim().isEmpty()) {
            _errorMessage.value = "Counter name cannot be empty"
            return
        }
        
        // Check for duplicate names (excluding current counter)
        if (_counters.value.any { 
            it.id != counterId && it.name.equals(newName.trim(), ignoreCase = true) 
        }) {
            _errorMessage.value = "Counter with this name already exists"
            return
        }
        
        viewModelScope.launch {
            try {
                repository.renameCounter(counterId, newName)
                clearError()
            } catch (e: Exception) {
                handleError("Failed to rename counter: ${e.message}")
            }
        }
    }
    
    fun selectCounter(counterId: Int?) {
        _selectedCounterId.value = counterId
    }
    
    fun deleteAllCounters() {
        viewModelScope.launch {
            try {
                repository.deleteAllCounters()
                _selectedCounterId.value = null
                clearError()
            } catch (e: Exception) {
                handleError("Failed to delete all counters: ${e.message}")
            }
        }
    }
    
    fun getCounterById(id: Int): CounterEntity? = _counters.value.find { it.id == id }
    
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
