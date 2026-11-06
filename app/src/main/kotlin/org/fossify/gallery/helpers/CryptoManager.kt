package org.fossify.gallery.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import com.bumptech.glide.Glide
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

data class EncryptedFileMetadata(
    val originalName: String,
    val lastModified: Long,
    val dateTaken: Long,
    val originalSize: Long,
    val mediaType: Int,
    val videoDuration: Int,
    val payloadOffset: Long,
    val contentIv: ByteArray,
    val fileKey: ByteArray
)

object CryptoManager {
    private const val MAGIC = "FVLTENC1"
    private const val VERSION_SYMMETRIC: Byte = 1
    private const val VERSION_ASYMMETRIC: Byte = 2
    private const val META_MAGIC_SYM = "FVLTMETA"
    private const val META_MAGIC_ASYM = "FVLTMET2"
    private const val VAULT_STR_PREFIX = "FVSTR1:"
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

    val isVaultUnlocked: Boolean
        get() = unlockedMasterKey != null

    fun lockVault() {
        unlockedMasterKey?.fill(0)
        unlockedMasterKey = null
        unlockedPrivateKey = null
        metadataCache.values.forEach { it.fileKey.fill(0) }
        metadataCache.clear()
        folderNameCache.clear()
    }

    fun isEncryptedFile(path: String): Boolean {
        return path.endsWith(ENCRYPTED_FILE_EXT, ignoreCase = true)
    }

    fun isEncryptedFolder(folder: File): Boolean {
        return folder.name.startsWith(ENCRYPTED_FOLDER_PREFIX) ||
            File(folder, ENCRYPTED_FOLDER_META_FILE).exists() ||
            (folder.exists() && folder.isDirectory && folder.list { _, name -> name.endsWith(ENCRYPTED_FILE_EXT, true) }?.isNotEmpty() == true)
    }

    fun isEncryptedFolderPath(context: Context, folderPath: String): Boolean {
        val clean = folderPath.trimEnd('/')
        if (context.config.encryptedFolders.contains(clean)) {
            return true
        }
        return isEncryptedFolder(File(clean))
    }

    // ==================== KEY MANAGEMENT (PURE PBKDF2 + RSA-2048 OAEP, NO KEYSTORE) ====================

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

