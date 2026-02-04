# Counter App - Android Native Application

A modern, elegant Android counter application built with Kotlin and Jetpack Compose. The app allows you to create, manage, and increment/decrement multiple counters with the ability to interact with them even when the phone is locked via notification actions.

## Features

### Core Features

- **Create Multiple Counters**: Create separate counters with custom names
- **Easy Management**: Increment, decrement, reset, or delete counters
- **Rename Counters**: Edit counter names anytime
- **Persistent Storage**: All counters are saved in Room Database
- **Clean & Modern UI**: Built with Jetpack Compose and Material Design 3
- **State Management**: Reactive state management with ViewModels and Flows

### Lock Screen Integration

- **Notification Actions**: Access increment/decrement buttons directly from notifications
- **Lock Screen Control**: Manage counters without unlocking your phone
- **Real-time Updates**: Notifications update instantly with counter changes
- **Background Operation**: Works seamlessly in background

### Edge Cases Handled

- Empty counter name validation
- Duplicate counter name prevention
- Non-negative counter values
- Database transaction safety with coroutines
- Notification permission handling (API 33+)
- Configuration changes and screen rotation
- App lifecycle management
- Error handling with user feedback

## Project Structure

```
counter/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/example/counter/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── CounterEntity.kt
│   │   │   │   │   ├── CounterDao.kt
│   │   │   │   │   └── CounterDatabase.kt
│   │   │   │   ├── domain/
│   │   │   │   │   └── CounterRepository.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── CounterViewModel.kt
│   │   │   │   │   ├── CounterScreen.kt
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       └── Type.kt
│   │   │   │   └── notification/
│   │   │   │       ├── CounterNotificationManager.kt
│   │   │   │       └── CounterActionReceiver.kt
│   │   │   ├── res/
│   │   │   │   ├── drawable/
│   │   │   │   │   ├── ic_launcher.xml
│   │   │   │   │   ├── ic_counter.xml
│   │   │   │   │   ├── ic_plus.xml
│   │   │   │   │   └── ic_minus.xml
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   └── xml/
│   │   │   │       ├── data_extraction_rules.xml
│   │   │   │       └── backup_rules.xml
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Architecture

### MVVM Architecture

- **Model**: Room Database entities and data access objects
- **View**: Jetpack Compose UI components
- **ViewModel**: `CounterViewModel` with state management

### Layers

1. **Data Layer**
   - `CounterEntity`: Database model
   - `CounterDao`: Database access operations
   - `CounterDatabase`: Database singleton

2. **Domain Layer**
   - `CounterRepository`: Business logic and data operations

3. **Presentation Layer**
   - `CounterViewModel`: UI state and event handling
   - `CounterScreen`: UI composables
   - Theme: Material Design 3 styling

4. **Notification Layer**
   - `CounterNotificationManager`: Notification creation and management
   - `CounterActionReceiver`: BroadcastReceiver for notification actions

## Technical Stack

- **Language**: Kotlin 1.9.10
- **Minimum SDK**: 28 (Android 9.0)
- **Target SDK**: 34 (Android 14)
- **Build System**: Gradle with Kotlin DSL
- **UI Framework**: Jetpack Compose with Material Design 3
- **Database**: Room Database with Kotlin Coroutines
- **State Management**: StateFlow and LiveData
- **Architecture Pattern**: MVVM with Repository pattern

## Key Dependencies

```
androidx.core:core-ktx:1.12.0
androidx.lifecycle:lifecycle-runtime-ktx:2.6.2
androidx.activity:activity-compose:1.8.0
androidx.compose.*:1.5.4 (Material3, UI, Icons)
androidx.room:room-runtime:2.6.1
androidx.room:room-ktx:2.6.1
org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3
```

## Setup Instructions

### Prerequisites

- Android Studio 2023.1 or higher
- JDK 17
- Android SDK 34
- Android Gradle Plugin 8.1.2

### Installation

1. **Clone/Extract the project**

   ```bash
   cd counter
   ```

2. **Open in Android Studio**
   - File → Open → Select the `counter` directory
   - Wait for Gradle sync to complete

3. **Build the project**

   ```bash
   ./gradlew build
   ```

4. **Run on emulator or device**
   - Connect an Android device or open an emulator
   - Click "Run" or use:
   ```bash
   ./gradlew installDebug
   ```

## Building the APK

### Debug APK

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK

```bash
./gradlew assembleRelease
```

**Note**: For release builds, you'll need to configure signing. Create a `keystore.properties` file or configure signing in `build.gradle.kts`.

## Usage Guide

### Creating a Counter

1. Tap the "+" button (FloatingActionButton) or the add icon in the top bar
2. Enter a unique counter name (max 50 characters)
3. Tap "Create" to add the counter

### Managing Counters

- **Increment**: Tap the "+" button on the counter card
- **Decrement**: Tap the "−" button (value won't go below 0)
- **Reset**: Tap the "Reset" button to set counter to 0
- **Rename**: Tap the edit icon and enter new name
- **Delete**: Tap the delete icon to remove the counter
- **View Details**: Tap the info icon to see creation and update timestamps

### Lock Screen Control

1. A notification is shown in the notification panel
2. When the phone is locked, swipe down to access notifications
3. Tap the "+" or "−" buttons in the notification to increment/decrement
4. Tap the notification body to open the app

## Permissions

The app requires the following permissions:

- `POST_NOTIFICATIONS` (Android 13+): To display lock screen notifications

These are automatically handled by the system on Android 13+.

## State Management Details

### ViewModel State

- **counters**: StateFlow of all counters
- **isLoading**: Loading state for async operations
- **errorMessage**: Error messages for user feedback
- **selectedCounterId**: Currently selected counter

### Data Persistence

- All counters are stored in Room Database
- Automatic saving on every operation
- Singleton pattern for database access
- Safe coroutine handling for all database operations

## Error Handling

The app handles various error scenarios:

1. **Empty Counter Name**: Shows error dialog preventing creation
2. **Duplicate Names**: Prevents creating counters with same name
3. **Negative Values**: Ensures counter values never go below 0
4. **Database Errors**: Graceful error handling with user-friendly messages
5. **Notification Errors**: Try-catch blocks to handle notification exceptions
6. **Permission Issues**: Handles missing notification permissions on API 33+

## Testing

### Manual Testing Checklist

- [ ] Create counter with valid name
- [ ] Attempt to create with duplicate name (should fail)
- [ ] Attempt to create with empty name (should fail)
- [ ] Increment counter to various values
- [ ] Decrement counter (verify it doesn't go below 0)
- [ ] Reset counter
- [ ] Rename counter
- [ ] Delete counter
- [ ] Rotate screen (verify state persistence)
- [ ] Kill and restart app (verify data persistence)
- [ ] Lock screen and use notification actions
- [ ] Create multiple counters and manage them

### Edge Cases Tested

- Empty database initialization
- Concurrent counter operations
- Screen rotation with pending operations
- App backgrounding and foregrounding
- Notification action on locked device
- Database corruption recovery
- Max string length (50 characters) for counter name

## Performance Considerations

- **Coroutines**: All database operations run on IO dispatcher
- **StateFlow**: Efficient reactive updates only when state changes
- **LazyColumn**: Efficient list rendering with Compose
- **Room Queries**: Optimized database queries
- **Notification Updates**: Minimal overhead for notification updates

## Future Enhancement Ideas

- Counter statistics and history
- Export/Import counters
- Dark mode support (Material Design 3 handles this)
- Custom counter colors
- Quick tile for faster access
- Widget support for home screen
- Cloud synchronization
- Counter categories/groups
- Sound effects for increment/decrement
- Haptic feedback

## Troubleshooting

### Build Issues

1. **Gradle sync fails**: Delete `.gradle` folder and retry
2. **Java version error**: Ensure JDK 17 is installed and selected
3. **Kotlin version mismatch**: Update Android Studio to latest version

### Runtime Issues

1. **Notifications not showing**: Check notification permission in Settings
2. **Database errors**: Reinstall app (data will be lost)
3. **State lost on rotation**: ViewModel automatically handles this

### Installation Issues

1. **APK not installing**: Ensure device is unlocked and developer mode enabled
2. **Build fails on release**: Configure proper keystore for signing

## Code Quality

- **Null Safety**: Full use of Kotlin's null safety features
- **Coroutines**: Proper scope management with SupervisorJob
- **Resource Management**: Proper cleanup in lifecycle methods
- **Material Design**: Full Material Design 3 compliance
- **Accessibility**: Proper content descriptions for icons
- **Responsive UI**: Adapts to different screen sizes

## License

This project is open source and available under the MIT License.

## Support

For issues, questions, or suggestions:

1. Check the troubleshooting section
2. Review the code comments for implementation details
3. Ensure all dependencies are correctly installed

## Version History

### v1.0.0 (Initial Release)

- Core counter functionality
- Lock screen notification control
- Room Database persistence
- Material Design 3 UI
- Full MVVM architecture
- Comprehensive error handling

---

**Built with ❤️ using Kotlin and Jetpack Compose**
