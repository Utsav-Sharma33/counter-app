package com.example.counter.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.counter.data.CounterDatabase
import com.example.counter.domain.CounterFailure
import com.example.counter.domain.CounterRepository
import com.example.counter.domain.CounterResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

internal enum class CounterAction {
    INCREMENT,
    DECREMENT
}

class CounterActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val counterId = intent.getIntExtra(EXTRA_COUNTER_ID, INVALID_COUNTER_ID)
        val action = parseAction(intent.action, counterId) ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        // This coroutine is bounded by the PendingResult lifecycle.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(RECEIVER_TIMEOUT_MILLIS) {
                    val repository = CounterRepository(
                        CounterDatabase.getDatabase(appContext).counterDao()
                    )
                    val result = when (action) {
                        CounterAction.INCREMENT -> repository.incrementCounter(counterId)
                        CounterAction.DECREMENT -> repository.decrementCounter(counterId)
                    }

                    if (result is CounterResult.Failure &&
                        result.reason == CounterFailure.STORAGE_ERROR
                    ) {
                        Log.e(TAG, "Unable to apply notification action for counter $counterId")
                    }

                    val counter = repository.getCounterByIdOnce(counterId)
                    if (counter == null) {
                        CounterNotificationManager.removeTrackedCounter(appContext, counterId)
                    } else {
                        CounterNotificationManager.syncCounterIfTracked(appContext, counter)
                    }
                }
            } catch (exception: TimeoutCancellationException) {
                Log.e(TAG, "Notification action timed out for counter $counterId", exception)
            } catch (exception: Exception) {
                Log.e(TAG, "Notification action failed for counter $counterId", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_INCREMENT = "com.example.counter.action.INCREMENT"
        const val ACTION_DECREMENT = "com.example.counter.action.DECREMENT"
        const val EXTRA_COUNTER_ID = "com.example.counter.extra.COUNTER_ID"

        private const val INVALID_COUNTER_ID = -1
        private const val RECEIVER_TIMEOUT_MILLIS = 8_000L
        private const val TAG = "CounterActionReceiver"

        internal fun parseAction(action: String?, counterId: Int): CounterAction? {
            if (counterId <= 0) return null
            return when (action) {
                ACTION_INCREMENT -> CounterAction.INCREMENT
                ACTION_DECREMENT -> CounterAction.DECREMENT
                else -> null
            }
        }
    }
}
