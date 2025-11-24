package com.rosan.installer.build.model.impl

import com.rosan.installer.build.repo.DeviceProfile

/**
 * 默认设备配置实现（从 RsConfig 获取）
 */
class DefaultDeviceProfile : DeviceProfile {
    override val supportedArchitectures: List<String>
        get() = com.rosan.installer.build.RsConfig.supportedArchitectures.map { it.arch }

    override val supportedDensities: List<String>
        get() = com.rosan.installer.build.RsConfig.supportedDensities.map { it.key }

    override val supportedLocales: List<String>
        get() = com.rosan.installer.build.RsConfig.supportedLocales
}