    /**
     * Initializes or updates the user's hidden item password/PIN/pattern credentials.
     * Both the 256-bit MasterKey and the RSA-2048 PrivateKey are encrypted strictly with a key
     * derived from the user's secret via PBKDF2WithHmacSHA256 (100,000 iterations) + AES-256-GCM.
     * Only the RSA-2048 PublicKey is stored in plain form so folders can still be encrypted while locked.
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

        val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
        val wrappingKey = deriveWrappingKey(secret, salt)
        val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val wrapped = cipher.doFinal(bundle)
        bundle.fill(0)

        val data = Base64.encodeToString(salt, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(wrapped, Base64.NO_WRAP)
        context.config.vaultWrappedKeyData = data

        unlockedMasterKey = masterKey.copyOf()
        unlockedPrivateKey = privateKey
        val marker = "pbkdf2_aes256_gcm_v1"
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
        val wrappedData = context.config.vaultWrappedKeyData
        if (wrappedData.isNotEmpty()) {
            try {
                val parts = wrappedData.split(":")
                if (parts.size == 3) {
                    val salt = Base64.decode(parts[0], Base64.NO_WRAP)
                    val iv = Base64.decode(parts[1], Base64.NO_WRAP)
                    val ciphertext = Base64.decode(parts[2], Base64.NO_WRAP)
                    val wrappingKey = deriveWrappingKey(secret, salt)
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_BITS, iv))
                    val bundle = cipher.doFinal(ciphertext)

                    if (bundle.size == 32) {
                        // Legacy bundle with only 32-byte MasterKey: upgrade to include RSA-2048 keypair
                        unlockedMasterKey = bundle
                        setupOrUpdateCredentials(context, secret, context.config.hiddenProtectionType)
                        return true
                    }

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
                    return true
                }
            } catch (e: Exception) {
                return false
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

    // ==================== FOLDER NAME ENCRYPTION (.fvault_ & .fmeta) ====================

    private val SYSTEM_PROTECTED_FOLDER_NAMES = setOf(
        "0", "dcim", "camera", "pictures", "download", "downloads", "movies", "music", "documents", "screenshots", "whatsapp", "telegram"
    )

    private fun canRenameFolderOnDisk(folder: File): Boolean {
        val parent = folder.parentFile ?: return false
        val internalRoot = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')
        if (folder.absolutePath.trimEnd('/').equals(internalRoot, ignoreCase = true)) {
            return false
        }
        if (folder.name.startsWith(ENCRYPTED_FOLDER_PREFIX)) {
            return false
        }
        if (folder.name.lowercase(Locale.ROOT) in SYSTEM_PROTECTED_FOLDER_NAMES &&
            parent.absolutePath.trimEnd('/').equals(internalRoot, ignoreCase = true)
        ) {
            return false
        }
        return true
    }

    private fun writeEncryptedFolderMeta(context: Context, folder: File, originalName: String) {
        val metaFile = File(folder, ENCRYPTED_FOLDER_META_FILE)
        val masterKey = unlockedMasterKey
        if (masterKey != null) {
            val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(masterKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            val encryptedName = cipher.doFinal(originalName.toByteArray(Charsets.UTF_8))

            FileOutputStream(metaFile).use { fos ->
                DataOutputStream(fos).use { dos ->
                    dos.write(META_MAGIC_SYM.toByteArray(Charsets.US_ASCII))
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
        val metaFile = File(folder, ENCRYPTED_FOLDER_META_FILE)
        if (!metaFile.exists()) return null

        return try {
            FileInputStream(metaFile).use { fis ->
                DataInputStream(fis).use { dis ->
                    val magicBytes = ByteArray(META_MAGIC_SYM.length)
                    dis.readFully(magicBytes)
                    val magic = String(magicBytes, Charsets.US_ASCII)
                    val aesKey: ByteArray = when (magic) {
                        META_MAGIC_SYM -> masterKey
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

    private fun randomHex(bytesCount: Int = 8): String {
        val bytes = ByteArray(bytesCount)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Encrypts a single plaintext media file into targetDir as `.fv_<hex>.fenc`.
     * Uses unlockedMasterKey if the vault is currently unlocked, or generates a random 256-bit ephemeral AES key
     * wrapped with the RSA-2048 PublicKey (`RSA/ECB/OAEPWithSHA-256AndMGF1Padding`) if locked.
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
            versionByte = VERSION_SYMMETRIC
        } else {
            val pubKeyB64 = context.config.vaultPublicKey
            if (pubKeyB64.isEmpty()) return null
            val pubKeyBytes = Base64.decode(pubKeyB64, Base64.NO_WRAP)
            fileKey = ByteArray(32).also { secureRandom.nextBytes(it) }
            rsaWrappedKey = wrapKeyWithPublicKey(pubKeyBytes, fileKey)
            versionByte = VERSION_ASYMMETRIC
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

        // Build plaintext header
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

        val headerIv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val headerCipher = Cipher.getInstance("AES/GCM/NoPadding")
        headerCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(fileKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, headerIv))
        val encryptedHeader = headerCipher.doFinal(headerPlainBytes)

        val contentIv = ByteArray(CTR_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val contentCipher = Cipher.getInstance("AES/CTR/NoPadding")
        contentCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(fileKey, "AES"), IvParameterSpec(contentIv))

        var encryptedFile = File(targetDir, "$ENCRYPTED_FILE_PREFIX${randomHex(8)}$ENCRYPTED_FILE_EXT")
        while (encryptedFile.exists()) {
            encryptedFile = File(targetDir, "$ENCRYPTED_FILE_PREFIX${randomHex(8)}$ENCRYPTED_FILE_EXT")
        }

        return try {
            val sourcePath = sourceFile.absolutePath
            FileOutputStream(encryptedFile).use { fos ->
                BufferedOutputStream(fos, 64 * 1024).use { bos ->
                    DataOutputStream(bos).use { dos ->
                        dos.write(MAGIC.toByteArray(Charsets.US_ASCII))
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
                                }
                            }
                            val finalBytes = contentCipher.doFinal()
                            if (finalBytes != null && finalBytes.isNotEmpty()) {
                                dos.write(finalBytes)
                            }
                        }
                        dos.flush()
                    }
                }
                fos.fd.sync()
            }

            encryptedFile.setLastModified(lastModified)

            if (masterKey != null) {
                val payloadOffset = (MAGIC.length + 1 + GCM_IV_LENGTH + 4 + encryptedHeader.size + CTR_IV_LENGTH).toLong()
                metadataCache[encryptedFile.absolutePath] = EncryptedFileMetadata(
                    originalName = originalName,
                    lastModified = lastModified,
                    dateTaken = dateTaken,
                    originalSize = originalSize,
                    mediaType = mediaType,
                    videoDuration = videoDuration,
                    payloadOffset = payloadOffset,
                    contentIv = contentIv,
                    fileKey = fileKey.copyOf()
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
     * Decrypts a `.fenc` file to targetDir with its original filename.
     * Requires the vault to be unlocked (`unlockedMasterKey != null`).
     */
    fun decryptFileToDirectory(
        context: Context,
        encryptedFile: File,
        targetDir: File,
        deleteEncryptedSource: Boolean = true
    ): File? {
        if (!encryptedFile.exists() || !isEncryptedFile(encryptedFile.name)) return null
        val meta = readMetadata(encryptedFile) ?: return null

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
     * Encrypts an entire folder in place:
     * 1. Adds `.nomedia`
     * 2. Encrypts all media files inside into `.fv_<hex>.fenc` and securely shreds the originals
     * 3. Stores original folder name in `.fmeta` and renames the folder to `.fvault_<hex>` (if not a system root folder)
     * 4. Purges MediaStore & Glide/Picasso caches.
     */
    fun encryptFolder(context: Context, folderPath: String): String {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) return folderPath

        val originalFolderName = getDecryptedFolderName(folder) ?: folder.name

        // Ensure .nomedia exists so Android MediaStore never indexes the folder
        val noMediaFile = File(folder, NOMEDIA)
        if (!noMediaFile.exists()) {
            try {
                noMediaFile.createNewFile()
            } catch (_: Exception) {
            }
        }

        // Write encrypted folder metadata (.fmeta) if not already written
        if (!File(folder, ENCRYPTED_FOLDER_META_FILE).exists()) {
            try {
                writeEncryptedFolderMeta(context, folder, originalFolderName)
            } catch (_: Exception) {
            }
        }

        // Encrypt every plaintext media file in the folder
        val files = folder.listFiles() ?: emptyArray()
        for (file in files) {
            if (file.isFile &&
                file.name != NOMEDIA &&
                file.name != ENCRYPTED_FOLDER_META_FILE &&
                !isEncryptedFile(file.name) &&
                (file.name.isMediaFile() || file.name.isRawFast() || file.name.isSvg())
            ) {
                encryptFileToDirectory(
                    context = context,
                    sourceFile = file,
                    targetDir = folder,
                    wipeSource = true,
                    clearCachesNow = false
                )
            }
        }

        // Rename folder on disk to .fvault_<hex> if allowed
        var finalFolder = folder
        if (canRenameFolderOnDisk(folder)) {
            val parent = folder.parentFile
            if (parent != null) {
                var targetFolder = File(parent, "$ENCRYPTED_FOLDER_PREFIX${randomHex(6)}")
                while (targetFolder.exists()) {
                    targetFolder = File(parent, "$ENCRYPTED_FOLDER_PREFIX${randomHex(6)}")
                }
                if (folder.renameTo(targetFolder)) {
                    finalFolder = targetFolder
                    folderNameCache.remove(folder.absolutePath)
                    if (isVaultUnlocked) {
                        folderNameCache[finalFolder.absolutePath] = originalFolderName
                    }
                    metadataCache.keys.filter { it.startsWith(folder.absolutePath + "/") }.forEach { oldKey ->
                        val meta = metadataCache.remove(oldKey)
                        if (meta != null) {
                            val newKey = finalFolder.absolutePath + oldKey.removePrefix(folder.absolutePath)
                            metadataCache[newKey] = meta
                        }
                    }
                    // Update any virtual album items pointing inside the old folder path
                    try {
                        val oldPrefix = folder.absolutePath.trimEnd('/') + "/"
                        val newPrefix = finalFolder.absolutePath.trimEnd('/') + "/"
                        context.virtualAlbumsDB.getAllItems().filter { it.fullPath.startsWith(oldPrefix) }.forEach { item ->
                            val newItemPath = newPrefix + item.fullPath.removePrefix(oldPrefix)
                            context.updateVirtualAlbumItemPath(item.fullPath, newItemPath)
                        }
                    } catch (_: Exception) {
                    }
                    // Update pinned/included/everShown folder references
                    if (context.config.pinnedFolders.contains(folder.absolutePath)) {
                        context.config.removePinnedFolders(setOf(folder.absolutePath))
                        context.config.addPinnedFolders(setOf(finalFolder.absolutePath))
                    }
                    val everShown = HashSet(context.config.everShownFolders)
                    if (everShown.remove(folder.absolutePath)) {
                        everShown.add(finalFolder.absolutePath)
                        context.config.everShownFolders = everShown
                    }
                }
            }
        }

        // Any virtual album referencing items inside this newly encrypted folder must also be marked hidden
        try {
            val prefix = finalFolder.absolutePath.trimEnd('/') + "/"
            val affectedAlbumIds = context.virtualAlbumsDB.getAllItems()
                .filter { it.fullPath.startsWith(prefix) || isEncryptedFile(it.fullPath) }
                .map { it.albumId }
                .toSet()
            for (albumId in affectedAlbumIds) {
                val album = context.virtualAlbumsDB.getAlbumById(albumId) ?: continue
                if (!album.isHidden || !isEncryptedVaultString(album.name)) {
                    val encName = encryptStringForVault(context, album.name)
                    context.virtualAlbumsDB.updateAlbumVisibilityAndName(albumId, true, encName)
                }
            }
        } catch (_: Exception) {
        }

        context.config.removeEncryptedFolder(folder.absolutePath)
        context.config.addEncryptedFolder(finalFolder.absolutePath)
        context.directoryDB.deleteDirPath(folder.absolutePath)
        clearAppImageCaches(context)
        return finalFolder.absolutePath
    }

