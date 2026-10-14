// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rosan.installer.R
import com.rosan.installer.core.app.DefaultInstallerActions.ACTION_SET_DEFAULT_INSTALLER
import com.rosan.installer.core.app.DefaultInstallerActions.EXTRA_LOCK
import com.rosan.installer.core.exception.InstallerException
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.usecase.ResolveAuthorizerCandidatesUseCase
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.provider.PrivilegedProvider
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.util.getErrorMessage
import com.rosan.installer.util.toast
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Exported entry point for locking or unlocking InstallerX as the default installer, so external
 * automation can restore the lock without going through the UI.
 *
 * Guarded by android.permission.DUMP in the manifest. A custom permission would leave this
 * unreachable, since callers can only hold one by declaring it in their own manifest, which the
 * shell UID and third-party automation cannot do.
 */
class DefaultInstallerReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val appSettingsRepo by inject<AppSettingsRepository>()
    private val privilegedProvider by inject<PrivilegedProvider>()
    private val capabilityProvider by inject<DeviceCapabilityProvider>()
    private val resolveAuthorizerCandidates by inject<ResolveAuthorizerCandidatesUseCase>()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SET_DEFAULT_INSTALLER) {
            Timber.Forest.w("Received an intent with unexpected action: ${intent.action}")
            return
        }

        val lock = intent.getBooleanExtra(EXTRA_LOCK, true)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            // The privileged calls block below every suspension point — runBlocking around the Shizuku
            // and Dhizuku permission flows, a process spawn for Root — so cancellation never reaches
            // them and a timeout wrapped around one of them would only fire once it had returned. The
            // attempt runs detached instead, so the timeout below bounds the waiting rather than the
            // work: an attempt that never answers is abandoned, and this receiver still replies.
            val attempt = attemptScope.async { applyLockState(context, lock) }
            try {
                withTimeout(REQUEST_TIMEOUT) { attempt.await() }
            } catch (e: TimeoutCancellationException) {
                Timber.Forest.w("No authorizer answered before the broadcast window closed.")
                context.toastFailure(NoAuthorizerResponseException())
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Timber.Forest.e(t, "Failed to change the default installer lock state.")
                context.toastFailure(t)
            } finally {
                // finish() has to run even when the refresh below fails, otherwise the goAsync
                // broadcast stays pending until the system times it out.
                runCatching { capabilityProvider.refreshPrivilegeStatus() }
                    .onFailure { Timber.Forest.w(it, "Failed to refresh the capability state.") }
                pendingResult.finish()
            }
        }
    }

    private suspend fun applyLockState(context: Context, lock: Boolean) {
        val prefs = appSettingsRepo.preferencesFlow.first()

        // Mirrors the guard on the in-app buttons: once a third-party Xposed module is declared as
        // forcing the takeover, there is nothing left for the app to lock or unlock.
        if (prefs.userSetLSPosedActive) {
            Timber.Forest.i("Xposed module manages the default installer; ignoring the request.")
            context.toastOnMain(context.getString(R.string.default_installer_lsposed_managed))
            return
        }

        // isDefaultInstaller short-circuits to true while the provider's Xposed flag is set, and that
        // flag is a mirror the settings screen keeps in sync rather than a live reading. Aligning it
        // with the preference just read keeps the check further down a real readback: a mirror left
        // over from an earlier state would otherwise let the check pass without reading anything.
        capabilityProvider.isLSPosedActive = prefs.userSetLSPosedActive

        // Smart authorization gates the fallback list on the install path, so it gates it here too:
        // with the switch off this collapses to the profile's own authorizer, which is what the home
        // page buttons use. The shared rule then puts the preferred authorizer first and the enabled
        // fallback list after it, the order both directions want.
        val requestedAuthorizers = if (prefs.tryMultipleAuthorizersOnInstall) {
            resolveAuthorizerCandidates(prefs.authorizer, prefs.customizeAuthorizer)
        } else {
            listOf(prefs.authorizer)
        }

        // Authorizer.None only acts when the app runs as the system package installer; without that it
        // returns without doing anything, so leaving it in would credit a success that never happened.
        // The buttons are disabled outright in that case, so this matches them too.
        val candidates = requestedAuthorizers.filter {
            it != Authorizer.None || capabilityProvider.isSystemApp
        }

        var lastFailure: Throwable = NoUsableAuthorizerException()
        for (authorizer in candidates) {
            try {
                privilegedProvider.setDefaultInstaller(authorizer, prefs.customizeAuthorizer, lock)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Every failure has to advance to the next candidate. A broken Root setup surfaces
                // as IllegalStateException from ProcessHookRecycler rather than PrivilegedException,
                // and stopping there would skip the fallback this receiver exists for.
                lastFailure = e
                Timber.Forest.w(e, "Authorizer $authorizer unavailable, trying next candidate.")
                continue
            }

            // A normal return is not proof that the state changed: an unlock issued through a
            // weaker authorizer clears only the standard preferred activity and leaves a persistent
            // entry behind, so it would report success while the lock is still in place.
            if (capabilityProvider.isDefaultInstaller != lock) {
                lastFailure = LockStateUnchangedException(authorizer, lock)
                Timber.Forest.w("Authorizer $authorizer reported success, but the readback disagrees (lock=$lock).")
                continue
            }

            val state = if (lock) "locked" else "unlocked"
            Timber.Forest.i("Default installer $state with $authorizer by request.")
            if (authorizer != prefs.authorizer) {
                context.toastOnMain(
                    context.getString(
                        R.string.default_installer_changed_with,
                        context.getString(authorizer.displayNameRes),
                    ),
                )
            }
            return
        }

        throw lastFailure
    }

    private suspend fun Context.toastFailure(t: Throwable) {
        // This receiver's own failures already read as complete sentences, so wrapping them in the
        // generic failure line would only repeat the same thing twice.
        val message = if (t is ExportedActionFailure) {
            t.getErrorMessage(this)
        } else {
            getString(R.string.default_installer_change_failed, t.getErrorMessage(this))
        }
        toastOnMain(message)
    }

    private suspend fun Context.toastOnMain(message: CharSequence) {
        withContext(Dispatchers.Main) { toast(message) }
    }

    companion object {
        // Bounds how long a command waits for an answer, so the goAsync result is always finished
        // inside the broadcast window. The attempt itself stays unbounded and is left to finish or
        // not on its own.
        private val REQUEST_TIMEOUT = 8.seconds

        // Attempts run here rather than as children of the broadcast coroutine, because the privileged
        // calls ignore cancellation and abandoning one must not mean waiting for it. Overlapping
        // commands are deliberately not serialized: a lock would only keep two concurrent readbacks
        // from disagreeing, which is already the conservative direction, while it would let one
        // stalled attempt block every later command for as long as it stays stalled. The commands are
        // idempotent.
        private val attemptScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
}

/**
 * Base for failures raised by this receiver. Their string resources are complete sentences, so they
 * are shown on their own instead of being wrapped in the generic failure line.
 */
private sealed class ExportedActionFailure(message: String) : InstallerException(message)

private class LockStateUnchangedException(authorizer: Authorizer, lock: Boolean) : ExportedActionFailure("Default installer lock state did not change after $authorizer (lock=$lock).") {
    override fun getStringResId() = R.string.default_installer_unchanged
}

private class NoUsableAuthorizerException : ExportedActionFailure("No authorizer candidate was available.") {
    override fun getStringResId() = R.string.default_installer_no_authorizer
}

private class NoAuthorizerResponseException : ExportedActionFailure("No authorizer answered in time.") {
    override fun getStringResId() = R.string.default_installer_timed_out
}
