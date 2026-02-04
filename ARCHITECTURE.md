# Counter App - Architecture & Code Documentation

## Architecture Overview

This document explains the architecture and key components of the Counter App.

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                   UI Layer (Compose)                    │
│  ┌──────────────────────────────────────────────────┐   │
│  │  MainActivity                                    │   │
│  │  ├── CounterApp (Main Composable)               │   │
│  │  ├── CounterListScreen                          │   │
│  │  ├── CounterCard (Reusable Component)           │   │
│  │  └── Dialogs (Create, Rename, Delete)           │   │
│  └──────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│              ViewModel Layer (State Management)         │
│  ┌──────────────────────────────────────────────────┐   │
│  │  CounterViewModel                                │   │
│  │  ├── counters: StateFlow<List<CounterEntity>>   │   │
│  │  ├── isLoading: StateFlow<Boolean>              │   │
│  │  ├── errorMessage: StateFlow<String?>           │   │
│  │  └── selectedCounterId: StateFlow<Int?>         │   │
│  └──────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│            Repository Layer (Business Logic)           │
│  ┌──────────────────────────────────────────────────┐   │
│  │  CounterRepository                               │   │
│  │  ├── getAllCounters()                            │   │
│  │  ├── createCounter(name)                         │   │
│  │  ├── incrementCounter(id)                        │   │
│  │  ├── deleteCounter(counter)                      │   │
│  │  └── ... (other operations)                      │   │
│  └──────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│               Data Layer (Persistence)                  │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Room Database                                   │   │
│  │  ├── CounterDao (Database Access)               │   │
│  │  ├── CounterDatabase (Singleton)                │   │
│  │  └── CounterEntity (Data Model)                 │   │
│  └──────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│            Notification Layer (System Integration)      │
│  ┌──────────────────────────────────────────────────┐   │
│  │  CounterNotificationManager                      │   │
│  │  └── CounterActionReceiver (Broadcast Handler)   │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

## Component Details

### 1. Data Layer

#### CounterEntity.kt

```kotlin
@Entity(tableName = "counters")
data class CounterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val count: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

**Purpose**: Represents a counter in the database

**Properties**:

- `id`: Unique identifier (auto-generated)
- `name`: Counter name (displayed in UI)
- `count`: Current counter value
- `createdAt`: Timestamp when created
- `updatedAt`: Timestamp when last modified

#### CounterDao.kt

**Purpose**: Database Access Object - defines database operations

**Key Methods**:

- `insertCounter()`: Create new counter
- `updateCounter()`: Update existing counter
- `deleteCounter()`: Remove counter
- `getAllCounters()`: Get all counters as Flow
- `incrementCounter()`: Atomic increment operation
- `decrementCounter()`: Atomic decrement operation
- `resetCounter()`: Set counter to 0
- `renameCounter()`: Update counter name

**Query Characteristics**:

- Uses Flow for reactive updates
- Atomic operations ensure data consistency
- Automatic timestamp updates

#### CounterDatabase.kt

**Purpose**: Room database singleton

**Features**:

- Singleton pattern prevents multiple database instances
- Thread-safe initialization with `synchronized`
- Fallback to destructive migration for versioning

### 2. Domain Layer

#### CounterRepository.kt

**Purpose**: Business logic layer between ViewModel and Database

**Key Features**:

- Validation before database operations
- Non-negative value checking
- Duplicate name prevention
- Error handling wrapper

**Methods**:

```kotlin
suspend fun createCounter(name: String): Long
suspend fun incrementCounter(id: Int)
suspend fun decrementCounter(id: Int)
suspend fun resetCounter(id: Int)
suspend fun setCounterValue(id: Int, value: Int)
suspend fun renameCounter(id: Int, newName: String)
suspend fun deleteCounter(counter: CounterEntity)
```

### 3. Presentation Layer

#### CounterViewModel.kt

**Purpose**: State management and business logic for UI

**State Flow Variables**:

```kotlin
val counters: StateFlow<List<CounterEntity>>        // All counters
val isLoading: StateFlow<Boolean>                   // Loading indicator
val errorMessage: StateFlow<String?>                // Error messages
val selectedCounterId: StateFlow<Int?>              // Selected counter
```

**Key Methods**:

- `createCounter(name)`: Create with validation
- `deleteCounter(counter)`: Delete with confirmation
- `incrementCounter(id)`: Increment value
- `decrementCounter(id)`: Decrement with min check
- `resetCounter(id)`: Reset to 0
- `setCounterValue(id, value)`: Set specific value
- `renameCounter(id, newName)`: Rename with validation
- `selectCounter(id)`: Update selection
- `deleteAllCounters()`: Clear all

**Validation Logic**:

- Empty name check
- Duplicate name prevention (case-insensitive)
- Non-negative value validation
- Error message management with auto-clear

#### CounterScreen.kt

**Purpose**: UI implementation using Jetpack Compose

**Key Composables**:

1. **CounterApp()**
   - Root composable
   - Manages overall layout with Scaffold
   - Shows empty state or list based on data

2. **CounterListScreen()**
   - Displays list of counters
   - Uses LazyColumn for efficient rendering
   - Handles item interactions

3. **CounterCard()**
   - Individual counter display
   - Shows name, count value
   - Provides action buttons (increment, decrement, reset, delete, rename)
   - Details section with timestamps

4. **Dialogs**
   - CreateCounterDialog: New counter input
   - RenameCounterDialog: Name update
   - DeleteConfirmDialog: Confirmation before deletion

5. **Helper Composables**
   - EmptyStateScreen: Shown when no counters exist
   - DetailsSection: Shows creation/update timestamps
   - ErrorSnackbar: Displays error messages

### 4. Notification Layer

#### CounterNotificationManager.kt

**Purpose**: Manage notification creation and lifecycle

**Key Features**:

- Android 13+ notification channel support
- Action buttons for increment/decrement
- Lock screen compatibility (Android 12+)
- BigText style for extended information
- PendingIntent management with FLAG_IMMUTABLE

**Methods**:

```kotlin
fun createNotificationChannel(context: Context)
fun showCounterNotification(context: Context, counter: CounterEntity)
fun dismissCounterNotification(context: Context, counterId: Int)
fun dismissAllNotifications(context: Context)
```

**Notification Features**:

- Small icon with counter name
- Large display number
- Two action buttons (+/−)
- Content intent opens app
- Lock screen notification support

#### CounterActionReceiver.kt

**Purpose**: Handle notification action broadcasts

**Actions Handled**:

- `ACTION_INCREMENT`: Increment counter from notification
- `ACTION_DECREMENT`: Decrement counter from notification

**Implementation**:

- BroadcastReceiver for intent handling
- CoroutineScope with SupervisorJob for async operations
- Updates database and refreshes notification
- Error handling for failed operations

### 5. Theme & Styling

#### Theme.kt

**Purpose**: Material Design 3 theming

**Color Schemes**:

**Light Theme**:

- Primary: Indigo (#6366F1)
- Secondary: Purple (#8B5CF6)
- Tertiary: Cyan (#06B6D4)
- Error: Red (#EF4444)

**Dark Theme**:

- Primary: Indigo (#6366F1) with adjusted luminance
- Secondary: Purple (#8B5CF6)
- Tertiary: Cyan (#06B6D4)
- Background: Dark Slate (#0F172A)

**Dynamic Coloring**:

- Uses Android 12+ Material You colors when available
- Fallback to predefined colors on older versions

## Data Flow

### Creating a Counter

```
User Input (Dialog)
    ↓
