# KG App — Android Studio project

Includes the supplied horror image and `music_kg.mp3`.

## Behavior
- App label: **KG App**.
- On first launch, Android's **Display over other apps** settings page opens. The user must grant access manually or return; Android does not permit an app to grant itself this permission.
- After returning from Settings, the app opens the supplied image full-screen and plays the supplied music in a loop.
- Morse instructions remain hidden until the user performs three quick two-finger taps, one finger landing over each eye on the image.
- After reveal, a tap shorter than 700 ms enters a dot; holding for at least 3 seconds enters a dash. The unlock sequence is K then G (`-.-` + `--.`), represented as `-.---.`.
- Five seconds idle resets the entered code.
- On success, music stops and the activity closes.
- The app does **not** display a forced overlay over other apps or prevent leaving with Android navigation. Overlay permission is requested because it was part of the requested setup, but it is not used to trap or cover other apps.

## Build
Open this folder in Android Studio and allow Gradle sync. Select **Build > Build APK(s)**. The project uses Android Gradle Plugin 8.6.1 and compile SDK 35; install the requested SDK components if prompted.
