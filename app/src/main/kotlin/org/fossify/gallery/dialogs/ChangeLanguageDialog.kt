package org.fossify.gallery.dialogs

import android.content.Context
import android.content.res.Resources
import android.os.Build
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
        val ALL_SUPPORTED_CODES = listOf(
            "ar", "az", "be", "bg", "bn", "bn-BD", "bqi", "br", "bs", "ca",
            "ckb", "cr", "cs", "cy", "da", "de", "el", "en", "en-GB", "en-IN",
            "eo", "es", "es-419", "es-US", "et", "eu", "fa", "fi", "fil", "fr",
            "ga", "gl", "gu", "hi", "hr", "hu", "ia", "id", "is", "it",
            "he", "ja", "kab", "kn", "ko-KR", "kr", "lt", "ltg", "lv", "mk",
            "ml", "ms", "my", "nb-NO", "ne", "nl", "nn", "or", "pa", "pa-PK",
            "pl", "pt", "pt-BR", "pt-PT", "ro", "ru", "sat", "si", "sk", "sl",
            "sr", "sv", "ta", "te", "th", "tr", "uk", "vi", "zgh", "zh-CN",
            "zh-HK", "zh-TW"
        )

        fun getDeviceSystemLocale(context: Context): Locale {
            // 1. Android 13+ (API 33+): Official LocaleManager system service unaffected by app-specific overrides
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
                    val locales = localeManager?.systemLocales
                    if (locales != null && !locales.isEmpty) {
                        val loc = locales.get(0)
                        if (loc != null && loc.language.isNotEmpty()) return loc
                    }
                } catch (_: Throwable) {}
            }

            // 2. Read standard Android system properties via reflection
            try {
                val sysPropClass = Class.forName("android.os.SystemProperties")
                val getMethod = sysPropClass.getMethod("get", String::class.java, String::class.java)
                for (prop in listOf("persist.sys.locale", "persist.sys.locales", "ro.product.locale")) {
                    val tag = getMethod.invoke(null, prop, "") as? String
                    if (!tag.isNullOrEmpty()) {
                        val cleanTag = tag.split(",").firstOrNull()?.trim() ?: ""
                        if (cleanTag.isNotEmpty()) {
                            val loc = Locale.forLanguageTag(cleanTag)
                            if (loc.language.isNotEmpty()) return loc
                        }
                    }
                }
            } catch (_: Throwable) {}

            // 3. Read Settings.System "system_locales"
            try {
                val localesStr = android.provider.Settings.System.getString(context.contentResolver, "system_locales")
                if (!localesStr.isNullOrEmpty()) {
                    val firstTag = localesStr.split(",").firstOrNull()?.trim() ?: ""
                    if (firstTag.isNotEmpty()) {
                        val loc = Locale.forLanguageTag(firstTag)
                        if (loc.language.isNotEmpty()) return loc
                    }
                }
            } catch (_: Throwable) {}

            // 4. Saved system locale tag in app config
            try {
                val savedTag = context.config.lastSystemLocaleTag
                if (savedTag.isNotEmpty()) {
                    val loc = Locale.forLanguageTag(savedTag)
                    if (loc.language.isNotEmpty()) return loc
                }
            } catch (_: Throwable) {}

            // 5. Fallback to Resources.getSystem() or Locale.getDefault()
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val locales = Resources.getSystem().configuration.locales
                if (!locales.isEmpty) locales.get(0) else Locale.getDefault()
            } else {
                @Suppress("DEPRECATION")
                Resources.getSystem().configuration.locale ?: Locale.getDefault()
            }
        }

        /** Returns the localized native display name for [locale], applying custom overrides where needed. */
        fun getNativeDisplayName(locale: Locale): String {
            val lang = locale.language
            if (lang.equals("az", ignoreCase = true)) {
                return "Azərbaycan dili"
            }
            if (lang.equals("tr", ignoreCase = true)) {
                return "Türkçe"
            }
            if (lang.equals("en", ignoreCase = true) && locale.country.isEmpty()) {
                return "English"
            }
            return locale.getDisplayName(locale)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        }

        fun getSystemLanguageLabel(context: Context, systemLocale: Locale = getDeviceSystemLocale(context)): String {
            val exactTag = systemLocale.toLanguageTag()
            val codeMatch = ALL_SUPPORTED_CODES.firstOrNull { it.equals(exactTag, ignoreCase = true) }
            if (codeMatch != null) {
                return getNativeDisplayName(Locale.forLanguageTag(codeMatch))
            }

            val lang = systemLocale.language
            if (lang.equals("en", ignoreCase = true)) {
                return "English"
            }
            val langMatch = ALL_SUPPORTED_CODES.firstOrNull { it.equals(lang, ignoreCase = true) }
            if (langMatch != null) {
                return getNativeDisplayName(Locale.forLanguageTag(langMatch))
            }

            val nativeLanguage = systemLocale.getDisplayLanguage(systemLocale)
            if (nativeLanguage.isNotEmpty()) {
                return nativeLanguage.replaceFirstChar { if (it.isLowerCase()) it.titlecase(systemLocale) else it.toString() }
            }
            return getNativeDisplayName(systemLocale)
        }
    }

    init {
        val allOptions = ArrayList<LanguageOption>()

        // 1. System default (Always at the very top)
        val systemLocale = getDeviceSystemLocale(activity)
        val systemLangLabel = getSystemLanguageLabel(activity, systemLocale)
        allOptions.add(
            LanguageOption(
                code = "",
                label = "${activity.getString(R.string.system_default)} - $systemLangLabel",
                nativeName = activity.getString(R.string.system_default),
                englishName = "System default"
            )
        )

        // 2. All supported languages sorted alphabetically by their display name (no exceptions)
        val collator = java.text.Collator.getInstance(systemLocale).apply {
            strength = java.text.Collator.PRIMARY
        }
        val sortedLanguages = ALL_SUPPORTED_CODES.mapNotNull { tag ->
            try {
                val locale = Locale.forLanguageTag(tag)
                val nativeName = getNativeDisplayName(locale)
                val englishName = locale.getDisplayName(Locale.ENGLISH)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
                val label = nativeName
                LanguageOption(code = tag, label = label, nativeName = nativeName, englishName = englishName)
            } catch (_: Exception) {
                null
            }
        }.sortedWith { o1, o2 -> collator.compare(o1.label, o2.label) }

        allOptions.addAll(sortedLanguages)

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

        activity.config.useEnglish = if (code.isEmpty()) false else (code.equals("en", ignoreCase = true) || code.startsWith("en-", ignoreCase = true))
        AppCompatDelegate.setApplicationLocales(localeList)
        callback(code)
    }
}
