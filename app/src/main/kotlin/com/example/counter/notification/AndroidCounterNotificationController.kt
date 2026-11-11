package com.example.counter.notification

import android.content.Context
import com.example.counter.data.CounterEntity

class AndroidCounterNotificationController(context: Context) : CounterNotificationController {
    private val appContext = context.applicationContext

    override fun trackedCounterIds(): Set<Int> =
        CounterNotificationManager.getTrackedCounterIds(appContext)

    override fun enableTracking(counterId: Int) {
        CounterNotificationManager.enableTracking(appContext, counterId)
    }

    override fun removeTrackedCounter(counterId: Int) {
        CounterNotificationManager.removeTrackedCounter(appContext, counterId)
    }

    override fun clearTrackedCounters() {
        CounterNotificationManager.clearTrackedCounters(appContext)
    }

    override fun syncCounterIfTracked(counter: CounterEntity) {
        CounterNotificationManager.syncCounterIfTracked(appContext, counter)
    }

    override fun syncTrackedCounters(counters: List<CounterEntity>) {
        CounterNotificationManager.syncTrackedCounters(appContext, counters)
    }
}
