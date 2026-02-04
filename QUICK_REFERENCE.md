# Counter App - Quick Reference Guide

## 🚀 Quick Start (Copy & Paste Commands)

### Open in Android Studio

```bash
cd c:\counter
# Then open in Android Studio: File → Open → Select this folder
```

### Build & Run

```bash
# Build debug version
./gradlew assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or from Android Studio: Click Run button or press Shift+F10
```

### Build Release

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

## 📁 Important File Locations

| Purpose       | Location                                          |
| ------------- | ------------------------------------------------- |
| Main Activity | `app/src/main/kotlin/.../MainActivity.kt`         |
| Database      | `app/src/main/kotlin/.../data/CounterDatabase.kt` |
| UI Screens    | `app/src/main/kotlin/.../ui/CounterScreen.kt`     |
| ViewModel     | `app/src/main/kotlin/.../ui/CounterViewModel.kt`  |
| Notifications | `app/src/main/kotlin/.../notification/`           |
| App Resources | `app/src/main/res/`                               |
| Build Config  | `app/build.gradle.kts`                            |
| Manifest      | `app/src/main/AndroidManifest.xml`                |

## 🔧 Common Tasks

### Change App Name

**File**: `app/src/main/res/values/strings.xml`

```xml
<string name="app_name">Your App Name</string>
```

### Change App Icon

**File**: `app/src/main/res/drawable/ic_launcher.xml`

- Replace the vector XML with your icon

### Change Theme Colors

**File**: `app/src/main/kotlin/.../ui/theme/Theme.kt`

```kotlin
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),  // Change this
    // ... other colors
)
```

### Change Package Name

1. **File**: `app/build.gradle.kts`

```kotlin
android {
    namespace = "com.yourcompany.yourapp"
    defaultConfig {
        applicationId = "com.yourcompany.yourapp"
    }
}
```

2. Rename source folders to match new package name

### Add New Counter Feature

Add to [CounterViewModel.kt](app/src/main/kotlin/com/example/counter/ui/CounterViewModel.kt):

```kotlin
fun myNewFeature(counterId: Int) {
    viewModelScope.launch {
        try {
            repository.myNewOperation(counterId)
            clearError()
        } catch (e: Exception) {
            handleError("Failed: ${e.message}")
        }
    }
}
```

## 📱 Device Requirements

- **Minimum Android**: 9.0 (API 28)
- **Target Android**: 14 (API 34)
- **Free Storage**: 50MB minimum

## 🎨 UI Architecture

```
MainActivity
└── CounterApp (Main Composable)
    ├── TopAppBar
    ├── Scaffold
    └── Content
        ├── CounterListScreen (if counters exist)
        │   └── LazyColumn
        │       └── CounterCard (for each counter)
        │           ├── Title & Buttons
        │           ├── Count Display
        │           ├── Control Buttons
        │           └── Details (optional)
        └── EmptyStateScreen (if no counters)
```

## 🔄 Key Class Methods

### CounterViewModel

```kotlin
createCounter(name: String)           // Create new counter
deleteCounter(counter: CounterEntity) // Delete counter
incrementCounter(counterId: Int)      // Increment by 1
decrementCounter(counterId: Int)      // Decrement by 1
resetCounter(counterId: Int)          // Reset to 0
renameCounter(counterId: Int, name)   // Rename counter
selectCounter(counterId: Int?)        // Select counter
```

### CounterRepository

```kotlin
getAllCounters(): Flow<List<CounterEntity>>
createCounter(name: String): Long
incrementCounter(id: Int)
decrementCounter(id: Int)
resetCounter(id: Int)
renameCounter(id: Int, newName: String)
deleteCounter(counter: CounterEntity)
```

### CounterNotificationManager

```kotlin
createNotificationChannel(context: Context)
showCounterNotification(context: Context, counter: CounterEntity)
dismissCounterNotification(context: Context, counterId: Int)
dismissAllNotifications(context: Context)
```

## 🐛 Common Issues & Solutions

| Issue                     | Solution                                                         |
| ------------------------- | ---------------------------------------------------------------- |
| App won't build           | Run `./gradlew clean` then `./gradlew build`                     |
| Gradle sync failed        | Delete `.gradle` folder and sync again                           |
| App crashes on startup    | Clear app data: Settings → Apps → Counter → Storage → Clear Data |
| Notifications not showing | Grant notification permission in Settings                        |
| Can't install APK         | Enable "Unknown sources" in device Settings                      |

## 📊 Project Statistics

- **Total Files**: 30+
- **Lines of Code**: ~3500+
- **Kotlin Files**: 12
- **XML Files**: 11
- **APK Size**: ~8-10 MB (debug), ~7-9 MB (release)

## ✨ Features at a Glance

| Feature             | Status      |
| ------------------- | ----------- |
| Create counters     | ✅ Complete |
| Increment/Decrement | ✅ Complete |
| Reset counter       | ✅ Complete |
| Rename counter      | ✅ Complete |
| Delete counter      | ✅ Complete |
| Persist data        | ✅ Complete |
| Lock screen control | ✅ Complete |
| Dark mode           | ✅ Complete |
| Error handling      | ✅ Complete |
| Input validation    | ✅ Complete |

## 🎯 Next Steps

1. **Open Project**: `File → Open → c:\counter`
2. **Sync Gradle**: Wait for sync to complete
3. **Connect Device**: Plug in Android phone (or use emulator)
4. **Run App**: Click Run button or `Shift+F10`
5. **Test**: Create counters and test all features
6. **Customize**: Change app name, colors, icons as needed
7. **Build Release**: Use `./gradlew assembleRelease` when ready

## 💡 Tips & Tricks

### Debug Database

- Android Studio → View → Tool Windows → Database Inspector
- Right-click on Room database to inspect

### View Logs

```bash
adb logcat | grep Counter
```

### Clear App Data

```bash
adb shell pm clear com.example.counter
```

### Reinstall App

```bash
adb uninstall com.example.counter
./gradlew installDebug
```

### Check Connected Devices

```bash
adb devices
```

## 🔐 Security Notes

- All data stored locally in SQLite database
- No network communication by default
- Proper permission handling for Android 13+
- Immutable PendingIntents used for notifications

## 📚 Documentation Files

- **README.md**: Main documentation
- **BUILD_GUIDE.md**: Building & deployment
- **ARCHITECTURE.md**: Technical deep-dive
- **SUMMARY.md**: Project summary
- **QUICK_REFERENCE.md**: This file!

## 🆘 Getting Help

1. Check the **README.md** for features and usage
2. Check **BUILD_GUIDE.md** for building issues
3. Check **ARCHITECTURE.md** for technical details
4. Read code comments in source files
5. Check Android documentation at developer.android.com

## 📞 Debugging Checklist

Before reporting issues:

- [ ] Cleaned project: `./gradlew clean`
- [ ] Synced Gradle
- [ ] Connected valid device (API 28+)
- [ ] Granted necessary permissions
- [ ] Checked logcat for errors
- [ ] Tried clearing app data

## 🎊 You're All Set!

The Counter App is ready to use. Everything needed for a professional Android application is included:

- ✅ Modern UI with Material Design 3
- ✅ Efficient database with Room
- ✅ Lock screen notifications
- ✅ Complete error handling
- ✅ Professional architecture
- ✅ Comprehensive documentation

**Happy coding! 🚀**

---

_Quick Reference Guide | Counter App v1.0 | February 2026_
