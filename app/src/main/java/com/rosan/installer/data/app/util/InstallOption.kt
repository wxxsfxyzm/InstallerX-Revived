/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2024 Zachary Wander
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 *
 * Original source:
 * @see [InstallOption](https://github.com/zacharee/InstallWithOptions/blob/main/app/src/main/java/dev/zwander/installwithoptions/data/InstallOption.kt)
 */
package com.rosan.installer.data.app.util

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.rosan.installer.R
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.util.OSUtils

@SuppressLint("LocalContextResourcesRead")
@Composable
fun rememberInstallOptions(authorizer: ConfigEntity.Authorizer): List<InstallOption> {
    val context = LocalContext.current

    return remember(authorizer) {
        getInstallOptions(authorizer).sortedBy { opt ->
            context.resources.getString(opt.labelResource)
        }
    }
}

private fun getInstallOptions(authorizer: ConfigEntity.Authorizer) = InstallOption::class.sealedSubclasses
    .mapNotNull { it.objectInstance }
    .filter {
        // First, check if the option is compatible with the current SDK version.
        val sdkVersionMatch = Build.VERSION.SDK_INT >= it.minSdk && Build.VERSION.SDK_INT <= it.maxSdk
        if (!sdkVersionMatch) {
            return@filter false
        }

        // Second, apply custom logic based on the authorizer.
        when (it) {
            // The AllowDowngrade option should only be available when the authorizer is Root, Shizuku(running as root).
            // Or running as SystemApp
            is InstallOption.AllowDowngrade -> authorizer == ConfigEntity.Authorizer.Root || authorizer == ConfigEntity.Authorizer.Shizuku || (authorizer == ConfigEntity.Authorizer.None && OSUtils.isSystemApp)
            // All other options are available by default.
            else -> true
        }
    }

@Suppress("unused")
sealed class InstallOption(
    val minSdk: Int = Build.VERSION_CODES.BASE,
    val maxSdk: Int = Int.MAX_VALUE,
    val value: Int,
    @param:StringRes val labelResource: Int,
    @param:StringRes val descResource: Int,
) {
    @Keep
    data object ReplaceExisting : InstallOption(
        minSdk = Int.MAX_VALUE, // Don't show on List because handled elsewhere
        value = 0x00000002,
        labelResource = R.string.replace_existing,
        descResource = R.string.replace_existing_desc,
    )

    @Keep
    data object AllowTest : InstallOption(
        value = 0x00000004,
        labelResource = R.string.config_allow_test,
        descResource = R.string.config_allow_test_desc,
    )

    @Keep
    data object Internal : InstallOption(
        value = 0x00000010,
        labelResource = R.string.internal,
        descResource = R.string.internal_desc,
    )

    @Keep
    data object External : InstallOption(
        value = 0x00000008,
        maxSdk = Build.VERSION_CODES.P,
        labelResource = R.string.external,
        descResource = R.string.external_desc,
    )

    @Keep
    data object FromAdb : InstallOption(
        value = 0x00000020,
        labelResource = R.string.from_adb,
        descResource = R.string.from_adb_desc,
    )

    @Keep
    data object AllUsers : InstallOption(
        value = 0x00000040,
        labelResource = R.string.config_all_users,
        descResource = R.string.config_all_users_desc,
    )

    @Keep
    data object AllowDowngrade : InstallOption(
        value = 0x00000080 or
                0x00100000,
        // Starting Android 15, only user root or system is allowed to downgrade app.
        // Simply setting maxSdk will break root install, so we handle it elsewhere.
        // maxSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        labelResource = R.string.config_allow_downgrade,
        descResource = R.string.config_allow_downgrade_desc,
    )

    @Keep
    data object GrantAllRequestedPermissions : InstallOption(
        value = 0x00000100,
        minSdk = Build.VERSION_CODES.M,
        labelResource = R.string.config_grant_all_permissions,
        descResource = R.string.config_grant_all_permissions_desc,
    )

//    @Keep
//    data object ForceVolumeUuid : InstallOption(
//        value = PackageManager.INSTALL_FORCE_VOLUME_UUID,
//        minSdk = Build.VERSION_CODES.M,
//        labelResource = R.string.force_volume_uuid,
//        descResource = R.string.force_volume_uuid_desc,
//    )

//    @Keep
//    data object ForcePermissionPrompt : InstallOption(
//        value = PackageManager.INSTALL_FORCE_PERMISSION_PROMPT,
//        minSdk = Build.VERSION_CODES.N,
//        labelResource = R.string.force_permission_prompt,
//        descResource = R.string.force_permission_prompt_desc,
//    )

    @Keep
    data object InstantApp : InstallOption(
        value = 0x00000800,
        minSdk = Build.VERSION_CODES.N,
        labelResource = R.string.instant_app,
        descResource = R.string.instant_app_desc,
    )

    @Keep
    data object DontKillApp : InstallOption(
        value = 0x00001000,
        minSdk = Build.VERSION_CODES.N,
        labelResource = R.string.dont_kill_app,
        descResource = R.string.dont_kill_app_desc,
    )

    @Keep
    data object FullApp : InstallOption(
        value = 0x00004000,
        minSdk = Build.VERSION_CODES.O,
        labelResource = R.string.full_app,
        descResource = R.string.full_app_desc,
    )

    @Keep
    data object AllocateAggressive : InstallOption(
        value = 0x00008000,
        minSdk = Build.VERSION_CODES.O,
        labelResource = R.string.allocate_aggressive,
        descResource = R.string.allocate_aggressive_desc,
    )

    @Keep
    data object VirtualPreload : InstallOption(
        value = 0x00010000,
        minSdk = Build.VERSION_CODES.O_MR1,
        labelResource = R.string.virtual_preload,
        descResource = R.string.virtual_preload_desc,
    )

    @Keep
    data object Apex : InstallOption(
        value = 0x00020000,
        minSdk = Build.VERSION_CODES.Q,
        labelResource = R.string.apex,
        descResource = R.string.apex_desc,
    )

    @Keep
    data object EnableRollback : InstallOption(
        value = 0x00040000,
        minSdk = Build.VERSION_CODES.Q,
        labelResource = R.string.enable_rollback,
        descResource = R.string.enable_rollback_desc,
    )

    @Keep
    data object DisableVerification : InstallOption(
        value = 0x00080000,
        minSdk = Build.VERSION_CODES.Q,
        labelResource = R.string.disable_verification,
        descResource = R.string.disable_verification_desc,
    )

    @Keep
    data object Staged : InstallOption(
        value = 0x00200000,
        minSdk = Build.VERSION_CODES.Q,
        labelResource = R.string.staged,
        descResource = R.string.staged_desc,
    )

    @Keep
    data object DryRun : InstallOption(
        value = 0x00800000,
        minSdk = Build.VERSION_CODES.Q,
        maxSdk = Build.VERSION_CODES.R,
        labelResource = R.string.dry_run,
        descResource = R.string.dry_run_desc,
    )

    // This has no effect
    /* @Keep
    data object AllWhitelistRestrictedPermissions : InstallOption(
        value = 0x00400000,
        minSdk = Build.VERSION_CODES.S,
        labelResource = R.string.config_all_whitelist_restricted_permissions,
        descResource = R.string.config_all_whitelist_restricted_permissions_desc,
    )*/

    @Keep
    data object DisableAllowedApexUpdateCheck : InstallOption(
        // Bug in AOSP from 12-13 where the APEX flag here shared a value with AllWhitelistRestrictedPermissions.
        value = 0x00800000,
        minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        labelResource = R.string.disable_allowed_apex_update_check,
        descResource = R.string.disable_allowed_apex_update_check_desc,
    )

    @Keep
    data object BypassLowTargetSdkBlock : InstallOption(
        value = 0x01000000,
        minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        labelResource = R.string.config_bypass_low_target_sdk,
        descResource = R.string.config_bypass_low_target_sdk_desc,
    )

    @Keep
    data object RequestUpdateOwnerShip : InstallOption(
        value = 1 shl 25,
        minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        labelResource = R.string.config_request_update_ownership,
        descResource = R.string.config_request_update_ownership_desc,
    )

    @Keep
    data object UnArchive : InstallOption(
        value = 1 shl 30,
        minSdk = Int.MAX_VALUE, // Don't show on List because handled elsewhere
        labelResource = R.string.unarchive,
        descResource = R.string.unarchive_desc,
    )
}