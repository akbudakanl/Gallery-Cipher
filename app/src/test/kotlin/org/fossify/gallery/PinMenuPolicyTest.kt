package org.fossify.gallery

import org.fossify.gallery.helpers.PinMenuPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinMenuPolicyTest {

    @Test
    fun emptySelectionHidesBothPinAndUnpin() {
        val result = PinMenuPolicy.resolve(emptyList(), setOf("/storage/emulated/0/DCIM"))
        assertFalse(result.showPin)
        assertFalse(result.showUnpin)
    }

    @Test
    fun onlyUnpinnedAlbumsShowPinAndHideUnpin() {
        val pinned = setOf("/storage/emulated/0/Favorites", "/storage/emulated/0/.Cipher/vault_pinned")

        val singleUnencrypted = PinMenuPolicy.resolve(
            selectedPaths = listOf("/storage/emulated/0/Pictures"),
            pinnedFolders = pinned
        )
        assertTrue(singleUnencrypted.showPin)
        assertFalse(singleUnencrypted.showUnpin)

        val multiEncrypted = PinMenuPolicy.resolve(
            selectedPaths = listOf(
                "/storage/emulated/0/.Cipher/vault_a",
                "virtual_album://hidden_1"
            ),
            pinnedFolders = pinned
        )
        assertTrue(multiEncrypted.showPin)
        assertFalse(multiEncrypted.showUnpin)
    }

    @Test
    fun onlyPinnedAlbumsShowUnpinAndHidePin() {
        val pinned = setOf(
            "/storage/emulated/0/DCIM",
            "/storage/emulated/0/Pictures",
            "/storage/emulated/0/.Cipher/vault_a",
            "virtual_album://hidden_1"
        )

        val singleUnencrypted = PinMenuPolicy.resolve(
            selectedPaths = listOf("/storage/emulated/0/DCIM"),
            pinnedFolders = pinned
        )
        assertFalse(singleUnencrypted.showPin)
        assertTrue(singleUnencrypted.showUnpin)

        val multiUnencrypted = PinMenuPolicy.resolve(
            selectedPaths = listOf("/storage/emulated/0/DCIM", "/storage/emulated/0/Pictures"),
            pinnedFolders = pinned
        )
        assertFalse(multiUnencrypted.showPin)
        assertTrue(multiUnencrypted.showUnpin)

        val singleEncrypted = PinMenuPolicy.resolve(
            selectedPaths = listOf("/storage/emulated/0/.Cipher/vault_a"),
            pinnedFolders = pinned
        )
        assertFalse(singleEncrypted.showPin)
        assertTrue(singleEncrypted.showUnpin)

        val multiEncrypted = PinMenuPolicy.resolve(
            selectedPaths = listOf("/storage/emulated/0/.Cipher/vault_a", "virtual_album://hidden_1"),
            pinnedFolders = pinned
        )
        assertFalse(multiEncrypted.showPin)
        assertTrue(multiEncrypted.showUnpin)
    }

    @Test
    fun mixedPinnedAndUnpinnedAlbumsHideBothPinAndUnpin() {
        val pinned = setOf(
            "/storage/emulated/0/DCIM",
            "/storage/emulated/0/.Cipher/vault_a"
        )

        val mixedUnencrypted = PinMenuPolicy.resolve(
            selectedPaths = listOf("/storage/emulated/0/DCIM", "/storage/emulated/0/Downloads"),
            pinnedFolders = pinned
        )
        assertFalse(mixedUnencrypted.showPin)
        assertFalse(mixedUnencrypted.showUnpin)

        val mixedEncrypted = PinMenuPolicy.resolve(
            selectedPaths = listOf(
                "/storage/emulated/0/.Cipher/vault_a",
                "/storage/emulated/0/.Cipher/vault_b"
            ),
            pinnedFolders = pinned
        )
        assertFalse(mixedEncrypted.showPin)
        assertFalse(mixedEncrypted.showUnpin)
    }
}
