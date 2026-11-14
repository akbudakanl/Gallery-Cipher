package org.fossify.gallery

import org.fossify.gallery.helpers.HOME_VIEW_FILES
import org.fossify.gallery.helpers.HOME_VIEW_FOLDERS
import org.fossify.gallery.helpers.VaultViewPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultViewPolicyTest {

    @Test
    fun appLaunchUsesHomeScreenViewOnLaunchSetting() {
        assertFalse(VaultViewPolicy.shouldShowFilesOnAppLaunch(HOME_VIEW_FOLDERS))
        assertTrue(VaultViewPolicy.shouldShowFilesOnAppLaunch(HOME_VIEW_FILES))
    }

    @Test
    fun vaultEntryFollowsActiveViewBeforeUnlock() {
        assertFalse(VaultViewPolicy.shouldOpenVaultInFilesView(isFromFilesView = false))
        assertTrue(VaultViewPolicy.shouldOpenVaultInFilesView(isFromFilesView = true))
    }

    @Test
    fun vaultExitFollowsLastActiveVaultView() {
        assertFalse(VaultViewPolicy.shouldExitVaultToFilesView(isExitingFromFilesView = false))
        assertTrue(VaultViewPolicy.shouldExitVaultToFilesView(isExitingFromFilesView = true))
    }
}
