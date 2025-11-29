package com.rosan.installer.data.installer.model.impl.installer.processor

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.core.graphics.drawable.toBitmap
import com.rosan.installer.data.installer.model.entity.ConfirmationDetails
import com.rosan.installer.data.recycle.util.useUserService
import com.rosan.installer.data.reflect.repo.ReflectRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.util.isSystemInstaller
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.lang.reflect.Field

class SessionProcessor : KoinComponent {
    private val context by inject<Context>()
    private val reflect by inject<ReflectRepo>()

    fun getSessionDetails(sessionId: Int, config: ConfigEntity): ConfirmationDetails {
        var label: CharSequence? = "N/A"
        var icon: Bitmap? = null

        if (context.isSystemInstaller()) {
            Timber.d("Handling CONFIRM_INSTALL as system installer.")
            val local = getSessionDetailsLocally(sessionId)
            label = local.first
            icon = local.second
        } else if (config.authorizer in listOf(
                ConfigEntity.Authorizer.Root,
                ConfigEntity.Authorizer.Shizuku,
                ConfigEntity.Authorizer.Customize
            )
        ) {
            Timber.d("Handling CONFIRM_INSTALL using ${config.authorizer} service.")
            var bundle: Bundle? = null
            useUserService(config) { bundle = it.privileged.getSessionDetails(sessionId) }

            if (bundle == null) {
                Timber.e("getSessionDetails() failed via ${config.authorizer}.")
                throw Exception("Failed to get session details from privileged service.")
            }

            bundle.let {
                label = it.getCharSequence("appLabel")
                val bytes = it.getByteArray("appIcon")
                if (bytes != null) icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } else {
            Timber.w("Received CONFIRM_INSTALL with unsupported authorizer (${config.authorizer}).")
        }
        return ConfirmationDetails(sessionId, label ?: "N/A", icon)
    }

    fun approveSession(sessionId: Int, granted: Boolean, config: ConfigEntity) {
        if (context.isSystemInstaller()) {
            Timber.d("Approving session locally as system installer.")
            approveSessionLocally(sessionId, granted)
        } else if (config.authorizer in listOf(
                ConfigEntity.Authorizer.Root,
                ConfigEntity.Authorizer.Shizuku,
                ConfigEntity.Authorizer.Customize
            )
        ) {
            Timber.d("Approving session using ${config.authorizer} service.")
            useUserService(config) { it.privileged.approveSession(sessionId, granted) }
        } else {
            Timber.w("approveSession called with unsupported authorizer (${config.authorizer}).")
        }
    }

    private fun getSessionDetailsLocally(sessionId: Int): Pair<CharSequence, Bitmap?> {
        val packageInstaller = context.packageManager.packageInstaller
        val sessionInfo = packageInstaller.getSessionInfo(sessionId)
            ?: run {
                Timber.e("Local getSessionInfo failed for id $sessionId")
                return "N/A" to null
            }

        var resolvedLabel: CharSequence? = null
        var resolvedIcon: Bitmap? = null
        var path: String? = null

        try {
            val resolvedField: Field? = reflect.getDeclaredField(
                sessionInfo::class.java, "resolvedBaseCodePath"
            )
            if (resolvedField != null) {
                resolvedField.isAccessible = true
                path = resolvedField.get(sessionInfo) as? String
            }
        } catch (e: Exception) {
            Timber.e(e, "Local reflection for 'resolvedBaseCodePath' failed")
        }

        if (!path.isNullOrEmpty()) {
            try {
                val pkgInfo = context.packageManager.getPackageArchiveInfo(
                    path, PackageManager.GET_PERMISSIONS
                )
                val appInfo = pkgInfo?.applicationInfo
                if (appInfo != null) {
                    appInfo.publicSourceDir = path
                    resolvedLabel = appInfo.loadLabel(context.packageManager)
                    val drawableIcon = appInfo.loadIcon(context.packageManager)
                    if (drawableIcon != null) {
                        resolvedIcon = (drawableIcon as? BitmapDrawable)?.bitmap
                            ?: drawableIcon.toBitmap(
                                drawableIcon.intrinsicWidth.coerceAtLeast(1),
                                drawableIcon.intrinsicHeight.coerceAtLeast(1)
                            )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Local APK parsing failed")
            }
        }

        val finalLabel = resolvedLabel ?: sessionInfo.appLabel ?: "N/A"
        val finalIcon = resolvedIcon ?: sessionInfo.appIcon
        return Pair(finalLabel, finalIcon)
    }

    private fun approveSessionLocally(sessionId: Int, granted: Boolean) {
        try {
            val installer = context.packageManager.packageInstaller
            val method = reflect.getMethod(
                installer::class.java,
                "setPermissionsResult",
                Int::class.java,
                Boolean::class.java
            )

            if (method != null) {
                method.invoke(installer, sessionId, granted)
            } else {
                Timber.e("Method setPermissionsResult not found via reflection")
            }
        } catch (e: Exception) {
            Timber.e(e, "Local approveSession failed")
            // Fallback: try abandon session
            if (!granted) {
                runCatching {
                    context.packageManager.packageInstaller.abandonSession(sessionId)
                }.onFailure {
                    Timber.e(it, "Local fallback abandonSession failed")
                }
            }
        }
    }
}