# Fossify Gallery Cipher - Fork Changelog
All notable changes and security enhancements specific to the **Fossify Gallery Cipher** fork will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

For upstream Fossify Gallery release history, see [CHANGELOG.md](CHANGELOG.md).  
For full architectural details, cryptography specifications, and user guides, see [DOCS.md](DOCS.md).

> **Versioning & Upstream Policy:** This fork uses an independent Semantic Versioning scheme (`X.Y.Z-cipher`) originally forked from upstream Fossify Gallery `1.14.1`. Version numbers do not map 1:1 to upstream releases; relevant upstream bug fixes and improvements are selectively backported and documented below.

---

## [Unreleased]
### Added
- **Zero-Pollution Folder Identity & Continuity Architecture (`FolderIdentity`, `MediaIdentity`, `GalleryDatabase`, `Favorite`, `VirtualAlbumItem`, `FavoritesDao`, `VirtualAlbumsDao`, `ReferenceBackupManager`, `VirtualAlbumSyncManager`, `FavoritesAndVirtualAlbumsImporter`):**
  - Extends file identity (`mediaStoreId`, `fileSize`, `lastModified`, `fingerprint`) with parent-folder continuity metadata **without writing any hidden marker files into shared user directories**:
    - `folderInode`: Unix/Android directory inode (`Os.stat(dir).st_ino`) preserved across same-volume renames and moves.
    - `folderSignature`: up to `5` sorted non-hidden sibling anchor signatures (`<size>:<sha256>`, excluding the item itself; directory scan capped at `2000` entries) so an entire moved or arbitrarily renamed folder can be identified cleanly.
    - `capturedAtSec`: reference creation timestamp in seconds for temporal validation against pre-existing copies in foreign folders.
  - Adds `FolderIdentity.CaptureSession` to cache directory inodes and sibling hashes once per folder during bulk favorite/virtual-album operations and imports.
  - Schema migration `14 -> 15` (`MIGRATION_14_15`) adds `folder_inode`, `folder_signature`, and `captured_at_sec` to `favorites` and `virtual_album_items`; DAO updates (`updateFavoriteIdentity`, `updateItemIdentity`) preserve non-zero/non-empty folder metadata, and JSON backups (`favorites.json`, `Virtual Album Saves/*.json`, and encrypted `Cipher_Secrets/JSON/*.json` manifests) serialize and restore all columns with backward-compatible defaults.
- **Non-Destructive Favorites Deletion Alignment (`DirectoryAdapter`, `DeleteDirectoryDialog`, `Favorites`, `Context`):**
  - Deleting the Favorites virtual directory now displays the same non-destructive confirmation prompt used by virtual albums instead of warning that a physical folder is being deleted, clearing only Favorites metadata and `favorites.json` while leaving original files untouched on disk.
- **Multi-Layer Batched Resolution, Two-Pass Batch Repair & Duplicate Ranking (`MediaIdentityResolver`, `MediaIdentityResolver.ResolutionSession`):**
  - Resolves relocated items across an ordered pipeline: `0a` cached path -> `0b` session relocated-folder cache (mapping up to `2` ancestor directory levels in `O(1)`) -> `1` batched `MediaStore._ID` lookup (`IN` chunks of `200`) -> `1.5` batched `MediaStore SIZE` query (projecting `DATE_ADDED`) -> `2` bounded filesystem fallback (max depth `4`, `5000` `stat()` visit cap, `40` candidates per size bucket; supporting `.nomedia` and `.fenc`/`.bin` vault files).
  - Batch repair executes two passes (`allowAmbiguous = false` then `true`) while excluding already claimed live paths (`excludedPaths`), and ranks verified candidates deterministically by `CandidateScore` (`exactMediaStoreId` > `inodeMatches` > `matchedAnchorCount` > `trailingFolderMatches` > `lastModifiedMatch` > `folderNameSimilarity` > `commonPrefixSegments` > `exactFileNameMatch` > shortest path).
