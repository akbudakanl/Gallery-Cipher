# Favorites & Virtual Albums Import / Export Architecture

Fossify Gallery Cipher provides a unified **Export / Import** mechanism in **Settings** (`SettingsActivity`) for backing up and restoring **Favorites** and **public Virtual Albums**.

> [!CAUTION]
> **Security Note:** Do **not** import backups from untrusted sources. Imported `.zip` archives and JSON manifests are parsed directly by the application and can reference arbitrary local file paths, alter or overwrite your Favorites list (when using replace mode), and create new Virtual Albums in your library. Only import backup packages that you created yourself or received from a source you explicitly trust.

---

## 1. Backup Format: JSON Inside a `.zip` Archive

Backups are stored as **JSON files packaged inside a `.zip` archive** (`application/zip`). This keeps both Favorites metadata and individual Virtual Album manifests portable in a single file while preserving full file and folder identity descriptors (`MediaIdentity` and `FolderIdentity`).

### Standard `.zip` Layout

```text
<appName>-favorites_<YYYY-MM-DD_HH-mm-ss>.zip
├── Favorites/
│   └── favorites.json
└── Virtual albums/
    ├── Vacation.json
    ├── Roadtrip.json
    └── ...
```

- **`Favorites/favorites.json`**: Created when at least one valid favorite exists. Serialized from `ReferenceBackupManager.FavoritesBackup`.
- **`Virtual albums/<album>.json`**: Created for each non-hidden virtual album. Serialized from `VirtualAlbumSyncManager.VirtualAlbumBackup`. If two albums produce the same filename, `FavoritesAndVirtualAlbumsBackupManager.resolveUniqueZipFileName` appends a numeric suffix (`Album(1).json`, `Album(2).json`, etc.).
- **Excluded Data:** Hidden/stealth virtual albums (`isHidden == true`), encrypted vault media (`Cipher_Secrets/`), and items in the recycle bin (`recycleBinPath`) are **never** included in exports.

---

## 2. Export Flow

Implemented across `SettingsActivity` (`setupExportFavorites`, `startExportFavoritesFlow`, `exportFavoritesTo`) and `FavoritesAndVirtualAlbumsBackupManager` (`hasDataToExport`, `exportToStream`, `writeExportPackage`):

1. **Availability Check (`hasDataToExport`):**
   - Verifies whether `favoritesDB.getValidFavoritePaths()` is non-empty or `virtualAlbumsDB.getAllAlbums()` contains at least one non-hidden virtual album (`!it.isHidden`).
   - If neither exists, the app displays a *"No items found"* toast and aborts.
2. **Destination Selection (`startExportFavoritesFlow`):**
   - **Android 10+ (API 29+ / `isQPlus()`):** Opens `ExportFavoritesDialog` for the filename, then launches Android's Storage Access Framework (`Intent.ACTION_CREATE_DOCUMENT`) with MIME type `application/zip`.
   - **Android 9 and below:** Requests `PERMISSION_WRITE_STORAGE`, opens `ExportFavoritesDialog` for the destination path, and writes directly to the chosen file.
3. **Data Collection (`exportToStream`):**
   - **Favorites:** Queries `favoritesDB.getAll()` and filters to paths present in `favoritesDB.getValidFavoritePaths()`.
   - **Virtual Albums:** Queries `virtualAlbumsDB.getAllAlbums().filter { !it.isHidden }`. For each album, excludes paths marked as deleted for that album (`getDeletedPathsForAlbum`) and any path under `recycleBinPath`.
4. **JSON Serialization & ZIP Packaging (`writeExportPackage`):**
   - Uses `GsonBuilder().setPrettyPrinting().create()` to serialize:
     - `FavoritesBackup` (`version`, `favorites` list of `FavoriteBackup` entries containing `fullPath`, `filename`, `parentPath`, `mediaStoreId`, `fileSize`, `lastModified`, `fingerprint`, `folderInode`, `folderSignature`, `capturedAtSec`).
     - `VirtualAlbumBackup` (`version`, `name`, `createdTs`, `isHidden`, `parentPhysicalPath`, and `items` list of `VirtualAlbumItemBackup` entries with `fullPath`, `addedTs`, `mediaStoreId`, `fileSize`, `lastModified`, `fingerprint`, `state`, `folderInode`, `folderSignature`, `capturedAtSec`).
   - Writes entries into a `ZipOutputStream` in UTF-8.
   - The exported `.zip` output is identical whether or not all-files storage access is granted.

---

## 3. Import Flow

Implemented across `SettingsActivity` (`setupImportFavorites`, `importFavorites`, `applyFavoritesAndAlbumsImport`), `FavoritesAndVirtualAlbumsBackupManager.parseImportPackage`, and `FavoritesAndVirtualAlbumsImporter`:

### Step 1: File Selection
- **Android 10+ (API 29+ / `isQPlus()`):** Launches `Intent.ACTION_GET_CONTENT` accepting `application/zip`, `application/x-zip-compressed`, `application/octet-stream`, and `text/plain`.
- **Android 9 and below:** Requests `PERMISSION_READ_STORAGE` and opens `FilePickerDialog`.

