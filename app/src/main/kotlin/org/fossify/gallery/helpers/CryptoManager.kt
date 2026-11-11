package org.fossify.gallery.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import com.bumptech.glide.Glide
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.macs.Poly1305
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.gallery.R
import org.fossify.gallery.extensions.config
import org.fossify.gallery.extensions.deleteVirtualAlbumItemFromAllAlbums
import org.fossify.gallery.extensions.directoryDB
import org.fossify.gallery.extensions.mediaDB
import org.fossify.gallery.extensions.updateVirtualAlbumItemPath
import org.fossify.gallery.extensions.virtualAlbumsDB
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PSource
import android.media.MediaScannerConnection
import android.os.Build
import androidx.biometric.BiometricManager
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.fossify.gallery.databases.GalleryDatabase
import org.fossify.gallery.extensions.favoritesDB
import org.fossify.gallery.fragments.VideoFragment
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class BoundedInputStream(
    private val wrapped: InputStream,
    private var remaining: Long
) : InputStream() {
    override fun read(): Int {
        if (remaining <= 0) return -1
        val b = wrapped.read()
        if (b != -1) remaining--
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (remaining <= 0) return -1
        val toRead = minOf(len.toLong(), remaining).toInt()
        val read = wrapped.read(b, off, toRead)
        if (read > 0) remaining -= read
        return read
    }

    override fun skip(n: Long): Long {
        if (n <= 0 || remaining <= 0) return 0L
        val toSkip = minOf(n, remaining)
        val skipped = wrapped.skip(toSkip)
        if (skipped > 0) remaining -= skipped
        return skipped
    }

    override fun available(): Int {
        return minOf(wrapped.available().toLong(), remaining).toInt()
    }

    override fun close() {
        wrapped.close()
    }
}

data class EncryptedFileMetadata(
    val originalName: String,
    val lastModified: Long,
    val dateTaken: Long,
    val originalSize: Long,
    val mediaType: Int,
    val videoDuration: Int,
    val payloadOffset: Long,
    val contentIv: ByteArray,
    val fileKey: ByteArray,
    val version: Byte = 1,
    val isMacAuthenticated: Boolean = false,
    val thumbnailBytes: ByteArray? = null
)

data class VaultPhysicalAlbum(
    val name: String,
    val fullPath: String,
    val folder: File,
    val itemCount: Int
)

object CryptoManager {
    private const val MAGIC_V1 = "FVLTENC1"
    private const val MAGIC_V2 = "FGVLTENC"
    private const val VERSION_SYMMETRIC: Byte = 1
    private const val VERSION_ASYMMETRIC: Byte = 2
    private const val VERSION_SYMMETRIC_AUTH: Byte = 3
    private const val VERSION_ASYMMETRIC_AUTH: Byte = 4
    private const val VERSION_V5_AUTH: Byte = 5
    private const val META_MAGIC_SYM = "FVLTMETA"
    private const val META_MAGIC_ASYM = "FVLTMET2"
    private const val META_MAGIC_V2 = "FGMETA2"
    private const val VAULT_HEADER_MAGIC = "FGVLT2"
    private const val VAULT_STR_PREFIX = "FVSTR1:"
    const val VAULT_RECYCLE_BIN_NAME = "\$\$VAULT_RECYCLE_BIN\$\$"
    const val VAULT_RESTORE_INFO_FILE = ".restore_info"

    // Cryptographic Key Separation Domains & MAC Specifications
    private const val DOMAIN_HEADER_KEY = "FossifyGallery:Header:AES256GCM"
    private const val DOMAIN_CONTENT_ENC_KEY = "FossifyGallery:Payload:AES256CTR"
    private const val DOMAIN_CONTENT_MAC_KEY = "FossifyGallery:Payload:Poly1305"
    private const val MAC_TAG_LENGTH = 16

    private fun deriveSubkey(key: ByteArray, domain: String): ByteArray {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(domain.toByteArray(Charsets.UTF_8))
    }

    // Argon2id Parameters (Memory-Hard Key Derivation)
    private const val ARGON2_MEMORY_KB = 65536 // 64 MB RAM cost
    private const val ARGON2_ITERATIONS = 3
    private const val ARGON2_PARALLELISM = 2
    private const val ARGON2_SALT_LENGTH = 16

    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val RSA_KEY_BITS = 2048
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val CTR_IV_LENGTH = 16
    private const val THUMB_MAX_DIMENSION = 320
    private const val THUMB_JPEG_QUALITY = 75

    private val secureRandom = SecureRandom()

    @Volatile
    private var unlockedMasterKey: ByteArray? = null

    @Volatile
    private var unlockedPrivateKey: PrivateKey? = null

    private val metadataCache = ConcurrentHashMap<String, EncryptedFileMetadata>()
    private val folderNameCache = ConcurrentHashMap<String, String>()
    private val encryptedFolderCache = ConcurrentHashMap<String, Boolean>()

    @Volatile
    private var cachedVaultRoot: File? = null
    @Volatile
    private var vaultRootSearchDone = false
    @Volatile
    var vaultRecycleBinFolder: File? = null

    private val activeCryptoOperations = AtomicInteger(0)
    private val pendingLock = AtomicBoolean(false)

    fun isOperationInProgress(): Boolean = activeCryptoOperations.get() > 0

    fun <T> runCryptoOperation(block: () -> T): T {
        activeCryptoOperations.incrementAndGet()
        try {
            return block()
        } finally {
            if (activeCryptoOperations.decrementAndGet() == 0 && pendingLock.getAndSet(false)) {
                lockVaultInternal()
            }
        }
    }

    val isVaultUnlocked: Boolean
        get() = unlockedMasterKey != null

    fun getMasterKeyCopy(): ByteArray? = unlockedMasterKey?.copyOf()

    fun invalidateFolderCache() {
        encryptedFolderCache.clear()
    }

    fun lockVault() {
        if (isOperationInProgress()) {
            pendingLock.set(true)
            return
        }
        lockVaultInternal()
    }

    private fun lockVaultInternal() {
        unlockedMasterKey?.fill(0)
        unlockedMasterKey = null
        unlockedPrivateKey = null
        metadataCache.values.forEach { it.fileKey.fill(0) }
        metadataCache.clear()
        folderNameCache.clear()
        encryptedFolderCache.clear()
        vaultRecycleBinFolder = null
        SessionRandomOrder.clearVaultKey()
        try {
            VideoFragment.clearFrameCache()
        } catch (_: Throwable) {}
    }

    fun isEncryptedFile(path: String): Boolean {
        return path.endsWith(ENCRYPTED_BIN_EXT, ignoreCase = true) ||
            path.endsWith(ENCRYPTED_FILE_EXT, ignoreCase = true)
    }

    /**
     * Returns true when the media item belongs to the encrypted vault content
     * zone, i.e. it is stored as an encrypted file or it is located inside one
     * of the encrypted (Cipher_Secrets) folders.
     */
    fun isVaultMediaPath(context: Context, path: String): Boolean {
        if (path.isEmpty()) return false
        return isEncryptedFile(path) || isEncryptedFolderPath(context, path.getParentPath())
    }

    fun isEncryptedFolder(folder: File): Boolean {
        if (!folder.exists() || !folder.isDirectory) return false
        return File(folder, VAULT_SUBFOLDER_META_FILE).exists() ||
            File(folder, ENCRYPTED_FOLDER_META_FILE).exists() ||
            folder.name.startsWith(ENCRYPTED_FOLDER_PREFIX)
    }

    fun isEncryptedFolderPath(context: Context, folderPath: String): Boolean {
        val clean = folderPath.trimEnd('/')
        if (clean.isEmpty()) return false

        encryptedFolderCache[clean]?.let { return it }

        if (context.config.encryptedFolders.contains(clean)) {
            encryptedFolderCache[clean] = true
            return true
        }

        val vaultRoot = findVaultRoot(context)
        if (vaultRoot != null) {
            val rootPath = vaultRoot.absolutePath.trimEnd('/')
            if (clean == rootPath || clean.startsWith("$rootPath/")) {
                encryptedFolderCache[clean] = true
                return true
            }
        }

        val folder = File(clean)
        val isEnc = isEncryptedFolder(folder)
        encryptedFolderCache[clean] = isEnc
        return isEnc
    }

    fun getDocumentsDirectory(): File {
        return try {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        } catch (_: Throwable) {
            null
        } ?: try {
            File(Environment.getExternalStorageDirectory(), "Documents")
        } catch (_: Throwable) {
            File("/storage/emulated/0/Documents")
        }
    }

    fun getDefaultVaultRoot(): File {
        val cipherDir = File(getDocumentsDirectory(), CIPHER_ROOT_DIR_NAME)
        return File(cipherDir, CIPHER_SECRETS_DIR_NAME)
    }

    fun getExistingCipherSecretsDir(context: Context): File? {
        val standardDir = getDefaultVaultRoot()
        if (standardDir.exists() && standardDir.isDirectory && (File(standardDir, VAULT_HEADER_FILE).exists() || standardDir.listFiles()?.isNotEmpty() == true)) {
            return standardDir
        }

        val docDir = File(getDocumentsDirectory(), CIPHER_SECRETS_DIR_NAME)
        if (docDir.exists() && docDir.isDirectory && (File(docDir, VAULT_HEADER_FILE).exists() || docDir.listFiles()?.isNotEmpty() == true)) {
            return docDir
        }

        val storedPath = context.config.vaultRootPath
        if (storedPath.isNotEmpty()) {
            val dir = File(storedPath)
            if (dir.exists() && dir.isDirectory && (File(dir, VAULT_HEADER_FILE).exists() || dir.listFiles()?.isNotEmpty() == true)) {
                return dir
            }
        }

        val found = findVaultRoot(context)
        if (found != null && found.exists() && (File(found, VAULT_HEADER_FILE).exists() || found.listFiles()?.isNotEmpty() == true)) {
            return found
        }

        return null
    }

    fun deleteExistingVault(context: Context) {
        val targets = mutableListOf<File>()
        targets.add(getDefaultVaultRoot())
        targets.add(File(getDocumentsDirectory(), CIPHER_SECRETS_DIR_NAME))
        val storedPath = context.config.vaultRootPath
        if (storedPath.isNotEmpty()) {
            targets.add(File(storedPath))
        }
        getExistingCipherSecretsDir(context)?.let { targets.add(it) }
        findVaultRoot(context)?.let { targets.add(it) }

        for (dir in targets.distinct()) {
            if (dir.exists()) {
                try {
                    dir.walkBottomUp().forEach { file ->
                        if (file.isFile) {
                            secureWipeAndDeleteFile(file, context)
                        } else if (file.isDirectory) {
                            file.delete()
                        }
                    }
                    dir.delete()
                } catch (_: Exception) {}
            }
        }

        val parentCipher = File(getDocumentsDirectory(), CIPHER_ROOT_DIR_NAME)
        if (parentCipher.exists() && parentCipher.isDirectory && parentCipher.listFiles().isNullOrEmpty()) {
            try { parentCipher.delete() } catch (_: Exception) {}
        }

        lockVault()
        unlockedMasterKey = null
        unlockedPrivateKey = null
        cachedVaultRoot = null
        vaultRootSearchDone = false
        encryptedFolderCache.clear()

        clearBiometricKey(context)
        context.config.vaultRootPath = ""
        context.config.vaultWrappedKeyData = ""
        context.config.vaultPublicKey = ""
        context.config.hiddenPasswordHash = ""
        context.config.isHiddenPasswordProtectionOn = false
        context.config.isVaultBiometricEnabled = false
        context.config.temporarilyShowHidden = false
        clearAppImageCaches(context)
    }

    fun hasVaultFolderOrCredentials(context: Context): Boolean {
        val standard = getDefaultVaultRoot()
        if (standard.exists() && standard.isDirectory) return true
        val legacy = File(getDocumentsDirectory(), CIPHER_SECRETS_DIR_NAME)
        if (legacy.exists() && legacy.isDirectory) return true
        if (context.config.vaultWrappedKeyData.isNotEmpty()) return true
        return getExistingCipherSecretsDir(context) != null
    }

    fun hasExistingVault(context: Context): Boolean {
        if (context.config.vaultWrappedKeyData.isNotEmpty()) return true
        return getExistingCipherSecretsDir(context) != null
    }

    // ==================== MASTER VAULT ROOT (STEALTH CONSOLIDATED CONTAINER) ====================

    fun getOrCreateVaultRoot(context: Context): File {
        cachedVaultRoot?.let { if (it.exists() && it.isDirectory) return it }

        val storedPath = context.config.vaultRootPath
        if (storedPath.isNotEmpty()) {
            val dir = File(storedPath)
            if (dir.exists() && dir.isDirectory) {
                cachedVaultRoot = dir
                return dir
            }
        }

        val existing = findVaultRoot(context)
        if (existing != null) {
            context.config.vaultRootPath = existing.absolutePath
            cachedVaultRoot = existing
            return existing
        }

        val newDir = getDefaultVaultRoot()
        if (!newDir.exists()) {
            newDir.mkdirs()
        }

        val noMedia = File(newDir, NOMEDIA)
        if (!noMedia.exists()) {
            try { noMedia.createNewFile() } catch (_: Exception) {}
        }

        context.config.vaultRootPath = newDir.absolutePath
        cachedVaultRoot = newDir
        vaultRootSearchDone = false
        return newDir
    }