- **Existing-Folder, Deleted-Folder & Foreign-Copy Guards (`MediaIdentityResolver`):**
  - *Existing Source Folder Guard (`allHintParentsExist`):* When the original source directory still exists on disk (for instance, emptied externally), broad `SIZE` and cross-folder fallback scans are suppressed so identical copies in another folder can never be pulled in; only exact `MediaStore._ID` moves or in-folder renames are allowed.
  - *Positive Continuity & Pre-Existing Copy Gate (`hasPositiveContinuity`):* When the source folder was completely deleted externally, candidates in foreign directories are rejected unless backed by `MediaStore._ID`, `folderInode`, sibling anchors, or valid folder continuity without an older pre-existing `MediaStore.DATE_ADDED` (`< capturedAtSec - 5`). Word-boundary checks in `computeFolderNameSimilarity` also prevent short-name false matches (e.g., `A` vs `Archive`).
- **Stable File Identity for Favorites and Virtual Album Items (`MediaIdentity`, `MediaIdentityResolver`, `Favorite`, `VirtualAlbumItem`, `FavoritesDao`, `VirtualAlbumsDao`, `GalleryDatabase`):**
  - Favorites and virtual album items used the absolute path as the identity of the referenced file. A rename or a move therefore broke the reference, and the item was silently dropped from the album.
  - The identity of a file is now described by the columns that survive a rename: the MediaStore id, the size, the modification time and a content fingerprint. The fingerprint is a SHA-256 over the first and the last 64 KB, so the cost does not depend on the size of the file and it stays stable under renames, moves and copies.
  - The resolver looks for a file again in three layers, from the most certain to the least: the MediaStore id, the full content fingerprint, and a cheap pre-filter of size and modification time that only narrows down the candidates. A candidate is never accepted without being confirmed by the fingerprint, so an ambiguous situation can never bind a reference to the wrong file.
  - Schema migration `13 -> 14` adds the identity columns to `favorites` and `virtual_album_items`.
- **Automatic Backups of the Favorites in the Cipher Folder (`ReferenceBackupManager`, `References`, `SplashActivity`, `MainActivity`):**
  - The favorites only lived in the database of the app, so clearing the app data removed them for good. The virtual albums survived because their backup files already lived in the Cipher folder on the shared storage.
  - The favorites are now backed up to `Documents/Cipher/Favorites/favorites.json` and restored when the app starts without any data. The backup is only written while the app may access the shared storage, because the folder cannot be read or written otherwise.
  - The references are also repaired when the app starts, so a file that was renamed or moved by another app is found again by its identity.
### Changed
- **The Cipher Folder Is Now a Standalone Folder in the Internal Storage (`CryptoManager`, `VirtualAlbumSyncManager`, `ReferenceBackupManager`):**
  - The vault, the saved virtual albums and the favorites backup no longer live in `Documents/Cipher`. They are now kept in a single top level folder `Fossify Gallery Cipher` in the internal storage, so the data of the app is clearly separated from the documents of the user and easy to back up as a whole.
  - The new container gets a `.nomedia` file, so it is never listed as a media folder by the system or by other gallery apps.
  - The vaults and the album saves of the older versions are found and moved into the new folder automatically, so nothing is lost by the update.
- **Virtual Albums Are No Longer Bound to a Single Physical Album (`VirtualAlbums`, `PickDirectoryDialog`, `Activity`, `FavoritesDao`, `VirtualAlbumsDao`):**
  - The strict 1-to-1 binding between a virtual album and its parent physical album was removed. An album now holds items from any number of physical folders, exactly like the favorites do.
  - The folder the items came from is only remembered as a hint for the copy dialog and no longer rejects new items, so a folder that was renamed or moved can no longer leave an album that refuses to accept any file.
  - The directory picker offers every virtual album as a copy destination, no matter which folder the source files live in.
