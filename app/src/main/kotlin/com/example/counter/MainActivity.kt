package com.example.counter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.example.counter.data.CounterDatabase
import com.example.counter.domain.CounterRepository
import com.example.counter.notification.CounterActionReceiver
import com.example.counter.notification.CounterNotificationManager
import com.example.counter.ui.CounterApp
import com.example.counter.ui.CounterViewModel
import com.example.counter.ui.CounterViewModelFactory
import com.example.counter.ui.theme.CounterAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: CounterViewModel
    private var pendingTrackingCounterId: Int? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val id = pendingTrackingCounterId
            pendingTrackingCounterId = null
            if (granted && id != null) viewModel.setTrackingEnabled(id, true)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = CounterRepository(CounterDatabase.getDatabase(this).counterDao())
        viewModel = ViewModelProvider(
            this,
            CounterViewModelFactory(repository, applicationContext)
        )[CounterViewModel::class.java]
        handleCounterIntent(intent)
        CounterNotificationManager.createNotificationChannel(this)
        enableEdgeToEdge()
        setContent {
            CounterAppTheme {
                CounterApp(viewModel, ::requestTrackingPermission)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCounterIntent(intent)
    }

    private fun handleCounterIntent(intent: android.content.Intent?) {
        val counterId = intent?.getIntExtra(CounterActionReceiver.EXTRA_COUNTER_ID, -1) ?: -1
        if (counterId > 0) viewModel.selectCounter(counterId)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try {
                val counters = withContext(Dispatchers.IO) {
                    CounterDatabase.getDatabase(applicationContext).counterDao().getAllCountersOnce()
                }
                CounterNotificationManager.syncTrackedCounters(applicationContext, counters)
            } catch (exception: Exception) {
                Log.e("MainActivity", "Unable to restore tracked counter notifications", exception)
            }
        }
    }

    private fun requestTrackingPermission(counterId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            pendingTrackingCounterId = counterId
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setTrackingEnabled(counterId, true)
        }
    }
}