    fun findVaultRoot(context: Context): File? {
        cachedVaultRoot?.let { if (it.exists() && it.isDirectory) return it }

        val storedPath = context.config.vaultRootPath
        if (storedPath.isNotEmpty()) {
            val dir = File(storedPath)
            if (dir.exists() && dir.isDirectory) {
                cachedVaultRoot = dir
                return dir
            }
        }

        val standardDir = getDefaultVaultRoot()
        if (standardDir.exists() && standardDir.isDirectory) {
            if (File(standardDir, VAULT_HEADER_FILE).exists() || File(standardDir, NOMEDIA).exists() || standardDir.listFiles()?.isNotEmpty() == true) {
                context.config.vaultRootPath = standardDir.absolutePath
                cachedVaultRoot = standardDir
                vaultRootSearchDone = false
                return standardDir
            }
        }

        val docDir = File(getDocumentsDirectory(), CIPHER_SECRETS_DIR_NAME)
        if (docDir.exists() && docDir.isDirectory) {
            if (File(docDir, VAULT_HEADER_FILE).exists() || File(docDir, NOMEDIA).exists() || docDir.listFiles()?.isNotEmpty() == true) {
                val targetDir = if (!standardDir.exists()) {
                    try {
                        standardDir.parentFile?.mkdirs()
                        if (docDir.renameTo(standardDir)) standardDir else docDir
                    } catch (_: Exception) {
                        docDir
                    }
                } else {
                    docDir
                }
                context.config.vaultRootPath = targetDir.absolutePath
                cachedVaultRoot = targetDir
                vaultRootSearchDone = false
                return targetDir
            }
        }

        if (vaultRootSearchDone) {
            return null
        }

        val documentsDir = getDocumentsDirectory()
        val candidates = listOfNotNull(
            documentsDir,
            File(documentsDir, CIPHER_ROOT_DIR_NAME),
            Environment.getExternalStorageDirectory(),
            File("/storage/emulated/0"),
            context.sdCardPath.takeIf { it.isNotEmpty() }?.let { File(it) }
        )
        for (root in candidates) {
            if (root.exists() && root.isDirectory) {
                val children = root.listFiles() ?: continue
                for (child in children) {
                    if (child.isDirectory && (File(child, VAULT_HEADER_FILE).exists() || (File(child, NOMEDIA).exists() && child.listFiles()?.any { isEncryptedFolder(it) } == true))) {
                        context.config.vaultRootPath = child.absolutePath
                        cachedVaultRoot = child
                        vaultRootSearchDone = false
                        return child
                    }
                }
            }
        }
        vaultRootSearchDone = true
        return null
    }

    // ==================== KEY MANAGEMENT (ARGON2id + AES-256-GCM + RSA-2048) ====================

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val BIOMETRIC_KEYSTORE_ALIAS = "vault_biometric_hw_key"

