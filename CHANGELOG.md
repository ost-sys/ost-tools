Latest Phone Version: v4.0.0-beta4  
Latest Wear OS Version: v3.0.0-beta4  

---

## Changelog — Android (v4.0.0-beta4)

### New Features & Improvements
- Added initial Welcome Screen and Setup flow.
- Introduced Developer Mode Manager and a built-in Logcat Dialog component.
- Implemented new Mesh Gradient Background component for modern visuals.
- Added Accessibility Service configuration for expanded system interaction.
- Redesigned cross-device file sharing UI (OST Share) with mDNS/NSD auto-discovery and real-time transfer progress tracking.
- Refreshed launcher icons, brand graphics, and application color palettes.
- Added new CustomTooltip component for rich contextual tooltips.
- Updated Stargazers Profile UI with unified FileProvider integration.
- Expanded device battery, display, and general device information retrieval logic.
- Refactored settings synchronization and persistence mechanisms.

### Bug Fixes
- Fixed device discovery issues where macOS devices were not consistently detected.
- Fixed NullPointerException crash during file sharing when unparceling DiscoveredDevice data.
- Fixed SocketTimeoutException during transfer acceptance by increasing read timeouts.
- Standardized FileProvider authority across all share intents.

---

## Changelog — Wear OS (v3.0.0-beta4)

### New Features & Improvements
- Added Welcome and Setup activities for new users.
- Introduced new diagnostic tools: Burn-In Recovery and Pixel Test.
- Added dedicated Battery and Display information screens.
- Completely refactored Wear OS file sharing (OST Share):
  - Built-in Wear OS file receiver and sender screens (SendUI, ReceiveUI, ReceivedFilesUI).
  - Improved layout in SendUI by removing unnecessary pagination.
  - Added gap for the top curve progress indicator to prevent TimeText from overlapping the progress bar.
- Refined Material 3 dynamic card list layouts:
  - Corrected card positioning in Received Files to dynamically switch between Top, Bottom, Middle, and Single shapes around date headers.
- Introduced WearOrbBackground for visual enhancements.
- Updated App Manager screen with improved detail views and uninstallation handling.
- Enhanced File Explorer and Reader applications, including major updates to PDF reader, image/video viewers, text editor, and music player screens.
- Replaced generic toasts with native Wear OS FailDialog and SuccessDialog feedback.

### Bug Fixes
- Fixed file sharing crashes on Wear OS when launching the transfer service.
- Fixed FileProvider authority handling in FileExplorerActivity.
- Improved list scrolling performance and component alignment across the app.