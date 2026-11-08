# Fossify Gallery Cipher - Fork Changelog
All notable changes and security enhancements specific to the **Fossify Gallery Cipher** fork will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

For upstream Fossify Gallery release history, see [CHANGELOG.md](CHANGELOG.md).  
For full architectural details, cryptography specifications, and user guides, see [DOCS.md](DOCS.md).

---

## [Unreleased]

## [1.16.0-cipher] - 2026-11-08
### Added
- **Zero-Disk Streaming Encrypted Downloader (`VaultShareActivity`):**
  - Integrated an Android Share target (`ACTION_SEND` and `ACTION_SEND_MULTIPLE`) enabling direct, encrypted media downloads from web browsers and other apps without ever writing unencrypted files to storage.
  - Enforced mandatory vault authentication (PIN, Password, or Pattern) before showing album options or downloading.
  - Implemented an interactive hidden physical album selector filtering exclusively for encrypted physical folders (excluding virtual JSON collections) with instant "+ Create New Hidden Album" capability.
  - Streaming payload (HTTP/HTTPS URL or Content URI) is piped directly into a 64 KB RAM buffer, encrypted on the fly via `AES-256-CTR` and authenticated with `Poly1305` MAC tags and `AES-256-GCM` headers, writing directly as `.bin` ciphertext to disk with zero flash memory plaintext remanence.
  - Embedded thumbnails for downloaded images are decoded entirely in RAM from the decrypted stream and registered in the in-memory metadata cache with zero plaintext disk caching.
- **Configurable Double-Tap Seek Duration:**
  - Added "Double-tap seek duration" setting under **Settings > Videos** with choices of 3, 5, 10, and 15 seconds.
  - Changed default seek / fast-forward / rewind duration from 10 seconds to 5 seconds across both in-app and standalone video players (`VideoFragment`, `VideoPlayerActivity`, and side-scroll controllers).
  - Included the preference in settings backup, export, and import routines (`VIDEO_DOUBLE_TAP_FAST_FORWARD`).

### Fixed
- **Restored Missing Video Player Controls:**
  - Fixed an issue where the mute/unmute toggle (top-right of playback bar) and playback speed adjustment button (top-left of playback bar) disappeared during playback.
  - Ensured player controls are properly revealed whenever video is initialized, autoplays, prepares, or updates duration, while preserving full fade-out behavior on single-tap focus and auto-hide timeouts.
- **Swipe-Down-to-Dismiss Gesture Reliability:**
  - Fixed gesture dismissal failure when swiping down from the top to return to the list view while viewing unzoomed media.
  - Removed the overly strict 20-pixel horizontal movement threshold and premature move latching that caused almost all normal downward swipe motions to be blocked.
  - Scaled dismiss threshold by display density (40dp) and adjusted maximum gesture duration to 500ms for smooth, effortless dismissals.
  - Maintained strict zoom isolation: downward swipe dismisses only when media is at original 1:1 scale (in videos: 2-finger pinch; in photos: 2-finger pinch or center double-tap).

## [1.15.0-cipher] - 2026-11-08
### Added
- **Centralized Cipher Storage Architecture:**
  - Consolidated all vault and virtual album assets under `Internal Storage > Documents > Cipher`.
  - Partitioned standard virtual albums into `Virtual Album Saves/` and cryptographic vaults / stealth hidden virtual albums into `Cipher_Secrets/`.
- **Dedicated Secret Vault Experience (Modes 1, 2, and 3):**
  - Integrated dedicated Mode 2 (Secret Vault) view, unlocked via credentials or a 3x bottom overscroll gesture, isolating encrypted physical vaults and stealth virtual albums away from standard media.
  - Implemented seamless back navigation to Mode 3 (Extended View), presenting standard albums, `.nomedia` system folders, and encrypted vaults in a single unified grid.
  - Added fast re-entry back into Mode 2 via the 3x bottom overscroll gesture from Mode 3.
- **Stealth Hidden Virtual Albums:**
  - Hidden virtual albums created from encrypted physical albums are persisted in `Cipher_Secrets/JSON/` with randomized 16-character hex identifiers.
  - Payloads encrypt album titles and member file paths via `AES-256-GCM` (`FVSTR1:...`), preventing plaintext metadata leakage on disk.
- **Strict Single Physical Album Binding for Virtual Albums:**
  - Enforced 1-to-1 binding between virtual albums and their parent physical album, preventing accidental cross-folder media mixing.
  - Removed confusing "Move" action within virtual albums while preserving non-destructive "Copy" flows.
  - Copying from a virtual album strictly suggests sibling virtual albums belonging to the same root physical album, and creating a new virtual album automatically links it to that same physical owner.
