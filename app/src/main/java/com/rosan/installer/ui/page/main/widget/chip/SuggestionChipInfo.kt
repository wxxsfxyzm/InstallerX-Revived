package com.rosan.installer.ui.page.main.widget.chip

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.KClass

// A data class to hold the information for a single suggestion chip.
// Use class instead of data class to allow for vararg checks.
class SuggestionChipInfo(
    vararg val errorClasses: KClass<out Throwable>,
    val selected: () -> Boolean,
    val onClick: () -> Unit,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
)
