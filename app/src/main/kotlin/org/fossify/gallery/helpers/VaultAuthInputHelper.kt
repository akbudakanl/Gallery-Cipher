package org.fossify.gallery.helpers

import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.PROTECTION_PIN
import org.fossify.gallery.R
import org.fossify.gallery.databinding.DialogVaultSecurityBinding
import org.fossify.gallery.extensions.applyVaultScreenProtection
import org.fossify.gallery.extensions.config

/**
 * Shared controller for vault PIN, alphanumeric password, and biometric authentication inputs
 * backed by [DialogVaultSecurityBinding]. Used by both [VaultSecurityDialog] and [VaultLockActivity].
 */
class VaultAuthInputHelper(
    private val activity: BaseSimpleActivity,
    private val binding: DialogVaultSecurityBinding
) {
    private val currentPin = StringBuilder()

    fun applyTouchFilter() {
        binding.root.filterTouchesWhenObscured = true
        binding.vaultPinHolder.filterTouchesWhenObscured = true
        binding.vaultPasswordHolder.filterTouchesWhenObscured = true
    }

    fun setupPinInput(onPinSubmit: (String) -> Unit) {
        val digitButtons = listOf(
            binding.pin0 to "0",
            binding.pin1 to "1",
            binding.pin2 to "2",
            binding.pin3 to "3",
            binding.pin4 to "4",
            binding.pin5 to "5",
            binding.pin6 to "6",
            binding.pin7 to "7",
            binding.pin8 to "8",
            binding.pin9 to "9"
        )

        for ((btn, digit) in digitButtons) {
            btn.setOnClickListener {
                if (currentPin.length < 16) {
                    currentPin.append(digit)
                    updatePinDots()
                }
            }
        }

        binding.pinBackspace.setOnClickListener {
            if (currentPin.isNotEmpty()) {
                currentPin.deleteCharAt(currentPin.length - 1)
                updatePinDots()
            }
        }

        binding.pinBackspace.setOnLongClickListener {
            currentPin.clear()
            updatePinDots()
            true
        }

        binding.pinOk.setOnClickListener {
            val pin = currentPin.toString()
            if (pin.length < 4) {
                activity.toast(org.fossify.commons.R.string.pin_must_be_4_digits_long)
            } else {
                onPinSubmit(pin)
            }
        }
    }

    fun setupPasswordInput(onPasswordSubmit: (String) -> Unit) {
        binding.vaultPasswordOkBtn.setOnClickListener {
            submitPassword(onPasswordSubmit)
        }
        binding.vaultPasswordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_NULL) {
                submitPassword(onPasswordSubmit)
                true
            } else {
                false
            }
        }
    }

    private fun submitPassword(onPasswordSubmit: (String) -> Unit) {
        val pwd = binding.vaultPasswordInput.text?.toString() ?: ""
        if (pwd.isNotEmpty()) {
            onPasswordSubmit(pwd)
        }
    }

    fun showProtectionType(type: Int, focusWindow: Window?) {
        val resolvedType = if (type == PROTECTION_PASSWORD) PROTECTION_PASSWORD else PROTECTION_PIN
        clearInputs()
        binding.vaultPatternHolder.beGone()
        binding.vaultPinHolder.beVisibleIf(resolvedType == PROTECTION_PIN)
        binding.vaultPasswordHolder.beVisibleIf(resolvedType == PROTECTION_PASSWORD)

        if (resolvedType == PROTECTION_PASSWORD) {
            focusPasswordInput(focusWindow)
        } else {
            activity.hideKeyboard()
        }
    }

    fun focusPasswordInput(window: Window?) {
        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        binding.vaultPasswordInput.postDelayed({
            binding.vaultPasswordInput.requestFocus()
            activity.showKeyboard(binding.vaultPasswordInput)
        }, 100)
    }

    fun clearInputs() {
        currentPin.clear()
        updatePinDots()
        binding.vaultPasswordInput.setText("")
    }

    private fun updatePinDots() {
        binding.vaultPinDots.text = "•".repeat(currentPin.length)
    }

    fun setupBiometricButton(enabled: Boolean, onAuthenticated: () -> Unit) {
        if (enabled && CryptoManager.canUnlockWithBiometrics(activity)) {
            binding.vaultBiometricBtn.beVisible()
            binding.vaultBiometricBtn.setOnClickListener {
                promptBiometricUnlock(onAuthenticated)
            }
        } else {
            binding.vaultBiometricBtn.beGone()
        }
    }

    fun promptBiometricUnlock(onAuthenticated: () -> Unit) {
        if (!CryptoManager.canUnlockWithBiometrics(activity)) return
        val cipher = CryptoManager.getBiometricDecryptCipher(activity)
        if (cipher == null) {
            if (activity.config.isVaultBiometricEnabled) {
                activity.toast(R.string.biometric_key_invalidated)
            }
            binding.vaultBiometricBtn.beGone()
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_unlock_vault))
            .setSubtitle(activity.getString(R.string.biometric_unlock_prompt))
            .setNegativeButtonText(activity.getString(R.string.use_password_or_pin))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authCipher = result.cryptoObject?.cipher
                    if (authCipher != null && CryptoManager.completeBiometricUnlock(activity, authCipher)) {
                        activity.applyVaultScreenProtection(true)
                        activity.hideKeyboard()
                        onAuthenticated()
                    } else {
                        activity.toast(org.fossify.commons.R.string.an_error_occurred)
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // User stays on PIN/password screen
                }

                override fun onAuthenticationFailed() {
                    // Fingerprint unrecognized; BiometricPrompt allows retry
                }
            }
        )
        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    fun unlockWithSecret(secret: String, currentType: Int): Boolean {
        val unlocked = CryptoManager.unlockWithSecret(activity, secret)
        if (unlocked) {
            activity.config.hiddenProtectionType = currentType
            activity.applyVaultScreenProtection(true)
            activity.hideKeyboard()
            return true
        }
        clearInputs()
        val errId = if (currentType == PROTECTION_PASSWORD) R.string.wrong_password else org.fossify.commons.R.string.wrong_pin
        activity.toast(errId)
        return false
    }
}