- **Automatic Vault Detection and Migration Flow:**
  - Added auto-discovery of existing `Documents/Cipher` vaults when setting up credentials on new devices or after an app reset.
  - Implemented a protective safety dialog for vault resets with a mandatory 10-second countdown, explicit checkbox confirmation, and an inverted "Yes" button position to prevent accidental vault wipe.
- **Session-Level Live Video Frame Cache (`sVideoFrameCache`):**
  - Added an in-memory LRU frame cache capturing the exact paused `TextureView` frame when swiping away or pausing a video.
  - Swiping back from 3+ items away instantly renders the exact paused frame instead of the 00:00 cover photo.
  - Background pre-warming prepares ExoPlayer during incoming swipes, eliminating buffering pauses and resuming playback with zero delay.
- **Cryptographic Hardening & Key Management:**
  - Upgraded `Argon2id` configuration to 64 MB RAM cost, 3 iterations, 2 lanes with strict input parameter bounds checking.
  - Integrated Android Keystore hardware-bound key wrapping layer (StrongBox / TEE) for device-tied master key protection (`vaultWrappedKeyData`).
  - Added V5 container format binding metadata header bytes directly into `Poly1305-AES` MAC to eliminate metadata substitution vectors.
  - Implemented pre-wipe verification: encrypts to `.tmp`, verifies Poly1305 MAC and performs full byte-for-byte SHA-256 decrypted readback before touching source media.
  - Full-file CSPRNG overwrite with 1 MiB chunks, `fsync(fd)`, file truncation, directory sync (`syncDir`), and pending-wipe recovery journal (`pending_wipe_journal.txt`).
  - Added recursive subfolder encryption and space pre-flight validation in `encryptFolder`, eliminating exposure of subfolders to root internal storage.
  - Hardened database and cache hygiene: SQLite `PRAGMA secure_delete = ON`, Room `JournalMode.TRUNCATE`, database scrubbing, and full SharedPreferences path scrubbing.
  - Comprehensive screen protection: applied `FLAG_SECURE` and Android 13+ recents screenshot suppression across all vault and hidden media views, touchjacking protection in `VaultSecurityDialog`, and immediate vault locking upon screen off (`ACTION_SCREEN_OFF`).
  - Excluded vault and app data from cloud backups and device-to-device transfers via `data_extraction_rules.xml`.

### Changed
- **Video Player & ViewPager Optimizations:**
  - Increased `ViewPager.offscreenPageLimit` to 3, keeping up to 3 adjacent fragments natively alive in memory.
  - Optimized the 250ms progress update loop to run strictly while media is actively playing, eliminating UI thread contention during swipes.
  - Replaced synchronous main-thread `FileDataSource` opening with non-blocking `DefaultDataSource.Factory`.
  - Switched seek behavior to `SeekParameters.CLOSEST_SYNC`, eliminating decoder stalls from sequential inter-frame reconstruction.
  - Pre-cached video durations using `mMedium.videoDuration` to avoid redundant `MediaMetadataRetriever` file queries.
- **Visual & UI Refinements:**
  - Removed all `(gizli)` and `(hidden)` label suffixes from folder names across both standard and encrypted vault displays.
  - Hidden the center play outline icon (`video_play_outline`) over video cover images and attached `onRenderedFirstFrame` to dismiss preview thumbnails the microsecond the first frame is rendered on the surface.

### Fixed
- **Accidental Viewer Dismissal on Horizontal Swipes:**
  - Prevented ViewPager horizontal drag intercept (`ACTION_CANCEL`) from triggering the downward drag-to-dismiss gesture (`activity?.finish()`).
  - Added horizontal motion detection to disable vertical dismissal when swiping between media, and scaled the dismiss threshold by display density.
- **Crashes in Directory Picker (`PickDirectoryDialog`):**
  - Moved directory scanning, virtual album ownership resolution, and path lookups to background threads to eliminate Room `Cannot access database on the main thread` exceptions.
- **Crashes on Vault Unlock:**
  - Ensured directory adapter binding runs strictly on the UI thread (`runOnUiThread`) using defensive thread-safe list copies, eliminating `ConcurrentModificationException`.
  - Guarded subfolder parent grouping in `addParentWithoutMediaFiles` using safe `maxOfOrNull` to avoid `NoSuchElementException`.
- **Crashes When Hiding Albums:**
  - Removed background calls to Activity SAF delete handlers in `CryptoManager`, and wrapped progress dialog dismissals in safe exception handlers.
