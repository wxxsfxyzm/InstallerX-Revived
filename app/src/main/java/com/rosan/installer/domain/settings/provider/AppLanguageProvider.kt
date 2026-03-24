package com.rosan.installer.domain.settings.provider

import com.rosan.installer.domain.settings.model.AppLanguageOption

interface AppLanguageProvider {
    fun getCurrentLanguageTag(): String?
    fun getSupportedLanguages(): List<AppLanguageOption>
    fun setCurrentLanguageTag(languageTag: String?)
}
