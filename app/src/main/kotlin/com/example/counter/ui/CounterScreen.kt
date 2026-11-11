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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.counter.R
import com.example.counter.data.CounterEntity

@Composable
fun CounterApp(
    viewModel: CounterViewModel,
    onRequestTrackingPermission: (Int) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteAllDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (state.counters.isNotEmpty()) {
                        IconButton(
                            onClick = { showDeleteAllDialog = true },
                            enabled = !state.isDeletingAll && !state.isCreating &&
                                state.busyCounterIds.isEmpty()
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete_all_counters)
                            )
                        }
                        IconButton(
                            onClick = { showCreateDialog = true },
                            enabled = !state.isDeletingAll
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = stringResource(R.string.create_counter)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.counters.isEmpty() && !state.isInitialLoading) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.create_counter)
                    )
                }
            }
        }
    ) { padding ->
        when {
            state.isInitialLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            state.counters.isEmpty() -> EmptyStateScreen(
                modifier = Modifier.padding(padding),
                onAddClick = { showCreateDialog = true }
            )
            else -> CounterListScreen(
                counters = state.counters,
                selectedCounterId = state.selectedCounterId,
                trackedCounterIds = state.trackedCounterIds,
                busyCounterIds = state.busyCounterIds,
                modifier = Modifier.padding(padding),
                onCounterClick = { viewModel.selectCounter(it.id) },
                onIncrement = viewModel::incrementCounter,
                onDecrement = viewModel::decrementCounter,
                onReset = viewModel::resetCounter,
                onRename = viewModel::renameCounter,
                onDelete = viewModel::deleteCounter,
                onTrackingToggle = { id, enabled ->
                    if (enabled) onRequestTrackingPermission(id)
                    else viewModel.setTrackingEnabled(id, false)
                }
            )
        }
    }

    if (showCreateDialog) {
        CreateCounterDialog(
            isCreating = state.isCreating,
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createCounter(name)
                showCreateDialog = false
            }
        )
    }

    if (showDeleteAllDialog) {
        ConfirmActionDialog(
            title = stringResource(R.string.delete_all_counters),
            message = stringResource(R.string.delete_all_confirmation),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onDismiss = { showDeleteAllDialog = false },
            onConfirm = {
                viewModel.deleteAllCounters()
                showDeleteAllDialog = false
            }
        )
    }
}

@Composable
fun EmptyStateScreen(modifier: Modifier = Modifier, onAddClick: () -> Unit) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.no_counters),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.create_first_counter),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAddClick) {
                Text(stringResource(R.string.create_counter))
            }
        }
    }
}

@Composable
fun CounterListScreen(
    counters: List<CounterEntity>,
    selectedCounterId: Int?,
    trackedCounterIds: Set<Int>,
    busyCounterIds: Set<Int>,
    modifier: Modifier = Modifier,
    onCounterClick: (CounterEntity) -> Unit,
    onIncrement: (Int) -> Unit,
    onDecrement: (Int) -> Unit,
    onReset: (Int) -> Unit,
    onRename: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
    onTrackingToggle: (Int, Boolean) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(counters, key = CounterEntity::id) { counter ->
            CounterCard(
                counter = counter,
                isSelected = selectedCounterId == counter.id,
                isTracked = counter.id in trackedCounterIds,
                isBusy = counter.id in busyCounterIds,
                onClick = { onCounterClick(counter) },
                onIncrement = { onIncrement(counter.id) },
                onDecrement = { onDecrement(counter.id) },
                onReset = { onReset(counter.id) },
                onRename = { onRename(counter.id, it) },
                onDelete = { onDelete(counter.id) },
                onTrackingToggle = { onTrackingToggle(counter.id, counter.id !in trackedCounterIds) }
            )
        }
    }
}

@Composable
fun CounterCard(
    counter: CounterEntity,
    isSelected: Boolean,
    isTracked: Boolean,
    isBusy: Boolean,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onTrackingToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetails by rememberSaveable(counter.id) { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable(counter.id) { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable(counter.id) { mutableStateOf(false) }
    var showResetDialog by rememberSaveable(counter.id) { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth()
            .semantics { selected = isSelected }
            .clickable(enabled = !isSelected) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = counter.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { showRenameDialog = true },
                    enabled = !isBusy
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.rename_counter)
                    )
                }
                IconButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !isBusy
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_counter)
                    )
                }
                IconButton(onClick = { showDetails = !showDetails }) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = stringResource(
                            if (showDetails) R.string.counter_details_collapse
                            else R.string.counter_details_expand
                        )
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = counter.count.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            TextButton(
                onClick = onTrackingToggle,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        if (isTracked) R.string.counter_tracking_disable
                        else R.string.counter_tracking_enable
                    )
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onDecrement,
                    enabled = !isBusy && counter.count > 0,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = stringResource(R.string.decrement)
                    )
                }
                Button(
                    onClick = { showResetDialog = true },
                    enabled = !isBusy && counter.count != 0,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.reset))
                }
                Button(
                    onClick = onIncrement,
                    enabled = !isBusy && counter.count < Int.MAX_VALUE,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.increment)
                    )
                }
            }

            if (isBusy) {
                LinearProgressIndicatorCompat()
            }

            if (showDetails) {
                Spacer(Modifier.height(12.dp))
                DetailsSection(counter)
            }
        }
    }

    if (showRenameDialog) {
        RenameCounterDialog(
            currentName = counter.name,
            onDismiss = { showRenameDialog = false },
            onRename = {
                onRename(it)
                showRenameDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        ConfirmActionDialog(
            title = stringResource(R.string.delete_counter),
            message = stringResource(R.string.delete_counter_confirmation_named, counter.name),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            }
        )
    }

    if (showResetDialog) {
        ConfirmActionDialog(
            title = stringResource(R.string.reset_counter),
            message = stringResource(R.string.reset_counter_confirmation, counter.name),
            confirmLabel = stringResource(R.string.reset),
            destructive = true,
            onDismiss = { showResetDialog = false },
            onConfirm = {
                onReset()
                showResetDialog = false
            }
        )
    }
}

@Composable
private fun LinearProgressIndicatorCompat() {
    androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
}

@Composable
fun DetailsSection(counter: CounterEntity, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(stringResource(R.string.details), style = MaterialTheme.typography.titleSmall)
        DetailRow(
            stringResource(R.string.created),
            android.text.format.DateFormat.format("MMM dd, yyyy HH:mm", counter.createdAt).toString()
        )
        DetailRow(
            stringResource(R.string.updated),
            android.text.format.DateFormat.format("MMM dd, yyyy HH:mm", counter.updatedAt).toString()
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun CreateCounterDialog(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var counterName by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_counter)) },
        text = {
            TextField(
                value = counterName,
                onValueChange = { counterName = it.take(50) },
                label = { Text(stringResource(R.string.counter_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (counterName.isNotBlank() && !isCreating) onCreate(counterName) }
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(counterName) },
                enabled = counterName.isNotBlank() && !isCreating
            ) {
                if (isCreating) CircularProgressIndicator(Modifier.size(18.dp))
                else Text(stringResource(R.string.create_counter))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text(stringResource(R.string.cancel))
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
    var newName by rememberSaveable(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_counter)) },
        text = {
            TextField(
                value = newName,
                onValueChange = { newName = it.take(50) },
                label = { Text(stringResource(R.string.new_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (newName.isNotBlank()) onRename(newName) }
                )
            )
        },
        confirmButton = {
            Button(onClick = { onRename(newName) }, enabled = newName.isNotBlank()) {
                Text(stringResource(R.string.rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (destructive) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ) else ButtonDefaults.buttonColors()
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
