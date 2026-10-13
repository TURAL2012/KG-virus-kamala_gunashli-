# Morse Horror Android app (safe version)

This Android Studio project uses the supplied horror image and `music_kg.mp3`.

## Behavior
- Opens a full-screen activity and loops the supplied music while it is open.
- A short tap enters a dot (`.`); a press lasting at least 3 seconds enters a dash (`-`).
- The unlock sequence is K then G, concatenated as `-.---.` (`-.-` + `--.`).
- Five seconds without a new symbol resets the code.
- On success, the music stops and the activity closes.
- It does not request overlay/accessibility/device-admin permissions, does not block other apps, and does not prevent the user leaving with Android navigation.

## Build
Open this folder in Android Studio and allow Gradle sync. Build > Build APK(s). The project uses Android Gradle Plugin 8.6.1 and compile SDK 35; Android Studio may prompt you to install the SDK components.
