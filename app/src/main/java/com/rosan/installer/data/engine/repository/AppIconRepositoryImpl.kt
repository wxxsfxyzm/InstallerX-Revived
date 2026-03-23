// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.repository

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import com.rosan.installer.data.engine.repository.AppIconRepositoryImpl.Companion.QUANTIZE_BITMAP_MAX_SIZE
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.DataEntity
import com.rosan.installer.domain.engine.repository.AppIconRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import android.os.Process as AndroidProcess

/**
 * Default implementation of [AppIconRepository].
 *
 * ## Concurrency design
 *
 * Icon loading results are cached as [Deferred]<Bitmap?> inside a bounded [LruCache].
 * A coroutine-friendly [Mutex] guards all cache reads and writes to ensure the
 * get-and-put-if-absent pattern is atomic without blocking OS threads.
 *
 * Each [Deferred] is created with [CoroutineStart.LAZY] inside an independent
 * [repoScope] (backed by [SupervisorJob] + [Dispatchers.IO]).
 * - **LAZY start** means that if two coroutines race on a cache miss, only the
 *   single Deferred that wins the [Mutex] and is subsequently [await]ed will
 *   actually perform I/O. Any duplicate Deferred that loses the race is never
 *   started and is silently garbage-collected.
 * - **Independent scope** decouples the Deferred's lifecycle from the caller's
 *   scope. A caller's cancellation will NOT cancel a shared Deferred that other
 *   callers may still be awaiting.
 *
 * ## Cache eviction policy
 *
 * [LruCache.entryRemoved] intentionally does **not** cancel evicted Deferred
 * instances. Doing so would cause a [CancellationException] in any coroutine
 * currently awaiting the evicted entry, which is indistinguishable from the
 * caller's own cancellation and would break structured concurrency.
 * Orphaned Deferred instances complete naturally (~ms of I/O) and are then GC'd.
 * All Deferred instances are cancelled collectively when [destroy] is called.
 *
 * ## Lifecycle
 *
 * The caller (typically a DI container) **must** call [destroy] when this
 * repository is no longer needed to cancel [repoScope] and release resources.
 */
