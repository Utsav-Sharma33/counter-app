package com.example.counter.domain

import android.database.sqlite.SQLiteConstraintException
import com.example.counter.data.CounterDao
import com.example.counter.data.CounterEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class CounterRepository(private val counterDao: CounterDao) {

    fun getAllCounters(): Flow<List<CounterEntity>> = counterDao.getAllCounters()

    fun getCounterById(id: Int): Flow<CounterEntity?> = counterDao.getCounterByIdFlow(id)

    suspend fun createCounter(name: String): CounterResult<Long> {
        CounterNamePolicy.validate(name)?.let { return CounterResult.Failure(it) }
        val trimmedName = name.trim()
        val normalizedName = CounterNamePolicy.normalize(trimmedName)

        return try {
            if (counterDao.getCounterByNormalizedName(normalizedName) != null) {
                return CounterResult.Failure(CounterFailure.DUPLICATE_NAME)
            }
            val id = counterDao.insertCounter(
                CounterEntity(
                    name = trimmedName,
                    normalizedName = normalizedName,
                    count = 0
                )
            )
            CounterResult.Success(id)
        } catch (exception: SQLiteConstraintException) {
            CounterResult.Failure(CounterFailure.DUPLICATE_NAME)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            CounterResult.Failure(CounterFailure.STORAGE_ERROR)
        }
    }

    suspend fun deleteCounter(counter: CounterEntity): CounterResult<Unit> =
        deleteCounter(counter.id)

    suspend fun deleteCounter(id: Int): CounterResult<Unit> = storageResult {
        if (counterDao.deleteCounterById(id) == 0) {
            CounterResult.Failure(CounterFailure.COUNTER_NOT_FOUND)
        } else {
            CounterResult.Success(Unit)
        }
    }

    suspend fun incrementCounter(id: Int): CounterResult<Unit> = storageResult {
        if (counterDao.incrementCounter(id) == 1) {
            CounterResult.Success(Unit)
        } else {
            val counter = counterDao.getCounterById(id)
                ?: return@storageResult CounterResult.Failure(CounterFailure.COUNTER_NOT_FOUND)
            if (counter.count == Int.MAX_VALUE) {
                CounterResult.Failure(CounterFailure.COUNTER_AT_MAXIMUM)
            } else {
                CounterResult.Failure(CounterFailure.STORAGE_ERROR)
            }
        }
    }

    suspend fun decrementCounter(id: Int): CounterResult<Unit> = storageResult {
        if (counterDao.decrementCounter(id) == 1) {
            CounterResult.Success(Unit)
        } else {
            val counter = counterDao.getCounterById(id)
                ?: return@storageResult CounterResult.Failure(CounterFailure.COUNTER_NOT_FOUND)
            if (counter.count == 0) {
                CounterResult.Failure(CounterFailure.COUNTER_AT_MINIMUM)
            } else {
                CounterResult.Failure(CounterFailure.STORAGE_ERROR)
            }
        }
    }

    suspend fun resetCounter(id: Int): CounterResult<Unit> = storageResult {
        if (counterDao.resetCounter(id) == 1) {
            CounterResult.Success(Unit)
        } else {
            CounterResult.Failure(CounterFailure.COUNTER_NOT_FOUND)
        }
    }

    suspend fun setCounterValue(id: Int, value: Int): CounterResult<Unit> {
        if (value < 0) return CounterResult.Failure(CounterFailure.INVALID_COUNT)
        return storageResult {
            if (counterDao.setCounterValue(id, value) == 1) {
                CounterResult.Success(Unit)
            } else {
                CounterResult.Failure(CounterFailure.COUNTER_NOT_FOUND)
            }
        }
    }

    suspend fun renameCounter(id: Int, newName: String): CounterResult<Unit> {
        CounterNamePolicy.validate(newName)?.let { return CounterResult.Failure(it) }
        val trimmedName = newName.trim()
        val normalizedName = CounterNamePolicy.normalize(trimmedName)

        return try {
            val current = counterDao.getCounterById(id)
                ?: return CounterResult.Failure(CounterFailure.COUNTER_NOT_FOUND)
            val duplicate = counterDao.getCounterByNormalizedName(normalizedName)
            if (duplicate != null && duplicate.id != id) {
                return CounterResult.Failure(CounterFailure.DUPLICATE_NAME)
            }
            val updated = counterDao.renameCounter(id, trimmedName, normalizedName)
            if (updated == 1) {
                CounterResult.Success(Unit)
            } else {
                CounterResult.Failure(CounterFailure.DUPLICATE_NAME)
            }
        } catch (exception: SQLiteConstraintException) {
            CounterResult.Failure(CounterFailure.DUPLICATE_NAME)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            CounterResult.Failure(CounterFailure.STORAGE_ERROR)
        }
    }

    suspend fun deleteAllCounters(): CounterResult<Unit> = storageResult {
        counterDao.deleteAllCounters()
        CounterResult.Success(Unit)
    }

    suspend fun getCounterByIdOnce(id: Int): CounterEntity? = counterDao.getCounterById(id)

    private suspend fun <T> storageResult(
        operation: suspend () -> CounterResult<T>
    ): CounterResult<T> = try {
        operation()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        CounterResult.Failure(CounterFailure.STORAGE_ERROR)
    }
}