CounterViewModel.createCounter(name)
    ↓
Validation (empty, duplicate checks)
    ↓
CounterRepository.createCounter(name)
    ↓
CounterDao.insertCounter(entity)
    ↓
Room Database (Atomic Insert)
    ↓
Flow Update
    ↓
UI Update (StateFlow triggers recomposition)
```

### Incrementing Counter

```
User Tap (Button or Notification)
    ↓
CounterViewModel.incrementCounter(id)
    ↓
CounterRepository.incrementCounter(id)
    ↓
CounterDao.incrementCounter(id) [Atomic]
    ↓
Room Database (SQL UPDATE count = count + 1)
    ↓
Flow Update
    ↓
UI Update + Notification Update
```

### Lock Screen Interaction

```
Notification Action Button Press
    ↓
CounterActionReceiver.onReceive()
    ↓
Repository Operation (increment/decrement)
    ↓
CounterNotificationManager.showCounterNotification()
    ↓
Notification Update (immediate refresh)
    ↓
Background Operation (non-blocking)
```

## Coroutine Management

### ViewModel Scopes

```kotlin
viewModelScope.launch {
    // Automatically cancelled when ViewModel is cleared
    // Uses Dispatchers.Main by default
    try {
        repository.operation()
        // Update state
    } catch (e: Exception) {
        handleError(e.message)
    }
}
```

### BroadcastReceiver Scopes

```kotlin
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
// SupervisorJob: One failed child doesn't cancel others
// Main.immediate: Use main dispatcher immediately
```

## Error Handling Strategy

### Validation Errors

- Checked before database operation
- User-friendly error messages
- Prevent invalid data entry

### Database Errors

- Try-catch blocks in repository
- Logged for debugging
- User notification on failure

### Notification Errors

- SecurityException handling for permissions
- Graceful degradation if notification fails
- Logged but doesn't crash app

## Performance Optimizations

### Database

- Atomic operations (increment/decrement in SQL)
- Flow-based reactive updates
- Indexed primary keys for fast queries

### UI

- LazyColumn for list rendering
- Recomposition scope minimization
- Efficient state management with StateFlow

### Notifications

- Minimal notification updates
- PendingIntent reuse
- FLAG_IMMUTABLE for security

## Thread Safety

### Singleton Pattern

```kotlin
synchronized(this) {
    INSTANCE ?: createDatabase()
}
```

### Room Coroutines

- DAO methods are suspend functions
- Automatically run on IO dispatcher
- Safe concurrent access

### StateFlow

- Thread-safe emission
- No synchronization needed in ViewModel

## Extension Points

### Custom Validators

Add in `CounterRepository`:

```kotlin
private fun validateCounterName(name: String): Boolean {
    // Add custom validation
}
```

### Custom Notifications

Modify `CounterNotificationManager`:

```kotlin
fun showCustomNotification(context: Context, counter: CounterEntity) {
    // Custom notification logic
}
```

### Database Migrations

In `CounterDatabase`:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Migration logic
    }
}
```

## Testing Recommendations

### Unit Tests

- Repository validation logic
- ViewModel state management
- Error handling paths

### Integration Tests

- Database operations
- BroadcastReceiver functionality
- Notification updates

### UI Tests

- Counter creation flow
- Button interactions
- Dialog functionality
- Notification actions

## Memory Management

### ViewModel Cleanup

- Automatic cancellation of coroutines
- Flow cleanup on ViewModel cleared
- No memory leaks from retained references

### Database Cleanup

- Singleton pattern prevents multiple instances
- Room handles connection pooling
- Automatic resource cleanup

### Notification Cleanup

- Dismissing notifications in onDestroy
- Proper PendingIntent flags
- No retained context references

---

**Document Version**: 1.0  
**Last Updated**: February 2026  
**Architecture Pattern**: MVVM with Repository Pattern  
**Framework**: Jetpack Compose with Material Design 3
