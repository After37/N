# Парадокс — Android puzzle prototype

Native Kotlin Android prototype: move on a 7×7 grid, use the delayed echo to activate a switch, and reach the exit.

## Build
Run `gradle assembleDebug` in an Android SDK environment, or push to `main` and download the debug APK from GitHub Actions artifacts.

## Prototype features
- Grid movement and walls
- Switch, gate, exit
- Delayed echo position
- Reset button
- Automated debug APK build

Next: test level solvability, add a level progression system and hints, then integrate ad test units only after gameplay is stable.
