# Counter App - Build and Deployment Guide

## Quick Start

### Building for Android Device

#### Option 1: Using Android Studio GUI

1. Open the project in Android Studio
2. Click on "Build" → "Build Bundle(s) / APK(s)" → "Build APK(s)"
3. Wait for the build to complete
4. The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

#### Option 2: Using Gradle Commands

**Debug APK** (for testing)

```bash
cd c:\counter
./gradlew assembleDebug
```

**Release APK** (for distribution)

```bash
./gradlew assembleRelease
```

### Installing on Device

#### Prerequisites

- Android device running Android 9.0 (API 28) or higher
- Developer Mode enabled on device
- USB debugging enabled
- USB cable

#### Installation Steps

1. **Connect Device**
   - Connect Android device via USB
   - Ensure USB debugging is enabled:
     - Settings → Developer Options → USB Debugging

2. **Install Using Android Studio**
   - Click "Run" button or press Shift + F10
   - Select your device
   - App will install and launch automatically

3. **Install Using Command Line**

   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Manual Installation**
   - Transfer the APK file to your device
   - Use a file manager to open the APK
   - Follow the installation prompts

### Granting Permissions

The app requires notification permission on Android 13+:

1. Open the app
2. Go to Settings → Apps → Counter App → Permissions
3. Enable "Notifications" permission
4. (The app will also request this on first run)

## Building for Release

### Prerequisites for Release Build

1. **Create Signing Configuration**

Create `app/keystore.properties` file:

```properties
storeFile=path/to/your/keystore.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

Or configure in `app/build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("path/to/keystore.jks")
        storePassword = "password"
        keyAlias = "alias"
        keyPassword = "password"
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
    }
}
```

2. **Generate Keystore** (if you don't have one)

```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

### Build Release APK

```bash
cd c:\counter
./gradlew assembleRelease
```

Output will be at: `app/build/outputs/apk/release/app-release.apk`

### Build Release App Bundle (AAB)

For Google Play Store:

```bash
./gradlew bundleRelease
```

Output will be at: `app/build/outputs/bundle/release/app-release.aab`

## Distribution

### For Direct Installation

- Share the debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Users can install directly on their devices

### For Google Play Store

1. Create a Google Play Developer account
2. Generate a signed release AAB:
   ```bash
   ./gradlew bundleRelease
   ```
3. Upload `app/build/outputs/bundle/release/app-release.aab` to Play Console
4. Follow Google Play's submission process

### For GitHub Releases

1. Build release APK
2. Create a GitHub release
3. Upload the APK file
4. Users can download directly from releases page

## APK Properties

### Debug APK

- File: `app-debug.apk`
- Size: ~8-10 MB (estimated)
- Signing: Android debug key
- Purpose: Testing and development

### Release APK

- File: `app-release.apk`
- Size: ~7-9 MB (estimated)
- Signing: Your custom keystore
- Purpose: Distribution and production

## System Requirements

### For Development

- Windows/Mac/Linux
- Android Studio 2023.1+
- JDK 17
- Android SDK 34
- Gradle 8.1+
- 4GB+ RAM
- 2GB+ disk space

### For Running App

- Android device: Android 9.0 (API 28) or higher
- RAM: 500MB minimum
- Storage: 50MB free space
- Permissions: Notification permission (Android 13+)

## Troubleshooting

### Build Issues

**"Gradle sync failed"**

```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

**"Incompatible Kotlin version"**

- Update Android Studio to latest version
- Check Kotlin plugin compatibility

**"Build timeout"**

- Increase gradle heap size in `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m
```

### Installation Issues

**"APK not installing"**

- Ensure device is connected: `adb devices`
- Allow installation from unknown sources in device Settings
- Clear Play Store cache: Settings → Apps → Play Store → Clear Cache

**"Signature verification failed"**

- Use debug APK for testing
- For release APK, ensure proper signing configuration

**"Insufficient storage"**

- Free up space on device (minimum 50MB)
- Uninstall other apps

### Runtime Issues

**"Notifications not working"**

- Grant notification permission in Settings
- Ensure Notifications are not disabled for the app
- Check if Do Not Disturb is enabled

**"App crashes on startup"**

- Clear app cache: Settings → Apps → Counter App → Clear Cache
- Clear app data: Settings → Apps → Counter App → Clear Storage
- Reinstall the app

## Testing the APK

### Functionality Tests

1. **Create Counter**
   - Open app
   - Tap "+" button
   - Enter counter name
   - Verify counter appears in list

2. **Increment/Decrement**
   - Tap "+" to increment
   - Tap "−" to decrement
   - Verify notifications update

3. **Lock Screen Control**
   - Lock the device
   - Swipe down to notification panel
   - Tap notification action buttons
   - Verify counter updates

4. **Persistence**
   - Create several counters
   - Close app completely
   - Reopen app
   - Verify all counters still exist with same values

### Performance Tests

- Launch and close app multiple times
- Quickly increment/decrement counters
- Monitor for memory leaks or lag
- Test with 10+ counters

## Advanced Configuration

### Custom App Name and Package

To customize the app name or package ID:

1. **Update package name in build.gradle.kts:**

```kotlin
android {
    namespace = "com.yourcompany.yourapp"
    defaultConfig {
        applicationId = "com.yourcompany.yourapp"
    }
}
```

2. **Rename source directories to match new package**

3. **Update strings.xml:**

```xml
<string name="app_name">Your App Name</string>
```

### Custom Branding

1. **Replace App Icon**
   - Add 192x192 PNG to `app/src/main/res/drawable/ic_launcher.xml`
   - Update colors in `ic_launcher.xml`

2. **Change Theme Colors**
   - Edit `app/src/main/kotlin/.../ui/theme/Theme.kt`
   - Update colors in `colors.xml`

3. **Modify App Name**
   - Edit `app/src/main/res/values/strings.xml`
   - Change `app_name` string value

## Version Updates

### Updating App Version

In `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 2  // Increment for each release
    versionName = "1.1.0"  // Semantic versioning
}
```

### Building Updated APK

```bash
./gradlew clean assembleRelease
```

## Performance Optimization

The app is already optimized, but you can further improve it:

1. **Enable ProGuard/R8** (already configured)
   - Removes unused code
   - Obfuscates code (release builds)

2. **Optimize Images**
   - Use PNG format for icons
   - Consider WebP for photos (not used currently)

3. **Monitor App Size**
   ```bash
   ./gradlew analyzeApkSize  # if available
   ```

## Security Notes

### For Release Builds

1. **Never commit keystore files** to version control
2. **Keep passwords secure** (use environment variables)
3. **Sign all release APKs** with the same keystore
4. **Use proper certificate** for app identity

### Data Security

- Room Database is file-based and local
- No network communication (by default)
- User data stays on device

## Support and Debugging

### Enable Debugging

```bash
adb logcat | grep Counter
```

### View App Logs

```bash
adb shell dumpsys alarm
```

### Clean Build

```bash
./gradlew clean
./gradlew assembleDebug
```

## Additional Resources

- [Android Developer Docs](https://developer.android.com/)
- [Kotlin Docs](https://kotlinlang.org/docs/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Material Design 3](https://m3.material.io/)

---

**Happy Building! 🚀**
