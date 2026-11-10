# Fossify Gallery Cipher - Fork Changelog
All notable changes and security enhancements specific to the **Fossify Gallery Cipher** fork will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

For upstream Fossify Gallery release history, see [CHANGELOG.md](CHANGELOG.md).  
For full architectural details, cryptography specifications, and user guides, see [DOCS.md](DOCS.md).

---

## [1.18.3-cipher] - 2026-11-10
### Fixed
- **Localization Register, Terminology & Completeness Refinements:**
  - **Spanish (`es`):** Aligned the Cipher/Vault prompt block (`place_finger_prompt`, `access_existing_cipher_secrets_prompt`, `delete_and_recreate_cipher_secrets_prompt`, `confirm_decrypt_hidden_file`) with formal `usted` register and unified `sensor de huellas`.
  - **Spanish (Latin America, `b+es+419`):** Updated `place_finger_prompt` to `tú` register (`Por favor, apoya tu dedo sobre el sensor de huellas`) for consistency with the rest of `values-b+es+419`.
  - **Japanese (`ja`):** Completed the missing second paragraph of `included_activity_placeholder` and fixed the `Andorid11` typo to `Android 11` in `faq_17_text`.
  - **Traditional Chinese (Taiwan, `zh-rTW`):** Fixed `MediaStorage` → `MediaStore` in `faq_17_text` and standardized mixed `app` occurrences to `應用程式`.
  - **Norwegian (`nn`, `nb-rNO`):** Improved plural grammar in `password_protect_excluded` (`Passordvern for synlegheita til ekskluderte mapper` / `Passordbeskytt synligheten til ekskluderte mapper`).
  - **Azerbaijani (`az`):** Clarified clipboard copy phrasing in `copy_to_clipboard_disabled_in_vault` (`köçürmə` → `kopyalama`).

### Documentation
- **Canonicalized Translation Blind-Test Reports (`translation-blind-tests/`):**
  - Updated 8 comparison reports (`az`, `de`, `es`, `b+es+419`, `ja`, `nb-rNO`, `nn`, `zh-rTW`) to reflect the revised translations and normalized English source-column formatting (`\n\n`, `…`, `-&gt;`, `&gt;`) to achieve 100% column parity across all 9 audited reports.

## [1.18.2-cipher] - 2026-11-10
### Changed
- **Refined In-App Language Selector (`ChangeLanguageDialog`, `SettingsActivity`):**
  - Removed the hardcoded top pinning of `Türkçe` and `English`; all 82 supported languages are now displayed strictly by their native display names and sorted alphabetically using a locale-aware `Collator` (`PRIMARY` strength).
  - Replaced `Locale.getDefault()` guessing with a 5-tier `getDeviceSystemLocale` resolver (`LocaleManager.systemLocales` on Android 13+, `android.os.SystemProperties`, `Settings.System` `system_locales`, persisted `lastSystemLocaleTag` in `Config`, and `Resources.getSystem()` fallback) to reliably show the real OS language even when an app-specific locale is active.
  - Standardized the top entry and Settings summary format to `System default - <Language>` via `getSystemLanguageLabel`.

### Fixed
- **System Default Locale Reset (`ChangeLanguageDialog`):**
  - Fixed `config.useEnglish` remaining `true` when switching back from English to `System default` (`code.isEmpty()`).
- **Language Dialog String Coverage (34 Locales):**
  - Added localized `language`, `search`, `system_default`, and `open_system_language_settings` strings across 34 additional `values-*/strings.xml` files (`bn`, `bn-rBD`, `bqi`, `br`, `ckb`, `cr`, `cy`, `en-rGB`, `en-rIN`, `fil`, `gu`, `hi`, `ia`, `is`, `kab`, `kn`, `kr`, `lt`, `ltg`, `lv`, `mk`, `ml`, `ms`, `my`, `ne`, `or`, `pa`, `pa-rPK`, `sat`, `si`, `sr`, `ta`, `te`, `zgh`).
