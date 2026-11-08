# Fossify Gallery Cipher — Security Hardening and Secure Deletion Guide

Scope: static code analysis (CryptoManager, VirtualAlbumSyncManager, EncryptedMediaDataSource, Glide/ExoPlayer paths, AndroidManifest.xml, DOCS.md). The application was not built or run. Platform-dependent items (SQLite build flags, MediaStore behavior, fstrim scheduling) must be verified on a target device.

## 1. Objectives and Honest Threat Model

Assumptions: modern Android with File-Based Encryption (FBE), internal storage only, non-rooted device, user reboots device after each encryption operation.

Target protections:

- T1: Non-root recovery applications and third-party apps
- T2: Device loss or theft; device rebooted into Before First Unlock (BFU) state
- T3: Side channels such as backups, shares, and screenshots
- T4: Privileged forensic extraction while device is powered on and unlocked at least once (After First Unlock - AFU): partial mitigation only

Non-guarantees: physical erasure of NAND flash cells; external copies produced by camera apps, cloud backups, or messaging apps; T4.

Impact of rebooting: Clears volatile RAM (cryptographic keys, decoded bitmaps, filesystem page cache pages for deleted files) and returns device to BFU state. Does not alter flash blocks or guarantee an immediate fstrim. Upon first unlock, the Credential Encrypted (CE) key is restored into memory.

## 2. fstrim, Discard, and Garbage Collection (GC)

- When a file is deleted, the filesystem marks its blocks as "free"; the flash controller remains unaware until a discard/TRIM command is issued.
- Two primary paths exist: F2FS discard mount options (asynchronous discard during checkpoints) and idle maintenance fstrim alongside F2FS garbage collection (runs when device is charging, screen off, and idle; typically daily). Mount options and scheduling vary across OEMs.
- During device shutdown, `/data` is unmounted and F2FS performs its final checkpoint; pending discards may partially flush during this step, but this is not guaranteed.
- Even upon receiving a discard command, the controller is not required to erase flash blocks immediately. In practice, blocks become unreadable shortly thereafter on most modern controllers, but no universal standard guarantees timing.
- Under FBE, freed blocks already contain encrypted ciphertext; TRIM serves as a secondary defense layer.
- An application cannot directly trigger fstrim or filesystem GC. Available mitigations: user guidance (plug into charger, turn screen off) and, for advanced users via USB debugging, `adb shell sm idle-maint run` (runs without root on most devices, though permission errors may occur; test on target OEM).
- Overwriting free space with large random dummy files is discouraged: causes flash wear, is time-consuming, risks device instability, and does not guarantee reaching older blocks due to F2FS out-of-place logging.

## 3. Vulnerabilities and Remediations

### P0: Immediate Priority

**3.1 Secure Deletion (CryptoManager.secureWipeAndDeleteFile)**

