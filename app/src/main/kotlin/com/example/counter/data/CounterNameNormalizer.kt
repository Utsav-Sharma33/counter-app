package com.example.counter.data

import java.util.Locale

object CounterNameNormalizer {
    fun normalize(name: String): String = name.trim().lowercase(Locale.ROOT)
}
