// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.updater.repository

import android.content.Context
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.core.env.AppConfig.OFFICIAL_PACKAGE_NAME
import com.rosan.installer.data.updater.model.GithubRelease
import com.rosan.installer.core.device.model.Level
import com.rosan.installer.domain.settings.model.preferences.GithubUpdateChannel
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.updater.model.UpdateInfo
import com.rosan.installer.domain.updater.repository.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.InputStream

class OnlineUpdateRepositoryImpl(
    private val context: Context,
    private val client: OkHttpClient,
    private val json: Json,
    private val appSettingsRepository: AppSettingsRepository
) : UpdateRepository {
    companion object {
        private const val REPO_OWNER = "wxxsfxyzm"
        private const val REPO_NAME = "InstallerX-Revived"

        private const val URL_NONE = ""
        private const val PROXY_7ED = "https://gh.sevencdn.com/"
    }

    private val _updateInfoFlow = MutableStateFlow<UpdateInfo?>(null)
    override val updateInfoFlow: StateFlow<UpdateInfo?> = _updateInfoFlow.asStateFlow()

    private var hasChecked = false
    private val updateMutex = Mutex()

    override suspend fun checkUpdate(force: Boolean): UpdateInfo? = withContext(Dispatchers.IO) {
        // If force check not required, and has checked, return cached value
        if (!force && hasChecked) {
            Timber.d("checkUpdate: Returning cached UpdateInfo")
            return@withContext _updateInfoFlow.value
        }

        // Use mutex lock to prevent concurrent execution
        updateMutex.withLock {
            // Check again after acquiring lock
            if (!force && hasChecked) {
                return@withContext _updateInfoFlow.value
            }

            if (AppConfig.isDebug || AppConfig.LEVEL == Level.UNSTABLE || context.packageName != OFFICIAL_PACKAGE_NAME) {
                // Log the exact values to see which condition caused the skip
                Timber.d("checkUpdate: Skipped. isDebug=${AppConfig.isDebug}, LEVEL=${AppConfig.LEVEL}, package=${context.packageName}")
                return@withContext null
            }

            try {
                val remoteRelease = fetchRemoteRelease()
                if (remoteRelease == null) {
                    // Log if network request or parsing failed
                    Timber.e("checkUpdate: fetchRemoteRelease() returned null")
                    return@withContext null
                }
                // Log successful fetch
                Timber.d("checkUpdate: Successfully fetched release. TagName=${remoteRelease.tagName}")

                val apkAsset = remoteRelease.assets.find {
                    it.name.contains("online", ignoreCase = true) &&
                            it.name.endsWith(".apk", ignoreCase = true)
                }

                if (apkAsset == null) {
                    // Log if no matching asset was found
                    Timber.e("checkUpdate: No matching online APK asset found in release")
                } else {
                    // Log the found asset name
                    Timber.d("checkUpdate: Found APK asset: ${apkAsset.name}")
                }

                val prefs = appSettingsRepository.preferencesFlow.first()
                val proxyUrl = when (prefs.githubUpdateChannel) {
                    GithubUpdateChannel.OFFICIAL -> URL_NONE
                    GithubUpdateChannel.PROXY_7ED -> PROXY_7ED
                    GithubUpdateChannel.CUSTOM -> prefs.customGithubProxyUrl
                }
                val browserDownloadUrl = apkAsset?.browserDownloadUrl ?: URL_NONE
                val downloadUrl = if (browserDownloadUrl.isNotEmpty() && proxyUrl.isNotEmpty()) {
                    if (proxyUrl.endsWith("/")) {
                        proxyUrl + browserDownloadUrl
                    } else {
                        "$proxyUrl/$browserDownloadUrl"
                    }
                } else {
                    browserDownloadUrl
                }

                val fileName = apkAsset?.name ?: ""

                // Updated regex: APK names no longer have 'v', they look like '...-online-26.03.1a2b3c4.apk'
                val versionRegex = Regex("-(\\d.+?)\\.apk", RegexOption.IGNORE_CASE)
                val matchResult = versionRegex.find(fileName)

                val remoteVersion = matchResult?.groupValues?.get(1)
                    ?: remoteRelease.tagName.removePrefix("v")

                val currentVersion = AppConfig.VERSION_NAME

                // Log versions before comparison to catch potential parsing issues
                Timber.d("checkUpdate: Preparing to compare. Local=$currentVersion, Remote=$remoteVersion")

                val hasUpdate = compareVersions(remoteVersion, currentVersion) > 0

                Timber.i("Update check: Local=$currentVersion, Remote=$remoteVersion, HasUpdate=$hasUpdate")

                val updateInfo = UpdateInfo(
                    hasUpdate = hasUpdate,
                    remoteVersion = remoteVersion,
                    releaseUrl = remoteRelease.htmlUrl ?: "",
                    downloadUrl = downloadUrl
                )

                // Cache result and mark as checked
                _updateInfoFlow.value = updateInfo
                hasChecked = true

                return@withContext updateInfo
            } catch (e: Exception) {
                // Log the stacktrace to identify crashes like NullPointerException or Regex errors
                Timber.e(e, "checkUpdate: Exception caught during execution")
                null
            }
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
     * Format: yy.MM[.patch][.hash]
     *
     * Rules:
     * 1. Compare numeric parts (yy.MM.patch) numerically
     * 2. If numeric parts are equal and both have hash suffixes:
     * - Different hashes = treat as update needed (return 1)
     * - Same hash = no update (return 0)
     * 3. If numeric parts are equal but only one has hash: prioritize version with hash
     *
     * @return positive if v1 > v2, negative if v1 < v2, 0 if equal
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val (numericV1, hashV1) = splitVersion(v1)
        val (numericV2, hashV2) = splitVersion(v2)

        // Compare numeric parts first
        val numericComparison = compareNumericVersion(numericV1, numericV2)
        if (numericComparison != 0) {
            return numericComparison
        }

        // Numeric parts are equal, compare hash parts
        return when {
            hashV1 != null && hashV2 != null -> {
                if (hashV1 == hashV2) 0 else 1
            }

            hashV1 != null && hashV2 == null -> 1
            hashV1 == null && hashV2 != null -> -1
            else -> 0
        }
    }

    /**
     * Dynamically split version string into numeric base and optional Git hash.
     * Supports formats like:
     * - "26.03" -> Pair("26.03", null)
     * - "26.03.01" -> Pair("26.03.01", null)
     * - "26.03.1a2b3c4" -> Pair("26.03", "1a2b3c4")
     * - "26.03.01.1a2b3c4" -> Pair("26.03.01", "1a2b3c4")
     */
    private fun splitVersion(version: String): Pair<String, String?> {
        val parts = version.split('.')
        if (parts.isEmpty()) return Pair(version, null)

        val lastPart = parts.last()

        // Git short hashes are hex strings of at least 7 characters.
        // Stable patches (e.g., "01", "2") are much shorter.
        val isHash = lastPart.length >= 7 && lastPart.matches(Regex("^[a-fA-F0-9]+\$"))

        return if (isHash) {
            // Rejoin everything except the last part as the numeric base
            val numericVersion = parts.dropLast(1).joinToString(".")
            Pair(numericVersion, lastPart)
        } else {
            // It's a pure stable version without a hash
            Pair(version, null)
        }
    }

    /**
     * Compare numeric version parts only (e.g., "26.03" vs "26.03.01")
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

            Pair(body.byteStream(), contentLength)
        } catch (e: Exception) {
            Timber.e(e, "Exception during download request")
            null
        }
    }
}
