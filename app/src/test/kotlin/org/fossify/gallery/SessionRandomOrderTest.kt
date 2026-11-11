package org.fossify.gallery

import org.fossify.gallery.helpers.SessionRandomOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class SessionRandomOrderTest {

    @Before
    fun setUp() {
        SessionRandomOrder.resetAllKeysForTesting()
    }

    @Test
    fun regularOrderRemainsDeterministicWithinSessionAndPreservesRelativeOrderOnInsertAndDelete() {
        val initial = (1..25).map { "/storage/emulated/0/Album$it" }.toMutableList()
        val copy = initial.reversed().toMutableList()

        SessionRandomOrder.sortByPathSessionOrder(initial, { it }, { false })
        SessionRandomOrder.sortByPathSessionOrder(copy, { it }, { false })

        // Regardless of initial input order, the session order is identical.
        assertEquals(initial, copy)

        // Adding a new album preserves the relative order of all existing albums.
        val withNewAlbum = (initial + "/storage/emulated/0/NewAlbum").toMutableList()
        SessionRandomOrder.sortByPathSessionOrder(withNewAlbum, { it }, { false })
        assertEquals(initial, withNewAlbum.filter { it != "/storage/emulated/0/NewAlbum" })

        // Removing an album preserves the relative order of remaining albums.
        val removedPath = initial[5]
        val withoutAlbum = initial.filter { it != removedPath }.toMutableList()
        SessionRandomOrder.sortByPathSessionOrder(withoutAlbum, { it }, { false })
        assertEquals(initial.filter { it != removedPath }, withoutAlbum)
    }

    @Test
    fun newSessionProducesDifferentOrder() {
        val paths = (1..30).map { "/storage/emulated/0/DCIM/IMG_$it.jpg" }
        val session1 = paths.toMutableList()
        SessionRandomOrder.sortByPathSessionOrder(session1, { it }, { false })

        SessionRandomOrder.resetAllKeysForTesting()
        val session2 = paths.toMutableList()
        SessionRandomOrder.sortByPathSessionOrder(session2, { it }, { false })

        assertNotEquals(session1, session2)
    }

    @Test
    fun clearVaultKeyResetsOnlyVaultOrderWhileKeepingRegularOrderIntact() {
        val regularPaths = (1..25).map { "/storage/emulated/0/Pictures/P_$it.jpg" }
        val vaultPaths = (1..25).map { "/storage/emulated/0/.vault/V_$it.enc" }

        val regularBefore = regularPaths.toMutableList()
        val vaultBefore = vaultPaths.toMutableList()
        SessionRandomOrder.sortByPathSessionOrder(regularBefore, { it }, { false })
        SessionRandomOrder.sortByPathSessionOrder(vaultBefore, { it }, { true })

        SessionRandomOrder.clearVaultKey()

        val regularAfter = regularPaths.reversed().toMutableList()
        val vaultAfter = vaultPaths.reversed().toMutableList()
        SessionRandomOrder.sortByPathSessionOrder(regularAfter, { it }, { false })
        SessionRandomOrder.sortByPathSessionOrder(vaultAfter, { it }, { true })

        assertEquals(regularBefore, regularAfter)
        assertNotEquals(vaultBefore, vaultAfter)
    }
}
