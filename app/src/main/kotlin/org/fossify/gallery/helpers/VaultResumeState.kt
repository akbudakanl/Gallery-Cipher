package org.fossify.gallery.helpers

import android.content.Context
import org.json.JSONObject

/**
 * Everything needed to put the person back where they were inside the vault after the keys were wiped from RAM
 * and the password was entered again: the open album, the open file, sorting / view type / grouping and the
 * scroll offset.
 *
 * Album and file names are sensitive, so the whole state is serialized and encrypted with the vault key
 * ([CryptoManager.encryptStringForVault]) before it is written into savedInstanceState. The key is the vault's
 * own, so the blob stays unreadable while the vault is locked and can be read again only after the password
 * was entered. If the state cannot be encrypted, nothing is stored at all (never in plain text).
 */
data class VaultResumeState(
    val inSecretVault: Boolean = false,
    val displayMode: Int = 0,
    val albumPath: String = "",
    val albumName: String = "",
    val openedFilePath: String = "",
    val openedFileName: String = "",
    val sorting: Int = 0,
    val viewType: Int = 0,
    val grouping: Int = 0,
    val scrollPosition: Int = 0,
    val scrollOffset: Int = 0
) {
    private fun toJson(includeNames: Boolean = true): String = JSONObject().apply {
        put("v", 1)
        put("vault", inSecretVault)
        put("mode", displayMode)
        put("album", albumPath)
        put("file", openedFilePath)
        if (includeNames) {
            put("albumName", albumName)
            put("fileName", openedFileName)
        }
        put("sort", sorting)
        put("view", viewType)
        put("group", grouping)
        put("pos", scrollPosition)
        put("off", scrollOffset)
    }.toString()

    /** Returns the encrypted blob, or null when it cannot be encrypted (then nothing may be stored). */
    fun encrypt(context: Context): String? {
        return try {
            var json = toJson()
            // decryptStringFromVault() rejects payloads above 8 KB, so drop the optional names if ever needed
            if (json.toByteArray(Charsets.UTF_8).size > MAX_PLAIN_BYTES) {
                json = toJson(includeNames = false)
            }
            val encrypted = CryptoManager.encryptStringForVault(context, json)
            if (CryptoManager.isEncryptedVaultString(encrypted)) encrypted else null
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val MAX_PLAIN_BYTES = 6000

        /** Returns null while the vault is locked, for an invalid blob, or for plain text. */
        fun decrypt(blob: String?): VaultResumeState? {
            if (blob.isNullOrEmpty() || !CryptoManager.isEncryptedVaultString(blob)) return null
            val json = try {
                CryptoManager.decryptStringFromVault(blob)
            } catch (_: Exception) {
                null
            } ?: return null

            return try {
                val o = JSONObject(json)
                VaultResumeState(
                    inSecretVault = o.optBoolean("vault", false),
                    displayMode = o.optInt("mode", 0),
                    albumPath = o.optString("album", ""),
                    albumName = o.optString("albumName", ""),
                    openedFilePath = o.optString("file", ""),
                    openedFileName = o.optString("fileName", ""),
                    sorting = o.optInt("sort", 0),
                    viewType = o.optInt("view", 0),
                    grouping = o.optInt("group", 0),
                    scrollPosition = o.optInt("pos", 0),
                    scrollOffset = o.optInt("off", 0)
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
