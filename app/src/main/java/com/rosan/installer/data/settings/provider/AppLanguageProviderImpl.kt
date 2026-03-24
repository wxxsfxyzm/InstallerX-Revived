package com.rosan.installer.data.settings.provider

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.AppLanguageOption
import com.rosan.installer.domain.settings.provider.AppLanguageProvider
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

class AppLanguageProviderImpl(private val context: Context) : AppLanguageProvider {
    private val cachedSupportedLanguages by lazy(LazyThreadSafetyMode.NONE) {
        buildList {
            add(
                AppLanguageOption(
                    languageTag = null,
                    displayName = context.getString(R.string.app_language_follow_system)
                )
            )
            addAll(loadLanguageTagsFromConfig().map(::toLanguageOption))
        }
    }

    override fun getCurrentLanguageTag(): String? =
        AppCompatDelegate.getApplicationLocales()
            .toLanguageTags()
            .substringBefore(',')
            .ifBlank { null }

    override fun getSupportedLanguages(): List<AppLanguageOption> = cachedSupportedLanguages

    override fun setCurrentLanguageTag(languageTag: String?) {
        val locales = if (languageTag.isNullOrBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    private fun loadLanguageTagsFromConfig(): List<String> {
        val parser = context.resources.getXml(R.xml.locales_config)
        val languageTags = mutableListOf<String>()

        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                    parser.getAttributeValue(ANDROID_NS, "name")?.let(languageTags::add)
                }
            }
        } finally {
            parser.close()
        }

        return languageTags
    }

    private fun toLanguageOption(languageTag: String): AppLanguageOption {
        val locale = Locale.forLanguageTag(languageTag)
        val displayName = locale.getDisplayName(locale)

        return AppLanguageOption(
            languageTag = languageTag,
            displayName = displayName.replaceFirstChar { firstChar ->
                if (firstChar.isLowerCase()) firstChar.titlecase(locale) else firstChar.toString()
            }
        )
    }

    private companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
