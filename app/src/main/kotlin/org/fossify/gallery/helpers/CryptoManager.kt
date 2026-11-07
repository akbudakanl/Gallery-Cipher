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
import org.fossify.gallery.extensions.config
import org.fossify.gallery.extensions.directoryDB
import org.fossify.gallery.extensions.mediaDB
import org.fossify.gallery.extensions.updateVirtualAlbumItemPath
import org.fossify.gallery.extensions.virtualAlbumsDB
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

object CryptoManager {
    private const val MAGIC_V1 = "FVLTENC1"
    private const val MAGIC_V2 = "FGVLTENC"
    private const val VERSION_SYMMETRIC: Byte = 1
    private const val VERSION_ASYMMETRIC: Byte = 2
    private const val VERSION_SYMMETRIC_AUTH: Byte = 3
    private const val VERSION_ASYMMETRIC_AUTH: Byte = 4
    private const val META_MAGIC_SYM = "FVLTMETA"
    private const val META_MAGIC_ASYM = "FVLTMET2"
    private const val META_MAGIC_V2 = "FGMETA2"
    private const val VAULT_HEADER_MAGIC = "FGVLT2"
    private const val VAULT_STR_PREFIX = "FVSTR1:"

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
    private const val ARGON2_MEMORY_KB = 32768 // 32 MB RAM cost
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

    val isVaultUnlocked: Boolean
        get() = unlockedMasterKey != null

    fun invalidateFolderCache() {
        encryptedFolderCache.clear()
    }

    fun lockVault() {
        unlockedMasterKey?.fill(0)
        unlockedMasterKey = null
        unlockedPrivateKey = null
        metadataCache.values.forEach { it.fileKey.fill(0) }
        metadataCache.clear()
        folderNameCache.clear()
        encryptedFolderCache.clear()
    }

