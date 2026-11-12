package org.fossify.gallery

import org.fossify.gallery.helpers.MediaIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

/**
 * Tests for the identity layer that lets a reference survive a rename or a move.
 */
class MediaIdentityTest {
    private fun createTempDir(name: String): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "fossify_identity_${name}_${System.currentTimeMillis()}")
        dir.mkdirs()
        return dir
    }

    private fun writeFile(file: File, size: Int, seed: Byte = 1): File {
        val content = ByteArray(size) { ((it + seed) % 251).toByte() }
        file.writeBytes(content)
        return file
    }

    @Test
    fun testFingerprintIsStableForTheSameContent() {
        val dir = createTempDir("stable")
        try {
            val a = writeFile(File(dir, "a.bin"), 4096, 7)
            val b = writeFile(File(dir, "b.bin"), 4096, 7)

            assertEquals(
                MediaIdentity.computeFingerprint(a),
                MediaIdentity.computeFingerprint(b)
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun testFingerprintChangesWithTheContent() {
        val dir = createTempDir("changed")
        try {
            val a = writeFile(File(dir, "a.bin"), 4096, 7)
            val b = writeFile(File(dir, "b.bin"), 4096, 9)

            assertNotEquals(
                MediaIdentity.computeFingerprint(a),
                MediaIdentity.computeFingerprint(b)
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun testFingerprintSurvivesARename() {
        val dir = createTempDir("rename")
        try {
            val original = writeFile(File(dir, "before.jpg"), 8192, 3)
            val before = MediaIdentity.computeFingerprint(original)

            val renamed = File(dir, "after.jpg")
            assertTrue(original.renameTo(renamed))

            assertEquals(before, MediaIdentity.computeFingerprint(renamed))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun testFingerprintSurvivesAMove() {
        val dir = createTempDir("move")
        try {
            val source = File(dir, "source")
            source.mkdirs()
            val target = File(dir, "target")
            target.mkdirs()

            val original = writeFile(File(source, "photo.jpg"), 8192, 3)
            val before = MediaIdentity.computeFingerprint(original)

            val moved = File(target, "photo.jpg")
            assertTrue(original.renameTo(moved))

            assertEquals(before, MediaIdentity.computeFingerprint(moved))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun testFingerprintUsesTheHeadAndTheTailOfALargeFile() {
        val dir = createTempDir("large")
        try {
            // A file that is larger than the sampled chunks. Only the head and the tail are
            // hashed, so a difference in the middle does not change the fingerprint.
            val file = File(dir, "large.bin")
            RandomAccessFile(file, "rw").use { raf ->
                raf.setLength(1024 * 1024)
                raf.seek(0)
                raf.write(ByteArray(64 * 1024) { 1 })
                raf.seek(1024 * 1024 - 64 * 1024)
                raf.write(ByteArray(64 * 1024) { 2 })
            }

            val before = MediaIdentity.computeFingerprint(file)

            // Change only the middle of the file.
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(512 * 1024)
                raf.write(ByteArray(1024) { 9 })
            }

            assertEquals(before, MediaIdentity.computeFingerprint(file))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun testIdentityOfAMissingFileIsNotUsable() {
        val identity = MediaIdentity.of(File("/does/not/exist.jpg"))
        assertFalse(identity.isUsable)
        assertFalse(identity.hasFingerprint)
    }

    @Test
    fun testIdentityCapturesTheSizeAndTheTimestamp() {
        val dir = createTempDir("meta")
        try {
            val file = writeFile(File(dir, "meta.bin"), 2048, 5)
            val identity = MediaIdentity.of(file)

            assertEquals(2048L, identity.size)
            assertEquals(file.lastModified(), identity.lastModified)
            assertTrue(identity.hasFingerprint)
            assertTrue(identity.isUsable)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun testEmptyFilesHaveNoFingerprint() {
        val dir = createTempDir("empty")
        try {
            val file = File(dir, "empty.bin")
            file.writeBytes(ByteArray(0))

            assertEquals("", MediaIdentity.computeFingerprint(file))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun testFingerprintOfAMissingFileIsEmpty() {
        assertEquals("", MediaIdentity.computeFingerprint(File("/does/not/exist.jpg")))
    }
}
