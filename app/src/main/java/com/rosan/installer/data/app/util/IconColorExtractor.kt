package com.rosan.installer.data.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import com.kyant.m3color.quantize.QuantizerCelebi
import com.kyant.m3color.score.Score
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.repo.AppIconRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class IconColorExtractor : KoinComponent {
    private val appIconRepo: AppIconRepo by inject()

    /**
     * Extracts the Material 3 seed color from an app's icon.
     * This is the main entry point for this utility.
     *
     * @param sessionId The installer session ID.
     * @param packageName The package name to get the icon from.
     * @param entityToInstall The specific AppEntity for context.
     * @param preferSystemIcon Whether to prefer the system's cached icon.
     * @return ARGB formatted seed color (Int), or null if extraction fails.
     */
    suspend fun extractColorFromApp(
        sessionId: String,
        packageName: String,
        entityToInstall: AppEntity.BaseEntity?,
        preferSystemIcon: Boolean
    ): Int? {
        return try {
            //  Get the icon drawable
            val iconSizePx = 256 // A reasonably high resolution for color quantization
            val iconDrawable = appIconRepo.getIcon(
                sessionId = sessionId,
                packageName = packageName,
                entityToInstall = entityToInstall,
                iconSizePx = iconSizePx,
                preferSystemIcon = preferSystemIcon
            ) ?: return null

            // Convert drawable to bitmap
            val bitmap = drawableToBitmap(iconDrawable)

            // Extract the seed color from the bitmap
            extractSeedColorFromBitmap(bitmap)
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract color for package: $packageName")
            null
        }
    }

    /**
     * Converts a Drawable to a Bitmap.
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Performs the actual color quantization and scoring.
     */
    private suspend fun extractSeedColorFromBitmap(
        bitmap: Bitmap,
        maxColors: Int = 128,
        fallbackColorArgb: Int = -12417548 // 0xFF3F51B5 - Indigo 500
    ): Int = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val colorToCountMap: Map<Int, Int> = QuantizerCelebi.quantize(pixels, maxColors)
        val sortedColors: List<Int> = Score.score(colorToCountMap, 1, fallbackColorArgb, true)

        sortedColors.first()
    }
}