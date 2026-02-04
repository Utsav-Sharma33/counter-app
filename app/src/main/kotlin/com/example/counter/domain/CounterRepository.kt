package com.example.counter.domain

import com.example.counter.data.CounterDao
import com.example.counter.data.CounterEntity
import kotlinx.coroutines.flow.Flow

class CounterRepository(private val counterDao: CounterDao) {
    
    fun getAllCounters(): Flow<List<CounterEntity>> = counterDao.getAllCounters()
    
    fun getCounterById(id: Int): Flow<CounterEntity?> = counterDao.getCounterByIdFlow(id)
    
    suspend fun createCounter(name: String): Long {
        val counter = CounterEntity(
            name = name.trim(),
            count = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return counterDao.insertCounter(counter)
    }
    
    suspend fun deleteCounter(counter: CounterEntity) {
        counterDao.deleteCounter(counter)
    }
    
    suspend fun incrementCounter(id: Int) {
        counterDao.incrementCounter(id)
    }
    
    suspend fun decrementCounter(id: Int) {
        val counter = counterDao.getCounterById(id)
        if (counter != null && counter.count > 0) {
            counterDao.decrementCounter(id)
        }
    }
    
    suspend fun resetCounter(id: Int) {
        counterDao.resetCounter(id)
    }
    
    suspend fun setCounterValue(id: Int, value: Int) {
        if (value >= 0) {
            counterDao.setCounterValue(id, value)
        }
    }
    
    suspend fun renameCounter(id: Int, newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isNotEmpty()) {
            counterDao.renameCounter(id, trimmedName)
        }
    }
    
    suspend fun deleteAllCounters() {
        counterDao.deleteAllCounters()
    }
    
    suspend fun getCounterByIdOnce(id: Int): CounterEntity? = counterDao.getCounterById(id)
}
