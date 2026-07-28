**Latest Phone Version:** v4.0.0-beta5
**Latest Wear OS Version:** v3.0.0-beta5

---

## Changelog — Android (v4.0.0-beta5)

### New Features & Improvements
- Redesigned Battery Info Screen: Introduced a Material 3 Hero Unit with CircularWavyProgressIndicator, Sparkline Metric Graphs for temperature/voltage history, and Health & Cycle Count M3 cards.
- Advanced Device Info: Expanded Device Info screen to display Security Patch Level, Kernel Version, Bootloader Version, Baseband/Radio Version, Partition Style, Project Treble Support, and System Uptime.
- Detailed Network Metrics: Expanded Network Info screen to show DNS Servers, Gateway IP, Network Interface Name, and Active VPN Status via ConnectivityManager.
- Regional Settings & Temperature Units: Linked regional temperature unit selection (Celsius/Fahrenheit) across CPU Info and Battery Info screens with dynamic conversion.
- MiniGames & Minesweeper Modernization: Wired MiniGames launch button directly into MainActivity's TopAppBar and completely redesigned the Minesweeper game board to be fully responsive across smartphones, tablets, foldables, and landscape modes.
- Experimental Features & Beta Badging: Integrated custom BetaBadge components with cross-platform navigation support, tagging ROM, RAM, and Minigames in mobile bottom sheets, top app bars, and tablet/desktop side navigation drawers.
- Architecture Refactoring: Extracted SharedPreferences logic out of ViewModels into dedicated repositories (PhoneTimingSettingsRepository, GithubTokenRepository).
- Permissions Expansion: Massively updated AndroidManifest.xml to support new system integrations, including permissions for Bluetooth, Media (Audio/Images/Video), Package Installation & Deletion, Biometrics, Notifications, and Activity Recognition.
- Codebase Cleanup & Localization: Consolidated hardcoded text into strings.xml resource files and simplified the Android CI/CD workflow.

### Bug Fixes
- Merged community PR fixing a navbar background glitch in MainActivity.

---

## Changelog — Wear OS (v3.0.0-beta5)

### New Features & Improvements
- Wear OS Player Overhaul: Complete redesign of the media player adhering strictly to official Google Horologist guidelines with a Material 3 Expressive UI.
- Adaptive Color Palette: Added dynamic UI color extraction from track cover art using Palette/Bitmap color extraction, smoothly adapting background and playback controls.
- File Explorer Image Thumbnails: The File Explorer now generates and caches image thumbnails, displaying previews instead of generic icons via ThumbnailCache.
- File Explorer Hidden Files: Added an Explorer Settings dialog with a toggle to hide or show hidden files, persisting via shared preferences.
- Direct APK Installation: Users can now tap on .apk files directly in the Wear OS File Explorer to trigger the system package installer.
- Text Editor Upgrades: Improved the Text Editor UI with a BasicTextField border and a functional save action coupled with a loading spinner while saving.
- PDF Reader URI Support: PdfReaderActivity can now open PDFs directly from content URIs in addition to absolute file paths.
- Regional Settings & Temperature Units: Linked regional temperature unit selection (Celsius/Fahrenheit) across CPU Info and Battery Info screens.
- Architecture Refactoring: Migrated SharedPreferences logic to WearTimingSettingsRepository.
- Permissions Expansion: Updated AndroidManifest.xml to support package installation, Bluetooth, media reading, and foreground services.
- Codebase Cleanup & Localization: Consolidated hardcoded text into strings.xml resource files.

**This is a pre-release version! Bugs and instability are possible.**
