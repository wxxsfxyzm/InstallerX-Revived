package com.rosan.installer.data.updater.model.impl

import android.content.Context
import com.rosan.installer.BuildConfig
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Level
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.updater.model.entity.GithubRelease
import com.rosan.installer.data.updater.repo.UpdateChecker
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

class OnlineUpdateChecker(
    private val context: Context,
    private val client: OkHttpClient,
    private val json: Json
) : UpdateChecker {
    companion object {
        private const val REPO_OWNER = "wxxsfxyzm"
        private const val REPO_NAME = "InstallerX-Revived"
    }

    override fun check(): UpdateChecker.CheckResult? {
        // Skip check for Debug builds
        if (RsConfig.isDebug) {
            Timber.d("Update check skipped: Debug build")
            return null
        }

        // Skip check for Unstable builds
        if (RsConfig.LEVEL == Level.UNSTABLE) {
            Timber.d("Update check skipped: Unstable build")
            return null
        }

        // Skip check for non-target app
        if (context.packageName != BuildConfig.APPLICATION_ID) {
            Timber.d("Update check skipped: Not the target app")
            return null
        }

        return try {
            val remoteRelease = fetchRemoteRelease() ?: return null

            // 1. Prioritize finding the Online version of the APK Asset
            // Filename example: InstallerX-Revived-online-v2.3.1.87e0cc9.apk
            val apkAsset = remoteRelease.assets.find {
                it.name.contains("online", ignoreCase = true) &&
                        it.name.endsWith(".apk", ignoreCase = true)
            }

            val downloadUrl = apkAsset?.browserDownloadUrl ?: ""
            val fileName = apkAsset?.name ?: ""

            // 2. Extract version number from filename using regex (matches vX.X.X.hash format)
            val versionRegex = Regex("v(\\d.+?)\\.apk", RegexOption.IGNORE_CASE)
            val matchResult = versionRegex.find(fileName)

            // If extraction is successful, use the extracted version; otherwise, fallback to Tag Name
            val remoteVersion = matchResult?.groupValues?.get(1)
                ?: remoteRelease.tagName.removePrefix("v")

            val currentVersion = RsConfig.VERSION_NAME

            // 3. Compare versions
            val hasUpdate = compareVersions(remoteVersion, currentVersion) > 0

            Timber.i("Update check: Local=$currentVersion, Remote=$remoteVersion (parsed from $fileName), HasUpdate=$hasUpdate, ApkUrl=$downloadUrl")

            UpdateChecker.CheckResult(
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
        val url = if (RsConfig.LEVEL == Level.STABLE) {
            "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
        } else {
            "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases"
        }

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bodyString = response.body.string()

            return if (RsConfig.LEVEL == Level.STABLE) {
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
     * Example: "2.3.1.87e0cc9" -> Pair("2.3.1", "87e0cc9")
     *          "2.3.1" -> Pair("2.3.1", null)
     */
    private fun splitVersion(version: String): Pair<String, String?> {
        val parts = version.split('.')

        // Find the first non-numeric part (hash identifier)
        val numericParts = mutableListOf<String>()
        var hashPart: String? = null

        for (part in parts) {
            if (part.toIntOrNull() != null) {
                numericParts.add(part)
            } else {
                // Found non-numeric part, treat as hash
                hashPart = part
                break
            }
        }

        val numericVersion = numericParts.joinToString(".")
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

    override fun download(url: String): DataEntity? {
        Timber.d("Starting download stream from: $url")

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json") // Or application/octet-stream
            .build()

        try {
            // Execute the request here but do not close it immediately
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Timber.e("Download failed: ${response.code}")
                response.close() // Manually close on failure
                return null
            }

            val body = response.body

            val contentLength = body.contentLength()
            if (contentLength <= 0) {
                Timber.w("Content-Length is missing or invalid. Stream install might fail.")
                // InstallIt throws an exception if sizeBytes <= 0.
                // GitHub Releases usually provide Content-Length.
            }

            // Return StreamDataEntity
            // OkHttp's byteStream() automatically handles connection release when reading ends or is closed
            return DataEntity.StreamDataEntity(
                stream = body.byteStream(),
                length = contentLength
            )

        } catch (e: Exception) {
            Timber.e(e, "Exception during download request")
            return null
        }
    }
}