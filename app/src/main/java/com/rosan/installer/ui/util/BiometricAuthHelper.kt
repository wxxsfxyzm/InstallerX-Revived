package com.rosan.installer.ui.util

import android.content.Context
import android.content.Intent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.rosan.installer.data.app.model.exception.AuthenticationFailedException
import com.rosan.installer.ui.activity.BiometricsAuthenticationActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resumeWithException

/**
 * Performs biometric authentication and throws an exception if authentication fails.
 *
 * This function triggers the device's biometric prompt (fingerprint, face, device credential or other supported biometrics)
 * and suspends until the user either successfully authenticates or cancels/fails the authentication.
 *
 * @receiver The [Context] used to open [BiometricsAuthenticationActivity] and init [BiometricPrompt].
 * @param title The title displayed on the biometric prompt dialog.
 * @param subTitle The subtitle displayed on the biometric prompt dialog.
 *
 * @throws AuthenticationFailedException Thrown if the user fails biometric authentication or cancels the prompt.
 */
suspend fun Context.doBiometricAuthOrThrow(title: String, subTitle: String) {
    val biometricManager = BiometricManager.from(this)
    if (biometricManager.canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS) {
        Timber.tag("BiometricAuth")
            .w("Can't do biometricAuth, because device not support BiometricAuth or no biometric or device credential is enrolled.")
        return
    }
    val executor = ContextCompat.getMainExecutor(this)

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subTitle)
        .setAllowedAuthenticators(
            BIOMETRIC_STRONG or
                    BIOMETRIC_WEAK or
                    DEVICE_CREDENTIAL
        )
        .build()

    return suspendCancellableCoroutine { continuation ->
        BiometricsAuthenticationActivity.onActivityReady = { activity ->
            val biometricPrompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {

                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        super.onAuthenticationSucceeded(result)
                        if (continuation.isActive) {
                            activity.finish()
                            continuation.resume(Unit) { _, _, _ -> }
                        }
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence
                    ) {
                        super.onAuthenticationError(errorCode, errString)
                        if (continuation.isActive) {
                            activity.finish()
                            continuation.resumeWithException(
                                AuthenticationFailedException("Biometric auth error ($errorCode): $errString")
                            )
                        }
                    }
                }
            )

            biometricPrompt.authenticate(promptInfo)

            continuation.invokeOnCancellation {
                biometricPrompt.cancelAuthentication()
                activity.finish()
            }
        }

        val intent = Intent(this, BiometricsAuthenticationActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        this.startActivity(intent)
    }
}

/**
 * Performs biometric authentication and returns whether it was successful.
 *
 * This function triggers the device's biometric prompt (fingerprint, face, device credential,
 * or other supported biometrics) and suspends until the user either successfully authenticates
 * or cancels/fails the authentication.
 *
 * @receiver The [Context] used to open [BiometricsAuthenticationActivity] and init [BiometricPrompt].
 * @param title The title displayed on the biometric prompt dialog.
 * @param subTitle The subtitle displayed on the biometric prompt dialog.
 *
 * @return `true` if the authentication was successful, `false` otherwise (e.g., canceled or failed).
 */
suspend fun Context.doBiometricAuth(title: String, subTitle: String): Boolean {
    try {
        this.doBiometricAuthOrThrow(title, subTitle)
        return true
    } catch (e: AuthenticationFailedException) {
        Timber.tag("BiometricAuth").i(e, "Authentication failed")
        return false
    }
}