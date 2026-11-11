package com.example.counter.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import android.database.sqlite.SQLiteConstraintException
import com.example.counter.domain.CounterFailure
import com.example.counter.domain.CounterRepository
import com.example.counter.domain.CounterResult
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
    fun concurrentIncrementsPreserveEverySuccessfulUpdate() = runBlocking {
        val id = dao.insertCounter(CounterEntity(name = "Concurrent increments", count = 0))
        val jobs = List(40) {
            async(Dispatchers.IO) { dao.incrementCounter(id.toInt()) }
        }

        assertEquals(40, jobs.awaitAll().sum())
        assertEquals(40, dao.getCounterById(id.toInt())?.count)
    }

    @Test
    fun uniqueNormalizedNameIndexRejectsDuplicateRows() = runBlocking {
        dao.insertCounter(CounterEntity(name = "Water", normalizedName = "water"))

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                dao.insertCounter(CounterEntity(name = " water ", normalizedName = "water"))
            }
        }
        assertEquals(1, dao.getAllCountersOnce().size)
    }

    @Test
    fun repositoryMapsDatabaseUniquenessRaceToDuplicateName() = runBlocking {
        dao.insertCounter(CounterEntity(name = "Water", normalizedName = "water"))
        val racingDao = object : CounterDao by dao {
            override suspend fun getCounterByNormalizedName(normalizedName: String): CounterEntity? =
                null
        }
        val repository = CounterRepository(racingDao)

        val result = repository.createCounter(" water ")

        assertEquals(
            CounterFailure.DUPLICATE_NAME,
            (result as CounterResult.Failure).reason
        )
        assertEquals(1, dao.getAllCountersOnce().size)
    }

    @Test
    fun setCounterValueRejectsNegativeValuesWithoutMutation() = runBlocking {
        val id = dao.insertCounter(CounterEntity(name = "Bounded", count = 5))

        assertEquals(0, dao.setCounterValue(id.toInt(), -1, timestamp = 99L))
        val counter = dao.getCounterById(id.toInt())
        assertEquals(5, counter?.count)
        assertTrue(counter?.updatedAt != 99L)
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