class AppIconRepositoryImpl(
    private val context: Context
) : AppIconRepository {

    companion object {
        /** Google Blue (#4285F4) — fallback seed color when extraction yields no result. */
        private const val FALLBACK_SEED_COLOR: Int = 0xFF4285F4.toInt()

        /** Bitmap size (px) used for color extraction — balances accuracy and memory. */
        private const val COLOR_EXTRACT_SIZE_PX = 256

        /** Max bitmap dimension (px) before pixel quantization — caps the pixel array at ~64 KB. */
        private const val QUANTIZE_BITMAP_MAX_SIZE = 128

        /** Maximum distinct colors extracted during quantization. */
        private const val DEFAULT_MAX_QUANTIZE_COLORS = 128

        /** Maximum entries in the icon LRU cache. */
        private const val MAX_CACHE_ENTRIES = 50
    }

    private val pm: PackageManager by lazy { context.packageManager }

    /** Current user ID derived from the process UID using the standard 100000-range formula. */
    private val currentUserId: Int by lazy {
        AndroidProcess.myUid() / 100000
    }

    // ---- Scope & synchronization ----

    /**
     * Independent scope whose lifetime is managed by [destroy].
     * [SupervisorJob] ensures a single icon-load failure does not cancel siblings.
     */
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Protects all reads/writes to [iconCache].
     * Preferred over `ConcurrentHashMap.computeIfAbsent` because it does not
     * block the underlying OS thread, aligning with the coroutine model.
     */
    private val cacheMutex = Mutex()

    // ---- Cache ----

    /**
     * Structured cache key — eliminates substring-matching bugs during invalidation.
     */
    private data class CacheKey(
        val sessionId: String,
        val packageName: String,
        val userId: Int,
        val iconSizePx: Int
    )

    /**
     * Bounded LRU cache storing [Deferred]<Bitmap?> loading operations.
     * See class-level KDoc for the eviction rationale.
     */
    private val iconCache = object : LruCache<CacheKey, Deferred<Bitmap?>>(MAX_CACHE_ENTRIES) {
        override fun entryRemoved(
            evicted: Boolean,
            key: CacheKey,
            oldValue: Deferred<Bitmap?>,
            newValue: Deferred<Bitmap?>?
        ) {
            // Intentional no-op — see class-level KDoc.
        }
    }

    /** Lazily allocated fallback icon, reused across all fallback paths. */
    private val fallbackSystemIcon: Bitmap? by lazy {
        ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)
            ?.toSafeBitmap(COLOR_EXTRACT_SIZE_PX)
    }

    // ---- Public API ----

    override suspend fun getIcon(
        sessionId: String,
        packageName: String,
        entityToInstall: AppEntity?,
        userId: Int,
        iconSizePx: Int,
        preferSystemIcon: Boolean
    ): Bitmap? {
        val cacheKey = CacheKey(sessionId, packageName, userId, iconSizePx)

        // Atomic get-or-create under Mutex; LAZY start avoids redundant I/O.
        val deferred = cacheMutex.withLock {
            iconCache.get(cacheKey) ?: repoScope.async(start = CoroutineStart.LAZY) {
                loadIconInternal(packageName, entityToInstall, userId, iconSizePx, preferSystemIcon)
            }.also { iconCache.put(cacheKey, it) }
        }

        return try {
            deferred.await() ?: fallbackSystemIcon
        } catch (_: CancellationException) {
            // If the *caller* was cancelled, ensureActive() rethrows immediately,
            // honouring structured concurrency.
            // If only the Deferred was cancelled externally (e.g., repoScope shutdown),
            // we degrade gracefully.
            currentCoroutineContext().ensureActive()

            Timber.w("Cached deferred was externally cancelled for package: %s", packageName)
            conditionalRemove(cacheKey, deferred)
            fallbackSystemIcon
        } catch (e: Exception) {
            Timber.w(e, "Failed to load icon for package: %s", packageName)
            conditionalRemove(cacheKey, deferred)
            fallbackSystemIcon
        }
    }

    override suspend fun extractColorFromApp(
        sessionId: String,
        packageName: String,
        entityToInstall: AppEntity.BaseEntity?,
        preferSystemIcon: Boolean,
        userId: Int?
    ): Int? {
        val targetUserId = userId ?: currentUserId
        val iconBitmap = getIcon(
            sessionId, packageName, entityToInstall,
            targetUserId, COLOR_EXTRACT_SIZE_PX, preferSystemIcon
        )
        return iconBitmap?.extractSeedColor()
    }

    override suspend fun extractColorFromDrawable(drawable: Drawable?): Int? {
        if (drawable == null) return null
        return try {
            drawable.toSafeBitmap(COLOR_EXTRACT_SIZE_PX).extractSeedColor()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract color from drawable")
            null
        }
    }

    override suspend fun clearCacheForSession(sessionId: String) {
        cacheMutex.withLock {
            iconCache.snapshot().keys
                .filter { it.sessionId == sessionId }
                .forEach { iconCache.remove(it) }
        }
    }

    override suspend fun clearCacheForPackage(packageName: String) {
        cacheMutex.withLock {
            iconCache.snapshot().keys
                .filter { it.packageName == packageName }
                .forEach { iconCache.remove(it) }
        }
    }

    /**
     * Releases all resources held by this repository.
     * Must be invoked when the owning DI scope is destroyed
     * (e.g., Hilt `@Singleton` scope end, Koin `onClose { destroy() }`, etc.).
     */
    fun destroy() {
        repoScope.cancel()
        iconCache.evictAll()
    }

    // ---- Internal loading logic ----

    /**
     * Core icon resolution strategy.
     *
     * **When [preferSystemIcon] is true** (typical for upgrades):
     * 1. Installed app icon via system launcher (themed, adaptive).
     * 2. APK file icon via system resource loader (themed, adaptive).
     * 3. Raw APK drawable parsed from the entity.
     *
     * **When [preferSystemIcon] is false** (typical for new installs):
     * 1. Raw APK drawable parsed from the entity.
     * 2. Installed app icon via system launcher (fallback).
     */
    private suspend fun loadIconInternal(
        packageName: String,
        entityToInstall: AppEntity?,
        userId: Int,
        iconSizePx: Int,
        preferSystemIcon: Boolean
    ): Bitmap? {
        val baseEntity = entityToInstall as? AppEntity.BaseEntity
        val rawApkIconDrawable = baseEntity?.icon
        val apkPath = (baseEntity?.data as? DataEntity.FileEntity)?.path

        val installedAppInfo = try {
            pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

        return if (preferSystemIcon) {
            installedAppInfo?.let {
                AppIconCache.loadIconBitmap(context, it, userId, iconSizePx)
            }
                ?: apkPath?.let { loadIconFromApkWithSystemLoader(it, userId, iconSizePx) }
                ?: rawApkIconDrawable?.toSafeBitmap(iconSizePx)
        } else {
            rawApkIconDrawable?.toSafeBitmap(iconSizePx)
                ?: installedAppInfo?.let {
                    AppIconCache.loadIconBitmap(context, it, userId, iconSizePx)
                }
        }
    }

    /**
     * Loads an icon from a raw APK file using the system's package-manager
     * resource loader, which applies adaptive icon masks and themed overlays.
     */
    private suspend fun loadIconFromApkWithSystemLoader(
        apkPath: String,
        userId: Int,
        iconSizePx: Int
    ): Bitmap? {
        return try {
            val packageInfo = pm.getPackageArchiveInfo(apkPath, 0)
            packageInfo?.applicationInfo?.let { appInfo ->
                appInfo.sourceDir = apkPath
                appInfo.publicSourceDir = apkPath
                AppIconCache.loadIconBitmap(context, appInfo, userId, iconSizePx)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Could not load themed icon from APK at path: %s", apkPath)
            null
        }
    }

    // ---- Bitmap utilities ----

    /**
     * Converts a [Drawable] to a [Bitmap] with a maximum-size constraint.
     * Never upscales (scale factor capped at 1.0); only downscales when the
     * intrinsic dimensions exceed [maxSizePx].
     * Prevents `Canvas: trying to draw too large bitmap` crashes.
     */
    private fun Drawable.toSafeBitmap(maxSizePx: Int): Bitmap {
        val rawWidth = intrinsicWidth.takeIf { it > 0 } ?: maxSizePx
        val rawHeight = intrinsicHeight.takeIf { it > 0 } ?: maxSizePx

        val scale = minOf(1f, maxSizePx.toFloat() / rawWidth, maxSizePx.toFloat() / rawHeight)

        return this.toBitmap(
            width = (rawWidth * scale).toInt().coerceAtLeast(1),
            height = (rawHeight * scale).toInt().coerceAtLeast(1)
        )
    }

    /**
     * Extracts the most prominent seed color from a [Bitmap] using the
     * Celebi quantizer + HCT scoring algorithm (Material You color extraction).
     *
     * The bitmap is pre-scaled to at most [QUANTIZE_BITMAP_MAX_SIZE] px on each
     * axis, capping the pixel array at ~64 KB and keeping quantization fast.
     *
     * Any temporary scaled bitmap is recycled in a `finally` block to guarantee
     * prompt native memory release even when an exception occurs.
     */
    private suspend fun Bitmap.extractSeedColor(
        maxColors: Int = DEFAULT_MAX_QUANTIZE_COLORS,
        fallbackColorArgb: Int = FALLBACK_SEED_COLOR
    ): Int = withContext(Dispatchers.Default) {
        val needsScaling =
            width > QUANTIZE_BITMAP_MAX_SIZE || height > QUANTIZE_BITMAP_MAX_SIZE

        val scaledBitmap = if (needsScaling) {
            val scale = minOf(
                QUANTIZE_BITMAP_MAX_SIZE.toFloat() / width,
                QUANTIZE_BITMAP_MAX_SIZE.toFloat() / height
            )
            this@extractSeedColor.scale((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
        } else {
            this@extractSeedColor
        }

        try {
            val pixels = IntArray(scaledBitmap.width * scaledBitmap.height)
            scaledBitmap.getPixels(
                pixels, 0, scaledBitmap.width,
                0, 0, scaledBitmap.width, scaledBitmap.height
            )

            val quantized: Map<Int, Int> = QuantizerCelebi.quantize(pixels, maxColors)
            Score.score(quantized, 1, fallbackColorArgb, true).first()
        } finally {
            // Recycle only the temporary copy — never the caller's original bitmap.
            if (scaledBitmap !== this@extractSeedColor) {
                scaledBitmap.recycle()
            }
        }
    }

    // ---- Cache helpers ----

    /**
     * Removes [cacheKey] from the cache **only if** the stored value is still
     * the exact [expected] Deferred instance. Prevents accidental removal of a
     * newer retry that another coroutine may have inserted in the meantime.
     */
    private suspend fun conditionalRemove(cacheKey: CacheKey, expected: Deferred<Bitmap?>) {
        cacheMutex.withLock {
            if (iconCache.get(cacheKey) === expected) {
                iconCache.remove(cacheKey)
            }
        }
    }
}
