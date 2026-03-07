// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.updater.repository

import android.content.Context
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.data.updater.model.GithubRelease
import com.rosan.installer.domain.device.model.Level
import com.rosan.installer.domain.updater.model.UpdateInfo
import com.rosan.installer.domain.updater.repository.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.InputStream

class OnlineUpdateRepositoryImpl(
    private val context: Context,
    private val client: OkHttpClient,
    private val json: Json
) : UpdateRepository {
    companion object {
        private const val REPO_OWNER = "wxxsfxyzm"
        private const val REPO_NAME = "InstallerX-Revived"
        private const val OFFICIAL_PACKAGE_NAME = "com.rosan.installer.x.revived"
    }

    override suspend fun checkUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        if (AppConfig.isDebug || AppConfig.LEVEL == Level.UNSTABLE || context.packageName != OFFICIAL_PACKAGE_NAME) {
            Timber.d("Update check skipped based on environment rules.")
            return@withContext null
        }

        try {
            val remoteRelease = fetchRemoteRelease() ?: return@withContext null

            val apkAsset = remoteRelease.assets.find {
                it.name.contains("online", ignoreCase = true) &&
                        it.name.endsWith(".apk", ignoreCase = true)
            }

            val downloadUrl = apkAsset?.browserDownloadUrl ?: ""
            val fileName = apkAsset?.name ?: ""

            val versionRegex = Regex("v(\\d.+?)\\.apk", RegexOption.IGNORE_CASE)
            val matchResult = versionRegex.find(fileName)

            val remoteVersion = matchResult?.groupValues?.get(1)
                ?: remoteRelease.tagName.removePrefix("v")

            val currentVersion = AppConfig.VERSION_NAME
            val hasUpdate = compareVersions(remoteVersion, currentVersion) > 0

            Timber.i("Update check: Local=$currentVersion, Remote=$remoteVersion, HasUpdate=$hasUpdate")

            UpdateInfo(
                hasUpdate = hasUpdate,
                remoteVersion = remoteVersion,
                releaseUrl = remoteRelease.htmlUrl ?: "",
                downloadUrl = downloadUrl
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to check for updates")
            null
        }
    }

    private fun fetchRemoteRelease(): GithubRelease? {
        val url = if (AppConfig.LEVEL == Level.STABLE) {
            "https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases/latest"
        } else {
            "https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases"
        }

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bodyString = response.body.string()

            return if (AppConfig.LEVEL == Level.STABLE) {
                json.decodeFromString<GithubRelease>(bodyString)
            } else {
                val releases = json.decodeFromString<List<GithubRelease>>(bodyString)
                releases.firstOrNull { it.isPrerelease }
            }
        }
    }

    /**
     * Compare two version strings.
     * Format: major.minor.patch[.hash]
     *
     * Rules:
     * 1. Compare numeric parts (major.minor.patch) numerically
     * 2. If numeric parts are equal and both have hash suffixes:
     *    - Different hashes = treat as update needed (return 1)
     *    - Same hash = no update (return 0)
     * 3. If numeric parts are equal but only one has hash: prioritize version with hash
     *
     * @return positive if v1 > v2, negative if v1 < v2, 0 if equal
     */
    private fun compareVersions(v1: String, v2: String): Int {
        // Split version into numeric part and hash part
        // Example: "2.3.1.87e0cc9" -> ["2.3.1", "87e0cc9"]
        val (numericV1, hashV1) = splitVersion(v1)
        val (numericV2, hashV2) = splitVersion(v2)

        // Compare numeric parts first
        val numericComparison = compareNumericVersion(numericV1, numericV2)
        if (numericComparison != 0) {
            return numericComparison
        }

        // Numeric parts are equal, compare hash parts
        return when {
            // Both have hashes
            hashV1 != null && hashV2 != null -> {
                if (hashV1 == hashV2) 0  // Same hash, no update
                else 1  // Different hash, treat as update available
            }
            // Only v1 has hash (v1 is newer build)
            hashV1 != null && hashV2 == null -> 1
            // Only v2 has hash (v2 is newer build)
            hashV1 == null && hashV2 != null -> -1
            // Neither has hash
            else -> 0
        }
    }

    /**
     * Split version string into numeric part and optional hash part.
     * Format strictly guaranteed as: major.minor.patch[.hash]
     *
     * Examples:
     * - "2.3.3" -> Pair("2.3.3", null)
     * - "2.3.3.d69b04f" -> Pair("2.3.3", "d69b04f")
     * - "2.3.3.4365770" -> Pair("2.3.3", "4365770")
     */
    private fun splitVersion(version: String): Pair<String, String?> {
        val parts = version.split('.')

        // Extract exactly the first 3 parts for major.minor.patch
        val numericVersion = parts.take(3).joinToString(".")

        // Extract the 4th part as hash if it exists
        val hashPart = parts.getOrNull(3)

        return Pair(numericVersion, hashPart)
    }

    /**
     * Compare numeric version parts only (e.g., "2.3.1" vs "2.3.2")
     */
    private fun compareNumericVersion(v1: String, v2: String): Int {
        val parts1 = v1.split('.')
        val parts2 = v2.split('.')
        val length = maxOf(parts1.size, parts2.size)

        for (i in 0 until length) {
            val num1 = parts1.getOrNull(i)?.toIntOrNull() ?: 0
            val num2 = parts2.getOrNull(i)?.toIntOrNull() ?: 0

            val diff = num1.compareTo(num2)
            if (diff != 0) return diff
        }
        return 0
    }

    override suspend fun downloadUpdate(url: String): Pair<InputStream, Long>? = withContext(Dispatchers.IO) {
        Timber.d("Starting download stream from: $url")
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.e("Download failed: ${response.code}")
                response.close()
                return@withContext null
            }
            val body = response.body
            val contentLength = body.contentLength()

            // Return pure Java InputStream to Domain layer
            Pair(body.byteStream(), contentLength)
        } catch (e: Exception) {
            Timber.e(e, "Exception during download request")
            null
        }
    }
}


