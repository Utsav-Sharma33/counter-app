package com.example.counter.notification

import com.example.counter.data.CounterEntity

interface CounterNotificationController {
    fun trackedCounterIds(): Set<Int>
    fun enableTracking(counterId: Int)
    fun removeTrackedCounter(counterId: Int)
    fun clearTrackedCounters()
    fun syncCounterIfTracked(counter: CounterEntity)
    fun syncTrackedCounters(counters: List<CounterEntity>)
}
