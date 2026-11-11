package com.example.counter.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CounterDaoAtomicityTest {

    private lateinit var database: CounterDatabase
    private lateinit var dao: CounterDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CounterDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.counterDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun concurrentDecrementsNeverMakeCountNegative() = runBlocking {
        val id = dao.insertCounter(CounterEntity(name = "Concurrent", count = 10))
        val jobs = List(30) {
            async(Dispatchers.IO) { dao.decrementCounter(id.toInt()) }
        }

        jobs.awaitAll()

        assertEquals(0, dao.getCounterById(id.toInt())?.count)
    }

    @Test
    fun incrementAtIntMaximumDoesNotOverflow() = runBlocking {
        val id = dao.insertCounter(CounterEntity(name = "Maximum", count = Int.MAX_VALUE))

        assertEquals(0, dao.incrementCounter(id.toInt()))
        assertEquals(Int.MAX_VALUE, dao.getCounterById(id.toInt())?.count)
    }

    @Test
    fun decrementAtZeroDoesNotChangeTimestampOrCount() = runBlocking {
        val id = dao.insertCounter(
            CounterEntity(name = "Zero", count = 0, createdAt = 1L, updatedAt = 1L)
        )

        assertEquals(0, dao.decrementCounter(id.toInt(), timestamp = 99L))
        val counter = dao.getCounterById(id.toInt())
        assertEquals(0, counter?.count)
        assertEquals(1L, counter?.updatedAt)
    }
}
