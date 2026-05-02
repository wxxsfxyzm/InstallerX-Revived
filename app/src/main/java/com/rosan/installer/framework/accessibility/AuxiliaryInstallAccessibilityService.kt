// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.framework.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.rosan.installer.R
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class AuxiliaryInstallAccessibilityService : AccessibilityService(), KoinComponent {
    private val appSettingsRepo: AppSettingsRepository by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val powerManager by lazy { getSystemService(Context.POWER_SERVICE) as PowerManager }

    @Volatile
    private var autoConfirmUsbInstall = false

    @Volatile
    private var showToast = false

    @Volatile
    private var delayedRetry = true

    @Volatile
    private var requireScreenOn = true

    private var lastClickUptimeMillis = 0L
    private val continueInstallLabels by lazy {
        resources.getStringArray(R.array.auxiliary_install_continue_install_labels)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 200L
        }

        serviceScope.launch {
            appSettingsRepo.getBoolean(BooleanSetting.AuxiliaryInstallAutoConfirmUsb, false)
                .distinctUntilChanged()
                .collect { enabled ->
                    autoConfirmUsbInstall = enabled
                }
        }
        serviceScope.launch {
            appSettingsRepo.getBoolean(BooleanSetting.AuxiliaryInstallShowToast, false)
                .distinctUntilChanged()
                .collect { enabled ->
                    showToast = enabled
                }
        }
        serviceScope.launch {
            appSettingsRepo.getBoolean(BooleanSetting.AuxiliaryInstallDelayedRetry, true)
                .distinctUntilChanged()
                .collect { enabled ->
                    delayedRetry = enabled
                }
        }
        serviceScope.launch {
            appSettingsRepo.getBoolean(BooleanSetting.AuxiliaryInstallRequireScreenOn, true)
                .distinctUntilChanged()
                .collect { enabled ->
                    requireScreenOn = enabled
                }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!autoConfirmUsbInstall || event == null) return
        if (requireScreenOn && !powerManager.isInteractive) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!event.isTargetAdbInstallActivity()) return
        val root = rootInActiveWindow ?: event.source ?: return
        if (!clickContinueInstallButton(root) && delayedRetry) {
            mainHandler.postDelayed({
                if (autoConfirmUsbInstall && (!requireScreenOn || powerManager.isInteractive)) {
                    rootInActiveWindow?.let(::clickContinueInstallButton)
                }
            }, WINDOW_SETTLE_DELAY_MS)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun clickContinueInstallButton(root: AccessibilityNodeInfo): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastClickUptimeMillis < CLICK_COOLDOWN_MS) return true

        val buttonNode = continueInstallLabels.asSequence()
            .flatMap { label -> root.findAccessibilityNodeInfosByText(label).asSequence() }
            .firstOrNull { it.matchesAnyContinueInstallLabel() }
            ?: return false

        if (buttonNode.performClickAction()) {
            lastClickUptimeMillis = now
            showAutoConfirmedToastIfNeeded()
            Timber.d("Auto confirmed USB install prompt")
            return true
        }
        return false
    }

    private fun showAutoConfirmedToastIfNeeded() {
        if (!showToast) return
        mainHandler.post {
            Toast.makeText(
                applicationContext,
                R.string.auxiliary_install_auto_confirmed_toast,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun AccessibilityEvent.isTargetAdbInstallActivity(): Boolean {
        return packageName?.toString() == TARGET_PACKAGE &&
                className?.toString() == TARGET_ACTIVITY
    }

    private fun AccessibilityNodeInfo.matchesAnyContinueInstallLabel(): Boolean {
        val nodeText = text?.toString()?.trim()
        val nodeContentDescription = contentDescription?.toString()?.trim()
        return (nodeText != null && nodeText in continueInstallLabels) ||
                (nodeContentDescription != null && nodeContentDescription in continueInstallLabels)
    }

    private fun AccessibilityNodeInfo.performClickAction(): Boolean {
        var current: AccessibilityNodeInfo? = this
        while (current != null) {
            if (current.isEnabled && current.isClickable &&
                current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            ) return true
            current = current.parent
        }
        return false
    }

    private companion object {
        const val TARGET_PACKAGE = "com.miui.securitycenter"
        const val TARGET_ACTIVITY = "com.miui.permcenter.install.AdbInstallActivity"
        const val CLICK_COOLDOWN_MS = 1000L
        const val WINDOW_SETTLE_DELAY_MS = 200L
    }
}
