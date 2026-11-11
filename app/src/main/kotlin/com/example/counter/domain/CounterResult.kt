package com.example.counter.domain

sealed interface CounterResult<out T> {
    data class Success<T>(val value: T) : CounterResult<T>
    data class Failure(val reason: CounterFailure) : CounterResult<Nothing>
}

enum class CounterFailure {
    INVALID_NAME,
    NAME_TOO_LONG,
    DUPLICATE_NAME,
    COUNTER_NOT_FOUND,
    COUNTER_AT_MINIMUM,
    COUNTER_AT_MAXIMUM,
    INVALID_COUNT,
    STORAGE_ERROR
}
