// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.rosan.installer.ui.library.effect

import android.os.Build
import androidx.annotation.RequiresApi
import top.yukonga.miuix.kmp.blur.RuntimeShader
import kotlin.time.TimeSource

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class BgEffectPainter {
    val shaderCode by lazy { OS3_BG_FRAG }
    private var bound: FloatArray = FloatArray(4)
    val runtimeShader by lazy { RuntimeShader(shaderCode) }
    private var uResolution: FloatArray = floatArrayOf(0f, 0f)
    private var uAnimTime = TimeSource.Monotonic.markNow().elapsedNow().inWholeNanoseconds / 1_000_000_000.0f

    // 配置常量
    companion object {
        private const val U_TRANSLATE_Y = 0.0f
        private const val U_ALPHA_MULTI = 1.0f
        private const val U_NOISE_SCALE = 1.5f
        private const val U_POINT_OFFSET = 0.1f
        private const val U_POINT_RADIUS_MULTI = 1.0f
        private const val U_ALPHA_OFFSET = 0.5f
        private const val U_SHADOW_COLOR_MULTI = 0.3f
        private const val U_SHADOW_COLOR_OFFSET = 0.3f
        private const val U_SHADOW_NOISE_SCALE = 5.0f
        private const val U_SHADOW_OFFSET = 0.01f

        // 预设配置
        private val PHONE_LIGHT_POINTS = floatArrayOf(
            0.67f, 0.42f, 1.0f, 0.69f, 0.75f, 1.0f,
            0.14f, 0.71f, 0.95f, 0.14f, 0.27f, 0.8f,
        )

        private val PHONE_LIGHT_COLORS = floatArrayOf(
            0.57f, 0.76f, 0.98f, 1.0f,
            0.98f, 0.85f, 0.68f, 1.0f,
            0.98f, 0.75f, 0.93f, 1.0f,
            0.73f, 0.7f, 0.98f, 1.0f,
        )

        private val PHONE_DARK_POINTS = floatArrayOf(
            0.63f, 0.5f, 0.88f, 0.69f, 0.75f, 0.8f,
            0.17f, 0.66f, 0.81f, 0.14f, 0.24f, 0.72f,
        )

        private val PHONE_DARK_COLORS = floatArrayOf(
            0.0f, 0.31f, 0.58f, 1.0f,
            0.53f, 0.29f, 0.15f, 1.0f,
            0.46f, 0.06f, 0.27f, 1.0f,
            0.16f, 0.12f, 0.45f, 1.0f,
        )

        private val PAD_LIGHT_POINTS = floatArrayOf(
            0.67f, 0.37f, 0.88f, 0.54f, 0.66f, 1.0f,
            0.37f, 0.71f, 0.68f, 0.28f, 0.26f, 0.62f,
        )

        private val PAD_LIGHT_COLORS = floatArrayOf(
            0.57f, 0.76f, 0.98f, 1.0f,
            0.98f, 0.85f, 0.68f, 1.0f,
            0.98f, 0.75f, 0.93f, 0.95f,
            0.73f, 0.7f, 0.98f, 0.9f,
        )

        private val PAD_DARK_POINTS = floatArrayOf(
            0.55f, 0.42f, 1.0f, 0.56f, 0.75f, 1.0f,
            0.4f, 0.59f, 0.71f, 0.43f, 0.09f, 0.75f,
        )

        private val PAD_DARK_COLORS = floatArrayOf(
            0.0f, 0.31f, 0.58f, 1.0f,
            0.53f, 0.29f, 0.15f, 1.0f,
            0.46f, 0.06f, 0.27f, 1.0f,
            0.16f, 0.12f, 0.45f, 1.0f,
        )
    }

    // 当前配置
    private var uBgBound = floatArrayOf(0.0f, 0.4489f, 1.0f, 0.5511f)
    private var uPoints = PHONE_LIGHT_POINTS
    private var uColors = PHONE_LIGHT_COLORS
    private var uSaturateOffset = 0.2f
    private var uLightOffset = 0.1f

    private var deviceType: DeviceType = DeviceType.PHONE

    enum class DeviceType {
        PHONE,
        PAD,
    }

    init {
        initializeShader()
    }

    private fun initializeShader() {
        runtimeShader.apply {
            setFloatUniform("uTranslateY", U_TRANSLATE_Y)
            setFloatUniform("uNoiseScale", U_NOISE_SCALE)
            setFloatUniform("uPointOffset", U_POINT_OFFSET)
            setFloatUniform("uPointRadiusMulti", U_POINT_RADIUS_MULTI)
            setFloatUniform("uSaturateOffset", uSaturateOffset)
            setFloatUniform("uShadowColorMulti", U_SHADOW_COLOR_MULTI)
            setFloatUniform("uShadowColorOffset", U_SHADOW_COLOR_OFFSET)
            setFloatUniform("uShadowOffset", U_SHADOW_OFFSET)
            setFloatUniform("uBound", uBgBound)
            setFloatUniform("uAlphaMulti", U_ALPHA_MULTI)
            setFloatUniform("uLightOffset", uLightOffset)
            setFloatUniform("uAlphaOffset", U_ALPHA_OFFSET)
            setFloatUniform("uShadowNoiseScale", U_SHADOW_NOISE_SCALE)

            // 设置动态参数
            setFloatUniform("uPoints", uPoints)
            setFloatUniform("uColors", uColors)
        }
    }

    fun updateMaterials() {
        runtimeShader.apply {
            setFloatUniform("uAnimTime", uAnimTime)
            setFloatUniform("uResolution", uResolution)
        }
    }

    fun setAnimTime(f: Float) {
        uAnimTime = f
    }

    fun setResolution(width: Float, height: Float) {
        uResolution = floatArrayOf(width, height)
    }

    fun setResolution(fArr: FloatArray) {
        uResolution = fArr.copyOf()
    }

    private fun setColors(fArr: FloatArray) {
        uColors = fArr.copyOf()
        runtimeShader.setFloatUniform("uColors", fArr)
    }

    private fun setPoints(fArr: FloatArray) {
        uPoints = fArr.copyOf()
        runtimeShader.setFloatUniform("uPoints", fArr)
    }

    private fun setBound(fArr: FloatArray) {
        this.uBgBound = fArr.copyOf()
        this.runtimeShader.setFloatUniform("uBound", fArr)
    }

    private fun setLightOffset(f: Float) {
        this.uLightOffset = f
        this.runtimeShader.setFloatUniform("uLightOffset", f)
    }

    private fun setSaturateOffset(f: Float) {
        this.uSaturateOffset = f
        this.runtimeShader.setFloatUniform("uSaturateOffset", f)
    }

    fun setDeviceType(type: DeviceType) {
        deviceType = type
    }

    fun showRuntimeShader(logoHeight: Float, height: Float, width: Float, isDarkMode: Boolean) {
        calcAnimationBound(logoHeight, height, width)
        updateMode(isDarkMode)
    }

    fun updateMode(isDarkMode: Boolean) {
        when (deviceType) {
            DeviceType.PHONE if !isDarkMode -> setPhoneLight()
            DeviceType.PHONE if isDarkMode -> setPhoneDark()
            DeviceType.PAD if !isDarkMode -> setPadLight()
            DeviceType.PAD if isDarkMode -> setPadDark()
            else -> {}
        }
    }

    private fun setPhoneLight() {
        setLightOffset(0.1f)
        setSaturateOffset(0.2f)
        setPoints(PHONE_LIGHT_POINTS)
        setColors(PHONE_LIGHT_COLORS)
        setBound(bound)
    }

    private fun setPhoneDark() {
        setLightOffset(-0.1f)
        setSaturateOffset(0.2f)
        setPoints(PHONE_DARK_POINTS)
        setColors(PHONE_DARK_COLORS)
        setBound(bound)
    }

    private fun setPadLight() {
        setLightOffset(0.1f)
        setSaturateOffset(0.0f)
        setPoints(PAD_LIGHT_POINTS)
        setColors(PAD_LIGHT_COLORS)
        setBound(bound)
    }

    private fun setPadDark() {
        setLightOffset(-0.1f)
        setSaturateOffset(0.2f)
        setPoints(PAD_DARK_POINTS)
        setColors(PAD_DARK_COLORS)
        setBound(bound)
    }

    private fun calcAnimationBound(logoHeight: Float, totalHeight: Float, totalWidth: Float) {
        val heightRatio = logoHeight / totalHeight

        bound = if (totalWidth <= totalHeight) {
            // 竖屏
            floatArrayOf(0.0f, 1.0f - heightRatio, 1.0f, heightRatio)
        } else {
            // 横屏
            val widthRatio = logoHeight / totalWidth
            val xOffset = (totalWidth - logoHeight) / 2.0f / totalWidth
            floatArrayOf(xOffset, 1.0f - heightRatio, widthRatio, heightRatio)
        }
    }
}