Issues: Files exceeding 64 MB only overwrite the first 32 MB; only a single pass of zeroes is written (contradicting the documentation's claim of "multi-pass random + zero fill"); exceptions are caught and ignored while still deleting the file; function does not return a status.

Remediation: Overwrite the entire file length, invoke `force(true)`, truncate to 0, and delete. On error, return `false` so the caller preserves the original plaintext.

```kotlin
fun secureWipeAndDeleteFile(context: Context, file: File): Boolean {
    if (!file.exists()) return true
    val len = file.length()
    try {
        RandomAccessFile(file, "rw").use { raf ->
            val ch = raf.channel
            val buf = ByteArray(1 shl 20)
            var pos = 0L
            while (pos < len) {
                secureRandom.nextBytes(buf)
                val n = minOf(buf.size.toLong(), len - pos).toInt()
                var off = 0
                while (off < n) off += ch.write(ByteBuffer.wrap(buf, off, n - off), pos + off)
                pos += n
            }
            ch.force(true)
            ch.truncate(0)
            ch.force(true)
        }
    } catch (e: IOException) {
        return false   // caller must not delete the original and should notify the user
    }
    val deleted = file.delete()
    syncDir(file.parentFile)
    purgeTracesForFile(context, file.absolutePath)
    return deleted
}

private fun syncDir(dir: File?) {
    try {
        val fd = Os.open(dir!!.path, OsConstants.O_RDONLY, 0)
        try { Os.fsync(fd) } finally { Os.close(fd) }
    } catch (_: Exception) { /* best effort */ }
}
```

Notes: A single pass is sufficient; multi-pass overwrites offer no benefit on flash storage, and under FBE, written zeros are encrypted at rest anyway. Callers (`encryptFolder`, copy/move flows in `Activity.kt`) must check the return value and notify the user of any files that could not be securely wiped.

**3.2 Pre-Wipe Verification**

Currently, plaintext originals are deleted as long as encryption does not throw an unhandled exception. Instead, compute a SHA-256 digest of the plaintext stream during encryption; then decrypt the written ciphertext, verify its MAC, and confirm the digest matches. On mismatch, delete the encrypted artifact and keep the plaintext intact.

```kotlin
// during encryption: sha.update(buffer, 0, read) for every plaintext block read
val sha = MessageDigest.getInstance("SHA-256")
val expected = sha.digest()

// during verification
val check = MessageDigest.getInstance("SHA-256")
openDecryptedContentStream(enc)?.use { s ->
    val b = ByteArray(64 * 1024)
    while (true) { val n = s.read(b); if (n < 0) break; check.update(b, 0, n) }
}
val ok = verifyFileIntegrity(enc) && MessageDigest.isEqual(expected, check.digest())
if (!ok) { enc.delete(); return null }   // do not touch the original
```

**3.3 Credential Entropy and .vault Exposure**

Issues: Using a PIN (min 4 digits, ~13 bits entropy) or pattern (4–9 dots, ~18 bits entropy) renders Argon2id ineffective against offline brute force. The `.vault` file is stored under public Documents/Cipher; the wrapped bundle is duplicated in SharedPreferences and (since `allowBackup="true"`) in cloud backups. An attacker has everything needed for an offline dictionary attack.

Remediation:

- Device-bound key wrapping for daily operations: wrapped key = `GCM_Keystore( GCM_Argon2( MEK ) )`. The Keystore key resides in StrongBox (or TEE fallback) with `setUnlockedDeviceRequired(true)` so it cannot be used without an unlocked screen. Even if files are exfiltrated, the PIN cannot be brute-forced offline.
- Optional portability: A standalone recovery header protected solely by a strong passphrase (at least 6 Diceware words, ~77 bits entropy). If no strong passphrase is provided, avoid writing public headers altogether.
- Argon2id parameters: Base config of 64 MiB, t=3, p=2 (aligning with RFC 9106 low-memory recommendations); calibrate to ~0.5–1.0s on the target hardware.
- Enforce strict sanity ranges on parameters parsed by `unlockFromHeaderFile`.

```kotlin
val spec = KeyGenParameterSpec.Builder(
    "vault_wrap", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    .setKeySize(256)
    .setUnlockedDeviceRequired(true)
    .setIsStrongBoxBacked(hasStrongBox)   // fallback to TEE if unavailable
    .build()

// when parsing header
require(memoryKb in 8_192..262_144 && iterations in 1..10 && parallelism in 1..8)
```

**3.4 Long-Running Operation Resilience**

Issues: Encryption and wipe routines run inside `ensureBackgroundThread`. If the user leaves the app, `lockVault()` in `onDestroy` zeroes out keys mid-operation. If the process is killed by the OS, plaintext files remain stranded.

Remediation: Execute batch operations inside a foreground service (`dataSync` type); suppress auto-lock during active processing; avoid exposing album names in notifications; pre-flight check storage space (album size × 1.1) and battery level.

**3.5 Subdirectory Handling**

`encryptFolder` only encrypts top-level files; subdirectories are moved in plaintext under `internalStorageRoot` and their `.nomedia` file is removed. Encrypt subdirectories recursively (preferred) or display an explicit warning and prompt for confirmation before proceeding.

**3.6 Screen Capture and Recents Leaks**

`FLAG_SECURE` was absent; Android writes task snapshot images to disk when an app transitions to the background. Window security must be applied whenever the vault view (`mIsSecretVaultView`) or encrypted media is displayed.

```kotlin
fun Activity.applyVaultScreenProtection(enable: Boolean) {
    if (enable) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(!enable)
}
```

**3.7 Application Backups**

`allowBackup="true"` allows room databases, SharedPreferences, and wrapped credentials to escape into cloud backups. Set `allowBackup="false"` and configure `dataExtractionRules` to exclude all domains.

```xml
<application android:allowBackup="false"
    android:dataExtractionRules="@xml/data_extraction_rules" ... >

<!-- res/xml/data_extraction_rules.xml -->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root"/><exclude domain="file"/>
        <exclude domain="database"/><exclude domain="sharedpref"/>
    </cloud-backup>
    <device-transfer>
        <exclude domain="root"/><exclude domain="file"/>
        <exclude domain="database"/><exclude domain="sharedpref"/>
    </device-transfer>
</data-extraction-rules>
```

### P1: Short-Term Priority

**3.8 Room Database Forensic Artifacts**

Android's system SQLite is often compiled with `secure_delete` enabled, but this is not guaranteed across all vendor builds. Query `PRAGMA secure_delete` and enforce `ON`; run `wal_checkpoint(TRUNCATE)` followed by `VACUUM`. After encrypting an album, purge its paths from `mediaDB`, `directoryDB`, `favorites`, and `dateTakens`.

```kotlin
Room.databaseBuilder(ctx, GalleryDatabase::class.java, name)
    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
    .addCallback(object : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            db.query("PRAGMA secure_delete = ON").close()
        }
    })
    .build()

fun scrubDatabase(db: SupportSQLiteDatabase) {
    db.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
    db.execSQL("VACUUM")
}
```

**3.9 Glide and Subsystem Caches**

`clearDiskCache` simply unlinks files. Overwrite cached files prior to calling `clearDiskCache()`. Apply the same logic to `externalCacheDir`, `removeTempFolder()` contents, app widget cache (`MyWidgetProvider`, RESOURCE), and `PortraitPhotosAdapter` (RESOURCE). Ensure hidden albums cannot be selected as widget cover art.

**3.10 Virtual Album JSON Files**

`Virtual Album Saves/*.json` was deleted with plain `delete()` when an album was hidden; overwrite before unlinking (files are small, negligible I/O overhead). Hidden album filenames were derived from a hash of the album title (`fva_<hash>`), allowing rainbow/dictionary confirmation of album names. Use `HMAC(MEK, title)` or a random UUID. Align the 16-character hex description in DOCS.md with reality.

**3.11 SharedPreferences (Config)**

Audit preference keys containing paths: pinned, included, and excluded folders; album covers; folder-specific sort and view settings; last opened directory. Cleanse them upon hiding an album. Note: SharedPreferences rewrites the entire file upon modification, leaving stale flash blocks behind; here only FBE provides protection. Consequently, avoiding persistent storage of sensitive paths is ideal.

**3.12 MediaStore and System Indices**

`MediaStore.*.Thumbnails` tables have been deprecated since API 29; calling purge operations against them is mostly ineffective. After deletion, scan the parent directory (`scanFile`), verify rows have been removed, and look for `.thumbnails` and `.trashed-*` remnants. Check Fossify's built-in Recycle Bin for items originating from the album and shred them if present.

**3.13 File Timestamps and Size Leakage**

`encryptedFile.setLastModified(lastModified)` propagated original photo timestamps directly to ciphertext files. Replace with a fixed (e.g. midnight UTC) or randomized timestamp; the original timestamp is already preserved inside the encrypted header. Optionally, pad small files into discrete size buckets.

**3.14 Encryption Container Format (v5)**

In earlier formats, the per-file key was the MEK directly; Poly1305 MAC was only verified on full export/decryption rather than during streaming playback; headers and payloads were not cryptographically bound.

Recommendation: Transition to Google Tink StreamingAead (AES-GCM-HKDF with 1 MiB segments; random access via `SeekableByteChannel` preserving ExoPlayer seeking) with random per-file keys (wrapped with MEK, AAD = file UUID). Every segment is authenticated during both playback and export. Retain read support for v1–v4 while migrating in the background.

If retaining custom CTR + Poly1305: Authenticate the MAC on initial stream opening, incorporate header digest into the MAC input, and derive unique per-file keys.

**3.15 Key and Memory Hygiene**

- Take Argon2 credentials as `CharArray` rather than `String`; zero out `secretBytes` and `keyBytes` immediately after use.
- `unlockedPrivateKey` resides as an immutable object in memory and cannot be directly zeroed. Retain PKCS#8 byte arrays for on-demand derivation or delegate RSA keys to Android Keystore (non-exportable keys).
- On `lockVault()`, wipe bitmaps in `VideoFragment.sVideoFrameCache` using `eraseColor(0)` + `recycle()` and call `Glide.get(ctx).clearMemory()`. Previously, `clearFrameCache()` was only invoked from `ViewPagerActivity`, and `evictAll()` did not zero pixel buffers.
- Note: This is best-effort; exact JVM memory wiping cannot be guaranteed due to GC compaction and memory duplication. Acknowledge this limitation in documentation.

**3.16 Lock Triggers and Lifecycle**

Do not rely solely on `onDestroy` or delayed handler callbacks. Lock immediately upon application backgrounding (with a short, configurable tolerance) and on screen-off events.

```kotlin
ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) = VaultSession.lockNow(appContext)
})
registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
```

**3.17 Additional Defects**

- Calling `setupOrUpdateCredentials` while locked generates a new MEK, rendering existing files permanently inaccessible; reject attempts while locked.
- `deleteExistingVault` unlinks headers without shredding; overwrite header bytes even when wrapped.
- Remove blanket `catch (_: Exception) {}` blocks on critical I/O paths; elevate `EmptyCatchBlock` and `SwallowedException` detekt rules to errors for `helpers/`.
- Audit logcat outputs to ensure filenames and filesystem paths are never printed.

## 4. Post-Encryption Sanitization Pipeline

Order of operations is critical:

1. Pre-flight checks: verify storage is internal (warn on SD/USB), check FBE status (`DevicePolicyManager.getStorageEncryptionStatus()` == `ENCRYPTION_STATUS_ACTIVE_PER_USER`), confirm sufficient free space, and check battery level. Start foreground service.
2. Encrypt: write to temporary filename (`.tmp`), call `force()`, rename to target; compute SHA-256 digest of plaintext stream.
3. Verify: decrypt the encrypted file stream, verify Poly1305 MAC and SHA-256 digest match. If any check fails, abort and preserve originals.
4. Pending-wipe journal: record verified plaintext paths to a journal file in internal app-private storage (not Room; unencrypted is fine as data was already verified). On crash or process death, the wipe resumes on next launch.
5. Shred: execute the secure wipe function from section 3.1 for each file; remove successful entries from journal, report failures to the user.
6. Application traces: delete DB records + run checkpoint/VACUUM, purge favorites, virtual album JSON, SharedPreferences entries, Glide and external caches (shred then clear), temporary folder, MediaStore rows, and Recycle Bin items.
7. Remove plaintext directory; `fsync` parent directory. Shred and unlink the pending-wipe journal.
8. Secondary memory purge: flush Glide memory cache and frame cache bitmaps.
9. User report: display summary of files securely wiped, any failed files, traces purged, and recommended next steps.

## 5. Device Reboot and Idle Maintenance

- Applications cannot reboot Android devices programmatically; display a prominent "Reboot Device" recommendation on the completion screen.
- Recommended user routine: hide album, review the "securely wiped" summary, plug device into charger, lock screen, and leave idle (allowing idle maintenance / fstrim to run, ideally overnight), then reboot.
- Post-reboot first unlock: A WorkManager worker triggered by `BOOT_COMPLETED` (declared in manifest) checks the pending-wipe journal, wipes leftover plaintexts, repeats DB checkpoint/VACUUM, and cleans cache folders.
- Sequencing: Rebooting immediately clears volatile RAM but does not guarantee flash discard; idling first maximizes discard execution. Both practices complement each other.

## 6. Additional Hardening Measures

- Ensure export, share, "open with", print, and wallpaper flows never generate plaintext temporary files on disk. Sharing the raw encrypted file also poses operational risks; disable or explicitly gate these flows for vault media.
- Redact album names and thumbnail previews from notification channels and recent task views.
- Protect PIN and pattern unlock screens against tapjacking and window overlay attacks using `filterTouchesWhenObscured`.
- Pin dependency versions for BouncyCastle and Tink, and monitor CVE advisories.
- Zero network surface: Verify via CI that the merged manifest contains no `INTERNET` permission.

## 7. Verification and Testing Plan

- Unit tests: verify with known test vectors; single-bit flips must trigger MAC verification failure; truncated files; header swapping and payload swapping; incorrect password rejection; out-of-bounds Argon2 parameter rejection.
- Wipe logic testing: On an emulator or non-FBE test image, generate 4 KB, 50 MB, and 500 MB files containing distinct `CANARY-<uuid>` markers, hide the album, and scan raw userdata image dumps for the marker. (On an FBE device, raw block reads fail regardless, testing FBE rather than wipe logic; do not conflate the two).
- Application trace testing (debuggable build): Inspect `databases/` (including `-wal` and `-shm`), `shared_prefs/`, and `cache/` using `adb shell run-as <package>` for album names and identifiers. Query MediaStore with `adb shell content query --uri content://media/external/file --where "_data LIKE '%AlbumName%'"` and check `/sdcard` for `.thumbnails` and `.trashed-*`.
- Fault injection: read-only files, locked files, low storage conditions, mid-operation process termination, screen timeout, incoming calls.
- UI security: verify album content is suppressed in Recents and screenshots are blocked.
- Code quality: enforce detekt rules banning empty catch blocks in critical packages, and run Android lint.

## 8. Documentation Corrections (DOCS.md / FORK_CHANGELOG.md)

Recommended terminology:

- Secure deletion: "The entire content of the original file is overwritten, truncated, and deleted. Physical erasure cannot be guaranteed on flash storage; protection primarily relies on Android's file-based encryption (FBE) and system TRIM/discard operations."
- RAM: "Key material is zeroized on a best-effort basis; decrypted images may remain in memory briefly. A device reboot is recommended after operations."
- Disk writes: "The app does not write decrypted content to its own files; screenshots, task snapshots, and system swap are outside app control (FLAG_SECURE mitigates the first two)."
- Remove misleading claims such as "multi-pass shredding", "random + zero fill", and "never".

## 9. Priority Roadmap

- Week 1 (P0): 3.1, 3.2, 3.4, 3.6, 3.7, and documentation corrections.
- Week 2 (P0/P1): 3.3 (Keystore-bound wrapping and passphrase option), 3.5, 3.8–3.12, 3.16.
- Week 3+ (P1/P2): 3.14 (v5 format and migration), 3.15, 3.13, and testing testbed.

Timelines are approximate estimates. Format migration carries the highest risk and requires a backward compatibility plan (supporting v1–v4 reading).

## 10. Residual Risks

- Physical erasure is not guaranteed at the flash NAND controller level; reliance is placed on FBE and TRIM.
- Privileged forensic extraction may partially succeed while the device is in the AFU state.
- Pre-existing copies of the original files may exist in camera apps, cloud sync services, and messaging apps.
- OEM system modifications (Samsung, Xiaomi, etc.) may run proprietary gallery caches, thumbnailers, or recycle bin mechanisms.
- User operational security: weak PIN/pattern, taking external screenshots, or exporting/sharing unprotected media to other applications.
