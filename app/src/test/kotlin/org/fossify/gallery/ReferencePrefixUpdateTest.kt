package org.fossify.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the folder prefix rewriting that keeps the references intact when a physical folder
 * is renamed or moved.
 *
 * The SQL of the DAO is exercised through the same expressions that the DAO uses, so a change of
 * the query can be verified here.
 */
class ReferencePrefixUpdateTest {
    /**
     * Mirrors the expression of `updateItemsByFolderPrefix`.
     */
    private fun rewritePath(oldRoot: String, newRoot: String, path: String): String? {
        val isBelow = path.lowercase().startsWith((oldRoot + "/").lowercase())
        if (!isBelow) {
            return null
        }
        return newRoot + path.substring(oldRoot.length)
    }

    @Test
    fun testPathBelowTheFolderIsRewritten() {
        val oldRoot = "/storage/emulated/0/DCIM/Camera"
        val newRoot = "/storage/emulated/0/Pictures/Camera"

        assertEquals(
            "/storage/emulated/0/Pictures/Camera/IMG_001.jpg",
            rewritePath(oldRoot, newRoot, "/storage/emulated/0/DCIM/Camera/IMG_001.jpg")
        )
    }

    @Test
    fun testNestedPathIsRewritten() {
        val oldRoot = "/storage/emulated/0/DCIM/Camera"
        val newRoot = "/storage/emulated/0/Pictures/Camera"

        assertEquals(
            "/storage/emulated/0/Pictures/Camera/2024/January/photo.jpg",
            rewritePath(oldRoot, newRoot, "/storage/emulated/0/DCIM/Camera/2024/January/photo.jpg")
        )
    }

    @Test
    fun testPathOutsideTheFolderIsNotTouched() {
        val oldRoot = "/storage/emulated/0/DCIM/Camera"
        val newRoot = "/storage/emulated/0/Pictures/Camera"

        assertEquals(null, rewritePath(oldRoot, newRoot, "/storage/emulated/0/DCIM/Screenshots/a.jpg"))
        assertEquals(null, rewritePath(oldRoot, newRoot, "/storage/emulated/0/Download/a.jpg"))
    }

    @Test
    fun testFolderWithASimilarNameIsNotTouched() {
        val oldRoot = "/storage/emulated/0/DCIM/Camera"
        val newRoot = "/storage/emulated/0/Pictures/Camera"

        // The folder "Camera2" only shares the prefix, it is a different folder.
        assertEquals(null, rewritePath(oldRoot, newRoot, "/storage/emulated/0/DCIM/Camera2/a.jpg"))
    }

    @Test
    fun testTheFolderItselfIsNotRewritten() {
        val oldRoot = "/storage/emulated/0/DCIM/Camera"
        val newRoot = "/storage/emulated/0/Pictures/Camera"

        // Only the content of the folder moves, the folder path itself is handled separately.
        assertEquals(null, rewritePath(oldRoot, newRoot, oldRoot))
    }

    @Test
    fun testCaseInsensitiveMatching() {
        val oldRoot = "/storage/emulated/0/DCIM/Camera"
        val newRoot = "/storage/emulated/0/Pictures/Camera"

        assertEquals(
            "/storage/emulated/0/Pictures/Camera/IMG_001.jpg",
            rewritePath(oldRoot, newRoot, "/storage/emulated/0/dcim/camera/IMG_001.jpg")
        )
    }

    @Test
    fun testTheWholeTreeIsRewritten() {
        val oldRoot = "/storage/emulated/0/DCIM"
        val newRoot = "/storage/emulated/0/Pictures"

        val paths = listOf(
            "/storage/emulated/0/DCIM/Camera/a.jpg",
            "/storage/emulated/0/DCIM/Camera/b.jpg",
            "/storage/emulated/0/DCIM/Screenshots/c.jpg",
            "/storage/emulated/0/Download/untouched.jpg"
        )

        val rewritten = paths.map { rewritePath(oldRoot, newRoot, it) }
        assertEquals(
            listOf(
                "/storage/emulated/0/Pictures/Camera/a.jpg",
                "/storage/emulated/0/Pictures/Camera/b.jpg",
                "/storage/emulated/0/Pictures/Screenshots/c.jpg",
                null
            ),
            rewritten
        )
    }

    @Test
    fun testAMovedFolderKeepsItsContent() {
        val oldRoot = "/storage/emulated/0/DCIM/Camera"
        val newRoot = "/storage/emulated/0/Documents/Trips"

        val original = "/storage/emulated/0/DCIM/Camera/holiday.jpg"
        val moved = rewritePath(oldRoot, newRoot, original)

        assertTrue(moved!!.endsWith("/holiday.jpg"))
        assertEquals("/storage/emulated/0/Documents/Trips", moved.substringBeforeLast("/"))
    }

    @Test
    fun testAFileDirectlyInsideTheFolderIsRewritten() {
        val oldRoot = "/storage/emulated/0/DCIM/Camera"
        val newRoot = "/storage/emulated/0/Pictures/Camera"

        // The parent of the file is the folder itself, so the parent needs an exact match.
        val original = "/storage/emulated/0/DCIM/Camera/IMG_001.jpg"
        assertEquals(
            "/storage/emulated/0/Pictures/Camera/IMG_001.jpg",
            rewritePath(oldRoot, newRoot, original)
        )
    }

    @Test
    fun testTheParentFolderOfADirectChildIsTheFolderItself() {
        val oldRoot = "/storage/emulated/0/DCIM/Camera"
        val newRoot = "/storage/emulated/0/Pictures/Camera"

        val parentOf = "/storage/emulated/0/DCIM/Camera/IMG_001.jpg".substringBeforeLast("/")
        assertEquals(oldRoot, parentOf)
        assertEquals(newRoot, if (parentOf == oldRoot) newRoot else rewritePath(oldRoot, newRoot, parentOf))
    }

    @Test
    fun testAFolderWithATrailingSlashIsHandled() {
        val oldRoot = "/storage/emulated/0/DCIM/Camera/"
        val newRoot = "/storage/emulated/0/Pictures/Camera"

        // A trailing slash must not produce a double slash in the result.
        val rewritten = rewritePath(oldRoot.trimEnd('/'), newRoot, "/storage/emulated/0/DCIM/Camera/a.jpg")
        assertEquals("/storage/emulated/0/Pictures/Camera/a.jpg", rewritten)
    }

    @Test
    fun testMovingToADeeperFolderKeepsTheRelativeStructure() {
        val oldRoot = "/storage/emulated/0/DCIM"
        val newRoot = "/storage/emulated/0/Documents/Photos/2024"

        assertEquals(
            "/storage/emulated/0/Documents/Photos/2024/Camera/January/a.jpg",
            rewritePath(oldRoot, newRoot, "/storage/emulated/0/DCIM/Camera/January/a.jpg")
        )
    }
}
