# Fossify Gallery Cipher - Fork Changelog
All notable changes and security enhancements specific to the **Fossify Gallery Cipher** fork will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

For upstream Fossify Gallery release history, see [CHANGELOG.md](CHANGELOG.md).  
For full architectural details, cryptography specifications, and user guides, see [DOCS.md](DOCS.md).

---

## [Unreleased]

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
- **Cryptographic Key Derivation & Stream Encryption:**
  - Integrated `Argon2id` (32 MB RAM, 3 iterations, 2 lanes) for hardware-hardened master key protection.
  - Hybrid cipher architecture pairing `AES-256-GCM` authenticated headers with `AES-256-CTR` media streams (zero error propagation / bit-rot resilience) and `Poly1305` MAC integrity verification.
  - 2-stage secure file shredding (random byte overwrite + zeroization) before plaintext source file deletion.

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
