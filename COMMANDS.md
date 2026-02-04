# 🚀 Counter App - Getting Started Commands

## Quick Command Reference

Use this file for copy-paste commands to build and run your app.

---

## 📍 Navigate to Project

```bash
cd c:\counter
```

---

## 🏗️ Build Commands

### Clean Project (If needed)

```bash
./gradlew clean
```

### Build Debug APK

```bash
./gradlew assembleDebug
```

### Build Release APK (With signing)

```bash
./gradlew assembleRelease
```

### Build App Bundle for Play Store

```bash
./gradlew bundleRelease
```

### Complete Build (Clean + Debug)

```bash
./gradlew clean && ./gradlew assembleDebug
```

---

## 📱 Installation Commands

### Install Debug APK on Connected Device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Uninstall App

```bash
adb uninstall com.example.counter
```

### Clear App Data

```bash
adb shell pm clear com.example.counter
```

### Reinstall (Uninstall + Install)

```bash
adb uninstall com.example.counter && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔍 Debugging Commands

### Check Connected Devices

```bash
adb devices
```

### View App Logs

```bash
adb logcat | grep Counter
```

### View All Logs (in real-time)

```bash
adb logcat
```

### Clear Logs

```bash
adb logcat -c
```

### View Database Inspector in Android Studio

```
View → Tool Windows → Database Inspector
```

---

## 🎯 From Android Studio

### Open Project

```bash
# From command line, open folder in Android Studio
# File → Open → c:\counter
```

### Run App (Easiest)

```
Click green Run button
OR
Press: Shift + F10
```

### Debug App

```
Click green Debug button
OR
Press: Shift + F9
```

### Build APK from Android Studio

```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

---

## 📦 Output Locations

### After Building Debug APK

```
Location: app/build/outputs/apk/debug/app-debug.apk
Size: ~8-10 MB
Ready to: Install on device
```

### After Building Release APK

```
Location: app/build/outputs/apk/release/app-release.apk
Size: ~7-9 MB
Ready to: Upload to Play Store or distribute
```

### After Building App Bundle

```
Location: app/build/outputs/bundle/release/app-release.aab
Size: ~6-8 MB
Ready to: Upload to Google Play Console
```

---

## 🧪 Testing Commands

### Run Unit Tests

```bash
./gradlew test
```

### Run Connected Tests

```bash
./gradlew connectedAndroidTest
```

### Run Specific Test Class

```bash
./gradlew test -Dtest.single=CounterViewModelTest
```

### View Test Results

```
Location: app/build/reports/tests/debug/index.html
Location: app/build/reports/tests/release/index.html
```

---

## 🔐 Signing Configuration

### Create Keystore (One-time setup)

```bash
keytool -genkey -v -keystore my-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias my-key-alias
```

### After Creating Keystore

Create `app/keystore.properties`:

```properties
storeFile=path/to/my-release-key.jks
storePassword=your_password
keyAlias=my-key-alias
keyPassword=your_key_password
```

---

## 📝 Quick Verification

### Check Gradle Version

```bash
./gradlew --version
```

### Check Build Configuration

```bash
./gradlew projects
```

### Dry Run Build (Check without compiling)

```bash
./gradlew build --dry-run
```

### Show All Tasks

```bash
./gradlew tasks
```

---

## 🐛 Troubleshooting Commands

### Fix Gradle Issues

