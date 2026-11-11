package com.example.counter.domain

import com.example.counter.data.CounterEntity
import kotlinx.coroutines.flow.Flow

interface CounterDataSource {
    fun getAllCounters(): Flow<List<CounterEntity>>
    suspend fun getCounterByIdOnce(id: Int): CounterEntity?
    suspend fun createCounter(name: String): CounterResult<Long>
    suspend fun deleteCounter(id: Int): CounterResult<Unit>
    suspend fun incrementCounter(id: Int): CounterResult<Unit>
    suspend fun decrementCounter(id: Int): CounterResult<Unit>
    suspend fun resetCounter(id: Int): CounterResult<Unit>
    suspend fun setCounterValue(id: Int, value: Int): CounterResult<Unit>
    suspend fun renameCounter(id: Int, newName: String): CounterResult<Unit>
    suspend fun deleteAllCounters(): CounterResult<Unit>
}