- **Visibility Rules of Favorites and Virtual Albums (`VirtualAlbums`, `MediaFetcher`, `Context`, `FavoritesDao`, `CryptoManager`):**
  - Only the vault content is protected. A file coming from a hidden folder is referenced explicitly by the user, so it stays visible in the favorites and in the virtual albums, and it does not hide the album it was added to.
  - Adding such a file to an album no longer encrypts the name of the album, and an album that holds no vault content is visible without the option to show hidden files.
  - Favorites never hold vault content, so adding a vault file to them is refused.
- **RAM wipe is no longer triggered by `onStop` / screen off / UI hidden (`App`, `VaultSessionManager`, `MainActivity`, `MediaActivity`):**
  - Previously the vault keys, decrypted metadata and image caches were wiped the moment the last activity stopped. Now only the *last active time* is remembered (`VaultSessionManager`); the wipe happens once the app has been in the background for `VAULT_AUTO_LOCK_TIMEOUT_MS` (30 minutes). Set the constant to `0` to never wipe automatically. A deadline check (`SystemClock.elapsedRealtime()`) when the app returns to the foreground guarantees the lock even if the delayed task was held back by device sleep.
  - `MainActivity.onDestroy` / `MediaActivity.onDestroy` wipe immediately only on a real exit (`isFinishing`); a system-initiated destroy keeps the vault until the timeout.
- **Bottom actions are not shown for hidden files (`ViewPagerActivity`):**
  - The *"Show some action buttons at the bottom of the screen"* setting no longer applies to hidden files, no matter if they are images, GIFs or videos. The bottom action bar stays hidden for them.
  - The actions that would be shown at the bottom remain available in the top menu, so nothing gets lost when the bottom bar is not shown.
- **Extended details are never shown for encrypted files (`ViewPagerFragment`, `PhotoFragment`, `VideoFragment`):**
  - The *"Show extended details over fullscreen media"* setting, together with *"Hide extended details when status bar is hidden"* and *"Manage extended details"*, does not apply to encrypted files. Encrypted media never displays any extended details, even if the setting is enabled.
- **Smoother rotation by aspect ratio (`MediaDimensions`, `AspectRatioOrientation`, `ViewPagerActivity`, `VideoFragment`, `PhotoFragment`):**
  - The dimensions of the media are no longer read on the main thread. They are resolved in the background and cached, so the screen is rotated as soon as the aspect ratio of the media is known.
  - Videos use the exact display size reported by the media player, so the rotation is applied at the same time as the playback starts. With autoplay enabled the playback waits for the rotation, without it the rotation happens as soon as the video appears on the screen.
- **Files and Albums Are Now Labeled "Encrypt" and "Decrypt" (`strings.xml`, `cab_media.xml`, `cab_directories.xml`, `menu_viewpager.xml`, `DirectoryAdapter`, `ViewPagerActivity`):**
  - Hiding a file or an album in this fork means encrypting it into the vault, so the inherited "Hide" / "Unhide" labels were misleading. The context actions of the file grid, of the album grid and of the viewer now say "Encrypt" / "Decrypt", and "Encrypt album" / "Decrypt album" for an album.
  - A folder that is only hidden with a `.nomedia` file keeps the "Unhide" label, because nothing is decrypted in that case.
  - The success messages of the encryption and the labels of the encrypted album picker use the same wording now, and the settings entries for the favorites were renamed to "Export favorites and virtual albums" / "Import favorites and virtual albums", because the virtual albums are part of the same backup.
  - The missing translations of the sorting, sharing and encryption labels were added for the Turkish, German, Spanish (standard, Latin America and United States), Japanese, Norwegian (Bokmål and Nynorsk), Azerbaijani and Traditional Chinese locales.
