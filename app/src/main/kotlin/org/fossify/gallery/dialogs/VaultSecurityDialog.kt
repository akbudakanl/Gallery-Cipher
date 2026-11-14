package org.fossify.gallery.dialogs

import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.google.android.material.tabs.TabLayout
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.PROTECTION_PIN
import org.fossify.commons.helpers.SHOW_ALL_TABS
import org.fossify.gallery.R
import org.fossify.gallery.databinding.DialogVaultSecurityBinding
import org.fossify.gallery.extensions.applyVaultScreenProtection
import org.fossify.gallery.extensions.config
import org.fossify.gallery.helpers.CryptoManager
import org.fossify.gallery.helpers.PROTECTION_PASSWORD
import org.fossify.gallery.helpers.VaultAuthInputHelper

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
    private val inputHelper = VaultAuthInputHelper(activity, binding)
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

        inputHelper.setupPinInput(::handleSecretSubmission)
        inputHelper.setupPasswordInput(::handleSecretSubmission)

        switchProtectionType(currentType)

        inputHelper.setupBiometricButton(enabled = !isSetupMode) {
            dialog?.dismiss()
            callback(requiredHash, currentType, true)
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
                alertDialog.window?.apply {
                    addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    setDimAmount(1.0f)
                }
                inputHelper.applyTouchFilter()
                if (currentType == PROTECTION_PASSWORD) {
                    inputHelper.focusPasswordInput(alertDialog.window)
                }

                if (!isSetupMode && CryptoManager.canUnlockWithBiometrics(activity)) {
                    binding.root.post {
                        inputHelper.promptBiometricUnlock {
                            dialog?.dismiss()
                            callback(requiredHash, currentType, true)
                        }
                    }
                }
            }
        }
    }

    private fun switchProtectionType(type: Int) {
        currentType = if (type == PROTECTION_PASSWORD) PROTECTION_PASSWORD else PROTECTION_PIN
        firstStepSecret = ""
        inputHelper.showProtectionType(currentType, dialog?.window)
        updateStatusLabel()
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

    private fun handleSecretSubmission(secret: String) {
        if (isSetupMode) {
            if (firstStepSecret.isEmpty()) {
                firstStepSecret = secret
                inputHelper.clearInputs()
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
                inputHelper.clearInputs()
                updateStatusLabel()
                val errId = if (currentType == PROTECTION_PASSWORD) R.string.wrong_password else org.fossify.commons.R.string.wrong_pin
                activity.toast(errId)
            }
        } else if (inputHelper.unlockWithSecret(secret, currentType)) {
            dialog?.dismiss()
            callback(requiredHash, currentType, true)
        }
    }

    private fun cancelDialog() {
        activity.hideKeyboard()
        dialog?.dismiss()
        callback("", currentType, false)
    }
}