package com.example.counter.domain

import com.example.counter.data.CounterDao
import com.example.counter.data.CounterEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CounterRepositoryTest {

    private lateinit var dao: FakeCounterDao
    private lateinit var repository: CounterRepository

    @Before
    fun setUp() {
        dao = FakeCounterDao()
        repository = CounterRepository(dao)
    }

    @Test
    fun createTrimsNameAndStoresNormalizedKey() = runBlocking {
        val result = repository.createCounter("  Water  ")

        assertTrue(result is CounterResult.Success)
        val stored = dao.getCounterByIdOnce(1)
        assertEquals("Water", stored?.name)
        assertEquals("water", stored?.normalizedName)
    }

    @Test
    fun createRejectsBlankAndOverlongNames() = runBlocking {
        assertEquals(
            CounterFailure.INVALID_NAME,
            (repository.createCounter("   ") as CounterResult.Failure).reason
        )
        assertEquals(
            CounterFailure.NAME_TOO_LONG,
            (repository.createCounter("x".repeat(CounterNamePolicy.MAX_LENGTH + 1))
                as CounterResult.Failure).reason
        )
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun createRejectsCaseInsensitiveDuplicateNames() = runBlocking {
        repository.createCounter("Water")

        val duplicate = repository.createCounter(" water ")

        assertEquals(
            CounterFailure.DUPLICATE_NAME,
            (duplicate as CounterResult.Failure).reason
        )
        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun renameRejectsDuplicateAndPreservesOriginalName() = runBlocking {
        repository.createCounter("Water")
        repository.createCounter("Steps")

        val result = repository.renameCounter(2, " WATER ")

        assertEquals(
            CounterFailure.DUPLICATE_NAME,
            (result as CounterResult.Failure).reason
        )
        assertEquals("Steps", dao.getCounterByIdOnce(2)?.name)
    }

    @Test
    fun incrementAtMaximumReturnsExplicitFailure() = runBlocking {
        dao.insertCounter(CounterEntity(id = 1, name = "Max", count = Int.MAX_VALUE))

        val result = repository.incrementCounter(1)

        assertEquals(
            CounterFailure.COUNTER_AT_MAXIMUM,
            (result as CounterResult.Failure).reason
        )
        assertEquals(Int.MAX_VALUE, dao.getCounterByIdOnce(1)?.count)
    }

    @Test
    fun decrementAtZeroReturnsExplicitFailure() = runBlocking {
        dao.insertCounter(CounterEntity(id = 1, name = "Zero", count = 0))

        val result = repository.decrementCounter(1)

        assertEquals(
            CounterFailure.COUNTER_AT_MINIMUM,
            (result as CounterResult.Failure).reason
        )
        assertEquals(0, dao.getCounterByIdOnce(1)?.count)
    }

    @Test
    fun missingCounterIsReported() = runBlocking {
        val result = repository.resetCounter(404)

        assertEquals(
            CounterFailure.COUNTER_NOT_FOUND,
            (result as CounterResult.Failure).reason
        )
    }

    private class FakeCounterDao : CounterDao {
        val rows = MutableStateFlow<List<CounterEntity>>(emptyList())
        private var nextId = 1

        fun getCounterByIdOnce(id: Int): CounterEntity? = rows.value.firstOrNull { it.id == id }

        override suspend fun insertCounter(counter: CounterEntity): Long {
            val id = if (counter.id == 0) nextId++ else counter.id
            rows.value = rows.value + counter.copy(id = id)
            return id.toLong()
        }

        override suspend fun updateCounter(counter: CounterEntity) {
            rows.value = rows.value.map { if (it.id == counter.id) counter else it }
        }

        override suspend fun deleteCounter(counter: CounterEntity) {
            rows.value = rows.value.filterNot { it.id == counter.id }
        }

        override suspend fun deleteCounterById(id: Int): Int {
            val before = rows.value.size
            rows.value = rows.value.filterNot { it.id == id }
            return before - rows.value.size
        }

        override suspend fun getCounterById(id: Int): CounterEntity? = getCounterByIdOnce(id)

        override fun getCounterByIdFlow(id: Int): Flow<CounterEntity?> =
            kotlinx.coroutines.flow.map(rows) { list -> list.firstOrNull { it.id == id } }

        override suspend fun getCounterByNormalizedName(normalizedName: String): CounterEntity? =
            rows.value.firstOrNull { it.normalizedName == normalizedName }

        override fun getAllCounters(): Flow<List<CounterEntity>> = rows

        override suspend fun getAllCountersOnce(): List<CounterEntity> = rows.value

        override suspend fun incrementCounter(id: Int, timestamp: Long): Int {
            val current = getCounterByIdOnce(id) ?: return 0
            if (current.count == Int.MAX_VALUE) return 0
            rows.value = rows.value.map {
                if (it.id == id) it.copy(count = it.count + 1, updatedAt = timestamp) else it
            }
            return 1
        }

        override suspend fun decrementCounter(id: Int, timestamp: Long): Int {
            val current = getCounterByIdOnce(id) ?: return 0
            if (current.count <= 0) return 0
            rows.value = rows.value.map {
                if (it.id == id) it.copy(count = it.count - 1, updatedAt = timestamp) else it
            }
            return 1
        }

        override suspend fun setCounterValue(id: Int, newCount: Int, timestamp: Long): Int {
            if (newCount < 0 || getCounterByIdOnce(id) == null) return 0
            rows.value = rows.value.map {
                if (it.id == id) it.copy(count = newCount, updatedAt = timestamp) else it
            }
            return 1
        }

        override suspend fun resetCounter(id: Int, timestamp: Long): Int =
            setCounterValue(id, 0, timestamp)

        override suspend fun deleteAllCounters() {
            rows.value = emptyList()
        }

        override suspend fun renameCounter(
            id: Int,
            newName: String,
            normalizedName: String,
            timestamp: Long
        ): Int {
            if (rows.value.any { it.id != id && it.normalizedName == normalizedName }) return 0
            if (getCounterByIdOnce(id) == null) return 0
            rows.value = rows.value.map {
                if (it.id == id) it.copy(
                    name = newName,
                    normalizedName = normalizedName,
                    updatedAt = timestamp
                ) else it
            }
            return 1
        }
    }
}