- **Full Localization Parity & Error Fixes (9 Audited Locales):**
  - Brought `values-az`, `values-b+es+419`, `values-es-rUS`, `values-es`, `values-de`, `values-ja`, `values-nb-rNO`, `values-nn`, and `values-zh-rTW` to full parity with the canonical English source (`values/strings.xml`: 307 `<string>` + 2 `<plurals>` = 309 resource entries).
  - Corrected existing mistranslations, typos, missing clauses, formatting bugs, and regional terminology inconsistencies across `es` (29 revised entries), `zh-rTW` (14 revised entries), `nb-rNO` (3 fixes + 2 whitespace normalizations), `az` (3 fixes), `nn` (3 fixes), and `b+es+419` (1 revision).

### Documentation & Audit Terminology
- **Translation Blind-Test & Parity Reports (`translation-blind-tests/`):**
  - Added [`translation-blind-tests/README.md`](translation-blind-tests/README.md) and 9 detailed audit reports covering Azerbaijani (`az`), German (`de`), Spanish (`es`, `b+es+419`, `es-rUS`), Japanese (`ja`), Norwegian (`nb-rNO`, `nn`), and Traditional Chinese (`zh-rTW`).
  - **Audit Terminology Explained:**
    - **Completeness Audit:** Automated and manual key-by-key verification of each target `values-<lang>/strings.xml` against `app/src/main/res/values/strings.xml` (plus `fossify-commons` inherited keys such as `search`) to guarantee zero missing entries.
    - **Blind Translation & Consistency Review ("Blind Test"):** Missing or suspect strings are translated independently directly from the English source—without copying flawed legacy phrasing—and cross-checked against a standardized **Key Terminology Glossary** per language (e.g., consistent terms for `Settings`, `Default`, `Folder`, `Album`, `Secret Vault`, `Hidden`, `Excluded`, `Encrypt / Decrypt`, `Clipboard`), exact format placeholders (`%s`, `%d`), escape sequences (`&gt;`, `\'`, `\"`, `\n`), and language-specific CLDR plural categories (`one`, `many`, `other`).
    - **1-to-1 Parity Comparison:** Every report concludes with a complete row-by-row table pairing every canonical English resource entry with its localized counterpart for transparent verification.

### Notes — Translation Requests & Feedback
- **New Languages & Translation Corrections:**
  - If you notice any inaccurate, unnatural, or missing translations—or would like to request full audit/support for a new language—please submit a report, complaint, or Pull Request on the project's [GitHub Issues](https://github.com/akbudakanl/Fossify-Gallery-Cipher/issues) page.

## [1.18.1-cipher] - 2026-11-10
### Fixed
- **In-App Language Selection & Crash Resolution (`SettingsActivity`):**
  - Resolved application crash when tapping "General > Language" on devices with customized OEM ROMs (MIUI/HyperOS, ColorOS, One UI, etc.) caused by unhandled failures in `android.settings.APP_LOCALE_SETTINGS`.
  - Replaced the external system intent launch with a native in-app `ChangeLanguageDialog` with real-time search filtering.
  - Pinned System Default, Türkçe, and English at the top, followed by 75+ supported languages with both native and English names.
  - Integrated `AppCompatDelegate.setApplicationLocales` for seamless runtime language switching across all Android versions.
  - Registered `AppLocalesMetadataHolderService` in `AndroidManifest.xml` for automatic locale persistence on Android 12 and below.
- **Localization Refinements:**
  - Corrected `virtual_folder_name` Turkish string ("Virtual Klasör adı" -> "Sanal klasör adı").
  - Standardized progress percentage formatting in `CryptoProgressDialog` to `(50%)` format.

