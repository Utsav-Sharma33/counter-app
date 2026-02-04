package com.example.counter.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.counter.data.CounterDatabase
import com.example.counter.domain.CounterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CounterActionReceiver : BroadcastReceiver() {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    override fun onReceive(context: Context, intent: Intent) {
        val counterId = intent.getIntExtra("counter_id", -1)
        
        if (counterId == -1) {
            return
        }
        
        val database = CounterDatabase.getDatabase(context)
        val repository = CounterRepository(database.counterDao())
        
        when (intent.action) {
            "ACTION_INCREMENT" -> {
                scope.launch {
                    try {
                        repository.incrementCounter(counterId)
                        val counter = repository.getCounterByIdOnce(counterId)
                        if (counter != null) {
                            CounterNotificationManager.showCounterNotification(context, counter)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            "ACTION_DECREMENT" -> {
                scope.launch {
                    try {
                        repository.decrementCounter(counterId)
                        val counter = repository.getCounterByIdOnce(counterId)
                        if (counter != null) {
                            CounterNotificationManager.showCounterNotification(context, counter)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
