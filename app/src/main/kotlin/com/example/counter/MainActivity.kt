package com.example.counter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.counter.data.CounterDatabase
import com.example.counter.domain.CounterRepository
import com.example.counter.notification.CounterNotificationManager
import com.example.counter.ui.CounterApp
import com.example.counter.ui.CounterViewModel
import com.example.counter.ui.CounterViewModelFactory
import com.example.counter.ui.theme.CounterAppTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: CounterViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize database and repository
        val database = CounterDatabase.getDatabase(this)
        val repository = CounterRepository(database.counterDao())
        
        // Create ViewModel
        val factory = CounterViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(CounterViewModel::class.java)
        
        // Create notification channel
        CounterNotificationManager.createNotificationChannel(this)
        
        enableEdgeToEdge()
        
        setContent {
            CounterAppTheme {
                CounterApp(viewModel = viewModel)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up notifications if needed
        CounterNotificationManager.dismissAllNotifications(this)
    }
}