## [1.18.0-cipher] - 2026-11-10
### Security
- **Complete Removal of Network Permissions & Code-Level Network Lockdown:**
  - Permanently removed `android.permission.INTERNET` (enforced via `tools:node="remove"` in `AndroidManifest.xml` so no third-party AAR/dependency can re-introduce it during manifest merging) and `android.permission.WAKE_LOCK`.
  - Added strict `network_security_config.xml` (`cleartextTrafficPermitted="false"`, empty `<trust-anchors />`) and `android:usesCleartextTraffic="false"` to reject both cleartext HTTP and TLS connections.
  - Added defense-in-depth code-level network blocking in `App.kt` (`URL.setURLStreamHandlerFactory` blocking `http`, `https`, `ftp`, `ws`, `wss`, `gopher`; `ProxySelector.setDefault` rejecting network URIs with `SecurityException`; Picasso no-op `Downloader`) and replaced Glide's `GlideUrl` network loader in `SvgModule.kt` with `DisabledGlideNetworkLoader`.

### Removed
- **Removal of URL Sharing (`VaultShareActivity`, `VaultDownloader`) and In-App Vault Browser (`VaultWebViewActivity`):**
  - Removed `VaultShareActivity`, `VaultWebViewActivity`, `VaultDownloader`, `PickVaultAlbumDialog`, `menu_vault_browser`, and associated layouts, drawables, and string resources.
  - **Why these features were removed from the gallery:**
    - In Android, `android.permission.INTERNET` is granted per **Linux UID (entire APK)** rather than per Activity. It is not possible to grant network access exclusively to `VaultWebViewActivity` while cryptographically or kernel-isolating the rest of the app within the same APK.
    - Because a gallery application bundles numerous third-party media decoders and native `.so` libraries (`avif`, `jxl`, `awebp`, `apng`, `media3`, `android-gif-drawable`, `bouncycastle`, etc.), keeping `INTERNET` in the gallery introduces a supply-chain and vulnerability risk: if any dependency is compromised, code running in the same UID could bypass Java-level restrictions (e.g., via native syscalls) and exfiltrate gallery media over the network.
    - Removing `INTERNET` at the manifest/kernel boundary guarantees **zero network exfiltration capability** for the gallery out of the box—without requiring Android system network restrictions or external firewall apps.

### Notes & Safer Alternatives
- **Recommended Workflow — Offline Desktop CLI (`tools/fossify-cipher-cli/cipher_vault.py`):**
  - For maximum security, use the offline Linux/Tails CLI tool (`tools/fossify-cipher-cli/cipher_vault.py`). Running it on **Tails OS (in RAM)** encrypts photos and videos into `.bin` ciphertext offline before they ever reach your phone, keeping both your media and the Android gallery app completely air-gapped.
- **Upcoming Standalone Mobile Browser Project:**
  - To enable safe, zero-disk encrypted downloads directly on Android without a PC, **an independent, external WebView browser/downloader project** (a separate minimal APK with its own Linux UID and zero gallery read/decryption access) is currently in development.

## [1.17.2-cipher] - 2026-11-09
### Fixed
- **GIF Decoder Deduplication (`PhotoFragment`):**
  - Added `mHasGifSource` guard to prevent redundant concurrent GIF decoder initialization during activity lifecycle transitions and layout measurements.
  - Enhanced memory safety by resetting decode state upon caught exceptions or out-of-memory errors.
- **Video Autoplay Hardware Decoder & I/O Deconfliction (`VideoFragment`):**
  - Eliminated hardware decoder and storage I/O contention between Glide's video-frame extraction and ExoPlayer during autoplay by suppressing the initial thumbnail load when playback begins immediately.
  - Pre-emptively cancels in-flight Glide thumbnail decoding before player initialization.
  - Reordered listener initialization prior to `prepare()` to avoid missing initial player state events.

## [1.17.1-cipher] - 2026-11-09
### Added
- **Configurable Secret Vault Exit Behavior (`ON_VAULT_EXIT`):**
  - Prompt selection dialog upon pressing back to exit Secret Vault mode with "Keep showing hidden albums" and "Hide hidden albums" options, plus an unchecked "Remember my choice" checkbox.
  - Contextual guidance note directing users to **Settings > On leaving hidden section** for advanced preference options.
  - Comprehensive preference in Settings with 5 choices:
    1. Keep showing all hidden albums
    2. Only show encrypted albums
    3. Only show other hidden albums
    4. Hide all hidden albums
    5. Always ask (default)
  - Settings backup, export, and import support for the vault exit preference.
