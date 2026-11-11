package com.example.counter.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "counters",
    indices = [Index(value = ["normalizedName"], unique = true)]
)
data class CounterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val count: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val normalizedName: String = CounterNameNormalizer.normalize(name)
)