### Step 2: Package Parsing (`parseImportPackage`)
The parser reads the input stream into memory and inspects the header (`isZipArchive` checks for ZIP magic bytes `0x50 0x4B` followed by `0x03`, `0x05`, or `0x07`):

- **ZIP Archive (`parseZipPackage`):**
  - Iterates entries via `ZipInputStream`, normalizing `\` to `/`, stripping leading `/`, and **rejecting path traversal** (`..` path segments).
  - Processes only `.json` files:
    - Entries under `Favorites/` (case-insensitive) are parsed as `FavoritesBackup` (`parseFavoritesJson`), filtering out entries with null or blank `fullPath`.
    - Entries under `Virtual albums/` or `Virtual Album Saves/` (`CIPHER_VIRTUAL_ALBUMS_DIR_NAME`) are parsed as `VirtualAlbumBackup` (`parseVirtualAlbumJson`).
    - Encrypted/stealth virtual albums (`isHidden && encryptedPayload != null`) are ignored.
    - Supports legacy `mediaPaths` arrays when `items` is absent, and falls back to the `.json` filename (without extension) if `name` is missing.
- **Legacy / Plaintext Fallback (`parseLegacyOrPlainPackage`):**
  - If the file is not a ZIP archive and begins with `{`, attempts to parse it as a single `FavoritesBackup` or `VirtualAlbumBackup` JSON object.
  - Otherwise, treats each non-empty line as a plain favorite file path (`FavoriteBackup(fullPath = line)`).

### Step 3: Favorites Conflict Resolution (`ImportFavoritesConflictDialog`)
If the parsed package contains favorites (`pkg.favorites.isNotEmpty()`) **and** the device already has existing favorites (`hasExistingFavorites`), `SettingsActivity` prompts the user with `ImportFavoritesConflictDialog`:
- **Merge (`FavoritesImportMode.MERGE`):** Keeps existing favorites and adds/updates imported ones.
- **Replace (`FavoritesImportMode.DELETE_EXISTING`):** Clears `favoritesDB`, resets favorite flags in `mediaDB`, and removes the `FAVORITES` entry in `directoryDB` before importing.

If no existing favorites are present, `FavoritesImportMode.MERGE` is used automatically.

### Step 4: Restoring Favorites (`importFavoritesList` & `importSingleFavorite`)
For each `FavoriteBackup` entry:
1. **Path & Identity Resolution (`resolveExistingPath`):**
   - Checks if `fullPath` exists on disk.
   - If moved or renamed, falls back to `MediaIdentityResolver.resolve(...)` using the stored `MediaIdentity` and `FolderIdentity` fields (`mediaStoreId`, `fileSize`, `lastModified`, `fingerprint`, `folderInode`, `folderSignature`, `capturedAtSec`).
   - If the file cannot be located, the entry is skipped.
2. **Safety Filtering:**
   - Rejects any resolved path inside `recycleBinPath` or encrypted vault storage (`CryptoManager.isVaultMediaPath`).
3. **Database Insertion / Enrichment:**
   - If not already in `favoritesDB`, computes fresh `MediaIdentity` + `FolderIdentity` (preserving stored fallback metadata), inserts the `Favorite` record, and marks `mediaDB.updateFavorite(resolvedPath, true)`.
   - If already present, fills in missing `fingerprint` or `folderSignature` metadata via `favoritesDB.updateFavoriteIdentity(...)` and ensures `mediaDB` is marked as favorite.
4. **On-Disk Backup Sync:**
   - If shared-storage write access is granted (`canWriteInternalBackups`), updates `Fossify Gallery Cipher/Favorites/favorites.json` via `ReferenceBackupManager.saveFavorites(context)`.

### Step 5: Restoring Virtual Albums (`importVirtualAlbumsList` & `insertImportedVirtualAlbum`)
1. **Collision-Safe Naming:**
   - Collects all existing album names (and resolved names) case-insensitively and assigns a unique name via `resolveUniqueVirtualAlbumName` (e.g., `Vacation(1)`), ensuring existing virtual albums are never overwritten.
2. **Album Creation:**
   - Inserts a new non-hidden `VirtualAlbum` (`isHidden = false`) into `virtualAlbumsDB`.
3. **Item Resolution & Filtering (`buildSingleVirtualAlbumItem`):**
   - Rejects paths under `recycleBinPath` or `CryptoManager.isVaultMediaPath`, and deduplicates case-insensitively within the album (`seenPaths`).
   - Resolves the item via `resolveExistingPath`. If the file is not found and its parent directory does not exist on disk (`parentFile?.exists() != true`), the raw path is preserved so `UNRESOLVED` items from external/unmounted storage are retained; if the parent folder exists but the file is gone and unresolvable, the item is skipped.
   - Bulk-inserts resolved `VirtualAlbumItem` records via `dao.insertItems(itemsToInsert)`.
4. **On-Disk Backup Sync:**
   - If shared-storage write access is granted (`canWriteInternalBackups`), writes the restored album manifest to `Fossify Gallery Cipher/Virtual Album Saves/<album>.json` via `VirtualAlbumSyncManager.saveAlbumToFile(context, newAlbumId)`.
   - If shared-storage access is **not** granted, imported Favorites and Virtual Albums remain safely stored in the app's internal Room database without writing to shared storage.
