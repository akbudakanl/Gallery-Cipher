package org.fossify.gallery.activities

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.helpers.PROTECTION_PIN
import org.fossify.gallery.R
import org.fossify.gallery.databinding.ActivityVaultLockBinding
import org.fossify.gallery.extensions.applyVaultScreenProtection
import org.fossify.gallery.extensions.config
import org.fossify.gallery.helpers.CryptoManager
import org.fossify.gallery.helpers.PROTECTION_PASSWORD
import org.fossify.gallery.helpers.VaultAuthInputHelper
import org.fossify.gallery.helpers.VaultSessionManager

/**
 * Isolated full-screen lock gate shown when the user returns from background or turns the screen
 * back on while the vault is unlocked (inside the vault or keeping encrypted albums visible).
 */
class VaultLockActivity : SimpleActivity() {

    private lateinit var binding: ActivityVaultLockBinding
    private lateinit var inputHelper: VaultAuthInputHelper
    private var currentType: Int = PROTECTION_PIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VaultSessionManager.setLockScreenOpen(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        binding = ActivityVaultLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val backgroundColor = getProperBackgroundColor()
        val textColor = getProperTextColor()
        binding.vaultLockRoot.setBackgroundColor(backgroundColor)
        binding.vaultLockRoot.filterTouchesWhenObscured = true
        binding.vaultLockIcon.applyColorFilter(textColor)
        binding.vaultLockTitle.setTextColor(textColor)

        val securityBinding = binding.vaultSecurityContent
        securityBinding.vaultTabs.beGone()
        securityBinding.vaultStatusLabel.beGone()
        securityBinding.vaultPasswordInputLayout.hint = getString(R.string.enter_password)

        currentType = CryptoManager.getVaultProtectionType(this)
            ?: if (config.hiddenProtectionType == PROTECTION_PASSWORD) PROTECTION_PASSWORD else PROTECTION_PIN

        inputHelper = VaultAuthInputHelper(this, securityBinding)
        inputHelper.applyTouchFilter()
        inputHelper.setupPinInput(::handleSecretSubmission)
        inputHelper.setupPasswordInput(::handleSecretSubmission)
        inputHelper.showProtectionType(currentType, window)
        inputHelper.setupBiometricButton(enabled = true) {
            onAuthenticated()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })

        if (CryptoManager.canUnlockWithBiometrics(this)) {
            binding.root.post {
                if (!isFinishing && !isDestroyed) {
                    inputHelper.promptBiometricUnlock {
                        onAuthenticated()
                    }
                }
            }
        }
    }

    override fun isVaultAssistProtectionActive(): Boolean = true

    override fun onResume() {
        super.onResume()
        applyVaultScreenProtection(true)
    }

    override fun onDestroy() {
        VaultSessionManager.setLockScreenOpen(false)
        super.onDestroy()
    }

    private fun handleSecretSubmission(secret: String) {
        if (inputHelper.unlockWithSecret(secret, currentType)) {
            onAuthenticated()
        }
    }

    private fun onAuthenticated() {
        VaultSessionManager.onVaultReauthenticated(this)
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