### Fixed
- **The Destination Dialog of the Vault Showed a Misleading Virtual Album Option and Stacked Its Buttons (`PickDirectoryDialog`):**
  - The option of the copy dialog that creates a virtual album said only "Create virtual album", so an album that stays in the encrypted vault could be mistaken for a normal one. It now names the encryption as well, just like the option "Create New Encrypted Album" next to it does.
  - The buttons of the move dialog were stacked vertically, because the label of the third button was too long for the button bar of the dialog. It now uses the short label "New album", so the buttons are shown in a single row, exactly like in the copy dialog. The dialog that asks for the name of the new album still says "Create New Encrypted Album".
- **Favorites and Virtual Albums Broke When a Folder Was Renamed or Moved (`VirtualAlbums`, `Favorites`, `DirectoryAdapter`, `Activity`):**
  - Renaming or moving a physical folder left every reference pointing at the old location, so the album appeared empty and could not be filled with new files anymore.
  - The references below the folder are now rewritten along with it. The SQL uses `instr()` instead of `LIKE`, because a path may contain the wildcard characters of `LIKE`. The parent folder of an item is handled separately, since it may be the renamed folder itself.
- **Files Disappeared from Virtual Albums After a Rename (`VirtualAlbumSyncManager`, `MediaAdapter`, `ViewPagerActivity`, `Context`, `CryptoManager`):**
  - An item whose file was missing was removed from the album and from the backup. A rename or a move outside of the app looked exactly like that, so the item was gone for good and the identity needed to relocate it was destroyed with it.
  - The item is now only marked as unresolved and stays in the album and in the backup, so it can be repaired as soon as the file shows up again. The backup files also carry the identity of the items now, so a restored album is able to find its files again.
  - The multi-file rename flow did not update the references at all, it repairs them by their identity afterwards.
- **Favorites of Files in Hidden Folders Vanished from the List (`FavoritesDao`, `Context`):**
  - The favorites were joined with the media table, but that table only holds the non-hidden files. A favorite pointing at a file in a hidden folder was therefore dropped from the list. The join now keeps those favorites.
  - A favorite whose file was deleted outside of the app is removed instead of lingering in the list forever.
- **The Medium Path Update Never Matched a Row (`Context`, `MediumDao`):**
  - Root cause: `mediaDB.updateMedium` was called with the arguments in the wrong order, so the `WHERE full_path = ...` compared against the bare file name instead of the full path. The update silently did nothing and the media table kept the old path, which also broke the lookup of the cached media of a renamed file.
- **Rotation by aspect ratio did not work for encrypted files (`MediaDimensions`, `AspectRatioOrientation`, `ViewPagerActivity`):**
  - Root cause: the rotation relied on the file extension to determine the type of the media. The encrypted files use the `.bin` / `.fenc` extension, so their dimensions could never be read and the rotation was silently skipped.
  - The dimensions of encrypted files are now read from their decrypted content: images and GIFs from their header, videos from the frame embedded into the encrypted header or from the metadata of the decrypted stream. The EXIF orientation of encrypted images is read from the decrypted content as well.
### Added
- **Continue where you left off after the vault was wiped (`VaultResumeState`, `promptVaultUnlock`):**
  - Open album path, album/file names, sorting, view type, grouping and scroll offset are stored in `savedInstanceState` as a single blob encrypted with the vault key (`CryptoManager.encryptStringForVault`); nothing is stored in plain text.
  - After an automatic wipe or a process restart, the vault screens (`MainActivity`, `MediaActivity`, `ViewPagerActivity`) ask for the password again and restore the same album, file and scroll position instead of closing.
- **Vault media handling (`CryptoManager`, `Activity`, `DirectoryAdapter`, `PhotoFragment`, `ViewPagerActivity`):**
  - Only formats the gallery can display (images, GIFs, videos, RAWs, SVGs) are encrypted when a folder is encrypted or items are moved into the vault; other files (pdf, md, ...) are left untouched and the plaintext folder is kept if such files remain.
  - Decryption and copy/move into or out of the vault show a blocking `CryptoProgressDialog`; decrypt failures are reported instead of crashing.
  - Encrypted GIFs animate: bytes are decrypted straight into memory and handed to the GIF view.
  - Added a "Shuffle" menu item in the viewer for random-sorted albums, and `EXTRA_SECRET_VAULT_FILES` / `GetMediaAsynctask(isSecretVaultView)` for the viewer's vault-files mode.

