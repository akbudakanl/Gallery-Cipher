package org.fossify.gallery

import org.fossify.gallery.helpers.VaultLockPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultLockPolicyTest {

    @Test
    fun armsReauthOnlyWhenVaultIsUnlockedAndExists() {
        assertTrue(VaultLockPolicy.shouldArmReauth(isVaultUnlocked = true, hasExistingVault = true))
        assertFalse(VaultLockPolicy.shouldArmReauth(isVaultUnlocked = false, hasExistingVault = true))
        assertFalse(VaultLockPolicy.shouldArmReauth(isVaultUnlocked = true, hasExistingVault = false))
        assertFalse(VaultLockPolicy.shouldArmReauth(isVaultUnlocked = false, hasExistingVault = false))
    }

    @Test
    fun showsLockScreenOnlyForNonLockNonSplashWhenPendingAndNotAlreadyOpen() {
        assertTrue(
            VaultLockPolicy.shouldShowLockScreen(
                requiresVaultReauth = true,
                isLockScreenActivity = false,
                isSplashActivity = false,
                isLockScreenAlreadyActive = false
            )
        )
        assertFalse(
            VaultLockPolicy.shouldShowLockScreen(
                requiresVaultReauth = false,
                isLockScreenActivity = false,
                isSplashActivity = false,
                isLockScreenAlreadyActive = false
            )
        )
        assertFalse(
            VaultLockPolicy.shouldShowLockScreen(
                requiresVaultReauth = true,
                isLockScreenActivity = true,
                isSplashActivity = false,
                isLockScreenAlreadyActive = false
            )
        )
        assertFalse(
            VaultLockPolicy.shouldShowLockScreen(
                requiresVaultReauth = true,
                isLockScreenActivity = false,
                isSplashActivity = true,
                isLockScreenAlreadyActive = false
            )
        )
        assertFalse(
            VaultLockPolicy.shouldShowLockScreen(
                requiresVaultReauth = true,
                isLockScreenActivity = false,
                isSplashActivity = false,
                isLockScreenAlreadyActive = true
            )
        )
    }
}
