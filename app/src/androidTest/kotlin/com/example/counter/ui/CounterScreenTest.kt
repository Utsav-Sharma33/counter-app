package com.example.counter.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.counter.data.CounterEntity
import com.example.counter.domain.CounterDataSource
import com.example.counter.domain.CounterFailure
import com.example.counter.domain.CounterResult
import com.example.counter.notification.CounterNotificationController
import com.example.counter.ui.theme.CounterAppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CounterScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var dataSource: FakeCounterDataSource
    private lateinit var viewModel: CounterViewModel

    @Before
    fun setUp() {
        dataSource = FakeCounterDataSource()
        viewModel = CounterViewModel(dataSource, FakeNotificationController())
    }

    @Test
    fun incrementAndDecrementUpdateDisplayedCount() {
        showCounters(CounterEntity(id = 1, name = "Steps", count = 2))

        composeRule.onNodeWithContentDescription("Increment").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("3").assertExists()

        composeRule.onNodeWithContentDescription("Decrement").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("2").assertExists()
    }

    @Test
    fun resetRequiresConfirmation() {
        showCounters(CounterEntity(id = 1, name = "Steps", count = 8))

        composeRule.onNodeWithText("Reset").performClick()
        composeRule.onNodeWithText("Reset counter").assertExists()
        composeRule.onAllNodesWithText("Reset").onLast().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("0").assertExists()
    }

    @Test
    fun deleteRequiresConfirmationAndShowsEmptyState() {
        showCounters(CounterEntity(id = 1, name = "Steps", count = 4))

        composeRule.onNodeWithContentDescription("Delete counter").performClick()
        composeRule.onNodeWithText("Delete counter").assertExists()
        composeRule.onAllNodesWithText("Delete").onLast().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("No Counters Yet").assertExists()
    }

    @Test
    fun createAndRenameCounterUseStateDrivenCallbacks() {
        composeRule.setContent {
            CounterAppTheme {
                CounterApp(viewModel) { id -> viewModel.setTrackingEnabled(id, true) }
            }
        }

        composeRule.onNodeWithText("Create Counter").performClick()
        composeRule.onNodeWithText("Counter Name").performTextInput("Water")
        composeRule.onAllNodesWithText("Create Counter").onLast().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Water").assertExists()

        composeRule.onNodeWithContentDescription("Rename counter").performClick()
        composeRule.onNodeWithText("New Name").performTextReplacement("Distance")
        composeRule.onAllNodesWithText("Rename").onLast().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Distance").assertExists()
    }

    private fun showCounters(vararg counters: CounterEntity) {
        dataSource.rows.value = counters.toList()
        composeRule.setContent {
            CounterAppTheme {
                CounterApp(viewModel) { id -> viewModel.setTrackingEnabled(id, true) }
            }
        }
        composeRule.waitForIdle()
    }

    private class FakeCounterDataSource : CounterDataSource {
        val rows = MutableStateFlow<List<CounterEntity>>(emptyList())
        private var nextId = 10
        var incrementFailure: CounterFailure? = null

        override fun getAllCounters(): Flow<List<CounterEntity>> = rows
        override suspend fun getCounterByIdOnce(id: Int): CounterEntity? =
            rows.value.firstOrNull { it.id == id }

        override suspend fun createCounter(name: String): CounterResult<Long> {
            val id = nextId++
            rows.value = rows.value + CounterEntity(id = id, name = name.trim())
            return CounterResult.Success(id.toLong())
        }

        override suspend fun deleteCounter(id: Int): CounterResult<Unit> {
            if (getCounterByIdOnce(id) == null) {
                return CounterResult.Failure(CounterFailure.COUNTER_NOT_FOUND)
            }
            rows.value = rows.value.filterNot { it.id == id }
            return CounterResult.Success(Unit)
        }

        override suspend fun incrementCounter(id: Int): CounterResult<Unit> {
            incrementFailure?.let { return CounterResult.Failure(it) }
            return update(id) { it.copy(count = it.count + 1) }
        }

        override suspend fun decrementCounter(id: Int): CounterResult<Unit> =
            update(id) { it.copy(count = (it.count - 1).coerceAtLeast(0)) }

        override suspend fun resetCounter(id: Int): CounterResult<Unit> =
            update(id) { it.copy(count = 0) }

        override suspend fun setCounterValue(id: Int, value: Int): CounterResult<Unit> =
            update(id) { it.copy(count = value) }

        override suspend fun renameCounter(id: Int, newName: String): CounterResult<Unit> =
            update(id) { it.copy(name = newName.trim()) }

        override suspend fun deleteAllCounters(): CounterResult<Unit> {
            rows.value = emptyList()
            return CounterResult.Success(Unit)
        }

        private fun update(
            id: Int,
            transform: (CounterEntity) -> CounterEntity
        ): CounterResult<Unit> {
            val current = getCounterByIdOnce(id)
                ?: return CounterResult.Failure(CounterFailure.COUNTER_NOT_FOUND)
            rows.value = rows.value.map { if (it.id == id) transform(current) else it }
            return CounterResult.Success(Unit)
        }
    }

    private class FakeNotificationController : CounterNotificationController {
        private val trackedIds = mutableSetOf<Int>()
        override fun trackedCounterIds(): Set<Int> = trackedIds.toSet()
        override fun enableTracking(counterId: Int) { trackedIds += counterId }
        override fun removeTrackedCounter(counterId: Int) { trackedIds -= counterId }
        override fun clearTrackedCounters() { trackedIds.clear() }
        override fun syncCounterIfTracked(counter: CounterEntity) = Unit
        override fun syncTrackedCounters(counters: List<CounterEntity>) {
            trackedIds.retainAll(counters.map(CounterEntity::id).toSet())
        }
    }
}
