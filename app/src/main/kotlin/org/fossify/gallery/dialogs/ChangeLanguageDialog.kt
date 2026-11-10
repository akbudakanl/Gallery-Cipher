package org.fossify.gallery.dialogs

import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.toast
import org.fossify.gallery.R
import org.fossify.gallery.databinding.DialogSelectLanguageBinding
import org.fossify.gallery.extensions.config
import java.util.Locale

class ChangeLanguageDialog(
    private val activity: BaseSimpleActivity,
    private val callback: (selectedTag: String) -> Unit
) {
    data class LanguageOption(
        val code: String,
        val label: String,
        val nativeName: String,
        val englishName: String
    ) {
        override fun toString(): String = label
    }

    private var dialog: AlertDialog? = null
    private val binding = DialogSelectLanguageBinding.inflate(activity.layoutInflater)

    companion object {
        private val ALL_SUPPORTED_CODES = listOf(
            "ar", "az", "be", "bg", "bn", "bn-BD", "bqi", "br", "bs", "ca",
            "ckb", "cr", "cs", "cy", "da", "de", "el", "en-GB", "en-IN",
            "eo", "es", "es-419", "es-US", "et", "eu", "fa", "fi", "fil", "fr",
            "ga", "gl", "gu", "hi", "hr", "hu", "ia", "id", "is", "it",
            "he", "ja", "kab", "kn", "ko-KR", "kr", "lt", "ltg", "lv", "mk",
            "ml", "ms", "my", "nb-NO", "ne", "nl", "nn", "or", "pa", "pa-PK",
            "pl", "pt", "pt-BR", "pt-PT", "ro", "ru", "sat", "si", "sk", "sl",
            "sr", "sv", "ta", "te", "th", "uk", "vi", "zgh", "zh-CN",
            "zh-HK", "zh-TW"
        )
    }

    init {
        val allOptions = ArrayList<LanguageOption>()

        // 1. System default
        val systemLocale = Locale.getDefault()
        val systemNative = systemLocale.getDisplayName(systemLocale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(systemLocale) else it.toString() }
        allOptions.add(
            LanguageOption(
                code = "",
                label = "${activity.getString(R.string.system_default)} ($systemNative)",
                nativeName = activity.getString(R.string.system_default),
                englishName = "System default"
            )
        )

        // 2. Turkish (pinned at top for quick access)
        val trLocale = Locale.forLanguageTag("tr")
        allOptions.add(
            LanguageOption(
                code = "tr",
                label = "Türkçe (Turkish)",
                nativeName = "Türkçe",
                englishName = "Turkish"
            )
        )

        // 3. English (pinned at top for quick access)
        allOptions.add(
            LanguageOption(
                code = "en",
                label = "English",
                nativeName = "English",
                englishName = "English"
            )
        )

        // 4. Other languages sorted alphabetically by their display name
        val otherLanguages = ALL_SUPPORTED_CODES.mapNotNull { tag ->
            try {
                val locale = Locale.forLanguageTag(tag)
                val nativeName = locale.getDisplayName(locale)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                val englishName = locale.getDisplayName(Locale.ENGLISH)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
                val label = if (nativeName.equals(englishName, ignoreCase = true)) {
                    nativeName
                } else {
                    "$nativeName ($englishName)"
                }
                LanguageOption(code = tag, label = label, nativeName = nativeName, englishName = englishName)
            } catch (_: Exception) {
                null
            }
        }.sortedBy { it.label.lowercase(Locale.getDefault()) }

        allOptions.addAll(otherLanguages)

        val filteredOptions = ArrayList(allOptions)
        val adapter = ArrayAdapter(
            activity,
            android.R.layout.simple_list_item_single_choice,
            filteredOptions
        )
        binding.languageList.adapter = adapter
        binding.languageList.choiceMode = ListView.CHOICE_MODE_SINGLE

        // Determine currently active language tag
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentTag = if (!currentLocales.isEmpty) {
            currentLocales.get(0)?.toLanguageTag() ?: ""
        } else if (activity.config.useEnglish) {
            "en"
        } else {
            ""
        }

        fun updateSelection() {
            var selectedIdx = -1
            for (i in filteredOptions.indices) {
                val opt = filteredOptions[i]
                if (currentTag.isEmpty() && opt.code.isEmpty()) {
                    selectedIdx = i
                    break
                } else if (currentTag.isNotEmpty() && (opt.code.equals(currentTag, ignoreCase = true) ||
                            (currentTag.startsWith("en", ignoreCase = true) && opt.code.equals("en", ignoreCase = true)) ||
                            (currentTag.startsWith("tr", ignoreCase = true) && opt.code.equals("tr", ignoreCase = true)))) {
                    selectedIdx = i
                    break
                }
            }
            if (selectedIdx >= 0) {
                binding.languageList.setItemChecked(selectedIdx, true)
                binding.languageList.setSelection(selectedIdx)
            }
        }

        updateSelection()

        binding.languageSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                filteredOptions.clear()
                if (query.isEmpty()) {
                    filteredOptions.addAll(allOptions)
                } else {
                    filteredOptions.addAll(allOptions.filter {
                        it.label.contains(query, ignoreCase = true) ||
                                it.code.contains(query, ignoreCase = true) ||
                                it.nativeName.contains(query, ignoreCase = true) ||
                                it.englishName.contains(query, ignoreCase = true)
                    })
                }
                adapter.notifyDataSetChanged()
                updateSelection()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        })

        binding.languageList.setOnItemClickListener { _, _, position, _ ->
            if (position in filteredOptions.indices) {
                val chosen = filteredOptions[position]
                dialog?.dismiss()
                applyLanguage(chosen.code)
            }
        }

        val builder = activity.getAlertDialogBuilder()
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .setNeutralButton(R.string.open_system_language_settings) { _, _ ->
                try {
                    activity.launchChangeAppLanguageIntent()
                } catch (_: Throwable) {
                    activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                }
            }

        activity.setupDialogStuff(binding.root, builder, R.string.language) { alertDialog ->
            dialog = alertDialog
        }
    }

    private fun applyLanguage(code: String) {
        val localeList = if (code.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(code)
        }

        activity.config.useEnglish = code.equals("en", ignoreCase = true) || code.startsWith("en-", ignoreCase = true)
        AppCompatDelegate.setApplicationLocales(localeList)
        callback(code)
    }
}
