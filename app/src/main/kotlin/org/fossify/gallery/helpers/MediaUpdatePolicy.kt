package org.fossify.gallery.helpers

import org.fossify.gallery.models.Medium
import org.fossify.gallery.models.ThumbnailItem

/**
 * Pure helpers for diffing media adapter datasets and keeping multi-selection intact
 * across background refreshes.
 */
object MediaUpdatePolicy {

    /**
     * Computes a content hash of [items] ignoring transient/persistence-only fields such as
     * Room's auto-generated [Medium.id] and layout [Medium.gridPosition], so transitioning from
     * cached DB items to freshly scanned [Medium] items with identical content does not trigger
     * a redundant adapter reset.
     */
    fun computeMediaContentHash(items: List<ThumbnailItem>): Int {
        var result = 1
        for (item in items) {
            val itemHash = when (item) {
                is Medium -> {
                    var h = item.name.hashCode()
                    h = 31 * h + item.path.hashCode()
                    h = 31 * h + item.parentPath.hashCode()
                    h = 31 * h + item.modified.hashCode()
                    h = 31 * h + item.taken.hashCode()
                    h = 31 * h + item.size.hashCode()
                    h = 31 * h + item.type
                    h = 31 * h + item.videoDuration
                    h = 31 * h + item.isFavorite.hashCode()
                    h = 31 * h + item.deletedTS.hashCode()
                    h = 31 * h + item.mediaStoreId.hashCode()
                    h
                }

                else -> item.hashCode()
            }
            result = 31 * result + itemHash
        }
        return result
    }

    /**
     * Removes any key from [selectedKeys] that no longer exists among [items].
     */
    fun reconcileSelectedKeys(selectedKeys: MutableSet<Int>, items: List<ThumbnailItem>) {
        if (selectedKeys.isEmpty()) {
            return
        }

        val validKeys = HashSet<Int>(items.size)
        for (item in items) {
            if (item is Medium) {
                validKeys.add(item.path.hashCode())
            }
        }
        selectedKeys.retainAll(validKeys)
    }
}