- **Strict Vault Teardown & In-Memory Zeroization on Non-Cipher Modes:**
  - Instantly zeroes cryptographic keys in RAM (`CryptoManager.lockVault()`) and purges decrypted in-memory Glide caches (`clearAppImageCaches`) whenever leaving Secret Vault to non-encrypted or hidden modes (e.g., hiding all albums or showing only non-encrypted `.nomedia`/`.thumbnails` folders).
  - Deactivates background privacy defenses (suppresses `FLAG_SECURE` screen protection and hides Vault Browser from menus) when encrypted albums are not visible.

## [1.17.0-cipher] - 2026-11-09
### Added
- **Ephemeral Zero-Disk In-App Vault Browser (`VaultWebViewActivity`):**
  - Integrated an in-app privacy browser directly inside Secret Vault mode (Mode 2) allowing users to safely import downloadable media from web straight into encrypted storage without ever writing plaintext files to device storage or external browser caches.
  - Hardened native Chromium sandbox: zero-disk ephemeral caching (`LOAD_NO_CACHE`), pop-up windows blocked via `setSupportMultipleWindows(false)` and `javaScriptCanOpenWindowsAutomatically = false`, mixed content blocked (`MIXED_CONTENT_NEVER_ALLOW`), local file and content access blocked (`allowFileAccess = false`, `allowContentAccess = false`), and automatic denial of all sensor, camera, microphone, and geolocation requests (`onPermissionRequest -> deny()`).
  - In-memory direct vault streaming: web download listener (`setDownloadListener`) and image long-press actions stream media directly with session cookies through `CryptoManager.encryptStreamToDirectory` in 64 KB RAM buffers straight into `.bin` ciphertext in vault physical albums.
  - Complete memory zeroization on exit: purges cookies (`CookieManager.removeAllCookies`), local storage (`WebStorage.deleteAllData`), caches (`clearCache(true)`), and navigation history upon closing the activity.
- **Global Vault Screen Protection (`FLAG_SECURE`):**
  - Enforced `FLAG_SECURE` and Android 13+ recents screenshot suppression across the entire application (including the Vault Browser) from the moment the vault credentials are entered in Mode 2 until the vault is locked or the app is closed.
- **Global Clipboard Photo Copy Suppression in Vault Mode:**
  - Disabled photo copying to clipboard across the application whenever the vault is unlocked (`isVaultUnlocked`), preventing plaintext image data or URIs from leaking into third-party keyboard history or system clipboard databases.
- **Three Zero-Disk Media Ingestion Options (Detailed in `DOCS.md`):**
  - Clarified and documented the 3 zero-disk pathways for importing media into the vault without plaintext flash remanence:
    1. **Option 1 (Mobile Direct URL Share):** Share URL strings directly from privacy browsers (Brave, Firefox, Chrome) to `VaultShareActivity`. Transmitted via RAM-only Binder IPC; direct streaming download into `.bin` ciphertext in 64 KB RAM buffers.
    2. **Option 2 (Mobile Ephemeral Vault Browser):** In-app native Chromium browser in Mode 2 for downloading downloadable media from web with zero-disk caching and exit zeroization.
    3. **Option 3 (Desktop Linux CLI - Tails Recommended):** Offline media encryption tool (`tools/fossify-cipher-cli/cipher_vault.py`) running in system RAM (Tails OS recommended), outputting compatible `.bin` files ready to transfer directly to `Documents/Cipher/Cipher_Secrets/`.

### Removed
- **Permanent Removal of File-Based Sharing in `VaultShareActivity`:**
  - Removed `image/*`, `video/*`, `Intent.EXTRA_STREAM`, and content URI (`content://`, `file://`) sharing targets from `VaultShareActivity` and `VaultDownloader`.
  - Addressed flash wear-leveling data remanence: Chromium-based browsers (e.g. Brave, Chrome) write unencrypted temporary images to disk cache via `ChromeFileProvider` when sharing files. By rejecting file streams and accepting strictly `text/plain` URL strings, the app eliminates plaintext leaks from third-party browser caches.

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
