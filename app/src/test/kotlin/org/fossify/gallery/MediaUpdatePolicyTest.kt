package org.fossify.gallery

import org.fossify.gallery.helpers.MediaUpdatePolicy
import org.fossify.gallery.helpers.TYPE_IMAGES
import org.fossify.gallery.models.Medium
import org.fossify.gallery.models.ThumbnailSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaUpdatePolicyTest {

    private fun medium(
        id: Long?,
        path: String,
        modified: Long = 1000L,
        isFavorite: Boolean = false,
        gridPosition: Int = 0
    ) = Medium(
        id = id,
        name = path.substringAfterLast('/'),
        path = path,
        parentPath = path.substringBeforeLast('/'),
        modified = modified,
        taken = modified,
        size = 2048L,
        type = TYPE_IMAGES,
        videoDuration = 0,
        isFavorite = isFavorite,
        deletedTS = 0L,
        mediaStoreId = 10L,
        gridPosition = gridPosition
    )

    @Test
    fun contentHashIgnoresDatabaseIdAndGridPosition() {
        val cached = listOf(
            ThumbnailSection("Today"),
            medium(id = 1L, path = "/storage/emulated/0/DCIM/a.jpg", gridPosition = 0),
            medium(id = 2L, path = "/storage/emulated/0/DCIM/b.jpg", gridPosition = 1)
        )
        val scanned = listOf(
            ThumbnailSection("Today"),
            medium(id = null, path = "/storage/emulated/0/DCIM/a.jpg", gridPosition = 5),
            medium(id = null, path = "/storage/emulated/0/DCIM/b.jpg", gridPosition = 6)
        )

        assertEquals(
            MediaUpdatePolicy.computeMediaContentHash(cached),
            MediaUpdatePolicy.computeMediaContentHash(scanned)
        )
    }

    @Test
    fun contentHashChangesWhenMediaContentOrOrderChanges() {
        val initial = listOf(
            medium(id = 1L, path = "/storage/emulated/0/DCIM/a.jpg"),
            medium(id = 2L, path = "/storage/emulated/0/DCIM/b.jpg")
        )
        val reordered = listOf(
            medium(id = null, path = "/storage/emulated/0/DCIM/b.jpg"),
            medium(id = null, path = "/storage/emulated/0/DCIM/a.jpg")
        )
        val modifiedItem = listOf(
            medium(id = null, path = "/storage/emulated/0/DCIM/a.jpg", modified = 2000L),
            medium(id = null, path = "/storage/emulated/0/DCIM/b.jpg")
        )

        assertNotEquals(
            MediaUpdatePolicy.computeMediaContentHash(initial),
            MediaUpdatePolicy.computeMediaContentHash(reordered)
        )
        assertNotEquals(
            MediaUpdatePolicy.computeMediaContentHash(initial),
            MediaUpdatePolicy.computeMediaContentHash(modifiedItem)
        )
    }

    @Test
    fun reconcileSelectedKeysKeepsRemainingItemsAndDropsRemovedOnes() {
        val pathA = "/storage/emulated/0/DCIM/a.jpg"
        val pathB = "/storage/emulated/0/DCIM/b.jpg"
        val pathC = "/storage/emulated/0/DCIM/c.jpg"
        val selectedKeys = linkedSetOf(pathA.hashCode(), pathB.hashCode())

        val updatedItems = listOf(
            ThumbnailSection("Today"),
            medium(id = null, path = pathA),
            medium(id = null, path = pathC)
        )

        MediaUpdatePolicy.reconcileSelectedKeys(selectedKeys, updatedItems)

        assertEquals(linkedSetOf(pathA.hashCode()), selectedKeys)
    }

    @Test
    fun reconcileSelectedKeysBecomesEmptyWhenAllSelectedItemsRemoved() {
        val pathA = "/storage/emulated/0/DCIM/a.jpg"
        val pathB = "/storage/emulated/0/DCIM/b.jpg"
        val selectedKeys = linkedSetOf(pathA.hashCode())

        MediaUpdatePolicy.reconcileSelectedKeys(
            selectedKeys,
            listOf(medium(id = null, path = pathB))
        )

        assertTrue(selectedKeys.isEmpty())
    }
}
