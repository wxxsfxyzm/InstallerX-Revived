package com.rosan.installer.ui.page.main.settings.config.edit

sealed class EditViewEvent {
    data class SnackBar(val message: String) : EditViewEvent()
    object Saved : EditViewEvent()
}