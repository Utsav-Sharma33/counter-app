package com.example.counter.domain

import com.example.counter.data.CounterDao
import com.example.counter.data.CounterEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
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
    fun counterFlowsExposeCurrentRows() = runBlocking {
        dao.insertCounter(CounterEntity(id = 1, name = "Water"))

        assertEquals("Water", repository.getCounterById(1).first()?.name)
        assertEquals(listOf("Water"), repository.getAllCounters().first().map { it.name })
    }

    @Test
    fun deleteCounterEntityOverloadDeletesMatchingRow() = runBlocking {
        val entity = CounterEntity(id = 1, name = "Water")
        dao.insertCounter(entity)

        assertTrue(repository.deleteCounter(entity) is CounterResult.Success)
        assertTrue(dao.rows.value.isEmpty())
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
    fun setNegativeValueIsRejected() = runBlocking {
        val result = repository.setCounterValue(1, -1)

        assertEquals(
            CounterFailure.INVALID_COUNT,
            (result as CounterResult.Failure).reason
        )
    }

    @Test
    fun missingCounterIsReported() = runBlocking {
        val result = repository.resetCounter(404)

        assertEquals(
            CounterFailure.COUNTER_NOT_FOUND,
            (result as CounterResult.Failure).reason
        )
    }


    @Test
    fun renameTrimsNameAndAllowsKeepingOwnNormalizedName() = runBlocking {
        repository.createCounter("Water")

        val result = repository.renameCounter(1, "  WATER  ")

        assertTrue(result is CounterResult.Success)
        assertEquals("WATER", dao.getCounterByIdOnce(1)?.name)
        assertEquals("water", dao.getCounterByIdOnce(1)?.normalizedName)
    }

    @Test
    fun renameRejectsInvalidAndMissingCounter() = runBlocking {
        assertEquals(
            CounterFailure.INVALID_NAME,
            (repository.renameCounter(1, " ") as CounterResult.Failure).reason
        )
        assertEquals(
            CounterFailure.NAME_TOO_LONG,
            (repository.renameCounter(1, "x".repeat(CounterNamePolicy.MAX_LENGTH + 1))
                as CounterResult.Failure).reason
        )
        assertEquals(
            CounterFailure.COUNTER_NOT_FOUND,
            (repository.renameCounter(404, "New name") as CounterResult.Failure).reason
        )
    }

    @Test
    fun incrementAndDecrementUpdateExistingCounter() = runBlocking {
        dao.insertCounter(CounterEntity(id = 1, name = "Steps", count = 2))

        assertTrue(repository.incrementCounter(1) is CounterResult.Success)
        assertEquals(3, dao.getCounterByIdOnce(1)?.count)
        assertTrue(repository.decrementCounter(1) is CounterResult.Success)
        assertEquals(2, dao.getCounterByIdOnce(1)?.count)
    }

    @Test
    fun incrementAndDecrementReportMissingCounter() = runBlocking {
        assertEquals(
            CounterFailure.COUNTER_NOT_FOUND,
            (repository.incrementCounter(404) as CounterResult.Failure).reason
        )
        assertEquals(
            CounterFailure.COUNTER_NOT_FOUND,
            (repository.decrementCounter(404) as CounterResult.Failure).reason
        )
    }

    @Test
    fun setCounterValueUpdatesExistingCounterAndRejectsMissingCounter() = runBlocking {
        dao.insertCounter(CounterEntity(id = 1, name = "Steps", count = 1))

        assertTrue(repository.setCounterValue(1, 15) is CounterResult.Success)
        assertEquals(15, dao.getCounterByIdOnce(1)?.count)
        assertEquals(
            CounterFailure.COUNTER_NOT_FOUND,
            (repository.setCounterValue(404, 15) as CounterResult.Failure).reason
        )
    }

    @Test
    fun resetCounterSetsExistingValueToZero() = runBlocking {
        dao.insertCounter(CounterEntity(id = 1, name = "Steps", count = 9))

        assertTrue(repository.resetCounter(1) is CounterResult.Success)
        assertEquals(0, dao.getCounterByIdOnce(1)?.count)
    }

    @Test
    fun deleteCounterReportsSuccessAndMissingRows() = runBlocking {
        dao.insertCounter(CounterEntity(id = 1, name = "Water"))

        assertTrue(repository.deleteCounter(1) is CounterResult.Success)
        assertEquals(
            CounterFailure.COUNTER_NOT_FOUND,
            (repository.deleteCounter(1) as CounterResult.Failure).reason
        )
    }

    @Test
    fun deleteAllCountersClearsRows() = runBlocking {
        dao.insertCounter(CounterEntity(id = 1, name = "Water"))
        dao.insertCounter(CounterEntity(id = 2, name = "Steps"))

        assertTrue(repository.deleteAllCounters() is CounterResult.Success)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun storageExceptionsBecomeTypedFailures() = runBlocking {
        dao.failure = IllegalStateException("database unavailable")

        assertEquals(
            CounterFailure.STORAGE_ERROR,
            (repository.createCounter("Water") as CounterResult.Failure).reason
        )
        assertEquals(
            CounterFailure.STORAGE_ERROR,
            (repository.incrementCounter(1) as CounterResult.Failure).reason
        )
        assertEquals(
            CounterFailure.STORAGE_ERROR,
            (repository.renameCounter(1, "New") as CounterResult.Failure).reason
        )
    }


    @Test
    fun incrementUnexpectedNoOpForExistingCounterReturnsStorageFailure() = runBlocking {
        dao.insertCounter(CounterEntity(id = 1, name = "Steps", count = 3))
        dao.forceIncrementNoop = true

        assertEquals(
            CounterFailure.STORAGE_ERROR,
            (repository.incrementCounter(1) as CounterResult.Failure).reason
        )
    }

    @Test
    fun decrementUnexpectedNoOpForPositiveCounterReturnsStorageFailure() = runBlocking {
        dao.insertCounter(CounterEntity(id = 1, name = "Steps", count = 3))
        dao.forceDecrementNoop = true

        assertEquals(
            CounterFailure.STORAGE_ERROR,
            (repository.decrementCounter(1) as CounterResult.Failure).reason
        )
    }

    @Test
    fun storageOperationPropagatesCancellation() = runBlocking {
        dao.failure = kotlinx.coroutines.CancellationException("cancelled")

        var propagated = false
        try {
            repository.incrementCounter(1)
        } catch (_: kotlinx.coroutines.CancellationException) {
            propagated = true
        }

        assertTrue(propagated)
    }

    @Test
    fun cancellationIsNeverConvertedToStorageFailure() = runBlocking {
        dao.failure = kotlinx.coroutines.CancellationException("cancelled")

        var propagated = false
        try {
            repository.createCounter("Water")
        } catch (_: kotlinx.coroutines.CancellationException) {
            propagated = true
        }

        assertTrue(propagated)
    }

    private class FakeCounterDao : CounterDao {
        val rows = MutableStateFlow<List<CounterEntity>>(emptyList())
        private var nextId = 1
        var failure: Exception? = null
        var forceIncrementNoop = false
        var forceDecrementNoop = false

        private fun throwIfConfigured() { failure?.let { throw it } }

        fun getCounterByIdOnce(id: Int): CounterEntity? = rows.value.firstOrNull { it.id == id }

        override suspend fun insertCounter(counter: CounterEntity): Long {
            throwIfConfigured()
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
            throwIfConfigured()
            val before = rows.value.size
            rows.value = rows.value.filterNot { it.id == id }
            return before - rows.value.size
        }

        override suspend fun getCounterById(id: Int): CounterEntity? {
            throwIfConfigured()
            return getCounterByIdOnce(id)
        }

        override fun getCounterByIdFlow(id: Int): Flow<CounterEntity?> =
            rows.map { list -> list.firstOrNull { it.id == id } }

        override suspend fun getCounterByNormalizedName(normalizedName: String): CounterEntity? {
            throwIfConfigured()
            return rows.value.firstOrNull { it.normalizedName == normalizedName }
        }

        override fun getAllCounters(): Flow<List<CounterEntity>> = rows

        override suspend fun getAllCountersOnce(): List<CounterEntity> = rows.value

        override suspend fun incrementCounter(id: Int, timestamp: Long): Int {
            throwIfConfigured()
            if (forceIncrementNoop) return 0
            val current = getCounterByIdOnce(id) ?: return 0
            if (current.count == Int.MAX_VALUE) return 0
            rows.value = rows.value.map {
                if (it.id == id) it.copy(count = it.count + 1, updatedAt = timestamp) else it
            }
            return 1
        }

        override suspend fun decrementCounter(id: Int, timestamp: Long): Int {
            throwIfConfigured()
            if (forceDecrementNoop) return 0
            val current = getCounterByIdOnce(id) ?: return 0
            if (current.count <= 0) return 0
            rows.value = rows.value.map {
                if (it.id == id) it.copy(count = it.count - 1, updatedAt = timestamp) else it
            }
            return 1
        }

        override suspend fun setCounterValue(id: Int, newCount: Int, timestamp: Long): Int {
            throwIfConfigured()
            if (newCount < 0 || getCounterByIdOnce(id) == null) return 0
            rows.value = rows.value.map {
                if (it.id == id) it.copy(count = newCount, updatedAt = timestamp) else it
            }
            return 1
        }

        override suspend fun resetCounter(id: Int, timestamp: Long): Int =
            setCounterValue(id, 0, timestamp)

        override suspend fun deleteAllCounters() {
            throwIfConfigured()
            rows.value = emptyList()
        }

        override suspend fun renameCounter(
            id: Int,
            newName: String,
            normalizedName: String,
            timestamp: Long
        ): Int {
            throwIfConfigured()
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