## [1.18.3-cipher] - 2026-11-10
### Added
- **Mode 2 Encrypted Vault Recycle Bin (`CryptoManager`, `MainActivity`, `MediaActivity`, `ViewPagerActivity`, `DirectoryAdapter`):**
  - Added dedicated cryptographic recycle bin under `Cipher_Secrets/` with randomized folder name, `.nomedia`, and AES-256-GCM encrypted metadata (`$$VAULT_RECYCLE_BIN$$` -> "Recycle Bin").
  - Tracks original parent folder locations in an encrypted `.restore_info` manifest with restore, permanent CSPRNG wipe (`CryptoManager.secureWipeAndDeleteFile`), and bin emptying support.
  - Appears in Mode 2 directory grid at index 0 (or bottom if "Show recycle bin last" enabled) only when non-empty (`mediaCnt > 0`).
  - Seamlessly respects the *"Always skip delete confirmation dialog"* preference (`config.skipDeleteConfirmation` and `config.tempSkipDeleteConfirmation`).
- **All-Folder Directory Picker in Copy & Move (`PickDirectoryDialog`):**
  - When hidden items are unlocked in Mode 2 or Mode 3, `PickDirectoryDialog` now surfaces all three folder types: standard non-hidden folders, unencrypted hidden folders (with `.nomedia`), and decrypted physical vault folders.
  - Bypassed Android 11+ SAF path restrictions for encrypted vault destinations.

### Fixed
- **File Deletion Bug in Encrypted, Hidden, and Virtual Albums (`MediaActivity`, `ViewPagerActivity`, `SearchActivity`, `MediaAdapter`):**
  - Resolved failure where deleting encrypted `.bin` files, `.nomedia` hidden files, or virtual album items did nothing due to `isMediaFile()` filtering and Android 11+ MediaStore `deleteSdk30` non-indexing of hidden paths.
  - Implemented direct filesystem deletion (`File.delete()`), secure CSPRNG wiping, database cleanup, thumbnail trace purging, and virtual album manifest synchronization.
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
  - If you notice any inaccurate, unnatural, or missing translations—or would like to request full audit/support for a new language—please submit a report, complaint, or Pull Request on the project's [GitHub Issues](https://github.com/akbudakanl/Gallery-Cipher/issues) page.

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
  - For maximum security, use the offline Linux CLI tool (`tools/fossify-cipher-cli/cipher_vault.py`). Running it on a **diskless live-boot Linux distribution such as Tails (in RAM)** encrypts photos and videos into `.bin` ciphertext offline before they ever reach your phone, keeping both your media and the Android gallery app completely air-gapped.
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
- **Configurable Encrypted Section Exit Behavior (`ON_VAULT_EXIT`):**
  - Prompt selection dialog upon pressing back to exit Secret Vault mode asking whether to show encrypted album(s) with Yes/No options, quantity-aware phrasing, and an unchecked "Always ask" checkbox.
  - Contextual guidance note directing users to **Settings > On leaving encrypted section** for preference options.
  - Streamlined preference in Settings with 3 choices:
    1. Keep showing encrypted albums
    2. Hide encrypted albums
    3. Always ask (default)
  - Settings backup, export, and import support for the vault exit preference.
  - Independent management for unencrypted hidden folders (.nomedia) via temporarily show / stop showing excluded.
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
    3. **Option 3 (Desktop Linux CLI):** Offline media encryption tool (`tools/fossify-cipher-cli/cipher_vault.py`) for Linux (diskless live-boot systems such as Tails running in RAM are ideal), outputting compatible `.bin` files ready to transfer directly to `Documents/Cipher/Cipher_Secrets/`.

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
