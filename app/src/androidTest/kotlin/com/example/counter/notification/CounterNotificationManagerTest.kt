package com.example.counter.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.counter.data.CounterEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterNotificationManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun trackingPreferencesAreIndependentPerCounter() {
        CounterNotificationManager.clearTrackedCounters(context)
        CounterNotificationManager.enableTracking(context, 10)
        CounterNotificationManager.enableTracking(context, 20)
        assertEquals(setOf(10, 20), CounterNotificationManager.getTrackedCounterIds(context))
        CounterNotificationManager.removeTrackedCounter(context, 10)
        assertEquals(setOf(20), CounterNotificationManager.getTrackedCounterIds(context))
        CounterNotificationManager.clearTrackedCounters(context)
        assertTrue(CounterNotificationManager.getTrackedCounterIds(context).isEmpty())
    }

    @Test
    fun invalidIdsAreNotTracked() {
        CounterNotificationManager.clearTrackedCounters(context)
        CounterNotificationManager.enableTracking(context, 0)
        CounterNotificationManager.enableTracking(context, -1)
        assertFalse(CounterNotificationManager.getTrackedCounterIds(context).contains(0))
        assertFalse(CounterNotificationManager.getTrackedCounterIds(context).contains(-1))
    }

    @Test
    fun untrackedCounterDoesNotBecomeTrackedDuringSync() {
        CounterNotificationManager.clearTrackedCounters(context)
        CounterNotificationManager.syncCounterIfTracked(
            context, CounterEntity(id = 42, name = "Untracked")
        )
        assertTrue(CounterNotificationManager.getTrackedCounterIds(context).isEmpty())
    }
}
