# PulseHUD — Android source project

PulseHUD is a native Kotlin/Jetpack Compose starter project for a floating gaming monitor.
Target device: Infinix running Android 16. Minimum SDK: Android 10 (API 29).

## Included in this starter
- Futuristic dark dashboard UI
- Floating system overlay service
- Touch-through live mode and separate draggable edit mode
- Best-effort AccessibilityService click event counting
- Current CPS, peak CPS and total clicks
- Configurable rolling CPS interval (250–5000 ms)
- Battery percentage and native battery temperature
- CPS/FPS/battery visibility toggles
- Basic layout, text alignment, color, size and opacity controls
- Built-in preset bases and named custom preset saving
- Local total-click persistence and reset control
- Font file picker foundation (font selection/rendering is not yet wired into the overlay)

## Important limitations in version 0.1
1. Accessibility click events are not equivalent to raw touchscreen events. Android only reports events exposed by the foreground app. Many games use custom surfaces and will not emit `TYPE_VIEW_CLICKED`; CPS may remain zero in those games.
2. The FPS field currently displays an em dash. The project deliberately does not pretend that its own overlay frame rate is the game's FPS. App/game-wide FPS requires privileged support that varies by device and Android build.
3. The custom preset workshop currently saves preset names and their selected base. Full preset serialization/import/export is a next implementation step.
4. The font picker currently records a permitted font URI. Dynamic per-widget font rendering is a next implementation step.
5. X/Y are saved after dragging the overlay in edit mode. Numeric X/Y entry and independent per-widget coordinates are planned follow-ups.
6. Session history toggle is a preference foundation; detailed session database/history screens are not implemented yet.
7. Android 13+ may require notification permission. Android also requires the user to manually enable overlay permission and the Accessibility service.

## Build on a computer
1. Install Android Studio (current stable) and Android SDK Platform 36.
2. Open this `PulseHUD` folder as an existing project.
3. Let Gradle sync and download dependencies.
4. Connect an Android device with USB debugging enabled, or use Android Studio's APK build menu.
5. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. The debug APK is normally at `app/build/outputs/apk/debug/app-debug.apk`.

## Build on your Android phone
A practical route is AndroidIDE (if compatible with your Android 16 device):
1. Install AndroidIDE from its official distribution/source and install its requested JDK/SDK/build tools.
2. Extract the PulseHUD ZIP into a writable project folder.
3. Open the extracted folder in AndroidIDE as a Gradle project.
4. Allow Gradle sync. If AndroidIDE cannot install API 36 build tools or the required Gradle plugin, use a PC/remote build environment instead.
5. Run the `app` configuration or use the Gradle task `:app:assembleDebug`.
6. Find the generated APK under `app/build/outputs/apk/debug/`.

## First launch
1. Open PulseHUD.
2. Tap **START OVERLAY** and grant Display over other apps.
3. Tap **ENABLE CLICK ACCESSIBILITY**, find PulseHUD in Android Accessibility settings, and enable it only if you agree to the described event collection.
4. Return to PulseHUD and start the overlay.
5. Tap **EDIT POSITION** to drag the panel. Tap **TOUCH-THROUGH** when finished.
6. Use Android's notification shade / app screen to stop the overlay.

## Privacy
The starter counts events delivered to its AccessibilityService and stores a total-click count in app-private SharedPreferences. It does not upload data. Accessibility access is powerful; enable it only after reviewing the service description and source.
