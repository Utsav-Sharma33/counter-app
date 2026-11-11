package com.example.counter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.counter.data.CounterEntity

@Composable
fun CounterApp(
    viewModel: CounterViewModel,
    onRequestTrackingPermission: (Int) -> Unit
) {
    val counters by viewModel.counters.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedCounterId by viewModel.selectedCounterId.collectAsState()
    val trackedCounterIds by viewModel.trackedCounterIds.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Counter App",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    if (counters.isNotEmpty()) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add Counter",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (counters.isEmpty()) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Counter", tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading counters...")
                }
            }
            counters.isEmpty() -> {
                EmptyStateScreen(
                    modifier = Modifier.padding(paddingValues),
                    onAddClick = { showCreateDialog = true }
                )
            }
            else -> {
                CounterListScreen(
                    counters = counters,
                    selectedCounterId = selectedCounterId,
                    trackedCounterIds = trackedCounterIds,
                    modifier = Modifier.padding(paddingValues),
                    onCounterClick = { viewModel.selectCounter(it.id) },
                    onAddClick = { showCreateDialog = true },
                    onTrackingToggle = { id, isTracked ->
                        if (isTracked) viewModel.setTrackingEnabled(id, false)
                        else onRequestTrackingPermission(id)
                    },
                    viewModel = viewModel
                )
            }
        }
    }
    
    if (showCreateDialog) {
        CreateCounterDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createCounter(name)
                showCreateDialog = false
            },
            errorMessage = errorMessage,
            onErrorDismiss = { viewModel.clearError() }
        )
    }
    
    // Show error snackbar
    if (errorMessage != null && errorMessage != "") {
        ErrorSnackbar(message = errorMessage!!, onDismiss = { viewModel.clearError() })
    }
}

@Composable
fun EmptyStateScreen(modifier: Modifier = Modifier, onAddClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "No Counters Yet",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Create your first counter to get started",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onAddClick,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create Counter", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun CounterListScreen(
    counters: List<CounterEntity>,
    selectedCounterId: Int?,
    trackedCounterIds: Set<Int>,
    modifier: Modifier = Modifier,
    onCounterClick: (CounterEntity) -> Unit,
    onAddClick: () -> Unit,
    onTrackingToggle: (Int, Boolean) -> Unit,
    viewModel: CounterViewModel
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(counters, key = CounterEntity::id) { counter ->
            CounterCard(
                counter = counter,
                isSelected = selectedCounterId == counter.id,
                isTracked = counter.id in trackedCounterIds,
                onClick = { onCounterClick(counter) },
                onIncrementClick = { viewModel.incrementCounter(counter.id) },
                onDecrementClick = { viewModel.decrementCounter(counter.id) },
                onDeleteClick = { viewModel.deleteCounter(counter) },
                onResetClick = { viewModel.resetCounter(counter.id) },
                onTrackingToggle = { onTrackingToggle(counter.id, counter.id in trackedCounterIds) },
                viewModel = viewModel
            )
        }
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun CounterCard(
    counter: CounterEntity,
    isSelected: Boolean,
    isTracked: Boolean,
    onClick: () -> Unit,
    onIncrementClick: () -> Unit,
    onDecrementClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onResetClick: () -> Unit,
    onTrackingToggle: () -> Unit,
    viewModel: CounterViewModel,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isSelected) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = counter.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = { showRenameDialog = true }) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Rename",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                
                IconButton(onClick = { showDetails = !showDetails }) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = "Details",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Counter display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = counter.count.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            TextButton(onClick = onTrackingToggle, modifier = Modifier.fillMaxWidth()) {
                Text(if (isTracked) "Stop notification tracking" else "Track in notification")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDecrementClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("−", fontSize = 20.sp, color = Color.White)
                }
                
                Button(
                    onClick = onResetClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Reset", fontSize = 14.sp, color = Color.White)
                }
                
                Button(
                    onClick = onIncrementClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+", fontSize = 20.sp, color = Color.White)
                }
            }
            
            // Details section
            if (showDetails) {
                Spacer(modifier = Modifier.height(12.dp))
                DetailsSection(counter = counter)
            }
        }
    }
    
    if (showRenameDialog) {
        RenameCounterDialog(
            currentName = counter.name,
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                viewModel.renameCounter(counter.id, newName)
                showRenameDialog = false
            }
        )
    }
    
    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            counterName = counter.name,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                onDeleteClick()
                showDeleteConfirm = false
            }
        )
    }
}

@Composable
fun DetailsSection(counter: CounterEntity, modifier: Modifier = Modifier) {
    val createdDate = android.text.format.DateFormat.format(
        "MMM dd, yyyy HH:mm",
        counter.createdAt
    )
    val updatedDate = android.text.format.DateFormat.format(
        "MMM dd, yyyy HH:mm",
        counter.updatedAt
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = "Details",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        DetailRow(label = "Created:", value = createdDate.toString())
        DetailRow(label = "Updated:", value = updatedDate.toString())
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CreateCounterDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    errorMessage: String?,
    onErrorDismiss: () -> Unit
) {
    var counterName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create New Counter", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                TextField(
                    value = counterName,
                    onValueChange = { counterName = it.take(50) }, // Limit to 50 chars
                    label = { Text("Counter Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = TextFieldDefaults.colors(),
                    maxLines = 1,
                    singleLine = true
                )
                
                if (!errorMessage.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onErrorDismiss,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Dismiss error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (counterName.trim().isNotEmpty()) {
                        onCreate(counterName)
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RenameCounterDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Rename Counter", fontWeight = FontWeight.Bold)
        },
        text = {
            TextField(
                value = newName,
                onValueChange = { newName = it.take(50) },
                label = { Text("New Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = TextFieldDefaults.colors(),
                maxLines = 1,
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.trim().isNotEmpty()) {
                        onRename(newName)
                    }
                }
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    counterName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete Counter", fontWeight = FontWeight.Bold)
        },
        text = {
            Text("Are you sure you want to delete the counter \"$counterName\"? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ErrorSnackbar(message: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            color = MaterialTheme.colorScheme.error,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
