// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.core.execution.authorization

import android.content.pm.PackageManager
import com.rosan.installer.BuildConfig
import com.rosan.installer.domain.privileged.exception.PrivilegedException
import com.rosan.installer.domain.privileged.model.PrivilegedErrorType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import rikka.shizuku.Shizuku
import rikka.sui.Sui

suspend fun <T> requireShizukuPermissionGranted(action: suspend () -> T): T {
    callbackFlow {
        Sui.init(BuildConfig.APPLICATION_ID)
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            if (Shizuku.pingBinder()) {
                send(Unit)
            } else {
                close(
                    PrivilegedException(
                        errorType = PrivilegedErrorType.SHIZUKU_NOT_WORK,
                        message = "Shizuku service is not running (ping failed)."
                    )
                )
            }
            awaitClose()
        } else {
            val requestCode = (Int.MIN_VALUE..Int.MAX_VALUE).random()
            val listener =
                Shizuku.OnRequestPermissionResultListener { _requestCode, grantResult ->
                    if (_requestCode != requestCode) return@OnRequestPermissionResultListener
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
                        trySend(Unit)
                    else close(Exception("sui/shizuku permission denied"))
                }
            Shizuku.addRequestPermissionResultListener(listener)
            Shizuku.requestPermission(requestCode)
            awaitClose { Shizuku.removeRequestPermissionResultListener(listener) }
        }
    }.catch {
        throw PrivilegedException(
            errorType = PrivilegedErrorType.SHIZUKU_NOT_WORK,
            cause = it
        )
    }.first()

    return action()
}
