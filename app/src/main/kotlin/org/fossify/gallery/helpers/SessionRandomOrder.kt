package org.fossify.gallery.helpers

import android.content.Context
import org.fossify.gallery.extensions.isHiddenVirtualAlbumPath
import org.fossify.gallery.models.Directory
import org.fossify.gallery.models.Medium
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Provides cryptographically unpredictable, session-scoped deterministic ordering for
 * SORT_BY_RANDOM without storing any file or folder paths in global memory maps.
 *
 * - Regular media uses a process-ephemeral 256-bit key generated via [SecureRandom].
 * - Encrypted vault items/albums use a separate ephemeral 256-bit key that is zeroized
 *   immediately whenever [CryptoManager.lockVault] locks the vault.
 */
object SessionRandomOrder {
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val KEY_SIZE_BYTES = 32

    private val lock = Any()
    private val secureRandom = SecureRandom()

    private var regularSessionKey: ByteArray? = null
    private var vaultSessionKey: ByteArray? = null

    private fun createRegularMac(): Mac = synchronized(lock) {
        val key = regularSessionKey ?: ByteArray(KEY_SIZE_BYTES).also {
            secureRandom.nextBytes(it)
            regularSessionKey = it
        }
        createMac(key)
    }

    private fun createVaultMac(): Mac = synchronized(lock) {
        val key = vaultSessionKey ?: ByteArray(KEY_SIZE_BYTES).also {
            secureRandom.nextBytes(it)
            vaultSessionKey = it
        }
        createMac(key)
    }

    /**
     * Zeroizes and discards the vault-specific random ordering key.
     * Called from [CryptoManager] when the vault is locked so no key material
     * linked to vault ordering remains in memory.
     */
    fun clearVaultKey() {
        synchronized(lock) {
            vaultSessionKey?.fill(0)
            vaultSessionKey = null
        }
    }

    fun sortDirectories(context: Context, dirs: MutableList<Directory>) {
        sortByPathSessionOrder(
            items = dirs,
            pathSelector = { it.path },
            isVaultSelector = { isVaultDirectory(context, it.path) }
        )
    }

    fun sortMedia(context: Context, media: MutableList<Medium>) {
        sortByPathSessionOrder(
            items = media,
            pathSelector = { it.path },
            isVaultSelector = { isVaultMedium(context, it) }
        )
    }

    internal fun <T> sortByPathSessionOrder(
        items: MutableList<T>,
        pathSelector: (T) -> String,
        isVaultSelector: (T) -> Boolean
    ) {
        if (items.size <= 1) return

        val regularMac = createRegularMac()
        var vaultMac: Mac? = null

        val ranks = HashMap<String, Long>(items.size * 2)
        for (item in items) {
            val path = pathSelector(item)
            if (ranks.containsKey(path)) continue
            val mac = if (isVaultSelector(item)) {
                vaultMac ?: createVaultMac().also { vaultMac = it }
            } else {
                regularMac
            }
            ranks[path] = computeRank(mac, path)
        }

        items.sortWith { a, b ->
            val pathA = pathSelector(a)
            val pathB = pathSelector(b)
            val cmp = (ranks[pathA] ?: 0L).compareTo(ranks[pathB] ?: 0L)
            if (cmp != 0) cmp else pathA.compareTo(pathB)
        }
    }

    internal fun resetAllKeysForTesting() {
        synchronized(lock) {
            regularSessionKey?.fill(0)
            regularSessionKey = null
            vaultSessionKey?.fill(0)
            vaultSessionKey = null
        }
    }

    internal fun computeRankWithKey(key: ByteArray, path: String): Long {
        return computeRank(createMac(key), path)
    }

    private fun isVaultDirectory(context: Context, path: String): Boolean {
        return context.isHiddenVirtualAlbumPath(path) || CryptoManager.isEncryptedFolderPath(context, path)
    }

    private fun isVaultMedium(context: Context, medium: Medium): Boolean {
        return CryptoManager.isEncryptedFile(medium.path) ||
            CryptoManager.isEncryptedFolderPath(context, medium.parentPath)
    }

    private fun createMac(key: ByteArray): Mac {
        return Mac.getInstance(HMAC_ALGORITHM).apply {
            init(SecretKeySpec(key, HMAC_ALGORITHM))
        }
    }

    private fun computeRank(mac: Mac, path: String): Long {
        mac.reset()
        val digest = mac.doFinal(path.toByteArray(Charsets.UTF_8))
        return ByteBuffer.wrap(digest, 0, Long.SIZE_BYTES).long
    }
}
