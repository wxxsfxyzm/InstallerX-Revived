package com.rosan.installer.data.app.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import timber.log.Timber
import java.security.MessageDigest

/**
 * A utility object for extracting and hashing application signatures.
 * REFACTORED to handle unconventional file paths like file descriptors.
 */
object SignatureUtils {

    private const val HASH_ALGORITHM = "SHA-256"

    /**
     * Gets the signature hash from an APK file path.
     *
     * @param context The application context.
     * @param apkPath The absolute path to the APK file, which can be a regular path or a file descriptor path.
     * @return The SHA-256 hash of the first signature, or null if it fails.
     */
    fun getApkSignatureHash(context: Context, apkPath: String): String? {
        return try {
            // Now calls a dedicated helper for archive files.
            val packageInfo = getPackageArchiveInfoFromPath(context, apkPath)
            val signature = getFirstSignature(packageInfo)
            signature?.let { hashSignature(it) }
        } catch (e: Exception) {
            // This will catch any exceptions, including the previous NameNotFoundException if something is wrong.
            Timber.e(e, "Failed to get signature hash from APK: $apkPath")
            null
        }
    }

    /**
     * Gets the signature hash from an already installed application.
     *
     * @param context The application context.
     * @param packageName The package name of the installed app.
     * @return The SHA-256 hash of the first signature, or null if not found or an error occurs.
     */
    fun getInstalledAppSignatureHash(context: Context, packageName: String): String? {
        return try {
            // MODIFIED: Now calls a dedicated helper for installed packages.
            val packageInfo = getInstalledPackageInfo(context, packageName)
            val signature = getFirstSignature(packageInfo)
            signature?.let { hashSignature(it) }
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.d("Package not found, can't get signature: $packageName")
            null // This is an expected case, not an error.
        } catch (e: Exception) {
            Timber.e(e, "Failed to get signature hash for installed package: $packageName")
            null
        }
    }

    /**
     * Hashes a Signature object using SHA-256.
     *
     * @param signature The signature to hash.
     * @return A hex string representation of the hash.
     */
    private fun hashSignature(signature: Signature): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val hashBytes = digest.digest(signature.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    // Dedicated function to get PackageInfo from an APK file path.
    @Suppress("DEPRECATION")
    private fun getPackageArchiveInfoFromPath(context: Context, apkPath: String): PackageInfo? {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        // This method correctly handles ANY valid file path, including file descriptors.
        return pm.getPackageArchiveInfo(apkPath, flags)
    }

    // Dedicated function to get PackageInfo from an installed package name.
    @Suppress("DEPRECATION")
    private fun getInstalledPackageInfo(context: Context, packageName: String): PackageInfo? {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return pm.getPackageInfo(packageName, flags)
    }

    /**
     * Safely extracts the first signature from a PackageInfo object.
     * Handles both modern SigningInfo and legacy Signature[] arrays.
     */
    private fun getFirstSignature(packageInfo: PackageInfo?): Signature? {
        packageInfo ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo
            if (signingInfo?.hasMultipleSigners() == true) {
                signingInfo.apkContentsSigners?.firstOrNull()
            } else {
                signingInfo?.signingCertificateHistory?.firstOrNull()
            }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures?.firstOrNull()
        }
    }
}