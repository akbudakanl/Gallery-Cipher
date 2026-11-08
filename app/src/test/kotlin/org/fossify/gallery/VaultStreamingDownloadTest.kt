package org.fossify.gallery

import org.fossify.gallery.helpers.CryptoManager
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.security.SecureRandom
import java.util.regex.Pattern

class VaultStreamingDownloadTest {

    @Test
    fun testUrlPatternExtraction() {
        val pattern = Pattern.compile("https?://[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE)

        val directUrl = "https://example.com/gallery/image123.jpg"
        val matcher1 = pattern.matcher(directUrl)
        assertTrue(matcher1.find())
        assertEquals("https://example.com/gallery/image123.jpg", matcher1.group())

        val textWithUrl = "Check out this video: https://cdn.site.org/media/clip.mp4?hd=1&token=xyz in the chat"
        val matcher2 = pattern.matcher(textWithUrl)
        assertTrue(matcher2.find())
        assertEquals("https://cdn.site.org/media/clip.mp4?hd=1&token=xyz", matcher2.group())

        val invalidText = "There is no link here at all"
        val matcher3 = pattern.matcher(invalidText)
        assertFalse(matcher3.find())
    }

    @Test
    fun testStreamingEncryptionAndDecryptionRoundtrip() {
        // Create an ephemeral test directory
        val tempDir = File(System.getProperty("java.io.tmpdir"), "fossify_stream_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            // Generate 128 KB of random payload (simulating an image or video stream from network)
            val random = SecureRandom()
            val originalPayload = ByteArray(128 * 1024)
            random.nextBytes(originalPayload)

            // Setup a test master key in CryptoManager via reflection or simulated vault unlock
            val masterKeyField = CryptoManager::class.java.getDeclaredField("unlockedMasterKey")
            masterKeyField.isAccessible = true
            val testMasterKey = ByteArray(32).also { random.nextBytes(it) }
            masterKeyField.set(CryptoManager, testMasterKey)

            // Stream directly into CryptoManager.encryptStreamToDirectory
            val inputStream = ByteArrayInputStream(originalPayload)
            val filename = "test_stream_photo.jpg"

            var reportedBytes = 0L
            val encryptedFile = CryptoManager.encryptStreamToDirectory(
                context = null,
                inputStream = inputStream,
                targetDir = tempDir,
                originalName = filename,
                contentLength = originalPayload.size.toLong(),
                onProgress = { bytesRead, _ ->
                    reportedBytes = bytesRead
                }
            )

            assertNotNull("Encrypted file should be created", encryptedFile)
            assertTrue("Encrypted file should exist on disk", encryptedFile!!.exists())
            assertTrue("Encrypted file should have .bin extension", encryptedFile.name.endsWith(".bin"))
            assertEquals("Progress callback should report full length", originalPayload.size.toLong(), reportedBytes)

            // Verify that plaintext never leaked into the encrypted file directly
            val rawEncryptedBytes = encryptedFile.readBytes()
            assertFalse("Encrypted file must not contain raw plaintext chunk",
                rawEncryptedBytes.take(100).toByteArray().contentEquals(originalPayload.take(100).toByteArray())
            )

            // Verify cryptographic MAC integrity
            val isIntegrityValid = CryptoManager.verifyFileIntegrity(encryptedFile)
            assertTrue("File MAC authentication tag must verify successfully", isIntegrityValid)

            // Read metadata
            val metadata = CryptoManager.readMetadata(encryptedFile)
            assertNotNull("Metadata must be readable", metadata)
            assertEquals("Original filename must match", filename, metadata!!.originalName)
            assertEquals("Original size must match", originalPayload.size.toLong(), metadata.originalSize)

            // Decrypt stream in RAM and verify decrypted content is identical byte-for-byte
            val decryptedStream = CryptoManager.openDecryptedContentStream(encryptedFile)
            assertNotNull("Decrypted content stream should open", decryptedStream)

            val decryptedBytes = decryptedStream!!.readBytes()
            assertEquals("Decrypted length must match original", originalPayload.size, decryptedBytes.size)
            assertArrayEquals("Decrypted bytes must match original payload byte-for-byte", originalPayload, decryptedBytes)

            // Lock / clear master key
            masterKeyField.set(CryptoManager, null)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
