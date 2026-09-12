# Build Information

## Project Information
  - **Project Name:** Bongo Budget
  - **Package Name:** com.budjet.app

## Build Environment
  - **Android Studio / IntelliJ IDEA (Community Edition):** Ladybug / Iguana or later (IDEA 2024.3+)
  - **Gradle Version:** 8.12
  - **Java Version:** 11 (Source & Target compatibility), JDK 17 (Build)

## SDK and tools
  - **compileSdkVersion:** 34
  - **buildToolsVersion:** 34
  - **minSdkVersion:** 24
  - **targetSdkVersion:** 34

## Gradle
  - **AGP Version:** 8.7.3

## Dependencies
  - `androidx.appcompat:appcompat:1.7.0`
  - `com.google.android.material:material:1.12.0`
  - `androidx.activity:activity:1.9.0`
  - `androidx.constraintlayout:constraintlayout:2.1.4`
  - `androidx.lifecycle:lifecycle-livedata:2.8.3`
  - `androidx.lifecycle:lifecycle-viewmodel:2.8.3`
  - `androidx.room:room-runtime:2.6.1`
  - `com.google.code.gson:gson:2.10.1`

## Build Status
  - **Success:** Yes

---

## Build Instructions

### Common steps

1. **Clone the repository**
   - Make sure [git](https://git-scm.com) is installed.

   ```bash
   sudo pacman -S git
   ```

   - Open a terminal and clone the project repository:

   ```bash
   git clone "https://github.com/rootminusone8004/bongo-budget"
   cd bongo-budget
   ```

### Android Studio / IntelliJ IDEA

1. **Install prerequisites**
   _Notice_: The commands given apply for *Arch Linux and its derivatives*.

   - Ensure you have the following packages installed:
     - [Android Studio](https://developer.android.com/studio) or [IntelliJ IDEA](https://www.jetbrains.com/idea): In Arch Linux, community packages are available in the *extra* repository:

   ```bash
   sudo pacman -S android-studio
   # or
   sudo pacman -S intellij-idea-community-edition
   ```

   _Note_: Make sure your IDE is set properly for Android development with the Android SDK installed.

     - Java Development Kit (JDK): Here, [openjdk](https://openjdk.org) version **17** is recommended:

   ```bash
   sudo pacman -S jdk17-openjdk
   ```

2. **Open the project in Android Studio / IntelliJ IDEA**
   - Launch Android Studio or IntelliJ IDEA.
   - Select the option to open an existing project.
   - Navigate to the cloned project repository and select it.

3. **Sync project with gradle files**
   - After the project opens, sync it with Gradle files.
   - Wait for the sync process to complete.

4. **Configure build variants**
   - Open the **Build Variants** tool window (usually on the lower left toolbar).
   - Select either `debug` or `release`.

5. **Build the project**
   - To build the APK, click on **Build** in the menu bar.
   - Select **Build Bundle(s) / APK(s)** > **Build APK(s)**.

6. **Run the app**
   - To run the app on an emulator or connected device, click the green play button in the toolbar.
   - Ensure you have an emulator configured or an Android device connected via USB with *Developer Options* and *USB Debugging* enabled.

### Terminal

1. **Install prerequisites**

   _Notice_: The commands given apply for *Arch Linux and its derivatives*.

   - Java Development Kit (JDK 17):

   ```bash
   sudo pacman -S jdk17-openjdk
   ```

   - Android tools (adb):

   ```bash
   sudo pacman -S android-tools
   ```

2. **Open the project in terminal**

   ```bash
   cd bongo-budget
   ```

3. **Run unit tests**

   ```bash
   ./gradlew testDebugUnitTest
   ```

4. **Build the project**

   - To build the **Debug APK**:

   ```bash
   ./gradlew assembleDebug
   ```

   The debug APK will be generated in `app/build/outputs/apk/debug/app-debug.apk`.

   - To build the **Release APK** (with R8 code shrinking and optimization):

   ```bash
   ./gradlew assembleRelease
   ```

   The signed release APK will be generated in `app/build/outputs/apk/release/app-release.apk`.

5. **Install the app**

   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   # or release:
   adb install app/build/outputs/apk/release/app-release.apk
   ```

### Fastlane

You can also run tasks and automate builds using [Fastlane](https://fastlane.tools):

1. **Install dependencies**:
   ```bash
   bundle install
   ```

2. **Available Lanes**:
   - **Run unit tests**:
     ```bash
     bundle exec fastlane android test
     ```
   - **Build debug APK**:
     ```bash
     bundle exec fastlane android build
     ```
   - **Build release APK**:
     ```bash
     bundle exec fastlane android release
     ```
   - **Clean and assemble release**:
     ```bash
     bundle exec fastlane android beta
     ```

## Download

You can get the app APK directly from:
  1. [GitHub Releases](https://github.com/rootminusone8004/bongo-budget/releases)