    /**
     * Decrypts an encrypted folder back to its original folder name and original plaintext files.
     * Requires the vault to be unlocked (`unlockedMasterKey != null`).
     */
    fun decryptFolder(context: Context, folderPath: String): String {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) return folderPath
        if (!isVaultUnlocked) return folderPath

        val originalFolderName = getDecryptedFolderName(folder) ?: folder.name

        // Decrypt all .fenc files inside
        val files = folder.listFiles() ?: emptyArray()
        for (file in files) {
            if (file.isFile && isEncryptedFile(file.name)) {
                decryptFileToDirectory(
                    context = context,
                    encryptedFile = file,
                    targetDir = folder,
                    deleteEncryptedSource = true
                )
            }
        }

        // Remove .fmeta and .nomedia
        File(folder, ENCRYPTED_FOLDER_META_FILE).delete()
        File(folder, NOMEDIA).delete()
        folderNameCache.remove(folder.absolutePath)

        // Rename .fvault_<hex> back to originalFolderName
        var finalFolder = folder
        if (folder.name.startsWith(ENCRYPTED_FOLDER_PREFIX) && originalFolderName != folder.name) {
            val parent = folder.parentFile
            if (parent != null) {
                var targetFolder = File(parent, originalFolderName)
                var idx = 1
                while (targetFolder.exists() && targetFolder.absolutePath != folder.absolutePath) {
                    targetFolder = File(parent, "${originalFolderName}_$idx")
                    idx++
                }
                if (folder.renameTo(targetFolder)) {
                    finalFolder = targetFolder
                    try {
                        val oldPrefix = folder.absolutePath.trimEnd('/') + "/"
                        val newPrefix = finalFolder.absolutePath.trimEnd('/') + "/"
                        context.virtualAlbumsDB.getAllItems().filter { it.fullPath.startsWith(oldPrefix) }.forEach { item ->
                            val newItemPath = newPrefix + item.fullPath.removePrefix(oldPrefix)
                            context.updateVirtualAlbumItemPath(item.fullPath, newItemPath)
                        }
                    } catch (_: Exception) {
                    }
                    if (context.config.pinnedFolders.contains(folder.absolutePath)) {
                        context.config.removePinnedFolders(setOf(folder.absolutePath))
                        context.config.addPinnedFolders(setOf(finalFolder.absolutePath))
                    }
                    val everShown = HashSet(context.config.everShownFolders)
                    if (everShown.remove(folder.absolutePath)) {
                        everShown.add(finalFolder.absolutePath)
                        context.config.everShownFolders = everShown
                    }
                }
            }
        }

