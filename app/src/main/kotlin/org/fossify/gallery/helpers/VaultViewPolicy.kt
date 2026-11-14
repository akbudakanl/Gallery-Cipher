package org.fossify.gallery.helpers

/**
 * Determines which screen view (folders vs files) to show at app launch, vault entry, and vault exit.
 *
 * The startup preference ([HOME_SCREEN_VIEW_ON_LAUNCH]) only governs the initial unencrypted view
 * when the app is launched. Entering or exiting the vault always follows the active view immediately
 * preceding that transition.
 */
object VaultViewPolicy {
    fun shouldShowFilesOnAppLaunch(homeScreenViewOnLaunch: Int): Boolean {
        return homeScreenViewOnLaunch == HOME_VIEW_FILES
    }

    fun shouldOpenVaultInFilesView(isFromFilesView: Boolean): Boolean = isFromFilesView

    fun shouldExitVaultToFilesView(isExitingFromFilesView: Boolean): Boolean = isExitingFromFilesView
}
