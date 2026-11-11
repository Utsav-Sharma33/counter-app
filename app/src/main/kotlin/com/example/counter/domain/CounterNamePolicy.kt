package com.example.counter.domain

import com.example.counter.data.CounterNameNormalizer

object CounterNamePolicy {
    const val MAX_LENGTH = 50

    fun validate(name: String): CounterFailure? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> CounterFailure.INVALID_NAME
            trimmed.length > MAX_LENGTH -> CounterFailure.NAME_TOO_LONG
            else -> null
        }
    }

    fun normalize(name: String): String = CounterNameNormalizer.normalize(name)
}
