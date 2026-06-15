// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appinstaller

import android.content.IIntentReceiver
import android.content.IIntentSender
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.IBinder
import com.rosan.installer.core.reflection.ReflectionProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

class LocalIntentReceiver(private val reflect: ReflectionProvider) {
    private companion object {
        const val RESULT_TIMEOUT_MS = 5 * 60 * 1000L
    }

    private val channel = Channel<Intent>(Channel.BUFFERED)

    private val localSender = object : IIntentSender.Stub() {
        override fun send(
            code: Int,
            intent: Intent?,
            resolvedType: String?,
            whitelistToken: IBinder?,
            finishedReceiver: IIntentReceiver?,
            requiredPermission: String?,
            options: Bundle?
        ) {
            if (intent == null) {
                Timber.w("LocalIntentReceiver received a null intent")
                return
            }

            val result = channel.trySend(intent)
            if (result.isFailure) {
                Timber.w(result.exceptionOrNull(), "LocalIntentReceiver failed to enqueue result intent")
            }
        }
    }

    fun getIntentSender() =
        reflect.getDeclaredConstructor(
            IntentSender::class.java, IIntentSender::class.java
        )!!.newInstance(localSender) as IntentSender

    suspend fun getResult(): Intent = withTimeout(RESULT_TIMEOUT_MS.milliseconds) {
        channel.receive()
    }
}
