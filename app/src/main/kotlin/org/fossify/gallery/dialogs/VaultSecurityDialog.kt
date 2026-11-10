package org.fossify.gallery.dialogs

import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.PROTECTION_PIN
import org.fossify.commons.helpers.SHOW_ALL_TABS
import org.fossify.gallery.R
import org.fossify.gallery.databinding.DialogVaultSecurityBinding
import org.fossify.gallery.extensions.applyVaultScreenProtection
import org.fossify.gallery.extensions.config
import org.fossify.gallery.helpers.CryptoManager
import org.fossify.gallery.helpers.PROTECTION_PASSWORD

class VaultSecurityDialog(
    private val activity: BaseSimpleActivity,
    private val requiredHash: String,
    private val showTabIndex: Int,
    private val minimalist: Boolean = false,
    private val isChangePasswordMode: Boolean = false,
    private val callback: (hash: String, type: Int, success: Boolean) -> Unit
) {
    private var dialog: AlertDialog? = null
    private val binding = DialogVaultSecurityBinding.inflate(activity.layoutInflater)
    private val isSetupMode = isChangePasswordMode || (!CryptoManager.hasExistingVault(activity) && (requiredHash.isEmpty() || showTabIndex == SHOW_ALL_TABS))

    private var currentType: Int = when (showTabIndex) {
        PROTECTION_PASSWORD -> PROTECTION_PASSWORD
        PROTECTION_PIN -> PROTECTION_PIN
        else -> {
            val vaultType = CryptoManager.getVaultProtectionType(activity)
            when (vaultType) {
                PROTECTION_PASSWORD -> PROTECTION_PASSWORD
                PROTECTION_PIN -> PROTECTION_PIN
                else -> if (activity.config.hiddenProtectionType == PROTECTION_PASSWORD) PROTECTION_PASSWORD else PROTECTION_PIN
            }
        }
    }

    private var firstStepSecret = ""
    private var currentPin = StringBuilder()

    init {
        setupDialog()
    }

    private fun setupDialog() {
        val textColor = activity.getProperTextColor()
        val primaryColor = activity.getProperPrimaryColor()
        val backgroundColor = activity.getProperBackgroundColor()

        binding.vaultPatternHolder.beGone()

        if (isSetupMode) {
            binding.vaultTabs.apply {
                beVisible()
                setBackgroundColor(backgroundColor)
                setTabTextColors(textColor, primaryColor)
                setSelectedTabIndicatorColor(primaryColor)

                removeAllTabs()
                val pinTab = newTab().setText(org.fossify.commons.R.string.pin).setTag(PROTECTION_PIN)
                val passwordTab = newTab().setText(R.string.alphanumeric_password).setTag(PROTECTION_PASSWORD)

                addTab(pinTab, currentType == PROTECTION_PIN)
                addTab(passwordTab, currentType == PROTECTION_PASSWORD)

                addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        val type = tab?.tag as? Int ?: PROTECTION_PIN
                        switchProtectionType(type)
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                })
            }
        } else {
            binding.vaultTabs.beGone()
        }

        if (minimalist && !isSetupMode) {
            binding.vaultStatusLabel.beGone()
        }
        binding.vaultPasswordInputLayout.hint = activity.getString(R.string.enter_password)

        setupPinInput()
        setupPasswordInput()

        switchProtectionType(currentType)

        if (!isSetupMode && CryptoManager.canUnlockWithBiometrics(activity)) {
            binding.vaultBiometricBtn.beVisible()
            binding.vaultBiometricBtn.setOnClickListener {
                promptBiometricUnlock()
            }
        } else {
            binding.vaultBiometricBtn.beGone()
        }

        val builder = activity.getAlertDialogBuilder()
        if (!minimalist) {
            builder.setNegativeButton(org.fossify.commons.R.string.cancel) { _, _ ->
                cancelDialog()
            }
        }
        builder.setOnCancelListener {
            cancelDialog()
        }

        builder.apply {
            activity.setupDialogStuff(binding.root, this) { alertDialog ->
                dialog = alertDialog
                alertDialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                binding.root.filterTouchesWhenObscured = true
                binding.vaultPinHolder.filterTouchesWhenObscured = true
                binding.vaultPasswordHolder.filterTouchesWhenObscured = true
                if (currentType == PROTECTION_PASSWORD) {
                    alertDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                    alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                    binding.vaultPasswordInput.postDelayed({
                        binding.vaultPasswordInput.requestFocus()
                        activity.showKeyboard(binding.vaultPasswordInput)
                    }, 100)
                }

                if (!isSetupMode && CryptoManager.canUnlockWithBiometrics(activity)) {
                    binding.root.post {
                        promptBiometricUnlock()
                    }
                }
            }
        }
    }

    private fun promptBiometricUnlock() {
        if (isSetupMode || !CryptoManager.canUnlockWithBiometrics(activity)) return
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
                        dialog?.dismiss()
                        callback(requiredHash, currentType, true)
                    } else {
                        activity.toast(org.fossify.commons.R.string.an_error_occurred)
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Stays in dialog for PIN/password entry
                }

                override fun onAuthenticationFailed() {
                    // Fingerprint unrecognized, biometric UI lets user retry
                }
            }
        )
        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    private fun switchProtectionType(type: Int) {
        currentType = if (type == PROTECTION_PASSWORD) PROTECTION_PASSWORD else PROTECTION_PIN
        firstStepSecret = ""
        currentPin.clear()
        binding.vaultPinDots.text = ""
        binding.vaultPasswordInput.setText("")
        binding.vaultPatternHolder.beGone()

        binding.vaultPinHolder.beVisibleIf(currentType == PROTECTION_PIN)
        binding.vaultPasswordHolder.beVisibleIf(currentType == PROTECTION_PASSWORD)

        updateStatusLabel()

        if (currentType == PROTECTION_PASSWORD) {
            dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            binding.vaultPasswordInput.postDelayed({
                binding.vaultPasswordInput.requestFocus()
                activity.showKeyboard(binding.vaultPasswordInput)
            }, 100)
        } else {
            activity.hideKeyboard()
        }
    }

    private fun updateStatusLabel() {
        if (minimalist && !isSetupMode) {
            binding.vaultStatusLabel.beGone()
            return
        }
        binding.vaultStatusLabel.beVisible()
        if (isChangePasswordMode) {
            if (currentType == PROTECTION_PASSWORD) {
                binding.vaultStatusLabel.setText(
                    if (firstStepSecret.isNotEmpty()) R.string.repeat_new_password
                    else R.string.enter_new_password
                )
            } else {
                binding.vaultStatusLabel.setText(
                    if (firstStepSecret.isNotEmpty()) R.string.repeat_new_pin
                    else R.string.enter_new_pin
                )
            }
        } else if (currentType == PROTECTION_PASSWORD) {
            binding.vaultStatusLabel.setText(
                if (isSetupMode && firstStepSecret.isNotEmpty()) R.string.repeat_password
                else R.string.enter_password
            )
        } else {
            binding.vaultStatusLabel.setText(
                if (isSetupMode && firstStepSecret.isNotEmpty()) org.fossify.commons.R.string.repeat_pin
                else org.fossify.commons.R.string.enter_pin
            )
        }
    }

    private fun setupPinInput() {
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
                handleSecretSubmission(pin)
            }
        }
    }

    private fun updatePinDots() {
        binding.vaultPinDots.text = "•".repeat(currentPin.length)
    }

    private fun setupPasswordInput() {
        binding.vaultPasswordOkBtn.setOnClickListener {
            submitPassword()
        }
        binding.vaultPasswordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_NULL) {
                submitPassword()
                true
            } else {
                false
            }
        }
    }

    private fun submitPassword() {
        val pwd = binding.vaultPasswordInput.text?.toString() ?: ""
        if (pwd.isEmpty()) {
            return
        }
        handleSecretSubmission(pwd)
    }

    private fun handleSecretSubmission(secret: String) {
        if (isSetupMode) {
            if (firstStepSecret.isEmpty()) {
                firstStepSecret = secret
                currentPin.clear()
                updatePinDots()
                binding.vaultPasswordInput.setText("")
                updateStatusLabel()
            } else if (firstStepSecret == secret) {
                val hash = CryptoManager.setupOrUpdateCredentials(activity, secret, currentType)
                activity.config.hiddenProtectionType = currentType
                activity.applyVaultScreenProtection(true)
                activity.hideKeyboard()
                dialog?.dismiss()
                callback(hash, currentType, true)
            } else {
                firstStepSecret = ""
                currentPin.clear()
                updatePinDots()
                binding.vaultPasswordInput.setText("")
                updateStatusLabel()
                val errId = if (currentType == PROTECTION_PASSWORD) R.string.wrong_password else org.fossify.commons.R.string.wrong_pin
                activity.toast(errId)
            }
        } else {
            val unlocked = CryptoManager.unlockWithSecret(activity, secret)
            if (unlocked) {
                activity.config.hiddenProtectionType = currentType
                activity.applyVaultScreenProtection(true)
                activity.hideKeyboard()
                dialog?.dismiss()
                callback(requiredHash, currentType, true)
            } else {
                currentPin.clear()
                updatePinDots()
                binding.vaultPasswordInput.setText("")
                val errId = if (currentType == PROTECTION_PASSWORD) R.string.wrong_password else org.fossify.commons.R.string.wrong_pin
                activity.toast(errId)
            }
        }
    }

    private fun cancelDialog() {
        activity.hideKeyboard()
        dialog?.dismiss()
        callback("", currentType, false)
    }
}