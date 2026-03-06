package com.rosan.installer.domain.engine.model

import android.R
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.ContextCompat

data class AppEntityInfo(
    val icon: Drawable?,
    val title: String
)

fun AppEntity.getInfo(context: Context): AppEntityInfo = when (this) {
    is AppEntity.BaseEntity -> AppEntityInfo(
        icon = this.icon ?: ContextCompat.getDrawable(context, R.drawable.sym_def_app_icon),
        title = this.label ?: this.packageName
    )

    else -> {
        val packageManager = context.packageManager
        var applicationInfo: ApplicationInfo? = null
        try {
            applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                packageManager?.getApplicationInfo(
                    this.packageName,
                    PackageManager.ApplicationInfoFlags.of(0L)
                )
            else
                packageManager?.getApplicationInfo(this.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        val icon = applicationInfo?.loadIcon(packageManager)
        val label = applicationInfo?.loadLabel(packageManager)?.toString()
        AppEntityInfo(
            icon = icon ?: ContextCompat.getDrawable(context, R.drawable.sym_def_app_icon),
            title = label ?: this.packageName
        )
    }
}

fun List<AppEntity>.sortedBest(): List<AppEntity> = this.sortedWith(
    compareBy(
        {
            it.packageName
        },
        {
            it.name
        }
    )
)

fun List<AppEntity>.getInfo(context: Context): AppEntityInfo =
    this.sortedBest().first().getInfo(context)