package org.fossify.gallery.dialogs

import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import com.google.android.material.tabs.TabLayout
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.PROTECTION_PATTERN
import org.fossify.commons.helpers.PROTECTION_PIN
import org.fossify.commons.helpers.SHOW_ALL_TABS
import org.fossify.gallery.R
import org.fossify.gallery.databinding.DialogVaultSecurityBinding
import org.fossify.gallery.helpers.CryptoManager
import org.fossify.gallery.helpers.PROTECTION_PASSWORD

class VaultSecurityDialog(
    private val activity: BaseSimpleActivity,
    private val requiredHash: String,
    private val showTabIndex: Int,
    private val minimalist: Boolean = false,
    private val callback: (hash: String, type: Int, success: Boolean) -> Unit
) {
    private var dialog: AlertDialog? = null
    private val binding = DialogVaultSecurityBinding.inflate(activity.layoutInflater)
    private val isSetupMode = requiredHash.isEmpty() || showTabIndex == SHOW_ALL_TABS

    private var currentType: Int = when {
        showTabIndex == PROTECTION_PIN || showTabIndex == PROTECTION_PASSWORD -> showTabIndex
        else -> PROTECTION_PATTERN
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

        binding.vaultPatternView.updateColors(textColor, primaryColor)

        if (isSetupMode) {
            binding.vaultTabs.apply {
                beVisible()
                setBackgroundColor(backgroundColor)
                setTabTextColors(textColor, primaryColor)
                setSelectedTabIndicatorColor(primaryColor)

                addTab(newTab().setText(org.fossify.commons.R.string.pattern).setTag(PROTECTION_PATTERN))
                addTab(newTab().setText(org.fossify.commons.R.string.pin).setTag(PROTECTION_PIN))
                addTab(newTab().setText(R.string.alphanumeric_password).setTag(PROTECTION_PASSWORD))

                addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        val type = tab?.tag as? Int ?: PROTECTION_PATTERN
                        switchProtectionType(type)
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                })
            }
        } else {
            binding.vaultTabs.beGone()
        }

        if (minimalist) {
            binding.vaultTabs.beGone()
            binding.vaultStatusLabel.beGone()
            binding.vaultPasswordInputLayout.hint = null
        }

        setupPatternInput()
        setupPinInput()
        setupPasswordInput()

        switchProtectionType(currentType)

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
                if (currentType == PROTECTION_PASSWORD) {
                    alertDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                    alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                    binding.vaultPasswordInput.requestFocus()
                    activity.showKeyboard(binding.vaultPasswordInput)
                }
            }
        }
    }

    private fun switchProtectionType(type: Int) {
        currentType = type
        firstStepSecret = ""
        currentPin.clear()
        binding.vaultPinDots.text = ""
        binding.vaultPasswordInput.setText("")
        binding.vaultPatternView.clearPattern()

        binding.vaultPatternHolder.beVisibleIf(type == PROTECTION_PATTERN)
        binding.vaultPinHolder.beVisibleIf(type == PROTECTION_PIN)
        binding.vaultPasswordHolder.beVisibleIf(type == PROTECTION_PASSWORD)

        updateStatusLabel()

        if (type == PROTECTION_PASSWORD) {
            dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            binding.vaultPasswordInput.requestFocus()
        } else {
            activity.hideKeyboard()
        }
    }

    private fun updateStatusLabel() {
        if (minimalist) {
            binding.vaultStatusLabel.beGone()
            return
        }
        binding.vaultStatusLabel.beVisible()
        when (currentType) {
            PROTECTION_PATTERN -> {
                binding.vaultStatusLabel.setText(
                    if (isSetupMode && firstStepSecret.isNotEmpty()) org.fossify.commons.R.string.repeat_pattern
                    else org.fossify.commons.R.string.insert_pattern
                )
            }
            PROTECTION_PIN -> {
                binding.vaultStatusLabel.setText(
                    if (isSetupMode && firstStepSecret.isNotEmpty()) org.fossify.commons.R.string.repeat_pin
                    else org.fossify.commons.R.string.enter_pin
                )
            }
            PROTECTION_PASSWORD -> {
                binding.vaultStatusLabel.setText(
                    if (isSetupMode && firstStepSecret.isNotEmpty()) R.string.repeat_password
                    else R.string.enter_password
                )
            }
        }
    }

    private fun setupPatternInput() {
        binding.vaultPatternView.onPatternDetectedListener = { pattern ->
            if (pattern.length < 4) {
                activity.toast(R.string.connect_at_least_4_dots_prompt)
                binding.vaultPatternView.clearPattern()
            } else {
                handleSecretSubmission(pattern)
            }
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
                binding.vaultPatternView.clearPattern()
                updateStatusLabel()
            } else if (firstStepSecret == secret) {
                val hash = CryptoManager.setupOrUpdateCredentials(activity, secret, currentType)
                activity.hideKeyboard()
                dialog?.dismiss()
                callback(hash, currentType, true)
            } else {
                firstStepSecret = ""
                currentPin.clear()
                updatePinDots()
                binding.vaultPasswordInput.setText("")
                binding.vaultPatternView.clearPattern()
                updateStatusLabel()
                val errId = when (currentType) {
                    PROTECTION_PIN -> org.fossify.commons.R.string.wrong_pin
                    PROTECTION_PATTERN -> org.fossify.commons.R.string.wrong_pattern
                    else -> R.string.wrong_password
                }
                activity.toast(errId)
            }
        } else {
            val unlocked = CryptoManager.unlockWithSecret(activity, secret)
            if (unlocked) {
                activity.hideKeyboard()
                dialog?.dismiss()
                callback(requiredHash, currentType, true)
            } else {
                currentPin.clear()
                updatePinDots()
                binding.vaultPasswordInput.setText("")
                binding.vaultPatternView.clearPattern()
                val errId = when (currentType) {
                    PROTECTION_PIN -> org.fossify.commons.R.string.wrong_pin
                    PROTECTION_PATTERN -> org.fossify.commons.R.string.wrong_pattern
                    else -> R.string.wrong_password
                }
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