```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

### Update Gradle Wrapper

```bash
./gradlew wrapper --gradle-version latest
```

### View Gradle Cache

```bash
# On Windows:
dir %USERPROFILE%\.gradle\caches
# On Mac/Linux:
ls ~/.gradle/caches
```

### Clear Gradle Cache

```bash
rm -rf ~/.gradle/caches
./gradlew build
```

---

## 🎨 Customization Commands

### Change App Name

Edit: `app/src/main/res/values/strings.xml`

```xml
<string name="app_name">Your App Name</string>
```

### Change Package Name

Edit: `app/build.gradle.kts`

```kotlin
android {
    namespace = "com.yourcompany.yourapp"
    defaultConfig {
        applicationId = "com.yourcompany.yourapp"
    }
}
```

### Change Version

Edit: `app/build.gradle.kts`

```kotlin
defaultConfig {
    versionCode = 2
    versionName = "1.1.0"
}
```

---

## 📊 Analysis Commands

### Analyze APK Size (if available)

```bash
./gradlew analyzeApkSize
```

### View Dependency Tree

```bash
./gradlew dependencies
```

### Check for Unused Dependencies

```bash
./gradlew dependencyReport
```

### View App Bundle Size

```bash
./gradlew bundleReleaseSize
```

---

## 🚀 One-Command Build & Run

### Complete Build and Run (Android Studio)

```
1. Open c:\counter in Android Studio
2. Press Shift+F10
```

### Complete Build and Run (Command Line)

```bash
cd c:\counter
./gradlew clean
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📦 Distribution Commands

### Build for Play Store

```bash
./gradlew clean bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

### Build for Direct Distribution

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### Create GitHub Release

```bash
# After building APK, upload to:
# https://github.com/your-repo/releases
```

---

## 📱 Device Management

### List Connected Devices

```bash
adb devices
```

### Get Device Android Version

```bash
adb shell android -v
```

### Get Device API Level

```bash
adb shell getprop ro.build.version.sdk
```

### Connect to Network Device

```bash
adb connect device-ip:5555
```

### Disconnect Device

```bash
adb disconnect device-ip
```

---

## 💾 Database Management

### Access Device Database

```bash
adb pull /data/data/com.example.counter/databases/counter_database
```

### Push Database

```bash
adb push counter_database /data/data/com.example.counter/databases/
```

### View Database Files

```bash
adb shell "ls -la /data/data/com.example.counter/databases/"
```

---

## 🎯 Workflow Example

### Start to Finish (Typical Developer Workflow)

```bash
# 1. Navigate to project
cd c:\counter

# 2. Clean previous builds
./gradlew clean

# 3. Build debug APK
./gradlew assembleDebug

# 4. Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 5. View logs
adb logcat | grep Counter

# 6. (If making changes)
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 7. When ready for release
./gradlew assembleRelease
# APK available at: app/build/outputs/apk/release/app-release.apk
```

---

## 🔗 Useful Links

### In Terminal

```bash
# Open Android Studio
studio .

# Open in default browser (docs)
start README.md
```

### File Locations

```bash
# View all documentation
dir *.md

# View build outputs
dir app\build\outputs\apk\debug\
dir app\build\outputs\apk\release\

# View source code
dir app\src\main\kotlin\com\example\counter\
```

---

## 📋 Pre-Flight Checklist

Before running:

```bash
# ✅ Check Android SDK
adb devices

# ✅ Check Gradle
./gradlew --version

# ✅ Check Java
java -version

# ✅ Check device storage
adb shell "df -h /data"

# ✅ Check permissions
adb shell "pm list permissions"
```

---

## 🎉 Success Commands

When everything works:

```bash
# Celebrate! 🎊
echo "Counter App is ready!"

# View build output
dir app\build\outputs\

# Share APK
copy app\build\outputs\apk\debug\app-debug.apk .
```

---

## 💡 Pro Tips

1. **Speed Up Build**: `./gradlew assembleDebug -x test`
2. **Use Gradle Daemon**: Already enabled by default
3. **Parallel Builds**: `org.gradle.parallel=true` in gradle.properties
4. **Increase Heap**: `org.gradle.jvmargs=-Xmx2048m`
5. **Install Stripped**: `adb install-multiple` for faster install

---

## 🆘 When Stuck

```bash
# 1. Clean everything
./gradlew clean
rm -rf .gradle/

# 2. Rebuild
./gradlew build

# 3. Check logs
adb logcat

# 4. Clear device data
adb shell pm clear com.example.counter

# 5. Reinstall
adb uninstall com.example.counter
./gradlew installDebug
```

---

## 🚀 Ready to Go!

```bash
# The simplest way to start:
cd c:\counter
# Open in Android Studio and press Shift+F10
```

---

**Quick Command Reference | Counter App v1.0**  
_Copy and paste any command above to build and run your app_
