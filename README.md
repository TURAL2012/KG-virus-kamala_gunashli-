# KG App — Updated Android Studio source

Includes the supplied horror image and `music_kg.mp3`.

## Behavior
- App label: **KG App**.
- On first launch, asks Android to open this app's own **Display over other apps** settings page using its package URI. Android/OEM settings may still route to a broader settings screen; the user must grant permission manually.
- After returning from settings, the image appears full-screen **inside KG App**.
- Morse instructions stay hidden until three two-finger taps land on both eyes.
- After reveal, a short tap enters a dot and a left-to-right swipe anywhere on the image enters a dash.
- The code is K followed by G (`-.- --.`), represented as `-.---.`. Five seconds without input resets the code.
- Music runs in a foreground service and continues when the activity is closed. A persistent notification provides a **Stop music** action. Correct Morse input stops the music and closes KG App.
- The app intentionally does not force a persistent image overlay over other apps or the home screen, and it does not prevent normal Android navigation.

## Build
Open this folder in Android Studio and allow Gradle sync. Select **Build > Build APK(s)**. The project uses Android Gradle Plugin 8.6.1 and compile SDK 35; install the requested SDK components if prompted.
