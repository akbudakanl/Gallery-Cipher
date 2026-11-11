package org.fossify.gallery

import org.fossify.gallery.helpers.CryptoManager
import org.fossify.gallery.helpers.DEFAULT_SECRET_ALBUM_NAME
import org.fossify.gallery.helpers.VAULT_DEFAULT_ALBUM_NAME
import org.fossify.gallery.helpers.VAULT_SUBFOLDER_META_FILE
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptedRenameAndDefaultAlbumTest {

    private fun setMasterKey(key: ByteArray?) {
        val field = CryptoManager::class.java.getDeclaredField("unlockedMasterKey")
        field.isAccessible = true
        field.set(CryptoManager, key)
    }

    private fun clearCaches() {
        val metaCacheField = CryptoManager::class.java.getDeclaredField("metadataCache")
        metaCacheField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (metaCacheField.get(CryptoManager) as ConcurrentHashMap<*, *>).clear()

        val folderCacheField = CryptoManager::class.java.getDeclaredField("folderNameCache")
        folderCacheField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (folderCacheField.get(CryptoManager) as ConcurrentHashMap<*, *>).clear()
    }

    private fun writeMetaV2(folder: File, masterKey: ByteArray, rawName: String) {
        val random = SecureRandom()
        val iv = ByteArray(12).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(masterKey, "AES"), GCMParameterSpec(128, iv))
        val encryptedName = cipher.doFinal(rawName.toByteArray(Charsets.UTF_8))

        FileOutputStream(File(folder, VAULT_SUBFOLDER_META_FILE)).use { fos ->
            DataOutputStream(fos).use { dos ->
                dos.write("FGMETA2".toByteArray(Charsets.US_ASCII))
                dos.write(iv)
                dos.writeInt(encryptedName.size)
                dos.write(encryptedName)
            }
        }
    }

    @Test
    fun testRenameEncryptedFileKeepsRandomBinNameAndPreservesPayload() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "fossify_rename_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            val random = SecureRandom()
            val masterKey = ByteArray(32).also { random.nextBytes(it) }
            setMasterKey(masterKey)
            clearCaches()

            val originalPayload = ByteArray(96 * 1024).also { random.nextBytes(it) }
            val originalName = "initial_secret_photo.jpg"

            val encryptedFile = CryptoManager.encryptStreamToDirectory(
                context = null,
                inputStream = ByteArrayInputStream(originalPayload),
                targetDir = tempDir,
                originalName = originalName,
                contentLength = originalPayload.size.toLong()
            )

            assertNotNull(encryptedFile)
            val binFile = encryptedFile!!
            val diskNameBefore = binFile.name
            assertTrue(diskNameBefore.endsWith(".bin"))
            assertFalse(diskNameBefore.contains("initial_secret_photo"))
            assertEquals(originalName, CryptoManager.getDisplayName(binFile.absolutePath))
            assertTrue("Integrity before rename must pass", CryptoManager.verifyFileIntegrity(binFile))

            // Rename to a longer name
            val renamedLong = "very_long_renamed_holiday_picture_2026.jpg"
            assertTrue(CryptoManager.renameEncryptedFile(binFile, renamedLong))
            assertEquals("Disk .bin filename must remain unrelated", diskNameBefore, binFile.name)
            assertFalse(binFile.name.contains("holiday"))

            // Clear in-memory cache so we verify disk persistence
            clearCaches()
            assertEquals(renamedLong, CryptoManager.getDisplayName(binFile.absolutePath))
            assertTrue(CryptoManager.verifyFileIntegrity(binFile))
            assertArrayEquals(originalPayload, CryptoManager.openDecryptedContentStream(binFile)!!.readBytes())

            // Rename to a shorter name
            val renamedShort = "a.png"
            assertTrue(CryptoManager.renameEncryptedFile(binFile, renamedShort))
            assertEquals(diskNameBefore, binFile.name)

            clearCaches()
            assertEquals(renamedShort, CryptoManager.getDisplayName(binFile.absolutePath))
            assertTrue(CryptoManager.verifyFileIntegrity(binFile))
            assertArrayEquals(originalPayload, CryptoManager.openDecryptedContentStream(binFile)!!.readBytes())
        } finally {
            setMasterKey(null)
            clearCaches()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testDefaultSecretAlbumAlwaysResolvesToFixedEnglishName() {
        val tempRoot = File(System.getProperty("java.io.tmpdir"), "fossify_default_album_test_${System.currentTimeMillis()}")
        tempRoot.mkdirs()

        try {
            val random = SecureRandom()
            val masterKey = ByteArray(32).also { random.nextBytes(it) }
            setMasterKey(masterKey)
            clearCaches()

            val variants = listOf(
                VAULT_DEFAULT_ALBUM_NAME,
                "Varsayılan Gizli Albüm",
                "Default Hidden Album",
                DEFAULT_SECRET_ALBUM_NAME
            )

            variants.forEachIndexed { idx, rawName ->
                val folder = File(tempRoot, "hex$idx")
                folder.mkdirs()
                writeMetaV2(folder, masterKey, rawName)
                clearCaches()
                assertEquals("Default Secret Album", CryptoManager.getDecryptedFolderName(folder, null))
                assertTrue(CryptoManager.isDefaultHiddenAlbum(folder))
            }

            val customFolder = File(tempRoot, "customhex")
            customFolder.mkdirs()
            writeMetaV2(customFolder, masterKey, "My Custom Album")
            clearCaches()
            assertEquals("My Custom Album", CryptoManager.getDecryptedFolderName(customFolder, null))
            assertFalse(CryptoManager.isDefaultHiddenAlbum(customFolder))
        } finally {
            setMasterKey(null)
            clearCaches()
            tempRoot.deleteRecursively()
        }
    }
}
