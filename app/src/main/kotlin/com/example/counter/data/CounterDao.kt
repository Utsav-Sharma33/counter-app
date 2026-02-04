package com.example.counter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterDao {
    
    @Insert
    suspend fun insertCounter(counter: CounterEntity): Long
    
    @Update
    suspend fun updateCounter(counter: CounterEntity)
    
    @Delete
    suspend fun deleteCounter(counter: CounterEntity)
    
    @Query("SELECT * FROM counters WHERE id = :id")
    suspend fun getCounterById(id: Int): CounterEntity?
    
    @Query("SELECT * FROM counters WHERE id = :id")
    fun getCounterByIdFlow(id: Int): Flow<CounterEntity?>
    
    @Query("SELECT * FROM counters ORDER BY createdAt DESC")
    fun getAllCounters(): Flow<List<CounterEntity>>
    
    @Query("SELECT * FROM counters ORDER BY createdAt DESC")
    suspend fun getAllCountersOnce(): List<CounterEntity>
    
    @Query("UPDATE counters SET count = count + 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun incrementCounter(id: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE counters SET count = count - 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun decrementCounter(id: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE counters SET count = :newCount, updatedAt = :timestamp WHERE id = :id")
    suspend fun setCounterValue(id: Int, newCount: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE counters SET count = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun resetCounter(id: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM counters")
    suspend fun deleteAllCounters()
    
    @Query("UPDATE counters SET name = :newName, updatedAt = :timestamp WHERE id = :id")
    suspend fun renameCounter(id: Int, newName: String, timestamp: Long = System.currentTimeMillis())
}
