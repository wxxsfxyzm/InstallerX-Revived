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
package com.rosan.installer.domain.engine.model.install

import android.content.pm.PackageManagerHidden
import android.os.Build
import androidx.annotation.StringRes
import com.rosan.installer.R

enum class InstallOption(
    val minSdk: Int = Build.VERSION_CODES.BASE,
    val maxSdk: Int = Int.MAX_VALUE,
    val value: Int,
    @param:StringRes val labelResource: Int,
    @param:StringRes val descResource: Int,
) {

    AllowTest(
        value = PackageManagerHidden.INSTALL_ALLOW_TEST,
        labelResource = R.string.config_allow_test,
        descResource = R.string.config_allow_test_desc,
    ),

    Internal(
        value = PackageManagerHidden.INSTALL_INTERNAL,
        labelResource = R.string.internal,
        descResource = R.string.internal_desc,
    ),

    External(
        value = PackageManagerHidden.INSTALL_EXTERNAL,
        maxSdk = Build.VERSION_CODES.P,
        labelResource = R.string.external,
        descResource = R.string.external_desc,
    ),

    FromAdb(
        value = PackageManagerHidden.INSTALL_FROM_ADB,
        labelResource = R.string.from_adb,
        descResource = R.string.from_adb_desc,
    ),

    AllUsers(
        value = PackageManagerHidden.INSTALL_ALL_USERS,
        labelResource = R.string.config_all_users,
        descResource = R.string.config_all_users_desc,
    ),

    AllowDowngrade(
        value = PackageManagerHidden.INSTALL_REQUEST_DOWNGRADE or
                PackageManagerHidden.INSTALL_ALLOW_DOWNGRADE,
        labelResource = R.string.config_allow_downgrade,
        descResource = R.string.config_allow_downgrade_desc,
    ),

    GrantAllRequestedPermissions(
        value = PackageManagerHidden.INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS,
        minSdk = Build.VERSION_CODES.M,
        labelResource = R.string.config_grant_all_permissions,
        descResource = R.string.config_grant_all_permissions_desc,
    ),

    InstantApp(
        value = PackageManagerHidden.INSTALL_INSTANT_APP,
        minSdk = Build.VERSION_CODES.N,
        labelResource = R.string.instant_app,
        descResource = R.string.instant_app_desc,
    ),

    DontKillApp(
        value = PackageManagerHidden.INSTALL_DONT_KILL_APP,
        minSdk = Build.VERSION_CODES.N,
        labelResource = R.string.dont_kill_app,
        descResource = R.string.dont_kill_app_desc,
    ),

    FullApp(
        value = PackageManagerHidden.INSTALL_FULL_APP,
        minSdk = Build.VERSION_CODES.O,
        labelResource = R.string.full_app,
        descResource = R.string.full_app_desc,
    ),

    AllocateAggressive(
        value = PackageManagerHidden.INSTALL_ALLOCATE_AGGRESSIVE,
        minSdk = Build.VERSION_CODES.O,
        labelResource = R.string.allocate_aggressive,
        descResource = R.string.allocate_aggressive_desc,
    ),

    VirtualPreload(
        value = PackageManagerHidden.INSTALL_VIRTUAL_PRELOAD,
        minSdk = Build.VERSION_CODES.O_MR1,
        labelResource = R.string.virtual_preload,
        descResource = R.string.virtual_preload_desc,
    ),

    Apex(
        value = PackageManagerHidden.INSTALL_APEX,
        minSdk = Build.VERSION_CODES.Q,
        labelResource = R.string.apex,
        descResource = R.string.apex_desc,
    ),

    EnableRollback(
        value = PackageManagerHidden.INSTALL_ENABLE_ROLLBACK,
        minSdk = Build.VERSION_CODES.Q,
        labelResource = R.string.enable_rollback,
        descResource = R.string.enable_rollback_desc,
    ),

    DisableVerification(
        value = PackageManagerHidden.INSTALL_DISABLE_VERIFICATION,
        minSdk = Build.VERSION_CODES.Q,
        labelResource = R.string.disable_verification,
        descResource = R.string.disable_verification_desc,
    ),

    Staged(
        value = PackageManagerHidden.INSTALL_STAGED,
        minSdk = Build.VERSION_CODES.Q,
        labelResource = R.string.staged,
        descResource = R.string.staged_desc,
    ),

    DryRun(
        value = PackageManagerHidden.INSTALL_DRY_RUN,
        minSdk = Build.VERSION_CODES.Q,
        maxSdk = Build.VERSION_CODES.R,
        labelResource = R.string.dry_run,
        descResource = R.string.dry_run_desc,
    ),

    DisableAllowedApexUpdateCheck(
        value = PackageManagerHidden.INSTALL_DISABLE_ALLOWED_APEX_UPDATE_CHECK,
        minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        labelResource = R.string.disable_allowed_apex_update_check,
        descResource = R.string.disable_allowed_apex_update_check_desc,
    ),

    BypassLowTargetSdkBlock(
        value = PackageManagerHidden.INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK,
        minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        labelResource = R.string.config_bypass_low_target_sdk,
        descResource = R.string.config_bypass_low_target_sdk_desc,
    ),

    RequestUpdateOwnerShip(
        value = PackageManagerHidden.INSTALL_REQUEST_UPDATE_OWNERSHIP,
        minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        labelResource = R.string.config_request_update_ownership,
        descResource = R.string.config_request_update_ownership_desc,
    )
}