    fun isEncryptedFile(path: String): Boolean {
        return path.endsWith(ENCRYPTED_BIN_EXT, ignoreCase = true) ||
            path.endsWith(ENCRYPTED_FILE_EXT, ignoreCase = true)
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

    fun hasExistingVault(context: Context): Boolean {
        if (context.config.vaultWrappedKeyData.isNotEmpty()) return true
        return findVaultRoot(context) != null
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

        val externalRoot = Environment.getExternalStorageDirectory()
        var randomName = randomHex(6)
        var newDir = File(externalRoot, randomName)
        while (newDir.exists()) {
            randomName = randomHex(6)
            newDir = File(externalRoot, randomName)
        }
        newDir.mkdirs()

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
        cachedVaultRoot?.let { if (it.exists() && File(it, VAULT_HEADER_FILE).exists()) return it }

        val storedPath = context.config.vaultRootPath
        if (storedPath.isNotEmpty()) {
            val dir = File(storedPath)
            if (dir.exists() && File(dir, VAULT_HEADER_FILE).exists()) {
                cachedVaultRoot = dir
                return dir
            }
        }

        if (vaultRootSearchDone) {
            return null
        }

        val candidates = listOfNotNull(
            Environment.getExternalStorageDirectory(),
            File("/storage/emulated/0"),
            context.sdCardPath.takeIf { it.isNotEmpty() }?.let { File(it) }
        )
        for (root in candidates) {
            if (root.exists() && root.isDirectory) {
                val children = root.listFiles() ?: continue
                for (child in children) {
                    if (child.isDirectory && File(child, VAULT_HEADER_FILE).exists()) {
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
        generator.generateBytes(secretBytes, keyBytes, 0, keyBytes.size)
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun deriveWrappingKey(secret: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(secret.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(keyBytes, "AES")
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
            }
            fos.fd.sync()
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

                    val wrappingKey = if (kdfType == 1.toByte()) {
                        deriveKeyArgon2id(secret, salt, memoryKb, iterations, parallelism)
                    } else {
                        deriveWrappingKey(secret, salt)
                    }

                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_BITS, iv))
                    val bundle = cipher.doFinal(wrapped)

                    DataInputStream(ByteArrayInputStream(bundle)).use { bdis ->
                        val masterKey = ByteArray(32)
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
                }
            }
        }
    }

    fun getDecryptedFolderName(folder: File): String? {
        folderNameCache[folder.absolutePath]?.let { return it }
        val masterKey = unlockedMasterKey ?: return null
        val metaFile = when {
            File(folder, VAULT_SUBFOLDER_META_FILE).exists() -> File(folder, VAULT_SUBFOLDER_META_FILE)
            File(folder, ENCRYPTED_FOLDER_META_FILE).exists() -> File(folder, ENCRYPTED_FOLDER_META_FILE)
            else -> return null
        }

        return try {
            FileInputStream(metaFile).use { fis ->
                DataInputStream(fis).use { dis ->
                    val magicBytes = ByteArray(META_MAGIC_SYM.length.coerceAtLeast(META_MAGIC_V2.length))
                    val readLen = if (metaFile.name == VAULT_SUBFOLDER_META_FILE) META_MAGIC_V2.length else META_MAGIC_SYM.length
                    dis.readFully(magicBytes, 0, readLen)
                    val magic = String(magicBytes, 0, readLen, Charsets.US_ASCII)

                    val aesKey: ByteArray = when (magic) {
                        META_MAGIC_V2, META_MAGIC_SYM -> masterKey
                        META_MAGIC_ASYM -> {
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
                    }
                }
                fos.fd.sync()
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
                    }
                    fos.fd.sync()
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

    /**
     * Encrypts an entire folder into the master random vault root directory as a randomized subfolder:
     * 1. Creates a random subfolder inside the master vault root.
     * 2. Writes .nomedia in the subfolder.
     * 3. Writes encrypted subfolder metadata (.meta) storing the original album name with MEK.
     * 4. Encrypts all media files into random hex .bin files and shreds the originals.
     * 5. Deletes original plaintext folder.
     */
    fun encryptFolder(
        context: Context,
        folderPath: String,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): String {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) return folderPath

        val originalFolderName = getDecryptedFolderName(folder) ?: folder.name
        val vaultRoot = getOrCreateVaultRoot(context)

        // 1. Move any subfolders directly to internal storage so they remain unencrypted and unhidden
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

        var subfolder = File(vaultRoot, randomHex(6))
        while (subfolder.exists()) {
            subfolder = File(vaultRoot, randomHex(6))
        }
        subfolder.mkdirs()

        val noMedia = File(subfolder, NOMEDIA)
        if (!noMedia.exists()) {
            try { noMedia.createNewFile() } catch (_: Exception) {}
        }

        writeEncryptedFolderMeta(context, subfolder, originalFolderName)

        val filesToEncrypt = folder.listFiles()?.filter { file ->
            file.isFile &&
                file.name != NOMEDIA &&
                file.name != ENCRYPTED_FOLDER_META_FILE &&
                file.name != VAULT_SUBFOLDER_META_FILE &&
                file.name != VAULT_HEADER_FILE &&
                !isEncryptedFile(file.name)
        } ?: emptyList()

        val total = filesToEncrypt.size
        onProgress?.invoke(0, total)

        for ((index, file) in filesToEncrypt.withIndex()) {
            encryptFileToDirectory(
                context = context,
                sourceFile = file,
                targetDir = subfolder,
                wipeSource = true,
                clearCachesNow = false
            )
            onProgress?.invoke(index + 1, total)
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
            if (!deletedDir && context is BaseSimpleActivity) {
                context.deleteFile(folder.toFileDirItem(context), allowDeleteFolder = true, isDeletingMultipleFiles = false)
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
    ): String {
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
        targetFolder.mkdirs()

        val filesToDecrypt = folder.listFiles()?.filter {
            it.isFile && isEncryptedFile(it.name)
        } ?: emptyList()

        val total = filesToDecrypt.size
        onProgress?.invoke(0, total)

        for ((index, file) in filesToDecrypt.withIndex()) {
            decryptFileToDirectory(
                context = context,
                encryptedFile = file,
                targetDir = targetFolder,
                deleteEncryptedSource = true
            )
            onProgress?.invoke(index + 1, total)
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
                        VERSION_SYMMETRIC, VERSION_SYMMETRIC_AUTH -> masterKey.copyOf()
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

                    val isMacAuth = (version == VERSION_SYMMETRIC_AUTH || version == VERSION_ASYMMETRIC_AUTH)
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
                    } else if (version != VERSION_SYMMETRIC && version != VERSION_SYMMETRIC_AUTH) {
                        return null
                    }

                    val headerIv = ByteArray(GCM_IV_LENGTH)
                    dis.readFully(headerIv)

                    val encHeaderLen = dis.readInt()
                    if (encHeaderLen <= 0 || encHeaderLen > 5 * 1024 * 1024) return null

                    val encHeader = ByteArray(encHeaderLen)
                    dis.readFully(encHeader)

                    val isMacAuth = (version == VERSION_SYMMETRIC_AUTH || version == VERSION_ASYMMETRIC_AUTH)
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
                } catch (_: Exception) {
                }
                try {
                    contentResolver.delete(
                        MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                        "${MediaStore.Video.Thumbnails.VIDEO_ID} = ?",
                        arrayOf(mediaId.toString())
                    )
                } catch (_: Exception) {
                }
            }

            contentResolver.delete(filesUri, selection, selectionArgs)
        } catch (_: Exception) {
        }

        try {
            context.mediaDB.deleteMediumPath(plaintextPath)
        } catch (_: Exception) {
        }
    }

    fun clearAppImageCaches(context: Context) {
        val appContext = context.applicationContext
        ensureBackgroundThread {
            try {
                Glide.get(appContext).clearDiskCache()
            } catch (_: Exception) {
            }
            try {
                val cacheDirs = listOfNotNull(
                    File(appContext.cacheDir, "image_manager_disk_cache"),
                    File(appContext.cacheDir, "picasso-cache"),
                    File(appContext.cacheDir, "glide"),
                    appContext.externalCacheDir
                )
                for (dir in cacheDirs) {
                    if (dir.exists() && dir.isDirectory) {
                        dir.deleteRecursively()
                    }
                }
            } catch (_: Exception) {
            }
        }

        try {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    Glide.get(appContext).clearMemory()
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
    }

    fun secureWipeAndDeleteFile(context: Context, file: File) {
        if (!file.exists()) return
        val originalPath = file.absolutePath

        try {
            val length = file.length()
            if (length > 0L) {
                RandomAccessFile(file, "rw").use { raf ->
                    val chunk = ByteArray(64 * 1024)
                    val fullWipeThreshold = 64L * 1024L * 1024L // 64 MB
                    if (length <= fullWipeThreshold) {
                        var remaining = length
                        raf.seek(0L)
                        while (remaining > 0L) {
                            val toWrite = minOf(remaining, chunk.size.toLong()).toInt()
                            raf.write(chunk, 0, toWrite)
                            remaining -= toWrite
                        }
                    } else {
                        val headSize = 16L * 1024L * 1024L
                        val midSize = 8L * 1024L * 1024L
                        val tailSize = 8L * 1024L * 1024L

                        fun wipeRegion(start: Long, count: Long) {
                            raf.seek(start)
                            var rem = count
                            while (rem > 0L) {
                                val toWrite = minOf(rem, chunk.size.toLong()).toInt()
                                raf.write(chunk, 0, toWrite)
                                rem -= toWrite
                            }
                        }

                        wipeRegion(0L, headSize)
                        wipeRegion((length / 2L) - (midSize / 2L), midSize)
                        wipeRegion(length - tailSize, tailSize)
                    }
                    raf.fd.sync()
                    raf.setLength(0L)
                    raf.fd.sync()
                }
            }
        } catch (_: Exception) {
        }

        var deleted = file.delete()
        if (!deleted) {
            deleted = context.tryFastDocumentDelete(originalPath, false)
        }
        if (!deleted && context is BaseSimpleActivity) {
            context.deleteFile(file.toFileDirItem(context), allowDeleteFolder = false, isDeletingMultipleFiles = true)
        }

        purgeTracesForFile(context, originalPath)
    }
}
