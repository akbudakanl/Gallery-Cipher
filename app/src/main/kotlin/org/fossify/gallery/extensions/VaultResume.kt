package org.fossify.gallery.extensions

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.helpers.PROTECTION_PIN
import org.fossify.gallery.dialogs.VaultSecurityDialog
import org.fossify.gallery.helpers.CryptoManager
import org.fossify.gallery.helpers.PROTECTION_PASSWORD
import org.fossify.gallery.helpers.VaultSessionManager

/**
 * Asks for the vault password again after the vault was wiped from RAM (30 minutes in the background, process
 * restart). On success the vault is unlocked again and "temporarily show hidden" is re-enabled so the screen can
 * continue where the person left off. When the dialog is dismissed, [VaultSessionManager.markUnlockDeclined] is
 * set so that the screen below does not ask a second time right away.
 */
fun BaseSimpleActivity.promptVaultUnlock(onResult: (success: Boolean) -> Unit) {
    if (!hasVaultCreated()) {
        onResult(false)
        return
    }

    val initialType = CryptoManager.getVaultProtectionType(this)
        ?: if (config.hiddenProtectionType == PROTECTION_PASSWORD) PROTECTION_PASSWORD else PROTECTION_PIN

    VaultSecurityDialog(
        activity = this,
        requiredHash = config.hiddenPasswordHash,
        showTabIndex = initialType,
        minimalist = true
    ) { _, type, success ->
        if (success) {
            config.hiddenProtectionType = type
            config.temporarilyShowHidden = true
        } else {
            VaultSessionManager.markUnlockDeclined()
        }
        onResult(success)
    }
}

/** Returns (first visible position, offset of that item) or null when nothing is laid out yet. */
fun RecyclerView.captureScrollOffset(): Pair<Int, Int>? {
    val lm = layoutManager as? LinearLayoutManager ?: return null
    val position = lm.findFirstVisibleItemPosition()
    if (position == RecyclerView.NO_POSITION) return null
    val child = lm.findViewByPosition(position) ?: return Pair(position, 0)
    val offset = if (lm.orientation == RecyclerView.HORIZONTAL) child.left else child.top
    return Pair(position, offset)
}

fun RecyclerView.restoreScrollOffset(position: Int, offset: Int) {
    val lm = layoutManager as? LinearLayoutManager ?: return
    val count = adapter?.itemCount ?: return
    if (count == 0) return
    lm.scrollToPositionWithOffset(position.coerceIn(0, count - 1), offset)
}
