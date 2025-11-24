package com.rosan.installer.build.repo

/**
 * 设备配置接口，用于解耦硬件依赖
 */
interface DeviceProfile {
    val supportedArchitectures: List<String>
    val supportedDensities: List<String>
    val supportedLocales: List<String>
}