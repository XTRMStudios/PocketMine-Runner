# PocketMine Runner (Full Starter Project)

This is the **full Android project skeleton** for the PocketMine Runner app we discussed.

## Important
This zip is a **full project**, but it still needs **one real PHP binary file** from PMMP:

Place this file here after extracting the project:

`app/src/main/jniLibs/arm64-v8a/php`

You only need the `php` binary from the PMMP Android archive.

## What this project does
- Android app UI with Create / Start / Show Log / Clear
- Bundled PHP path via `jniLibs`
- Downloads `PocketMine-MP.phar` automatically
- Uses app-private `php.ini`, `cacert.pem`, and `resolv.conf`
- GitHub Actions workflow with manual version input that publishes the APK to Releases

## Files you must add manually
- `app/src/main/jniLibs/arm64-v8a/php`
- A real CA bundle in `app/src/main/assets/config/cacert.pem` if you want HTTPS validation to work properly

## Project structure
- `app/src/main/java/com/xtrmstudios/pocketminerunner/MainActivity.kt`
- `app/src/main/java/com/xtrmstudios/pocketminerunner/SetupManager.kt`
- `app/src/main/res/layout/activity_main.xml`
- `.github/workflows/android.yml`

## Build
Open in Android Studio or push to GitHub and run the workflow.

## Notes
This project assumes an **arm64 Android device**.
