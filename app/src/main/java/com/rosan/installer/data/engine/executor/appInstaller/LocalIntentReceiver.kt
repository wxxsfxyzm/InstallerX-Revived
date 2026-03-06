// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appInstaller

import android.content.IIntentReceiver
import android.content.IIntentSender
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.IBinder
import com.rosan.installer.core.reflection.ReflectionProvider

import kotlinx.coroutines.channels.Channel

class LocalIntentReceiver(private val reflect: ReflectionProvider) {
    private val channel = Channel<Intent>(Channel.UNLIMITED)

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
            if (intent != null) {
                channel.trySend(intent)
            }
        }
    }

    fun getIntentSender(): IntentSender =
        reflect.getDeclaredConstructor(
            IntentSender::class.java, IIntentSender::class.java
        )!!.newInstance(localSender) as IntentSender

    suspend fun getResult(): Intent {
        return channel.receive()
    }
}
