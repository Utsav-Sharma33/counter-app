package com.example.counter.ui

import com.example.counter.data.CounterEntity
import com.example.counter.domain.CounterDataSource
import com.example.counter.domain.CounterFailure
import com.example.counter.domain.CounterResult
import com.example.counter.notification.CounterNotificationController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class CounterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialLoadPublishesCountersAndStopsInitialLoading() = runTest(mainDispatcherRule.dispatcher) {
        val data = FakeCounterDataSource()
        data.rows.value = listOf(counter(1, "Water"))
        val viewModel = createViewModel(data)

        runCurrent()

        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertEquals(listOf(counter(1, "Water")), viewModel.uiState.value.counters)
    }

    @Test
    fun selectedCounterIsClearedWhenItsRowDisappears() = runTest(mainDispatcherRule.dispatcher) {
        val data = FakeCounterDataSource()
        data.rows.value = listOf(counter(1, "Water"))
        val viewModel = createViewModel(data)
        runCurrent()

        viewModel.selectCounter(1)
        data.rows.value = emptyList()
        runCurrent()

        assertNull(viewModel.uiState.value.selectedCounterId)
    }

    @Test
    fun createSuccessSelectsNewCounterAndClearsLoading() = runTest(mainDispatcherRule.dispatcher) {
        val data = FakeCounterDataSource()
        data.createResult = CounterResult.Success(7L)
        val viewModel = createViewModel(data)
        runCurrent()

        viewModel.createCounter("Steps")
        advanceUntilIdle()

        assertEquals(7, viewModel.uiState.value.selectedCounterId)
        assertFalse(viewModel.uiState.value.isCreating)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun duplicateCreateTapDoesNotStartSecondOperation() = runTest(mainDispatcherRule.dispatcher) {
        val data = FakeCounterDataSource()
        val gate = CompletableDeferred<CounterResult<Long>>()
        data.createGate = gate
        val viewModel = createViewModel(data)
        runCurrent()

        viewModel.createCounter("Steps")
        viewModel.createCounter("Steps")
        runCurrent()

        assertEquals(1, data.createCalls)
        assertTrue(viewModel.uiState.value.isCreating)

        gate.complete(CounterResult.Success(9L))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isCreating)
    }

    @Test
    fun domainFailureMapsToSafeMessage() = runTest(mainDispatcherRule.dispatcher) {
        val data = FakeCounterDataSource()
        data.incrementResult = CounterResult.Failure(CounterFailure.COUNTER_AT_MAXIMUM)
        data.rows.value = listOf(counter(1, "Steps", Int.MAX_VALUE))
        val viewModel = createViewModel(data)
        runCurrent()

        viewModel.incrementCounter(1)
        advanceUntilIdle()

        assertEquals(
            "The counter has reached its maximum value.",
            viewModel.uiState.value.errorMessage
        )
        assertTrue(viewModel.uiState.value.busyCounterIds.isEmpty())
    }

    @Test
    fun repeatedMutationForSameCounterIsIgnoredWhileBusy() = runTest(mainDispatcherRule.dispatcher) {
        val data = FakeCounterDataSource()
        val gate = CompletableDeferred<CounterResult<Unit>>()
        data.incrementGate = gate
        data.rows.value = listOf(counter(1, "Steps"))
        val viewModel = createViewModel(data)
        runCurrent()

        viewModel.incrementCounter(1)
        viewModel.incrementCounter(1)
        runCurrent()

        assertEquals(1, data.incrementCalls)
        assertTrue(1 in viewModel.uiState.value.busyCounterIds)

        gate.complete(CounterResult.Success(Unit))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.busyCounterIds.isEmpty())
    }

    private fun createViewModel(data: FakeCounterDataSource) =
        CounterViewModel(data, FakeNotificationController())

    private fun counter(id: Int, name: String, count: Int = 0) =
        CounterEntity(id = id, name = name, count = count)

    private class FakeCounterDataSource : CounterDataSource {
        val rows = MutableStateFlow<List<CounterEntity>>(emptyList())
        var createResult: CounterResult<Long> = CounterResult.Success(1L)
        var incrementResult: CounterResult<Unit> = CounterResult.Success(Unit)
        var createGate: CompletableDeferred<CounterResult<Long>>? = null
        var incrementGate: CompletableDeferred<CounterResult<Unit>>? = null
        var createCalls = 0
        var incrementCalls = 0

        override fun getAllCounters(): Flow<List<CounterEntity>> = rows
        override suspend fun getCounterByIdOnce(id: Int): CounterEntity? =
            rows.value.firstOrNull { it.id == id }

        override suspend fun createCounter(name: String): CounterResult<Long> {
            createCalls++
            return createGate?.await() ?: createResult
        }

        override suspend fun deleteCounter(id: Int): CounterResult<Unit> =
            CounterResult.Success(Unit)
        override suspend fun incrementCounter(id: Int): CounterResult<Unit> {
            incrementCalls++
            return incrementGate?.await() ?: incrementResult
        }
        override suspend fun decrementCounter(id: Int): CounterResult<Unit> =
            CounterResult.Success(Unit)
        override suspend fun resetCounter(id: Int): CounterResult<Unit> =
            CounterResult.Success(Unit)
        override suspend fun setCounterValue(id: Int, value: Int): CounterResult<Unit> =
            CounterResult.Success(Unit)
        override suspend fun renameCounter(id: Int, newName: String): CounterResult<Unit> =
            CounterResult.Success(Unit)
        override suspend fun deleteAllCounters(): CounterResult<Unit> =
            CounterResult.Success(Unit)
    }

    private class FakeNotificationController : CounterNotificationController {
        private val tracked = mutableSetOf<Int>()
        override fun trackedCounterIds(): Set<Int> = tracked.toSet()
        override fun enableTracking(counterId: Int) { tracked += counterId }
        override fun removeTrackedCounter(counterId: Int) { tracked -= counterId }
        override fun clearTrackedCounters() { tracked.clear() }
        override fun syncCounterIfTracked(counter: CounterEntity) = Unit
        override fun syncTrackedCounters(counters: List<CounterEntity>) {
            tracked.retainAll(counters.map(CounterEntity::id).toSet())
        }
    }

    class MainDispatcherRule(
        val dispatcher: kotlinx.coroutines.test.TestDispatcher = StandardTestDispatcher()
    ) : TestWatcher() {
        override fun starting(description: Description) {
            kotlinx.coroutines.Dispatchers.setMain(dispatcher)
        }
        override fun finished(description: Description) {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }
}
