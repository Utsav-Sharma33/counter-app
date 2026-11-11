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

- **Language**: Kotlin 2.2.21
- **Minimum SDK**: 28 (Android 9.0)
- **Compile SDK / Target SDK**: 36 (Android 16)
- **Build System**: Android Gradle Plugin 8.10.1 with Gradle 8.14
- **JDK**: 17
- **UI Framework**: Jetpack Compose with Material Design 3
- **Database**: Room 2.7.0 with Kotlin Coroutines
- **State Management**: StateFlow
- **Architecture Pattern**: MVVM with Repository pattern

## Dependency policy

Versions are pinned in the Gradle build files. Keep Android Gradle Plugin, Gradle, Kotlin, Compose compiler, and compile SDK upgrades coordinated. Avoid dynamic dependency versions. Run lint, unit tests, instrumentation tests, and a minified release build before publishing a toolchain upgrade.

## Setup Instructions

### Prerequisites

- Android Studio Meerkat or newer
- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- Gradle 8.14

### Installation

1. **Clone/Extract the project**

   ```bash
   cd counter
   ```

2. **Open in Android Studio**
   - File → Open → Select the `counter` directory
   - Wait for Gradle sync to complete

3. **Generate the local Gradle wrapper** (wrapper files are intentionally generated in CI and are not checked into this repository)

   ```bash
   gradle wrapper --gradle-version 8.14 --distribution-type bin
   chmod +x ./gradlew
   ```

4. **Build the project**

   ```bash
   ./gradlew build
   ```

5. **Run on emulator or device**
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

A local release build uses the signing environment variables below. If they are absent, Gradle can produce an unsigned release APK for local inspection, but it must not be distributed.

```bash
export COUNTER_RELEASE_STORE_FILE="/secure/path/release-key.jks"
export COUNTER_RELEASE_STORE_PASSWORD="..."
export COUNTER_RELEASE_KEY_ALIAS="..."
export COUNTER_RELEASE_KEY_PASSWORD="..."
./gradlew assembleRelease
```

Never put these values in source files, `gradle.properties`, shell scripts committed to Git, or build logs.

### GitHub Release process

- Push a semantic version tag such as `v1.2.3`.
- Configure these repository Actions secrets before creating a release tag:
  - `ANDROID_SIGNING_KEYSTORE_BASE64`: Base64-encoded Java keystore.
  - `ANDROID_SIGNING_STORE_PASSWORD`
  - `ANDROID_SIGNING_KEY_ALIAS`
  - `ANDROID_SIGNING_KEY_PASSWORD`
- The tag workflow validates all secrets, decodes the keystore into the runner's temporary directory, builds a minified signed release APK, verifies its signature, and publishes the APKs as a GitHub Release.
- The workflow sets the Android `versionName` from the tag and uses the GitHub Actions run number as `versionCode`. The run number increases between workflow runs.
- Branch and pull-request workflows build and upload the debug APK only. They do not publish releases.

The keystore and passwords must never be committed. If a signing key is lost or compromised, follow the relevant app-signing key rotation process before publishing another update.

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

The ViewModel exposes a single immutable `StateFlow<CounterUiState>`. The state includes the counter list, initial loading and mutation flags, selected counter, notification tracking IDs, per-counter busy state, and a user-safe error message.

## Data Persistence

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

Unit and instrumentation test cases are present in the repository, but they have not yet been executed. The checklist below is a manual test plan, not a record of completed validation.

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

### Edge Cases Covered by Test Cases (execution pending)

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
