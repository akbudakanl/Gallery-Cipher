package org.fossify.gallery.helpers

/**
 * Determines visibility of the pin and unpin actions in the directory selection menu.
 *
 * - Only unpinned selection -> show pin, hide unpin.
 * - Only pinned selection -> hide pin, show unpin.
 * - Mixed or empty selection -> hide both.
 */
object PinMenuPolicy {
    data class Visibility(val showPin: Boolean, val showUnpin: Boolean)

    fun resolve(selectedPaths: Collection<String>, pinnedFolders: Set<String>): Visibility {
        if (selectedPaths.isEmpty()) {
            return Visibility(showPin = false, showUnpin = false)
        }
        val allPinned = selectedPaths.all { pinnedFolders.contains(it) }
        val allUnpinned = selectedPaths.none { pinnedFolders.contains(it) }
        return Visibility(showPin = allUnpinned, showUnpin = allPinned)
    }
}