        context.config.removeEncryptedFolder(folder.absolutePath)
        context.config.removeEncryptedFolder(finalFolder.absolutePath)
        context.directoryDB.deleteDirPath(folder.absolutePath)

        val restoredPaths = finalFolder.listFiles()?.filter { it.isFile }?.map { it.absolutePath } ?: emptyList()
        if (restoredPaths.isNotEmpty()) {
            context.rescanPaths(ArrayList(restoredPaths))
        }
        return finalFolder.absolutePath
    }

    // ==================== ON-THE-FLY HEADER & STREAM DECRYPTION ====================

    fun readMetadata(file: File): EncryptedFileMetadata? {
        metadataCache[file.absolutePath]?.let { return it }
        val masterKey = unlockedMasterKey ?: return null
        if (!file.exists() || !file.isFile) return null

        return try {
            FileInputStream(file).use { fis ->
                DataInputStream(BufferedInputStream(fis, 8192)).use { dis ->
                    val magicBytes = ByteArray(MAGIC.length)
                    dis.readFully(magicBytes)
                    if (String(magicBytes, Charsets.US_ASCII) != MAGIC) return null

                    val version = dis.readByte()
                    var extraHeaderBytes = 0
                    val fileKey: ByteArray = when (version) {
                        VERSION_SYMMETRIC -> masterKey.copyOf()
                        VERSION_ASYMMETRIC -> {
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
                    if (encHeaderLen <= 0 || encHeaderLen > 5 * 1024 * 1024) return null

                    val encHeader = ByteArray(encHeaderLen)
                    dis.readFully(encHeader)

                    val contentIv = ByteArray(CTR_IV_LENGTH)
                    dis.readFully(contentIv)

                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(fileKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, headerIv))
                    val plainHeader = cipher.doFinal(encHeader)

                    DataInputStream(ByteArrayInputStream(plainHeader)).use { hdis ->
                        val originalName = hdis.readUTF()
                        val lastModified = hdis.readLong()
                        val dateTaken = hdis.readLong()
                        val originalSize = hdis.readLong()
                        val mediaType = hdis.readInt()
                        val videoDuration = hdis.readInt()
                        val payloadOffset = (MAGIC.length + 1 + extraHeaderBytes + GCM_IV_LENGTH + 4 + encHeaderLen + CTR_IV_LENGTH).toLong()

                        val meta = EncryptedFileMetadata(
                            originalName = originalName,
                            lastModified = lastModified,
                            dateTaken = dateTaken,
                            originalSize = originalSize,
                            mediaType = mediaType,
                            videoDuration = videoDuration,
                            payloadOffset = payloadOffset,
                            contentIv = contentIv,
                            fileKey = fileKey
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

    /**
     * Reads and decrypts the embedded thumbnail bytes from the `.fenc` header in memory.
     */
    fun readEmbeddedThumbnailBytes(file: File): ByteArray? {
        val meta = readMetadata(file) ?: return null
        if (!file.exists() || !file.isFile) return null

        return try {
            FileInputStream(file).use { fis ->
                DataInputStream(BufferedInputStream(fis, 8192)).use { dis ->
                    val magicBytes = ByteArray(MAGIC.length)
                    dis.readFully(magicBytes)
                    if (String(magicBytes, Charsets.US_ASCII) != MAGIC) return null

                    val version = dis.readByte()
                    if (version == VERSION_ASYMMETRIC) {
                        val wrappedLen = dis.readInt()
                        if (wrappedLen <= 0 || wrappedLen > 2048) return null
                        dis.skipBytes(wrappedLen)
                    } else if (version != VERSION_SYMMETRIC) {
                        return null
                    }

                    val headerIv = ByteArray(GCM_IV_LENGTH)
                    dis.readFully(headerIv)

                    val encHeaderLen = dis.readInt()
                    if (encHeaderLen <= 0 || encHeaderLen > 5 * 1024 * 1024) return null

                    val encHeader = ByteArray(encHeaderLen)
                    dis.readFully(encHeader)

                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(meta.fileKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, headerIv))
                    val plainHeader = cipher.doFinal(encHeader)

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

    /**
     * Increments a 16-byte AES-CTR IV/Counter by `blockOffset` (128-bit big-endian addition).
     */
    fun incrementCounter(baseIv: ByteArray, blockOffset: Long): ByteArray {
        if (blockOffset == 0L) return baseIv.copyOf()
        val counterInt = BigInteger(1, baseIv).add(BigInteger.valueOf(blockOffset))
        val bytes = counterInt.toByteArray()
        val result = ByteArray(CTR_IV_LENGTH)
        val copyLen = minOf(bytes.size, CTR_IV_LENGTH)
        System.arraycopy(bytes, bytes.size - copyLen, result, CTR_IV_LENGTH - copyLen, copyLen)
        return result
    }

    /**
     * Opens a streaming AES-256-CTR decrypted InputStream starting at `startOffset` within the original payload.
     * Supports O(1) random-access seeking for ExoPlayer and zero-disk-write decoding for Glide/Picasso.
     */
    fun openDecryptedContentStream(
        file: File,
        startOffset: Long = 0L
    ): InputStream? {
        val meta = readMetadata(file) ?: return null

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
            val cipher = Cipher.getInstance("AES/CTR/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(meta.fileKey, "AES"), IvParameterSpec(adjustedIv))

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
            cis
        } catch (e: Exception) {
            null
        }
    }

    // ==================== ANTI-RECOVERY / SECURE WIPE (MEASURES 1 & 2) ====================

    /**
     * Measure 1: Purges all traces of a plaintext file from Android MediaStore, system thumbnail tables,
     * and the internal Room database.
     */
    fun purgeTracesForFile(context: Context, plaintextPath: String) {
        try {
            val contentResolver = context.contentResolver
            val filesUri = MediaStore.Files.getContentUri("external")

            // Query MediaStore _ID before deleting so we can also purge system thumbnail tables
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

    /**
     * Measure 1 (Part 2): Clears Glide and Picasso disk caches so no unencrypted thumbnail remains in app cache.
     */
    fun clearAppImageCaches(context: Context) {
        val appContext = context.applicationContext
        ensureBackgroundThread {
            try {
                Glide.get(appContext).clearDiskCache()
            } catch (_: Exception) {
            }
            try {
                val cacheDirs = listOf(
                    File(appContext.cacheDir, "image_manager_disk_cache"),
                    File(appContext.cacheDir, "picasso-cache")
                )
                for (dir in cacheDirs) {
                    if (dir.exists() && dir.isDirectory) {
                        dir.deleteRecursively()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Measure 2: Overwrites the plaintext file bytes on disk using RandomAccessFile("rws") + fd.sync(),
     * truncates its length to 0, renames it to a single-character dummy name to scrub directory entry metadata,
     * and finally deletes it.
     */
    fun secureWipeAndDeleteFile(context: Context, file: File) {
        if (!file.exists()) return
        val originalPath = file.absolutePath

        // Measure 1: Remove from MediaStore, system thumbnails, and app DB
        purgeTracesForFile(context, originalPath)

        // Measure 2: Overwrite file blocks on disk, truncate to 0, rename to dummy name, delete
        try {
            val length = file.length()
            if (length > 0L) {
                RandomAccessFile(file, "rws").use { raf ->
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
                        // For huge videos (>64 MB), wipe first 16 MB, middle 8 MB, and last 8 MB
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

        // Scrub the filename in the directory entry before unlinking
        var fileToDelete = file
        try {
            val parent = file.parentFile
            if (parent != null) {
                val dummyFile = File(parent, "0")
                if (!dummyFile.exists() && file.renameTo(dummyFile)) {
                    fileToDelete = dummyFile
                }
            }
        } catch (_: Exception) {
        }

        if (!fileToDelete.delete()) {
            (context as? BaseSimpleActivity)?.deleteFileBg(
                fileToDelete.toFileDirItem(context),
                allowDeleteFolder = false,
                isDeletingMultipleFiles = true
            )
        }
    }
}