    fun isBiometricHardwareAvailable(context: Context): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Throwable) {
            false
        }
    }

    fun canUnlockWithBiometrics(context: Context): Boolean {
        return context.config.isVaultBiometricEnabled &&
            context.config.vaultBiometricWrappedBundle.isNotEmpty() &&
            isBiometricHardwareAvailable(context)
    }

    fun getOrCreateBiometricKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            if (!keyStore.containsAlias(BIOMETRIC_KEYSTORE_ALIAS)) {
                generateBiometricKeyInternal(keyStore)
            }
            keyStore.getKey(BIOMETRIC_KEYSTORE_ALIAS, null) as? SecretKey
        } catch (_: Throwable) {
            null
        }
    }

    private fun generateBiometricKeyInternal(keyStore: KeyStore) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        fun buildSpec(strongBox: Boolean): KeyGenParameterSpec {
            val builder = KeyGenParameterSpec.Builder(
                BIOMETRIC_KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(-1)
            }

            if (strongBox) {
                builder.setIsStrongBoxBacked(true)
            }
            return builder.build()
        }

        try {
            keyGenerator.init(buildSpec(strongBox = true))
            keyGenerator.generateKey()
        } catch (_: Throwable) {
            keyGenerator.init(buildSpec(strongBox = false))
            keyGenerator.generateKey()
        }
    }

    fun getBiometricEncryptCipher(): Cipher? {
        val key = getOrCreateBiometricKey() ?: return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher
        } catch (_: Throwable) {
            null
        }
    }

    fun completeBiometricSetup(context: Context, cipher: Cipher): Boolean {
        val masterKey = unlockedMasterKey ?: return false
        val privKey = unlockedPrivateKey ?: return false
        val privBytes = privKey.encoded ?: return false
        return try {
            val bundle = ByteArrayOutputStream().use { baos ->
                DataOutputStream(baos).use { dos ->
                    dos.write(masterKey)
                    dos.writeInt(privBytes.size)
                    dos.write(privBytes)
                }
                baos.toByteArray()
            }
            val encrypted = cipher.doFinal(bundle)
            bundle.fill(0)
            val iv = cipher.iv
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

            context.config.vaultBiometricWrappedBundle = Base64.encodeToString(combined, Base64.NO_WRAP)
            context.config.isVaultBiometricEnabled = true
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun getBiometricDecryptCipher(context: Context): Cipher? {
        val wrappedB64 = context.config.vaultBiometricWrappedBundle
        if (wrappedB64.isEmpty()) return null
        val combined = try {
            Base64.decode(wrappedB64, Base64.NO_WRAP)
        } catch (_: Throwable) {
            return null
        }
        if (combined.size <= GCM_IV_LENGTH) return null
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)

        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val key = keyStore.getKey(BIOMETRIC_KEYSTORE_ALIAS, null) as? SecretKey ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher
        } catch (e: KeyPermanentlyInvalidatedException) {
            clearBiometricKey(context)
            null
        } catch (_: Throwable) {
            null
        }
    }

    fun completeBiometricUnlock(context: Context, cipher: Cipher): Boolean {
        val wrappedB64 = context.config.vaultBiometricWrappedBundle
        if (wrappedB64.isEmpty()) return false
        val combined = try {
            Base64.decode(wrappedB64, Base64.NO_WRAP)
        } catch (_: Throwable) {
            return false
        }
        if (combined.size <= GCM_IV_LENGTH) return false
        val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        return try {
            val bundle = cipher.doFinal(encrypted)
            DataInputStream(ByteArrayInputStream(bundle)).use { dis ->
                val masterKey = ByteArray(32)
                dis.readFully(masterKey)
                val privLen = dis.readInt()
                val privBytes = ByteArray(privLen)
                dis.readFully(privBytes)
                val privKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(PKCS8EncodedKeySpec(privBytes))
                privBytes.fill(0)
                unlockedMasterKey = masterKey
                unlockedPrivateKey = privKey
            }
            bundle.fill(0)
            context.config.hiddenPasswordHash = "argon2id_aes256_gcm_v2"
            context.config.isHiddenPasswordProtectionOn = true
            findVaultRoot(context)?.let { context.config.vaultRootPath = it.absolutePath }
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun clearBiometricKey(context: Context) {
        context.config.isVaultBiometricEnabled = false
        context.config.vaultBiometricWrappedBundle = ""
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            if (keyStore.containsAlias(BIOMETRIC_KEYSTORE_ALIAS)) {
                keyStore.deleteEntry(BIOMETRIC_KEYSTORE_ALIAS)
            }
        } catch (_: Throwable) {}
    }

    fun deriveKeyArgon2id(
        secret: String,
        salt: ByteArray,
        memoryKb: Int = ARGON2_MEMORY_KB,
        iterations: Int = ARGON2_ITERATIONS,
        parallelism: Int = ARGON2_PARALLELISM
    ): SecretKeySpec {
        val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt)
            .withIterations(iterations)
            .withMemoryAsKB(memoryKb)
            .withParallelism(parallelism)
        val generator = Argon2BytesGenerator()
        generator.init(builder.build())
        val keyBytes = ByteArray(32)
        val secretBytes = secret.toByteArray(Charsets.UTF_8)
        try {
            generator.generateBytes(secretBytes, keyBytes, 0, keyBytes.size)
            val spec = SecretKeySpec(keyBytes, "AES")
            return spec
        } finally {
            secretBytes.fill(0)
            keyBytes.fill(0)
        }
    }

    private fun deriveWrappingKey(secret: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val chars = secret.toCharArray()
        val spec = PBEKeySpec(chars, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        try {
            val keyBytes = factory.generateSecret(spec).encoded
            val result = SecretKeySpec(keyBytes, "AES")
            keyBytes.fill(0)
            return result
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    private fun getOaepSpec(): OAEPParameterSpec {
        return OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
        )
    }

    private fun wrapKeyWithPublicKey(publicKeyBytes: ByteArray, ephemeralKey: ByteArray): ByteArray {
        val pubKey: PublicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(publicKeyBytes))
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, pubKey, getOaepSpec())
        return cipher.doFinal(ephemeralKey)
    }

    private fun unwrapKeyWithPrivateKey(privateKey: PrivateKey, wrappedKey: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey, getOaepSpec())
        return cipher.doFinal(wrappedKey)
    }

    private fun writeVaultHeader(
        headerFile: File,
        salt: ByteArray,
        memoryKb: Int,
        iterations: Int,
        parallelism: Int,
        iv: ByteArray,
        wrappedBundle: ByteArray,
        protectionType: Int = PROTECTION_PATTERN
    ) {
        FileOutputStream(headerFile).use { fos ->
            DataOutputStream(fos).use { dos ->
                dos.write(VAULT_HEADER_MAGIC.toByteArray(Charsets.US_ASCII))
                dos.writeByte(3) // Version 3
                dos.writeByte(1) // KDF: 1 = Argon2id
                dos.writeInt(iterations)
                dos.writeInt(memoryKb)
                dos.writeInt(parallelism)
                dos.writeByte(protectionType)
                dos.writeInt(salt.size)
                dos.write(salt)
                dos.write(iv)
                dos.writeInt(wrappedBundle.size)
                dos.write(wrappedBundle)
                dos.flush()
                fos.fd.sync()
            }
        }
    }

    private fun unlockFromHeaderFile(context: Context, headerFile: File, secret: String): Boolean {
        if (!headerFile.exists()) return false
        return try {
            FileInputStream(headerFile).use { fis ->
                DataInputStream(BufferedInputStream(fis)).use { dis ->
                    val magicBytes = ByteArray(VAULT_HEADER_MAGIC.length)
                    dis.readFully(magicBytes)
                    if (String(magicBytes, Charsets.US_ASCII) != VAULT_HEADER_MAGIC) return false

                    val version = dis.readByte().toInt()
                    val kdfType = dis.readByte()
                    val iterations = dis.readInt()
                    val memoryKb = dis.readInt()
                    val parallelism = dis.readInt()
                    val storedProtectionType = if (version >= 3) dis.readByte().toInt() else -1
                    val saltLen = dis.readInt()
                    if (saltLen <= 0 || saltLen > 256) return false
                    val salt = ByteArray(saltLen)
                    dis.readFully(salt)
                    val iv = ByteArray(GCM_IV_LENGTH)
                    dis.readFully(iv)
                    val wrappedLen = dis.readInt()
                    if (wrappedLen <= 0 || wrappedLen > 65536) return false
                    val wrapped = ByteArray(wrappedLen)
                    dis.readFully(wrapped)

                    if (kdfType == 1.toByte()) {
                        if (memoryKb !in 8_192..262_144 || iterations !in 1..10 || parallelism !in 1..8) {
                            return false
                        }
                    }

                    val wrappingKey = if (kdfType == 1.toByte()) {
                        deriveKeyArgon2id(secret, salt, memoryKb, iterations, parallelism)
                    } else {
                        deriveWrappingKey(secret, salt)
                    }

                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_BITS, iv))
                    val bundle = cipher.doFinal(wrapped)

                    val masterKey = ByteArray(32)
                    DataInputStream(ByteArrayInputStream(bundle)).use { bdis ->
                        bdis.readFully(masterKey)
                        val privLen = bdis.readInt()
                        val privBytes = ByteArray(privLen)
                        bdis.readFully(privBytes)
                        val privKey = KeyFactory.getInstance("RSA")
                            .generatePrivate(PKCS8EncodedKeySpec(privBytes))
                        privBytes.fill(0)
                        unlockedMasterKey = masterKey
                        unlockedPrivateKey = privKey
                    }
                    bundle.fill(0)



                    context.config.hiddenPasswordHash = "argon2id_aes256_gcm_v2"
                    context.config.isHiddenPasswordProtectionOn = true
                    if (storedProtectionType in listOf(PROTECTION_PATTERN, PROTECTION_PIN, PROTECTION_PASSWORD)) {
                        context.config.hiddenProtectionType = storedProtectionType
                    }
                    if (version < 3) {
                        try {
                            setupOrUpdateCredentials(context, secret, context.config.hiddenProtectionType)
                        } catch (_: Exception) {}
                    }
                    true
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    fun getVaultProtectionType(context: Context): Int? {
        val vaultRoot = findVaultRoot(context) ?: return null
        val headerFile = File(vaultRoot, VAULT_HEADER_FILE)
        if (!headerFile.exists()) return null
        return try {
            FileInputStream(headerFile).use { fis ->
                DataInputStream(BufferedInputStream(fis)).use { dis ->
                    val magicBytes = ByteArray(VAULT_HEADER_MAGIC.length)
                    dis.readFully(magicBytes)
                    if (String(magicBytes, Charsets.US_ASCII) != VAULT_HEADER_MAGIC) return null
                    val version = dis.readByte().toInt()
                    if (version >= 3) {
                        dis.readByte() // kdfType
                        dis.readInt()  // iterations
                        dis.readInt()  // memoryKb
                        dis.readInt()  // parallelism
                        val protType = dis.readByte().toInt()
                        if (protType in listOf(PROTECTION_PATTERN, PROTECTION_PIN, PROTECTION_PASSWORD)) {
                            protType
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Initializes or updates the user's hidden item password/PIN/pattern credentials.
     * Uses Argon2id (32 MB, 3 iterations, 2 threads) + AES-256-GCM.
     * The master bundle (MasterKey + PrivateKey) is written directly to .vault in the root folder.
     */
    fun setupOrUpdateCredentials(context: Context, secret: String, protectionType: Int): String {
        if (hasExistingVault(context) && !isVaultUnlocked) {
            throw IllegalStateException("Vault is locked! Cannot update credentials without unlocking first.")
        }
        val masterKey = unlockedMasterKey ?: ByteArray(32).also { secureRandom.nextBytes(it) }
        val privateKey: PrivateKey
        val publicKeyBytes: ByteArray

        val existingPriv = unlockedPrivateKey
        val existingPubB64 = context.config.vaultPublicKey
        if (existingPriv != null && existingPubB64.isNotEmpty()) {
            privateKey = existingPriv
            publicKeyBytes = Base64.decode(existingPubB64, Base64.NO_WRAP)
        } else {
            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(RSA_KEY_BITS, secureRandom)
            val kp = kpg.generateKeyPair()
            privateKey = kp.private
            publicKeyBytes = kp.public.encoded
            context.config.vaultPublicKey = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)
        }

        val privBytes = privateKey.encoded
        val bundle = ByteArrayOutputStream().use { baos ->
            DataOutputStream(baos).use { dos ->
                dos.write(masterKey)
                dos.writeInt(privBytes.size)
                dos.write(privBytes)
            }
            baos.toByteArray()
        }

        val salt = ByteArray(ARGON2_SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val wrappingKey = deriveKeyArgon2id(secret, salt, ARGON2_MEMORY_KB, ARGON2_ITERATIONS, ARGON2_PARALLELISM)
        val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val wrapped = cipher.doFinal(bundle)
        bundle.fill(0)

        clearBiometricKey(context)

        // Write vault root header (.vault) on disk for full multi-device portability
        val vaultRoot = getOrCreateVaultRoot(context)
        val vaultHeader = File(vaultRoot, VAULT_HEADER_FILE)
        writeVaultHeader(vaultHeader, salt, ARGON2_MEMORY_KB, ARGON2_ITERATIONS, ARGON2_PARALLELISM, iv, wrapped, protectionType)

        val data = Base64.encodeToString(salt, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(wrapped, Base64.NO_WRAP)
        context.config.vaultWrappedKeyData = data
        context.config.vaultRootPath = vaultRoot.absolutePath
        cachedVaultRoot = vaultRoot
        vaultRootSearchDone = false
        encryptedFolderCache.clear()

        unlockedMasterKey = masterKey.copyOf()
        unlockedPrivateKey = privateKey
        val marker = "argon2id_aes256_gcm_v2"
        context.config.hiddenPasswordHash = marker
        context.config.hiddenProtectionType = protectionType
        context.config.isHiddenPasswordProtectionOn = true

        // Ensure any existing hidden virtual albums have their names encrypted in SQLite
        ensureBackgroundThread {
            try {
                getOrCreateDefaultHiddenAlbum(context)
                context.virtualAlbumsDB.getAllAlbums().filter { it.isHidden && !isEncryptedVaultString(it.name) }.forEach { album ->
                    val albumId = album.id ?: return@forEach
                    val encName = encryptStringForVault(context, album.name)
                    context.virtualAlbumsDB.updateAlbumVisibilityAndName(albumId, true, encName)
                }
            } catch (_: Exception) {
            }
        }

        return marker
    }

    fun isEncryptedVaultString(text: String): Boolean {
        return text.startsWith(VAULT_STR_PREFIX)
    }

    fun encryptStringForVault(context: Context, plainText: String): String {
        if (isEncryptedVaultString(plainText)) return plainText
        val masterKey = unlockedMasterKey
        if (masterKey != null) {
            val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(masterKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val payload = ByteArrayOutputStream().use { baos ->
                DataOutputStream(baos).use { dos ->
                    dos.writeByte(VERSION_SYMMETRIC.toInt())
                    dos.write(iv)
                    dos.writeInt(encrypted.size)
                    dos.write(encrypted)
                }
                baos.toByteArray()
            }
            return VAULT_STR_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
        }

        val pubKeyB64 = context.config.vaultPublicKey
        if (pubKeyB64.isNotEmpty()) {
            return try {
                val pubKeyBytes = Base64.decode(pubKeyB64, Base64.NO_WRAP)
                val ephemeralKey = ByteArray(32).also { secureRandom.nextBytes(it) }
                val wrappedKey = wrapKeyWithPublicKey(pubKeyBytes, ephemeralKey)
                val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(ephemeralKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
                val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
                ephemeralKey.fill(0)

                val payload = ByteArrayOutputStream().use { baos ->
                    DataOutputStream(baos).use { dos ->
                        dos.writeByte(VERSION_ASYMMETRIC.toInt())
                        dos.writeInt(wrappedKey.size)
                        dos.write(wrappedKey)
                        dos.write(iv)
                        dos.writeInt(encrypted.size)
                        dos.write(encrypted)
                    }
                    baos.toByteArray()
                }
                VAULT_STR_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
            } catch (_: Exception) {
                plainText
            }
        }
        return plainText
    }

    fun decryptStringFromVault(storedText: String): String? {
        if (!isEncryptedVaultString(storedText)) return storedText
        val masterKey = unlockedMasterKey ?: return null
        val b64 = storedText.removePrefix(VAULT_STR_PREFIX)
        return try {
            val raw = Base64.decode(b64, Base64.NO_WRAP)
            DataInputStream(ByteArrayInputStream(raw)).use { dis ->
                val version = dis.readByte()
                val aesKey: ByteArray = when (version) {
                    VERSION_SYMMETRIC -> masterKey
                    VERSION_ASYMMETRIC -> {
                        val privKey = unlockedPrivateKey ?: return null
                        val wrappedLen = dis.readInt()
                        if (wrappedLen <= 0 || wrappedLen > 2048) return null
                        val wrappedKey = ByteArray(wrappedLen)
                        dis.readFully(wrappedKey)
                        unwrapKeyWithPrivateKey(privKey, wrappedKey)
                    }
                    else -> return null
                }
                val iv = ByteArray(GCM_IV_LENGTH)
                dis.readFully(iv)
                val len = dis.readInt()
                if (len <= 0 || len > 8192) return null
                val encrypted = ByteArray(len)
                dis.readFully(encrypted)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
                val plain = String(cipher.doFinal(encrypted), Charsets.UTF_8)
                if (version == VERSION_ASYMMETRIC) {
                    aesKey.fill(0)
                }
                plain
            }
        } catch (_: Exception) {
            null
        }
    }

    fun unlockWithSecret(context: Context, secret: String): Boolean {
        // 1. Try to find vault root from disk (enables unlocking a transferred vault on a new device!)
        val vaultRoot = findVaultRoot(context)
        if (vaultRoot != null) {
            val headerFile = File(vaultRoot, VAULT_HEADER_FILE)
            if (headerFile.exists()) {
                val unlocked = unlockFromHeaderFile(context, headerFile, secret)
                if (unlocked) {
                    context.config.vaultRootPath = vaultRoot.absolutePath
                    return true
                }
            }
        }

        // 2. Fallback to SharedPreferences wrappedData (PBKDF2 or cached Argon2id)
        val wrappedData = context.config.vaultWrappedKeyData
        if (wrappedData.isNotEmpty()) {
            try {
                val parts = wrappedData.split(":")
                if (parts.size == 3) {
                    val salt = Base64.decode(parts[0], Base64.NO_WRAP)
                    val iv = Base64.decode(parts[1], Base64.NO_WRAP)
                    val ciphertext = Base64.decode(parts[2], Base64.NO_WRAP)

                    // Try Argon2id first
                    var bundle: ByteArray? = try {
                        val wrappingKey = deriveKeyArgon2id(secret, salt)
                        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                        cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_BITS, iv))
                        cipher.doFinal(ciphertext)
                    } catch (_: Exception) {
                        null
                    }

                    // Fallback to PBKDF2
                    if (bundle == null) {
                        bundle = try {
                            val wrappingKey = deriveWrappingKey(secret, salt)
                            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                            cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_BITS, iv))
                            cipher.doFinal(ciphertext)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    if (bundle != null) {
                        DataInputStream(ByteArrayInputStream(bundle)).use { dis ->
                            val masterKey = ByteArray(32)
                            dis.readFully(masterKey)
                            val privLen = dis.readInt()
                            val privBytes = ByteArray(privLen)
                            dis.readFully(privBytes)
                            val privKey = KeyFactory.getInstance("RSA")
                                .generatePrivate(PKCS8EncodedKeySpec(privBytes))
                            privBytes.fill(0)
                            unlockedMasterKey = masterKey
                            unlockedPrivateKey = privKey
                        }
                        bundle.fill(0)

                        // Upgrade to Argon2id + write .vault in vault root if needed
                        setupOrUpdateCredentials(context, secret, context.config.hiddenProtectionType)
                        return true
                    }
                }
            } catch (_: Exception) {
            }
        }

        // Legacy fallback if user had a SHA-1 hash before vault initialization
        val expectedHash = context.config.hiddenPasswordHash
        val legacySha1 = secret.getHash()
        if (expectedHash.isNotEmpty() && expectedHash == legacySha1) {
            setupOrUpdateCredentials(context, secret, context.config.hiddenProtectionType)
            return true
        }
        return false
    }

    private fun String.getHash(): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val bytes = digest.digest(this.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ==================== FOLDER METADATA ENCRYPTION (.meta & .fmeta) ====================

    private fun writeEncryptedFolderMeta(context: Context, folder: File, originalName: String) {
        val metaFile = File(folder, VAULT_SUBFOLDER_META_FILE)
        val masterKey = unlockedMasterKey
        if (masterKey != null) {
            val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(masterKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            val encryptedName = cipher.doFinal(originalName.toByteArray(Charsets.UTF_8))

            FileOutputStream(metaFile).use { fos ->
                DataOutputStream(fos).use { dos ->
                    dos.write(META_MAGIC_V2.toByteArray(Charsets.US_ASCII))
                    dos.write(iv)
                    dos.writeInt(encryptedName.size)
                    dos.write(encryptedName)
                    dos.flush()
                    fos.fd.sync()
                }
            }
            folderNameCache[folder.absolutePath] = originalName
        } else {
            val pubKeyB64 = context.config.vaultPublicKey
            if (pubKeyB64.isEmpty()) return
            val pubKeyBytes = Base64.decode(pubKeyB64, Base64.NO_WRAP)
            val ephemeralKey = ByteArray(32).also { secureRandom.nextBytes(it) }
            val wrappedKey = wrapKeyWithPublicKey(pubKeyBytes, ephemeralKey)

            val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(ephemeralKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            val encryptedName = cipher.doFinal(originalName.toByteArray(Charsets.UTF_8))
            ephemeralKey.fill(0)

            FileOutputStream(metaFile).use { fos ->
                DataOutputStream(fos).use { dos ->
                    dos.write(META_MAGIC_ASYM.toByteArray(Charsets.US_ASCII))
                    dos.writeInt(wrappedKey.size)
                    dos.write(wrappedKey)
                    dos.write(iv)
                    dos.writeInt(encryptedName.size)
                    dos.write(encryptedName)
                    dos.flush()
                    fos.fd.sync()
                }
            }
            folderNameCache[folder.absolutePath] = originalName
        }
    }

    fun createHiddenPhysicalAlbum(context: Context, originalName: String): File? {
        if (unlockedMasterKey == null && context.config.vaultPublicKey.isEmpty()) {
            return null
        }
        val vaultRoot = getOrCreateVaultRoot(context)
        var subfolder = File(vaultRoot, randomHex(6))
        while (subfolder.exists()) {
            subfolder = File(vaultRoot, randomHex(6))
        }
        if (!subfolder.mkdirs()) return null

        val noMedia = File(subfolder, NOMEDIA)
        if (!noMedia.exists()) {
            try { noMedia.createNewFile() } catch (_: Exception) {}
        }

        writeEncryptedFolderMeta(context, subfolder, originalName)
        folderNameCache[subfolder.absolutePath] = originalName
        encryptedFolderCache[subfolder.absolutePath] = true
        return subfolder
    }

    fun getHiddenPhysicalAlbums(context: Context): List<VaultPhysicalAlbum> {
        val albums = ArrayList<VaultPhysicalAlbum>()
        val vaultRoot = getOrCreateVaultRoot(context)

        // 1. Vault Root (Main Vault album)
        val rootFiles = vaultRoot.listFiles() ?: emptyArray()
        val rootBinCount = rootFiles.count { isEncryptedFile(it.name) }
        albums.add(
            VaultPhysicalAlbum(
                name = context.getString(org.fossify.gallery.R.string.secret_vault),
                fullPath = vaultRoot.absolutePath,
                folder = vaultRoot,
                itemCount = rootBinCount
            )
        )

        // 2. Physical subfolders in vaultRoot (excluding virtual albums JSON/ and hidden/system folders)
        rootFiles.filter { it.isDirectory }.forEach { subfolder ->
            val folderName = subfolder.name
            if (folderName.equals("JSON", ignoreCase = true) || folderName.startsWith(".")) {
                return@forEach
            }
            if (isVaultRecycleBin(subfolder)) {
                return@forEach
            }

            val decryptedName = getDecryptedFolderName(subfolder, context) ?: folderName
            val binCount = subfolder.listFiles()?.count { isEncryptedFile(it.name) } ?: 0
            albums.add(
                VaultPhysicalAlbum(
                    name = decryptedName,
                    fullPath = subfolder.absolutePath,
                    folder = subfolder,
                    itemCount = binCount
                )
            )
        }

        // 3. Custom external encrypted physical folders from config
        for (path in context.config.encryptedFolders) {
            val folder = File(path)
            if (folder.exists() && folder.isDirectory && folder.absolutePath != vaultRoot.absolutePath) {
                if (albums.none { it.fullPath == folder.absolutePath }) {
                    val decryptedName = getDecryptedFolderName(folder) ?: folder.name
                    val binCount = folder.listFiles()?.count { isEncryptedFile(it.name) } ?: 0
                    albums.add(
                        VaultPhysicalAlbum(
                            name = decryptedName,
                            fullPath = folder.absolutePath,
                            folder = folder,
                            itemCount = binCount
                        )
                    )
                }
            }
        }

        return albums
    }

    fun getDecryptedRawFolderName(folder: File): String? {
        folderNameCache[folder.absolutePath]?.let { return it }
        val masterKey = unlockedMasterKey
        val privKey = unlockedPrivateKey
        if (masterKey == null && privKey == null) return null

        val metaFile = when {
            File(folder, VAULT_SUBFOLDER_META_FILE).exists() -> File(folder, VAULT_SUBFOLDER_META_FILE)
            File(folder, ENCRYPTED_FOLDER_META_FILE).exists() -> File(folder, ENCRYPTED_FOLDER_META_FILE)
            else -> return null
        }

        return try {
            BufferedInputStream(FileInputStream(metaFile)).use { bis ->
                bis.mark(16)
                val headerBytes = ByteArray(8)
                val bytesRead = bis.read(headerBytes, 0, 8)
                if (bytesRead < 7) return null
                val str7 = String(headerBytes, 0, 7, Charsets.US_ASCII)
                val str8 = if (bytesRead >= 8) String(headerBytes, 0, 8, Charsets.US_ASCII) else ""

                bis.reset()
                val (magic, skipLen) = when {
                    str8 == META_MAGIC_ASYM -> Pair(META_MAGIC_ASYM, 8)
                    str8 == META_MAGIC_SYM -> Pair(META_MAGIC_SYM, 8)
                    str7 == META_MAGIC_V2 -> Pair(META_MAGIC_V2, 7)
                    else -> return null
                }
                bis.skip(skipLen.toLong())

                DataInputStream(bis).use { dis ->
                    val aesKey: ByteArray = when (magic) {
                        META_MAGIC_V2, META_MAGIC_SYM -> masterKey ?: return null
                        META_MAGIC_ASYM -> {
                            val pk = privKey ?: return null
                            val wrappedLen = dis.readInt()
                            if (wrappedLen <= 0 || wrappedLen > 2048) return null
                            val wrappedKey = ByteArray(wrappedLen)
                            dis.readFully(wrappedKey)
                            unwrapKeyWithPrivateKey(pk, wrappedKey)
                        }
                        else -> return null
                    }
                    val iv = ByteArray(GCM_IV_LENGTH)
                    dis.readFully(iv)
                    val len = dis.readInt()
                    if (len <= 0 || len > 4096) return null
                    val encryptedName = ByteArray(len)
                    dis.readFully(encryptedName)
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
                    val name = String(cipher.doFinal(encryptedName), Charsets.UTF_8)
                    if (magic == META_MAGIC_ASYM) {
                        aesKey.fill(0)
                    }
                    folderNameCache[folder.absolutePath] = name
                    name
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isDefaultAlbumRawName(raw: String?): Boolean {
        return raw == VAULT_DEFAULT_ALBUM_NAME ||
            raw == "Varsayılan Gizli Albüm" ||
            raw == "Default Hidden Album" ||
            raw == DEFAULT_SECRET_ALBUM_NAME
    }

    fun isDefaultHiddenAlbum(folder: File): Boolean {
        return isDefaultAlbumRawName(getDecryptedRawFolderName(folder))
    }

    fun isDefaultHiddenAlbumPath(path: String): Boolean {
        if (path.isEmpty()) return false
        val folder = File(path.trimEnd('/'))
        return folder.exists() && folder.isDirectory && isDefaultHiddenAlbum(folder)
    }

    fun renameEncryptedFolder(context: Context, folder: File, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || !folder.exists() || !folder.isDirectory) return false
        if (!isEncryptedFolder(folder) || isVaultRecycleBin(folder) || isDefaultHiddenAlbum(folder)) return false
        if (unlockedMasterKey == null && context.config.vaultPublicKey.isEmpty()) return false
        return try {
            writeEncryptedFolderMeta(context, folder, trimmed)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getDecryptedFolderName(folder: File, context: Context? = null): String? {
        val raw = getDecryptedRawFolderName(folder) ?: return null
        if (raw == VAULT_RECYCLE_BIN_NAME) {
            return context?.getString(org.fossify.commons.R.string.recycle_bin) ?: "Recycle Bin"
        }
        if (isDefaultAlbumRawName(raw)) {
            return DEFAULT_SECRET_ALBUM_NAME
        }
        return raw
    }

    fun getOrCreateDefaultHiddenAlbum(context: Context): File? {
        if (unlockedMasterKey == null && context.config.vaultPublicKey.isEmpty()) {
            return null
        }
        val vaultRoot = getOrCreateVaultRoot(context)

        val subfolders = vaultRoot.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") } ?: emptyList()
        for (sub in subfolders) {
            if (isDefaultHiddenAlbum(sub)) {
                return sub
            }
        }

        return createHiddenPhysicalAlbum(context, VAULT_DEFAULT_ALBUM_NAME)
    }

    fun getVaultDestinationAlbums(context: Context): List<VaultPhysicalAlbum> {
        val albums = ArrayList<VaultPhysicalAlbum>()
        val defaultAlbum = getOrCreateDefaultHiddenAlbum(context)
        val defaultAlbumPath = defaultAlbum?.absolutePath
        val vaultRoot = findVaultRoot(context) ?: return emptyList()

        if (defaultAlbum != null && defaultAlbum.exists()) {
            val count = defaultAlbum.listFiles()?.count { isEncryptedFile(it.name) } ?: 0
            albums.add(
                VaultPhysicalAlbum(
                    name = DEFAULT_SECRET_ALBUM_NAME,
                    fullPath = defaultAlbum.absolutePath,
                    folder = defaultAlbum,
                    itemCount = count
                )
            )
        }

        val otherAlbums = ArrayList<VaultPhysicalAlbum>()
        vaultRoot.listFiles()?.filter { it.isDirectory }?.forEach { subfolder ->
            if (subfolder.name.startsWith(".") || subfolder.name.equals("JSON", ignoreCase = true)) return@forEach
            if (isVaultRecycleBin(subfolder)) return@forEach
            if (subfolder.absolutePath == defaultAlbumPath) return@forEach

            val decryptedName = getDecryptedFolderName(subfolder, context) ?: subfolder.name
            val binCount = subfolder.listFiles()?.count { isEncryptedFile(it.name) } ?: 0
            otherAlbums.add(
                VaultPhysicalAlbum(
                    name = decryptedName,
                    fullPath = subfolder.absolutePath,
                    folder = subfolder,
                    itemCount = binCount
                )
            )
        }

        for (path in context.config.encryptedFolders) {
            val folder = File(path)
            if (folder.exists() && folder.isDirectory && folder.absolutePath != vaultRoot.absolutePath && folder.absolutePath != defaultAlbumPath) {
                if (otherAlbums.none { it.fullPath == folder.absolutePath } && albums.none { it.fullPath == folder.absolutePath }) {
                    val decryptedName = getDecryptedFolderName(folder, context) ?: folder.name
                    val binCount = folder.listFiles()?.count { isEncryptedFile(it.name) } ?: 0
                    otherAlbums.add(
                        VaultPhysicalAlbum(
                            name = decryptedName,
                            fullPath = folder.absolutePath,
                            folder = folder,
                            itemCount = binCount
                        )
                    )
                }
            }
        }

        otherAlbums.sortBy { it.name.lowercase() }
        albums.addAll(otherAlbums)
        return albums
    }



    // ==================== VAULT RECYCLE BIN (MODE 2) ====================

    fun isVaultRecycleBin(folder: File): Boolean {
        if (vaultRecycleBinFolder != null && folder.absolutePath == vaultRecycleBinFolder?.absolutePath) {
            return true
        }
        val raw = getDecryptedRawFolderName(folder)
        if (raw == VAULT_RECYCLE_BIN_NAME) {
            vaultRecycleBinFolder = folder
            return true
        }
        return false
    }

    fun getVaultRecycleBin(context: Context): File? {
        vaultRecycleBinFolder?.let {
            if (it.exists() && it.isDirectory) return it
        }
        val vaultRoot = findVaultRoot(context) ?: return null
        val subfolders = vaultRoot.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") } ?: return null
        for (folder in subfolders) {
            if (isVaultRecycleBin(folder)) {
                vaultRecycleBinFolder = folder
                return folder
            }
        }
        return null
    }

    fun getOrCreateVaultRecycleBin(context: Context): File {
        getVaultRecycleBin(context)?.let { return it }
        val vaultRoot = getOrCreateVaultRoot(context)
        var binFolder: File
        do {
            binFolder = File(vaultRoot, randomHex(6))
        } while (binFolder.exists())
        binFolder.mkdirs()
        try {
            File(binFolder, NOMEDIA).createNewFile()
        } catch (_: Exception) {}
        writeEncryptedFolderMeta(context, binFolder, VAULT_RECYCLE_BIN_NAME)
        vaultRecycleBinFolder = binFolder
        return binFolder
    }

    fun isVaultRecycleBinPath(context: Context, path: String): Boolean {
        if (path.isEmpty()) return false
        val cleanPath = path.trimEnd('/')
        val binFolder = getVaultRecycleBin(context)
        if (binFolder != null) {
            val binPath = binFolder.absolutePath
            if (cleanPath == binPath || cleanPath.startsWith("$binPath/")) return true
        }
        return false
    }

    fun isVaultRecycleBinPath(path: String): Boolean {
        if (path.isEmpty()) return false
        val clean = path.trimEnd('/')
        vaultRecycleBinFolder?.let { binFolder ->
            val binPath = binFolder.absolutePath
            if (clean == binPath || clean.startsWith("$binPath/")) return true
        }
        val file = File(clean)
        val folder = if (file.isDirectory) file else file.parentFile
        if (folder != null && folder.exists()) {
            return isVaultRecycleBin(folder)
        }
        return false
    }

    fun getVaultRecycleBinCount(context: Context): Int {
        val binFolder = getVaultRecycleBin(context) ?: return 0
        return binFolder.listFiles()?.count { isEncryptedFile(it.name) } ?: 0
    }

    fun getVaultRecycleBinRestoreMap(context: Context): HashMap<String, String> {
        val binFolder = getVaultRecycleBin(context) ?: return HashMap()
        val restoreFile = File(binFolder, VAULT_RESTORE_INFO_FILE)
        if (!restoreFile.exists()) return HashMap()
        return try {
            val encryptedContent = restoreFile.readText(Charsets.UTF_8).trim()
            if (encryptedContent.isEmpty()) return HashMap()
            val json = decryptStringFromVault(encryptedContent) ?: return HashMap()
            val type = object : TypeToken<HashMap<String, String>>() {}.type
            Gson().fromJson(json, type) ?: HashMap()
        } catch (_: Exception) {
            HashMap()
        }
    }

    fun saveVaultRecycleBinRestoreMap(context: Context, map: HashMap<String, String>) {
        val binFolder = getOrCreateVaultRecycleBin(context)
        val restoreFile = File(binFolder, VAULT_RESTORE_INFO_FILE)
        if (map.isEmpty()) {
            if (restoreFile.exists()) {
                restoreFile.delete()
            }
            return
        }
        try {
            val json = Gson().toJson(map)
            val encryptedContent = encryptStringForVault(context, json)
            restoreFile.writeText(encryptedContent, Charsets.UTF_8)
        } catch (_: Exception) {}
    }

    fun moveEncryptedFilesToVaultRecycleBin(context: Context, paths: List<String>): Boolean {
        if (paths.isEmpty()) return true
        val binFolder = getOrCreateVaultRecycleBin(context)
        val restoreMap = getVaultRecycleBinRestoreMap(context)
        var success = true

        for (path in paths) {
            val sourceFile = File(path)
            if (!sourceFile.exists() || !isEncryptedFile(sourceFile.name)) continue
            val parentPath = sourceFile.parent ?: continue
            if (parentPath == binFolder.absolutePath) continue

            var destFile = File(binFolder, sourceFile.name)
            while (destFile.exists()) {
                destFile = File(binFolder, "${randomHex(8)}$ENCRYPTED_BIN_EXT")
            }

            val destPath = destFile.absolutePath
            if (sourceFile.renameTo(destFile)) {
                restoreMap[destFile.name] = parentPath
                context.updateVirtualAlbumItemPath(path, destPath)
                metadataCache.remove(path)?.let { meta ->
                    metadataCache[destPath] = meta
                }
            } else {
                success = false
            }
        }

        saveVaultRecycleBinRestoreMap(context, restoreMap)
        return success
    }

    fun restoreVaultRecycleBinFiles(context: Context, paths: List<String>): Int {
        if (paths.isEmpty()) return 0
        val binFolder = getVaultRecycleBin(context) ?: return 0
        val restoreMap = getVaultRecycleBinRestoreMap(context)
        val vaultRoot = getOrCreateVaultRoot(context)
        var restoredCount = 0

        for (path in paths) {
            val binFile = File(path)
            if (!binFile.exists() || binFile.parentFile?.absolutePath != binFolder.absolutePath) continue

            val originalParentPath = restoreMap[binFile.name]
            var targetFolder = if (!originalParentPath.isNullOrEmpty()) File(originalParentPath) else null

            if (targetFolder == null || !targetFolder.exists() || !targetFolder.isDirectory) {
                val existingFolders = vaultRoot.listFiles()?.filter {
                    it.isDirectory && !it.name.startsWith(".") && !isVaultRecycleBin(it)
                }
                targetFolder = existingFolders?.firstOrNull() ?: run {
                    var newSub = File(vaultRoot, randomHex(6))
                    while (newSub.exists()) {
                        newSub = File(vaultRoot, randomHex(6))
                    }
                    newSub.mkdirs()
                    try { File(newSub, NOMEDIA).createNewFile() } catch (_: Exception) {}
                    writeEncryptedFolderMeta(context, newSub, "Restored")
                    newSub
                }
            }

            var targetFile = File(targetFolder, binFile.name)
            while (targetFile.exists()) {
                targetFile = File(targetFolder, "${randomHex(8)}$ENCRYPTED_BIN_EXT")
            }

            val targetPath = targetFile.absolutePath
            if (binFile.renameTo(targetFile)) {
                restoreMap.remove(binFile.name)
                context.updateVirtualAlbumItemPath(path, targetPath)
                metadataCache.remove(path)?.let { meta ->
                    metadataCache[targetPath] = meta
                }
                restoredCount++
            }
        }

        saveVaultRecycleBinRestoreMap(context, restoreMap)
        return restoredCount
    }

    fun permanentlyDeleteVaultRecycleBinFiles(context: Context, paths: List<String>): Int {
        if (paths.isEmpty()) return 0
        val binFolder = getVaultRecycleBin(context) ?: return 0
        val restoreMap = getVaultRecycleBinRestoreMap(context)
        var deletedCount = 0

        for (path in paths) {
            val binFile = File(path)
            if (!binFile.exists() || binFile.parentFile?.absolutePath != binFolder.absolutePath) continue

            metadataCache.remove(path)
            context.deleteVirtualAlbumItemFromAllAlbums(path)
            secureWipeAndDeleteFile(context, binFile)
            restoreMap.remove(binFile.name)
            deletedCount++
        }

        saveVaultRecycleBinRestoreMap(context, restoreMap)
        return deletedCount
    }

    fun emptyVaultRecycleBin(context: Context): Boolean {
        val binFolder = getVaultRecycleBin(context) ?: return true
        val binFiles = binFolder.listFiles()?.filter { isEncryptedFile(it.name) } ?: emptyList()
        for (file in binFiles) {
            val path = file.absolutePath
            metadataCache.remove(path)
            context.deleteVirtualAlbumItemFromAllAlbums(path)
            secureWipeAndDeleteFile(context, file)
        }
        val restoreFile = File(binFolder, VAULT_RESTORE_INFO_FILE)
        if (restoreFile.exists()) {
            restoreFile.delete()
        }
        return true
    }

    // ==================== FILE ENCRYPTION & DECRYPTION ====================

    private fun getMediaTypeForName(name: String): Int {
        return when {
            name.isGif() -> TYPE_GIFS
            name.isRawFast() -> TYPE_RAWS
            name.isVideoFast() -> TYPE_VIDEOS
            name.isSvg() -> TYPE_SVGS
            else -> TYPE_IMAGES
        }
    }

    private fun generateEmbeddedThumbnail(file: File, mediaType: Int): ByteArray {
        return try {
            val bitmap: Bitmap? = if (mediaType == TYPE_VIDEOS) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(file.absolutePath)
                    retriever.getFrameAtTime(-1)
                } finally {
                    try {
                        retriever.release()
                    } catch (_: Exception) {
                    }
                }
            } else {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, opts)
                var sampleSize = 1
                while (opts.outWidth / sampleSize > THUMB_MAX_DIMENSION * 2 ||
                    opts.outHeight / sampleSize > THUMB_MAX_DIMENSION * 2
                ) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
            }

            if (bitmap == null) return ByteArray(0)
            val width = bitmap.width
            val height = bitmap.height
            val scale = THUMB_MAX_DIMENSION.toFloat() / maxOf(width, height).coerceAtLeast(1)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, THUMB_JPEG_QUALITY, baos)
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
            baos.toByteArray()
        } catch (e: Exception) {
            ByteArray(0)
        }
    }

    fun randomHex(bytesCount: Int = 8): String {
        val bytes = ByteArray(bytesCount)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Encrypts a single plaintext media file into targetDir as random hex .bin.
     */
    fun encryptFileToDirectory(
        context: Context,
        sourceFile: File,
        targetDir: File,
        wipeSource: Boolean = true,
        clearCachesNow: Boolean = false
    ): File? {
        if (!sourceFile.exists() || !sourceFile.isFile) return null
        if (isEncryptedFile(sourceFile.name)) return sourceFile

        val masterKey = unlockedMasterKey
        val fileKey: ByteArray
        val rsaWrappedKey: ByteArray?
        val versionByte: Byte

        if (masterKey != null) {
            fileKey = masterKey
            rsaWrappedKey = null
            versionByte = VERSION_SYMMETRIC_AUTH
        } else {
            val pubKeyB64 = context.config.vaultPublicKey
            if (pubKeyB64.isEmpty()) return null
            val pubKeyBytes = Base64.decode(pubKeyB64, Base64.NO_WRAP)
            fileKey = ByteArray(32).also { secureRandom.nextBytes(it) }
            rsaWrappedKey = wrapKeyWithPublicKey(pubKeyBytes, fileKey)
            versionByte = VERSION_ASYMMETRIC_AUTH
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val originalName = sourceFile.name
        val lastModified = sourceFile.lastModified()
        val dateTaken = lastModified
        val originalSize = sourceFile.length()
        val mediaType = getMediaTypeForName(originalName)
        val videoDuration = if (mediaType == TYPE_VIDEOS) (context.getDuration(sourceFile.absolutePath) ?: 0) else 0
        val thumbBytes = generateEmbeddedThumbnail(sourceFile, mediaType)

        val headerPlainBytes = ByteArrayOutputStream().use { baos ->
            DataOutputStream(baos).use { dos ->
                dos.writeUTF(originalName)
                dos.writeLong(lastModified)
                dos.writeLong(dateTaken)
                dos.writeLong(originalSize)
                dos.writeInt(mediaType)
                dos.writeInt(videoDuration)
                dos.writeInt(thumbBytes.size)
                if (thumbBytes.isNotEmpty()) {
                    dos.write(thumbBytes)
                }
            }
            baos.toByteArray()
        }

        val headerKey = deriveSubkey(fileKey, DOMAIN_HEADER_KEY)
        val contentEncKey = deriveSubkey(fileKey, DOMAIN_CONTENT_ENC_KEY)
        val contentMacKey = deriveSubkey(fileKey, DOMAIN_CONTENT_MAC_KEY)

        val headerIv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val headerCipher = Cipher.getInstance("AES/GCM/NoPadding")
        headerCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(headerKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, headerIv))
        val encryptedHeader = headerCipher.doFinal(headerPlainBytes)
        headerKey.fill(0)

        val contentIv = ByteArray(CTR_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val contentCipher = Cipher.getInstance("AES/CTR/NoPadding")
        contentCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(contentEncKey, "AES"), IvParameterSpec(contentIv))
        contentEncKey.fill(0)

        val poly1305 = Poly1305(AESEngine.newInstance())
        poly1305.init(ParametersWithIV(KeyParameter(contentMacKey), contentIv))
        contentMacKey.fill(0)

        var encryptedFile = File(targetDir, "${randomHex(8)}$ENCRYPTED_BIN_EXT")
        while (encryptedFile.exists()) {
            encryptedFile = File(targetDir, "${randomHex(8)}$ENCRYPTED_BIN_EXT")
        }

        return try {
            val sourcePath = sourceFile.absolutePath
            FileOutputStream(encryptedFile).use { fos ->
                BufferedOutputStream(fos, 64 * 1024).use { bos ->
                    DataOutputStream(bos).use { dos ->
                        dos.write(MAGIC_V2.toByteArray(Charsets.US_ASCII))
                        dos.writeByte(versionByte.toInt())
                        if (rsaWrappedKey != null) {
                            dos.writeInt(rsaWrappedKey.size)
                            dos.write(rsaWrappedKey)
                        }
                        dos.write(headerIv)
                        dos.writeInt(encryptedHeader.size)
                        dos.write(encryptedHeader)
                        dos.write(contentIv)

                        FileInputStream(sourceFile).use { fis ->
                            val buffer = ByteArray(64 * 1024)
                            var read: Int
                            while (fis.read(buffer).also { read = it } != -1) {
                                val out = contentCipher.update(buffer, 0, read)
                                if (out != null && out.isNotEmpty()) {
                                    dos.write(out)
                                    poly1305.update(out, 0, out.size)
                                }
                            }
                            val finalBytes = contentCipher.doFinal()
                            if (finalBytes != null && finalBytes.isNotEmpty()) {
                                dos.write(finalBytes)
                                poly1305.update(finalBytes, 0, finalBytes.size)
                            }
                        }
                        val macTag = ByteArray(MAC_TAG_LENGTH)
                        poly1305.doFinal(macTag, 0)
                        dos.write(macTag)
                        dos.flush()
                        bos.flush()
                        fos.fd.sync()
                    }
                }
            }

            encryptedFile.setLastModified(lastModified)

            if (masterKey != null) {
                val payloadOffset = (MAGIC_V2.length + 1 + GCM_IV_LENGTH + 4 + encryptedHeader.size + CTR_IV_LENGTH).toLong()
                metadataCache[encryptedFile.absolutePath] = EncryptedFileMetadata(
                    originalName = originalName,
                    lastModified = lastModified,
                    dateTaken = dateTaken,
                    originalSize = originalSize,
                    mediaType = mediaType,
                    videoDuration = videoDuration,
                    payloadOffset = payloadOffset,
                    contentIv = contentIv,
                    fileKey = fileKey.copyOf(),
                    version = versionByte,
                    isMacAuthenticated = true,
                    thumbnailBytes = if (thumbBytes.isNotEmpty()) thumbBytes else null
                )
            } else {
                fileKey.fill(0)
            }

            if (wipeSource) {
                context.updateVirtualAlbumItemPath(sourcePath, encryptedFile.absolutePath)
                secureWipeAndDeleteFile(context, sourceFile)
            }
            if (clearCachesNow) {
                clearAppImageCaches(context)
            }
            encryptedFile
        } catch (e: Exception) {
            if (masterKey == null) fileKey.fill(0)
            encryptedFile.delete()
            null
        }
    }

    /**
     * Directly encrypts a streaming InputStream (e.g. from network or content resolver)
     * into a hidden vault directory WITHOUT ever writing plaintext to disk.
     * Prevents any data remanence on flash storage.
     */
    fun encryptStreamToDirectory(
        context: Context? = null,
        inputStream: InputStream,
        targetDir: File,
        originalName: String,
        contentLength: Long = -1L,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null
    ): File? {
        val masterKey = unlockedMasterKey
        val fileKey: ByteArray
        val rsaWrappedKey: ByteArray?
        val versionByte: Byte

        if (masterKey != null) {
            fileKey = masterKey
            rsaWrappedKey = null
            versionByte = VERSION_SYMMETRIC_AUTH
        } else {
            val pubKeyB64 = context?.config?.vaultPublicKey ?: ""
            if (pubKeyB64.isEmpty()) return null
            val pubKeyBytes = Base64.decode(pubKeyB64, Base64.NO_WRAP)
            fileKey = ByteArray(32).also { secureRandom.nextBytes(it) }
            rsaWrappedKey = wrapKeyWithPublicKey(pubKeyBytes, fileKey)
            versionByte = VERSION_ASYMMETRIC_AUTH
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val lastModified = System.currentTimeMillis()
        val dateTaken = lastModified
        val initialSize = if (contentLength > 0L) contentLength else 0L
        val mediaType = getMediaTypeForName(originalName)
        val videoDuration = 0
        val thumbBytes = ByteArray(0)

        fun buildHeaderBytes(size: Long): ByteArray {
            return ByteArrayOutputStream().use { baos ->
                DataOutputStream(baos).use { dos ->
                    dos.writeUTF(originalName)
                    dos.writeLong(lastModified)
                    dos.writeLong(dateTaken)
                    dos.writeLong(size)
                    dos.writeInt(mediaType)
                    dos.writeInt(videoDuration)
                    dos.writeInt(thumbBytes.size)
                    if (thumbBytes.isNotEmpty()) {
                        dos.write(thumbBytes)
                    }
                }
                baos.toByteArray()
            }
        }

        val headerPlainBytes = buildHeaderBytes(initialSize)

        val headerKey = deriveSubkey(fileKey, DOMAIN_HEADER_KEY)
        val contentEncKey = deriveSubkey(fileKey, DOMAIN_CONTENT_ENC_KEY)
        val contentMacKey = deriveSubkey(fileKey, DOMAIN_CONTENT_MAC_KEY)

        val headerIv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val headerCipher = Cipher.getInstance("AES/GCM/NoPadding")
        headerCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(headerKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, headerIv))
        val encryptedHeader = headerCipher.doFinal(headerPlainBytes)

        val contentIv = ByteArray(CTR_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val contentCipher = Cipher.getInstance("AES/CTR/NoPadding")
        contentCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(contentEncKey, "AES"), IvParameterSpec(contentIv))
        contentEncKey.fill(0)

        val poly1305 = Poly1305(AESEngine.newInstance())
        poly1305.init(ParametersWithIV(KeyParameter(contentMacKey), contentIv))
        contentMacKey.fill(0)

        var encryptedFile = File(targetDir, "${randomHex(8)}$ENCRYPTED_BIN_EXT")
        while (encryptedFile.exists()) {
            encryptedFile = File(targetDir, "${randomHex(8)}$ENCRYPTED_BIN_EXT")
        }

        val headerIvOffset = MAGIC_V2.length + 1 + (if (rsaWrappedKey != null) 4 + rsaWrappedKey.size else 0)

        return try {
            var totalBytesRead = 0L
            FileOutputStream(encryptedFile).use { fos ->
                BufferedOutputStream(fos, 64 * 1024).use { bos ->
                    DataOutputStream(bos).use { dos ->
                        dos.write(MAGIC_V2.toByteArray(Charsets.US_ASCII))
                        dos.writeByte(versionByte.toInt())
                        if (rsaWrappedKey != null) {
                            dos.writeInt(rsaWrappedKey.size)
                            dos.write(rsaWrappedKey)
                        }
                        dos.write(headerIv)
                        dos.writeInt(encryptedHeader.size)
                        dos.write(encryptedHeader)
                        dos.write(contentIv)

                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            totalBytesRead += read
                            val out = contentCipher.update(buffer, 0, read)
                            if (out != null && out.isNotEmpty()) {
                                dos.write(out)
                                poly1305.update(out, 0, out.size)
                            }
                            onProgress?.invoke(totalBytesRead, contentLength)
                        }
                        val finalBytes = contentCipher.doFinal()
                        if (finalBytes != null && finalBytes.isNotEmpty()) {
                            dos.write(finalBytes)
                            poly1305.update(finalBytes, 0, finalBytes.size)
                        }
                        val macTag = ByteArray(MAC_TAG_LENGTH)
                        poly1305.doFinal(macTag, 0)
                        dos.write(macTag)
                        dos.flush()
                        bos.flush()
                        fos.fd.sync()
                    }
                }
            }

            if (contentLength <= 0L || contentLength != totalBytesRead) {
                val finalHeaderPlain = buildHeaderBytes(totalBytesRead)
                val newHeaderIv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
                headerCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(headerKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, newHeaderIv))
                val newEncryptedHeader = headerCipher.doFinal(finalHeaderPlain)
                RandomAccessFile(encryptedFile, "rw").use { raf ->
                    raf.seek(headerIvOffset.toLong())
                    raf.write(newHeaderIv)
                    raf.writeInt(newEncryptedHeader.size)
                    raf.write(newEncryptedHeader)
                    raf.fd.sync()
                }
            }
            headerKey.fill(0)

            encryptedFile.setLastModified(lastModified)

            val finalThumbBytes = if (mediaType == TYPE_IMAGES && masterKey != null) {
                generateEmbeddedThumbnailFromEncryptedFile(encryptedFile)
            } else null

            if (masterKey != null) {
                val payloadOffset = (headerIvOffset + GCM_IV_LENGTH + 4 + encryptedHeader.size + CTR_IV_LENGTH).toLong()
                metadataCache[encryptedFile.absolutePath] = EncryptedFileMetadata(
                    originalName = originalName,
                    lastModified = lastModified,
                    dateTaken = dateTaken,
                    originalSize = totalBytesRead,
                    mediaType = mediaType,
                    videoDuration = videoDuration,
                    payloadOffset = payloadOffset,
                    contentIv = contentIv,
                    fileKey = fileKey.copyOf(),
                    version = versionByte,
                    isMacAuthenticated = true,
                    thumbnailBytes = finalThumbBytes
                )
            } else {
                fileKey.fill(0)
            }

            encryptedFile
        } catch (e: Exception) {
            headerKey.fill(0)
            if (masterKey == null) fileKey.fill(0)
            encryptedFile.delete()
            null
        }
    }

    private fun generateEmbeddedThumbnailFromEncryptedFile(encryptedFile: File): ByteArray? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openDecryptedContentStream(encryptedFile)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            var sampleSize = 1
            while (boundsOpts.outWidth / sampleSize > THUMB_MAX_DIMENSION * 2 ||
                boundsOpts.outHeight / sampleSize > THUMB_MAX_DIMENSION * 2
            ) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = openDecryptedContentStream(encryptedFile)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            val width = bitmap.width
            val height = bitmap.height
            val scale = THUMB_MAX_DIMENSION.toFloat() / maxOf(width, height).coerceAtLeast(1)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, THUMB_JPEG_QUALITY, baos)
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
            baos.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    fun decryptFileToDirectory(
        context: Context,
        encryptedFile: File,
        targetDir: File,
        deleteEncryptedSource: Boolean = true
    ): File? {
        if (!encryptedFile.exists() || !isEncryptedFile(encryptedFile.name)) return null
        val meta = readMetadata(encryptedFile) ?: return null

        if (meta.isMacAuthenticated && !verifyFileIntegrity(encryptedFile)) {
            return null
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        var outFile = File(targetDir, meta.originalName)
        if (outFile.exists()) {
            val base = meta.originalName.substringBeforeLast('.', meta.originalName)
            val ext = meta.originalName.substringAfterLast('.', "")
            var idx = 1
            while (outFile.exists()) {
                val candidate = if (ext.isNotEmpty()) "${base}_$idx.$ext" else "${base}_$idx"
                outFile = File(targetDir, candidate)
                idx++
            }
        }

        return try {
            val encPath = encryptedFile.absolutePath
            openDecryptedContentStream(encryptedFile, 0L)?.use { cis ->
                FileOutputStream(outFile).use { fos ->
                    BufferedOutputStream(fos, 64 * 1024).use { bos ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (cis.read(buffer).also { read = it } != -1) {
                            bos.write(buffer, 0, read)
                        }
                        bos.flush()
                        fos.fd.sync()
                    }
                }
            } ?: return null

            outFile.setLastModified(meta.lastModified)

            if (deleteEncryptedSource) {
                context.updateVirtualAlbumItemPath(encPath, outFile.absolutePath)
                metadataCache.remove(encPath)?.fileKey?.fill(0)
                context.mediaDB.deleteMediumPath(encPath)
                encryptedFile.delete()
            }
            context.rescanPaths(arrayListOf(outFile.absolutePath))
            outFile
        } catch (e: Exception) {
            outFile.delete()
            null
        }
    }

    fun getDisplayName(path: String): String {
        if (isEncryptedFile(path)) {
            readMetadata(File(path))?.originalName?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return path.getFilenameFromPath()
    }

    fun moveMetadataCacheEntry(oldPath: String, newPath: String) {
        metadataCache.remove(oldPath)?.let { meta ->
            metadataCache[newPath] = meta
        }
    }

    /**
     * Renames the encrypted display name stored inside a .bin file's AES-GCM header.
     * The .bin filename on disk remains completely unrelated (random hex).
     * Re-encrypts only the header and shifts the authenticated payload without re-encrypting media bytes.
     */
    fun renameEncryptedFile(encryptedFile: File, newOriginalName: String): Boolean {
        if (!encryptedFile.exists() || !encryptedFile.isFile || !isEncryptedFile(encryptedFile.name)) return false
        val trimmedName = newOriginalName.trim()
        if (trimmedName.isEmpty()) return false
        if (unlockedMasterKey == null) return false

        val meta = readMetadata(encryptedFile) ?: return false
        if (meta.originalName == trimmedName) return true

        if (meta.isMacAuthenticated && !verifyFileIntegrity(encryptedFile)) {
            return false
        }

        val parentDir = encryptedFile.parentFile ?: return false
        val tempFile = File(parentDir, ".tmp_${randomHex(8)}$ENCRYPTED_BIN_EXT")

        return try {
            val magicBytes = ByteArray(8)
            val version: Byte
            var rsaWrappedKey: ByteArray? = null

            RandomAccessFile(encryptedFile, "r").use { raf ->
                raf.readFully(magicBytes)
                version = raf.readByte()
                if (version == VERSION_ASYMMETRIC || version == VERSION_ASYMMETRIC_AUTH) {
                    val wrappedLen = raf.readInt()
                    if (wrappedLen <= 0 || wrappedLen > 2048) return false
                    rsaWrappedKey = ByteArray(wrappedLen).also { raf.readFully(it) }
                }
            }

            val newExt = trimmedName.substringAfterLast('.', "")
            val updatedMediaType = if (newExt.isNotEmpty() && (
                    trimmedName.isImageFast() || trimmedName.isVideoFast() ||
                        trimmedName.isGif() || trimmedName.isRawFast() || trimmedName.isSvg()
                )
            ) {
                getMediaTypeForName(trimmedName)
            } else {
                meta.mediaType
            }

            val thumbBytes = readEmbeddedThumbnailBytes(encryptedFile) ?: meta.thumbnailBytes ?: ByteArray(0)
            val headerPlainBytes = ByteArrayOutputStream().use { baos ->
                DataOutputStream(baos).use { dos ->
                    dos.writeUTF(trimmedName)
                    dos.writeLong(meta.lastModified)
                    dos.writeLong(meta.dateTaken)
                    dos.writeLong(meta.originalSize)
                    dos.writeInt(updatedMediaType)
                    dos.writeInt(meta.videoDuration)
                    dos.writeInt(thumbBytes.size)
                    if (thumbBytes.isNotEmpty()) {
                        dos.write(thumbBytes)
                    }
                }
                baos.toByteArray()
            }

            val headerKey = if (meta.isMacAuthenticated) {
                deriveSubkey(meta.fileKey, DOMAIN_HEADER_KEY)
            } else {
                meta.fileKey
            }
            val headerIv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
            val headerCipher = Cipher.getInstance("AES/GCM/NoPadding")
            headerCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(headerKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, headerIv))
            val encryptedHeader = headerCipher.doFinal(headerPlainBytes)
            if (meta.isMacAuthenticated) {
                headerKey.fill(0)
            }

            val extraHeaderBytes = rsaWrappedKey?.let { 4 + it.size } ?: 0
            val newPayloadOffset = (8 + 1 + extraHeaderBytes + GCM_IV_LENGTH + 4 + encryptedHeader.size + CTR_IV_LENGTH).toLong()

            FileOutputStream(tempFile).use { fos ->
                BufferedOutputStream(fos, 64 * 1024).use { bos ->
                    DataOutputStream(bos).use { dos ->
                        dos.write(magicBytes)
                        dos.writeByte(version.toInt())
                        val wrapped = rsaWrappedKey
                        if (wrapped != null) {
                            dos.writeInt(wrapped.size)
                            dos.write(wrapped)
                        }
                        dos.write(headerIv)
                        dos.writeInt(encryptedHeader.size)
                        dos.write(encryptedHeader)
                        dos.write(meta.contentIv)

                        RandomAccessFile(encryptedFile, "r").use { raf ->
                            raf.seek(meta.payloadOffset)
                            val buffer = ByteArray(64 * 1024)
                            if (version == VERSION_V5_AUTH) {
                                val macKey = deriveSubkey(meta.fileKey, DOMAIN_CONTENT_MAC_KEY)
                                val poly = Poly1305(AESEngine.newInstance())
                                poly.init(ParametersWithIV(KeyParameter(macKey), meta.contentIv))
                                macKey.fill(0)
                                poly.update(headerIv, 0, headerIv.size)
                                poly.update(encryptedHeader, 0, encryptedHeader.size)

                                var remaining = meta.originalSize
                                while (remaining > 0) {
                                    val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                                    val read = raf.read(buffer, 0, toRead)
                                    if (read == -1) throw EOFException("Unexpected EOF copying encrypted payload")
                                    dos.write(buffer, 0, read)
                                    poly.update(buffer, 0, read)
                                    remaining -= read
                                }
                                val newTag = ByteArray(MAC_TAG_LENGTH)
                                poly.doFinal(newTag, 0)
                                dos.write(newTag)
                            } else {
                                var remaining = encryptedFile.length() - meta.payloadOffset
                                while (remaining > 0) {
                                    val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                                    val read = raf.read(buffer, 0, toRead)
                                    if (read == -1) break
                                    dos.write(buffer, 0, read)
                                    remaining -= read
                                }
                            }
                        }

                        dos.flush()
                        bos.flush()
                        fos.fd.sync()
                    }
                }
            }

            java.nio.file.Files.move(
                tempFile.toPath(),
                encryptedFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            )

            encryptedFile.setLastModified(meta.lastModified)
            syncDir(parentDir)

            metadataCache[encryptedFile.absolutePath] = meta.copy(
                originalName = trimmedName,
                mediaType = updatedMediaType,
                payloadOffset = newPayloadOffset,
                thumbnailBytes = if (thumbBytes.isNotEmpty()) thumbBytes else null
            )
            true
        } catch (e: Exception) {
            tempFile.delete()
            false
        }
    }

    /**
     * Encrypts an entire folder into the master random vault root directory as a randomized subfolder:
     * 1. Creates a random subfolder inside the master vault root.
     * 2. Writes .nomedia in the subfolder.
     * 3. Writes encrypted subfolder metadata (.meta) storing the original album name with MEK.
     * 4. Encrypts all media files into random hex .bin files (transactional: NO source files are deleted if any file fails).
     * 5. Only once ALL files are verified encrypted, securely wipes the originals and deletes the plaintext folder.
     */
    fun encryptFolder(
        context: Context,
        folderPath: String,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): String? {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) return folderPath

        if (unlockedMasterKey == null && context.config.vaultPublicKey.isEmpty()) {
            return null
        }

        val originalFolderName = getDecryptedFolderName(folder) ?: folder.name
        val vaultRoot = getOrCreateVaultRoot(context)

        val filesToEncrypt = folder.listFiles()?.filter { file ->
            file.isFile &&
                file.name != NOMEDIA &&
                file.name != ENCRYPTED_FOLDER_META_FILE &&
                file.name != VAULT_SUBFOLDER_META_FILE &&
                file.name != VAULT_HEADER_FILE &&
                !isEncryptedFile(file.name)
        } ?: emptyList()

        var subfolder = File(vaultRoot, randomHex(6))
        while (subfolder.exists()) {
            subfolder = File(vaultRoot, randomHex(6))
        }
        if (!subfolder.mkdirs()) return null

        val noMedia = File(subfolder, NOMEDIA)
        if (!noMedia.exists()) {
            try { noMedia.createNewFile() } catch (_: Exception) {}
        }

        writeEncryptedFolderMeta(context, subfolder, originalFolderName)

        val total = filesToEncrypt.size
        onProgress?.invoke(0, total)

        val encryptedPairs = ArrayList<Pair<File, File>>()
        var failed = false

        for ((index, file) in filesToEncrypt.withIndex()) {
            val enc = encryptFileToDirectory(
                context = context,
                sourceFile = file,
                targetDir = subfolder,
                wipeSource = false,
                clearCachesNow = false
            )
            if (enc != null && enc.exists() && enc.length() > 0L) {
                encryptedPairs.add(Pair(file, enc))
            } else {
                failed = true
                break
            }
            onProgress?.invoke(index + 1, total)
        }

        // ROLLBACK IF ENCRYPTION FAILED ON ANY FILE
        if (failed || encryptedPairs.size != filesToEncrypt.size) {
            subfolder.deleteRecursively()
            return null
        }

        // ALL FILES ENCRYPTED SUCCESSFULLY! WIPE AND DELETE ORIGINAL MEDIA FILES:
        for ((sourceFile, encFile) in encryptedPairs) {
            context.updateVirtualAlbumItemPath(sourceFile.absolutePath, encFile.absolutePath)
            secureWipeAndDeleteFile(context, sourceFile)
        }

        // Preserve any existing .bin files that were already encrypted in this folder
        val existingEncryptedFiles = folder.listFiles()?.filter { file ->
            file.isFile && isEncryptedFile(file.name)
        } ?: emptyList()
        for (encFile in existingEncryptedFiles) {
            var dest = File(subfolder, encFile.name)
            while (dest.exists()) {
                dest = File(subfolder, "${randomHex(8)}$ENCRYPTED_BIN_EXT")
            }
            val oldEncPath = encFile.absolutePath
            if (encFile.renameTo(dest)) {
                context.updateVirtualAlbumItemPath(oldEncPath, dest.absolutePath)
                metadataCache.remove(oldEncPath)?.let { meta ->
                    metadataCache[dest.absolutePath] = meta
                }
            }
        }

        // Move subfolders directly to internal storage (if any)
        val internalStorageRoot = File(context.internalStoragePath)
        val movedSubfolderPaths = ArrayList<String>()

        val childFiles = folder.listFiles() ?: emptyArray()
        for (child in childFiles) {
            if (child.isDirectory) {
                val cleanName = child.name.trimStart('.')
                var targetSubfolder = File(internalStorageRoot, if (cleanName.isEmpty()) "Subfolder" else cleanName)
                var collisionIndex = 1
                while (targetSubfolder.exists()) {
                    targetSubfolder = File(internalStorageRoot, "${if (cleanName.isEmpty()) "Subfolder" else cleanName}_$collisionIndex")
                    collisionIndex++
                }
                val moved = if (child.renameTo(targetSubfolder)) {
                    true
                } else {
                    try {
                        child.copyRecursively(targetSubfolder, overwrite = false) && child.deleteRecursively()
                    } catch (_: Exception) {
                        false
                    }
                }
                if (moved) {
                    val noMediaInSub = File(targetSubfolder, NOMEDIA)
                    if (noMediaInSub.exists()) {
                        try { noMediaInSub.delete() } catch (_: Exception) {}
                        try {
                            context.contentResolver.delete(
                                MediaStore.Files.getContentUri("external"),
                                "${MediaStore.Files.FileColumns.DATA} = ?",
                                arrayOf(noMediaInSub.absolutePath)
                            )
                        } catch (_: Exception) {}
                    }
                    context.directoryDB.deleteDirPath(child.absolutePath)
                    movedSubfolderPaths.add(targetSubfolder.absolutePath)
                }
            }
        }

        if (movedSubfolderPaths.isNotEmpty()) {
            val pathsToRescan = ArrayList<String>()
            for (path in movedSubfolderPaths) {
                pathsToRescan.add(path)
                File(path).walkTopDown().filter { it.isFile }.forEach {
                    pathsToRescan.add(it.absolutePath)
                }
            }
            context.rescanPaths(pathsToRescan)
        }

        // Delete original plaintext folder
        try {
            val oldNoMedia = File(folder, NOMEDIA)
            if (!oldNoMedia.exists()) {
                try { oldNoMedia.createNewFile() } catch (_: Exception) {}
            }

            val contentResolver = context.contentResolver
            val filesUri = MediaStore.Files.getContentUri("external")
            contentResolver.delete(
                filesUri,
                "${MediaStore.MediaColumns.DATA} LIKE ?",
                arrayOf("${folder.absolutePath}%")
            )

            folder.listFiles()?.forEach { child ->
                try { child.delete() } catch (_: Exception) {}
            }

            var deletedDir = folder.delete()
            if (!deletedDir) {
                deletedDir = folder.deleteRecursively()
            }
            if (!deletedDir) {
                deletedDir = context.tryFastDocumentDelete(folder.absolutePath, true)
            }
        } catch (_: Exception) {}

        context.config.removeEncryptedFolder(folder.absolutePath)
        context.config.addEncryptedFolder(subfolder.absolutePath)
        context.directoryDB.deleteDirPath(folder.absolutePath)
        folderNameCache[subfolder.absolutePath] = originalFolderName
        encryptedFolderCache[folder.absolutePath] = false
        encryptedFolderCache[subfolder.absolutePath] = true
        clearAppImageCaches(context)

        return subfolder.absolutePath
    }

    /**
     * Decrypts an encrypted subfolder back to plaintext inside Pictures/<originalFolderName>.
     */
    fun decryptFolder(
        context: Context,
        folderPath: String,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): String? {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) return folderPath
        if (!isVaultUnlocked) return folderPath

        val originalFolderName = getDecryptedFolderName(folder) ?: folder.name

        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        var targetFolder = File(picturesDir, originalFolderName)
        var idx = 1
        while (targetFolder.exists()) {
            targetFolder = File(picturesDir, "${originalFolderName}_$idx")
            idx++
        }
        if (!targetFolder.mkdirs() && !targetFolder.exists()) return null

        val filesToDecrypt = folder.listFiles()?.filter {
            it.isFile && isEncryptedFile(it.name)
        } ?: emptyList()

        val total = filesToDecrypt.size
        onProgress?.invoke(0, total)

        val decryptedPairs = ArrayList<Pair<File, File>>()
        var failed = false

        for ((index, file) in filesToDecrypt.withIndex()) {
            val decrypted = decryptFileToDirectory(
                context = context,
                encryptedFile = file,
                targetDir = targetFolder,
                deleteEncryptedSource = false
            )
            if (decrypted != null && decrypted.exists() && decrypted.length() > 0L) {
                decryptedPairs.add(Pair(file, decrypted))
            } else {
                failed = true
                break
            }
            onProgress?.invoke(index + 1, total)
        }

        // ROLLBACK IF DECRYPTION FAILED
        if (failed || decryptedPairs.size != filesToDecrypt.size) {
            targetFolder.deleteRecursively()
            return null
        }

        // ALL FILES DECRYPTED! Now delete encrypted files:
        for ((encFile, decryptedFile) in decryptedPairs) {
            val encPath = encFile.absolutePath
            context.updateVirtualAlbumItemPath(encPath, decryptedFile.absolutePath)
            metadataCache.remove(encPath)?.fileKey?.fill(0)
            context.mediaDB.deleteMediumPath(encPath)
            encFile.delete()
        }

        try {
            val contentResolver = context.contentResolver
            val filesUri = MediaStore.Files.getContentUri("external")
            contentResolver.delete(
                filesUri,
                "${MediaStore.MediaColumns.DATA} LIKE ?",
                arrayOf("${folder.absolutePath}%")
            )
            if (!folder.deleteRecursively()) {
                (context as? BaseSimpleActivity)?.deleteFileBg(
                    folder.toFileDirItem(context),
                    allowDeleteFolder = true,
                    isDeletingMultipleFiles = false
                )
            }
        } catch (_: Exception) {}
        folderNameCache.remove(folder.absolutePath)

        context.config.removeEncryptedFolder(folder.absolutePath)
        context.directoryDB.deleteDirPath(folder.absolutePath)
        encryptedFolderCache[folder.absolutePath] = false
        encryptedFolderCache[targetFolder.absolutePath] = false

        val restoredPaths = targetFolder.listFiles()?.filter { it.isFile }?.map { it.absolutePath } ?: emptyList()
        if (restoredPaths.isNotEmpty()) {
            context.rescanPaths(ArrayList(restoredPaths))
        }
        return targetFolder.absolutePath
    }

    // ==================== ON-THE-FLY HEADER & STREAM DECRYPTION ====================

    fun readMetadata(file: File): EncryptedFileMetadata? {
        metadataCache[file.absolutePath]?.let { return it }
        val masterKey = unlockedMasterKey ?: return null
        if (!file.exists() || !file.isFile) return null

        return try {
            FileInputStream(file).use { fis ->
                DataInputStream(BufferedInputStream(fis, 8192)).use { dis ->
                    val magicBytes = ByteArray(8)
                    dis.readFully(magicBytes)
                    val magicStr = String(magicBytes, Charsets.US_ASCII)
                    if (magicStr != MAGIC_V2 && magicStr != MAGIC_V1) return null

                    val version = dis.readByte()
                    var extraHeaderBytes = 0
                    val fileKey: ByteArray = when (version) {
                        VERSION_SYMMETRIC, VERSION_SYMMETRIC_AUTH, VERSION_V5_AUTH -> masterKey.copyOf()
                        VERSION_ASYMMETRIC, VERSION_ASYMMETRIC_AUTH -> {
                            val privKey = unlockedPrivateKey ?: return null
                            val wrappedLen = dis.readInt()
                            if (wrappedLen <= 0 || wrappedLen > 2048) return null
                            val wrappedKey = ByteArray(wrappedLen)
                            dis.readFully(wrappedKey)
                            extraHeaderBytes = 4 + wrappedLen
                            unwrapKeyWithPrivateKey(privKey, wrappedKey)
                        }
                        else -> return null
                    }

                    val headerIv = ByteArray(GCM_IV_LENGTH)
                    dis.readFully(headerIv)

                    val encHeaderLen = dis.readInt()
                    if (encHeaderLen <= 0 || encHeaderLen > 5 * 1024 * 1024) {
                        fileKey.fill(0)
                        return null
                    }

                    val encHeader = ByteArray(encHeaderLen)
                    dis.readFully(encHeader)

                    val contentIv = ByteArray(CTR_IV_LENGTH)
                    dis.readFully(contentIv)

                    val isMacAuth = (version == VERSION_SYMMETRIC_AUTH || version == VERSION_ASYMMETRIC_AUTH || version == VERSION_V5_AUTH)
                    val headerKey = if (isMacAuth) {
                        deriveSubkey(fileKey, DOMAIN_HEADER_KEY)
                    } else {
                        fileKey
                    }

                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(headerKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, headerIv))
                    val plainHeader = cipher.doFinal(encHeader)
                    if (isMacAuth) {
                        headerKey.fill(0)
                    }

                    DataInputStream(ByteArrayInputStream(plainHeader)).use { hdis ->
                        val originalName = hdis.readUTF()
                        val lastModified = hdis.readLong()
                        val dateTaken = hdis.readLong()
                        val originalSize = hdis.readLong()
                        val mediaType = hdis.readInt()
                        val videoDuration = hdis.readInt()
                        val thumbBytes = try {
                            if (hdis.available() >= 4) {
                                val thumbLen = hdis.readInt()
                                if (thumbLen in 1..(5 * 1024 * 1024) && hdis.available() >= thumbLen) {
                                    val tb = ByteArray(thumbLen)
                                    hdis.readFully(tb)
                                    tb
                                } else null
                            } else null
                        } catch (_: Exception) {
                            null
                        }
                        val payloadOffset = (8 + 1 + extraHeaderBytes + GCM_IV_LENGTH + 4 + encHeaderLen + CTR_IV_LENGTH).toLong()

                        val meta = EncryptedFileMetadata(
                            originalName = originalName,
                            lastModified = lastModified,
                            dateTaken = dateTaken,
                            originalSize = originalSize,
                            mediaType = mediaType,
                            videoDuration = videoDuration,
                            payloadOffset = payloadOffset,
                            contentIv = contentIv,
                            fileKey = fileKey,
                            version = version,
                            isMacAuthenticated = isMacAuth,
                            thumbnailBytes = thumbBytes
                        )
                        metadataCache[file.absolutePath] = meta
                        meta
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun readEmbeddedThumbnailBytes(file: File): ByteArray? {
        val meta = readMetadata(file) ?: return null
        if (meta.thumbnailBytes != null && meta.thumbnailBytes.isNotEmpty()) {
            return meta.thumbnailBytes
        }
        if (!file.exists() || !file.isFile) return null

        return try {
            FileInputStream(file).use { fis ->
                DataInputStream(BufferedInputStream(fis, 8192)).use { dis ->
                    val magicBytes = ByteArray(8)
                    dis.readFully(magicBytes)
                    val magicStr = String(magicBytes, Charsets.US_ASCII)
                    if (magicStr != MAGIC_V2 && magicStr != MAGIC_V1) return null

                    val version = dis.readByte()
                    if (version == VERSION_ASYMMETRIC || version == VERSION_ASYMMETRIC_AUTH) {
                        val wrappedLen = dis.readInt()
                        if (wrappedLen <= 0 || wrappedLen > 2048) return null
                        dis.skipBytes(wrappedLen)
                    } else if (version != VERSION_SYMMETRIC && version != VERSION_SYMMETRIC_AUTH && version != VERSION_V5_AUTH) {
                        return null
                    }

                    val headerIv = ByteArray(GCM_IV_LENGTH)
                    dis.readFully(headerIv)

                    val encHeaderLen = dis.readInt()
                    if (encHeaderLen <= 0 || encHeaderLen > 5 * 1024 * 1024) return null

                    val encHeader = ByteArray(encHeaderLen)
                    dis.readFully(encHeader)

                    val isMacAuth = (version == VERSION_SYMMETRIC_AUTH || version == VERSION_ASYMMETRIC_AUTH || version == VERSION_V5_AUTH)
                    val headerKey = if (isMacAuth) {
                        deriveSubkey(meta.fileKey, DOMAIN_HEADER_KEY)
                    } else {
                        meta.fileKey
                    }

                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(headerKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, headerIv))
                    val plainHeader = cipher.doFinal(encHeader)
                    if (isMacAuth) {
                        headerKey.fill(0)
                    }

                    DataInputStream(ByteArrayInputStream(plainHeader)).use { hdis ->
                        hdis.readUTF() // originalName
                        hdis.readLong() // lastModified
                        hdis.readLong() // dateTaken
                        hdis.readLong() // originalSize
                        hdis.readInt() // mediaType
                        hdis.readInt() // videoDuration
                        val thumbLen = hdis.readInt()
                        if (thumbLen <= 0) return null
                        val thumbBytes = ByteArray(thumbLen)
                        hdis.readFully(thumbBytes)
                        thumbBytes
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun incrementCounter(baseIv: ByteArray, blockOffset: Long): ByteArray {
        if (blockOffset == 0L) return baseIv.copyOf()
        val counterInt = BigInteger(1, baseIv).add(BigInteger.valueOf(blockOffset))
        val bytes = counterInt.toByteArray()
        val result = ByteArray(CTR_IV_LENGTH)
        val copyLen = minOf(bytes.size, CTR_IV_LENGTH)
        System.arraycopy(bytes, bytes.size - copyLen, result, CTR_IV_LENGTH - copyLen, copyLen)
        return result
    }

    fun openDecryptedContentStream(
        file: File,
        startOffset: Long = 0L
    ): InputStream? {
        val meta = readMetadata(file) ?: return null
        if (startOffset < 0L || startOffset >= meta.originalSize) {
            return ByteArrayInputStream(ByteArray(0))
        }

        return try {
            val blockIndex = startOffset / CTR_IV_LENGTH
            val byteSkipInBlock = (startOffset % CTR_IV_LENGTH).toInt()
            val fileSeekPosition = meta.payloadOffset + (blockIndex * CTR_IV_LENGTH)

            val fis = FileInputStream(file)
            try {
                fis.channel.position(fileSeekPosition)
            } catch (e: Exception) {
                fis.close()
                return null
            }

            val adjustedIv = incrementCounter(meta.contentIv, blockIndex)
            val encKey = if (meta.isMacAuthenticated) {
                deriveSubkey(meta.fileKey, DOMAIN_CONTENT_ENC_KEY)
            } else {
                meta.fileKey
            }

            val cipher = Cipher.getInstance("AES/CTR/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(encKey, "AES"), IvParameterSpec(adjustedIv))
            if (meta.isMacAuthenticated) {
                encKey.fill(0)
            }

            val cis = CipherInputStream(BufferedInputStream(fis, 64 * 1024), cipher)
            var remainingSkip = byteSkipInBlock
            while (remainingSkip > 0) {
                val skipped = cis.skip(remainingSkip.toLong()).toInt()
                if (skipped <= 0) {
                    if (cis.read() == -1) break
                    remainingSkip--
                } else {
                    remainingSkip -= skipped
                }
            }

            val remainingContentBytes = meta.originalSize - startOffset
            BoundedInputStream(cis, remainingContentBytes)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Verifies cryptographic integrity of the encrypted payload using Poly1305-AES MAC.
     * Guarantees tamper-resistance, authentic data origin, and bit-rot detection.
     */
    fun verifyFileIntegrity(file: File): Boolean {
        val meta = readMetadata(file) ?: return false
        if (!meta.isMacAuthenticated) {
            // Legacy v1/v2 payload (authenticated via GCM header only)
            return true
        }

        val expectedTotalSize = meta.payloadOffset + meta.originalSize + MAC_TAG_LENGTH
        if (file.length() != expectedTotalSize) {
            return false
        }

        val macKey = deriveSubkey(meta.fileKey, DOMAIN_CONTENT_MAC_KEY)
        val poly = Poly1305(AESEngine.newInstance())
        poly.init(ParametersWithIV(KeyParameter(macKey), meta.contentIv))
        macKey.fill(0)

        val storedTag = ByteArray(MAC_TAG_LENGTH)
        return try {
            RandomAccessFile(file, "r").use { raf ->
                if (meta.version == VERSION_V5_AUTH) {
                    raf.seek(8L + 1L)
                    val headerIv = ByteArray(GCM_IV_LENGTH)
                    raf.readFully(headerIv)
                    val encHeaderLen = raf.readInt()
                    val encHeader = ByteArray(encHeaderLen)
                    raf.readFully(encHeader)
                    poly.update(headerIv, 0, headerIv.size)
                    poly.update(encHeader, 0, encHeader.size)
                }
                raf.seek(meta.payloadOffset)
                val buffer = ByteArray(64 * 1024)
                var remaining = meta.originalSize
                while (remaining > 0) {
                    val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                    val read = raf.read(buffer, 0, toRead)
                    if (read == -1) return false
                    poly.update(buffer, 0, read)
                    remaining -= read
                }
                raf.readFully(storedTag)
            }
            val computedTag = ByteArray(MAC_TAG_LENGTH)
            poly.doFinal(computedTag, 0)
            MessageDigest.isEqual(computedTag, storedTag)
        } catch (_: Exception) {
            false
        }
    }

    // ==================== ANTI-RECOVERY / SECURE WIPE ====================

    private fun getPendingWipeJournalFile(context: Context): File {
        return File(context.filesDir, "pending_wipe_journal.txt")
    }

    private fun appendPendingWipe(context: Context, path: String) {
        try {
            val file = getPendingWipeJournalFile(context)
            file.appendText(path + "\n", Charsets.UTF_8)
        } catch (_: Exception) {}
    }

    private fun removePendingWipe(context: Context, path: String) {
        try {
            val file = getPendingWipeJournalFile(context)
            if (!file.exists()) return
            val lines = file.readLines(Charsets.UTF_8).filter { it.trim().isNotEmpty() && it.trim() != path }
            if (lines.isEmpty()) {
                secureWipeAndDeleteFile(file, null)
            } else {
                file.writeText(lines.joinToString("\n") + "\n", Charsets.UTF_8)
            }
        } catch (_: Exception) {}
    }

    fun processPendingWipeJournal(context: Context) {
        ensureBackgroundThread {
            try {
                val file = getPendingWipeJournalFile(context)
                if (!file.exists()) return@ensureBackgroundThread
                val lines = file.readLines(Charsets.UTF_8)
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty()) {
                        val target = File(trimmed)
                        if (target.exists()) {
                            secureWipeAndDeleteFile(target, context)
                        }
                    }
                }
                secureWipeAndDeleteFile(file, null)
            } catch (_: Exception) {}
        }
    }

    fun purgeTracesForFile(context: Context, plaintextPath: String) {
        try {
            val contentResolver = context.contentResolver
            val filesUri = MediaStore.Files.getContentUri("external")

            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.DATA} = ?"
            val selectionArgs = arrayOf(plaintextPath)
            var mediaId: Long? = null
            contentResolver.query(filesUri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    mediaId = cursor.getLong(0)
                }
            }

            if (mediaId != null) {
                try {
                    contentResolver.delete(
                        MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                        "${MediaStore.Images.Thumbnails.IMAGE_ID} = ?",
                        arrayOf(mediaId.toString())
                    )
                } catch (_: Exception) {}
                try {
                    contentResolver.delete(
                        MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                        "${MediaStore.Video.Thumbnails.VIDEO_ID} = ?",
                        arrayOf(mediaId.toString())
                    )
                } catch (_: Exception) {}
            }

            contentResolver.delete(filesUri, selection, selectionArgs)
            MediaScannerConnection.scanFile(context, arrayOf(plaintextPath), null, null)
        } catch (_: Exception) {}

        try {
            context.mediaDB.deleteMediumPath(plaintextPath)
            context.favoritesDB.deleteFavoritePath(plaintextPath)
            GalleryDatabase.getInstance(context.applicationContext).DateTakensDao().deleteDateTaken(plaintextPath)
            context.virtualAlbumsDB.removeItemFromAllAlbums(plaintextPath)
        } catch (_: Exception) {}

        try {
            val fileName = plaintextPath.substringAfterLast('/')
            val parent = plaintextPath.substringBeforeLast('/')
            val dcimThumbnails = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), ".thumbnails")
            val picThumbnails = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ".thumbnails")
            val parentThumbnails = File(parent, ".thumbnails")
            listOf(dcimThumbnails, picThumbnails, parentThumbnails).forEach { tDir ->
                if (tDir.exists() && tDir.isDirectory) {
                    tDir.listFiles { f -> f.name.contains(fileName) }?.forEach { thumbFile ->
                        secureWipeAndDeleteFile(thumbFile, null)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun purgeFolderTraces(context: Context, folderPath: String) {
        val cleanPath = folderPath.trimEnd('/')
        try {
            context.mediaDB.deleteMediaFromPath(cleanPath)
            context.mediaDB.deleteMediaByPrefix(cleanPath)
            context.directoryDB.deleteDirPath(cleanPath)
            context.directoryDB.deleteDirPrefix(cleanPath)
            context.favoritesDB.deleteFavoritesByPrefix(cleanPath)
            val db = GalleryDatabase.getInstance(context.applicationContext)
            db.DateTakensDao().deleteDateTakensFromPath(cleanPath)
            db.DateTakensDao().deleteDateTakensByPrefix(cleanPath)
            GalleryDatabase.scrubDatabase(context)
        } catch (_: Exception) {}

        try {
            context.config.scrubFolderPreferences(cleanPath)
        } catch (_: Exception) {}

        try {
            val contentResolver = context.contentResolver
            val filesUri = MediaStore.Files.getContentUri("external")
            contentResolver.delete(
                filesUri,
                "${MediaStore.Files.FileColumns.DATA} LIKE ? OR ${MediaStore.Files.FileColumns.DATA} = ?",
                arrayOf("$cleanPath/%", cleanPath)
            )
            MediaScannerConnection.scanFile(context, arrayOf(cleanPath), null, null)
        } catch (_: Exception) {}

        try {
            val recycleBin = File(context.filesDir, RECYCLE_BIN)
            if (recycleBin.exists() && recycleBin.isDirectory) {
                recycleBin.listFiles()?.forEach { binFile ->
                    if (binFile.absolutePath.contains(cleanPath)) {
                        secureWipeAndDeleteFile(binFile, null)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun clearAppImageCaches(context: Context) {
        val appContext = context.applicationContext
        ensureBackgroundThread {
            try {
                Glide.get(appContext).clearDiskCache()
            } catch (_: Exception) {}
            try {
                val cacheDirs = listOfNotNull(
                    File(appContext.cacheDir, "image_manager_disk_cache"),
                    File(appContext.cacheDir, "picasso-cache"),
                    File(appContext.cacheDir, "glide"),
                    appContext.externalCacheDir,
                    File(appContext.cacheDir, TEMP_FOLDER_NAME)
                )
                for (dir in cacheDirs) {
                    if (dir.exists() && dir.isDirectory) {
                        dir.walkBottomUp().forEach { f ->
                            if (f.isFile) {
                                secureWipeFileInternal(f)
                                f.delete()
                            } else if (f.isDirectory) {
                                f.delete()
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        try {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    Glide.get(appContext).clearMemory()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    fun secureWipeAndDeleteFile(context: Context, file: File): Boolean {
        return secureWipeAndDeleteFile(file, context)
    }

    fun secureWipeAndDeleteFile(file: File, context: Context? = null): Boolean {
        if (!file.exists()) return true
        val originalPath = file.absolutePath

        val wiped = secureWipeFileInternal(file)
        if (!wiped) {
            return false
        }

        var deleted = file.delete()
        if (!deleted && context != null) {
            deleted = context.tryFastDocumentDelete(originalPath, false)
        }

        syncDir(file.parentFile)

        if (context != null) {
            purgeTracesForFile(context, originalPath)
        }

        return deleted
    }

    private fun secureWipeFileInternal(file: File): Boolean {
        return try {
            val length = file.length()
            if (length > 0L) {
                val random = SecureRandom()
                val bufSize = 1024 * 1024 // 1 MiB chunk
                val buf = ByteArray(bufSize)
                RandomAccessFile(file, "rws").use { raf ->
                    val ch = raf.channel
                    var rem = length
                    ch.position(0L)
                    while (rem > 0L) {
                        val toWrite = minOf(rem, bufSize.toLong()).toInt()
                        random.nextBytes(buf)
                        val byteBuf = java.nio.ByteBuffer.wrap(buf, 0, toWrite)
                        while (byteBuf.hasRemaining()) {
                            ch.write(byteBuf)
                        }
                        rem -= toWrite
                    }
                    ch.force(true)
                    ch.truncate(0L)
                    ch.force(true)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun syncDir(dir: File?) {
        if (dir == null || !dir.exists() || !dir.isDirectory) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val fd = android.system.Os.open(dir.absolutePath, android.system.OsConstants.O_RDONLY, 0)
                try {
                    android.system.Os.fsync(fd)
                } finally {
                    android.system.Os.close(fd)
                }
            }
        } catch (_: Throwable) {
        }
    }
}