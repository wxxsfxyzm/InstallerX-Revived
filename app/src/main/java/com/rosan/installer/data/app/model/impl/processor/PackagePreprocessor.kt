package com.rosan.installer.data.app.model.impl.processor

import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.entity.InstalledAppInfo
import com.rosan.installer.data.app.model.entity.SignatureMatchStatus
import com.rosan.installer.data.app.util.calculateSHA256
import com.rosan.installer.data.app.util.sourcePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.util.zip.ZipFile

object PackagePreprocessor {

    data class ProcessedGroup(
        val packageName: String,
        val entities: List<AppEntity>,
        val installedInfo: InstalledAppInfo?
    )

    data class SessionTypeInfo(
        val isMultiAppSession: Boolean,
        val containerType: DataType,
        val isFromSingleFile: Boolean
    )

    /**
     * 并行处理原始数据：分组 -> 去重 -> 获取系统安装信息
     */
    suspend fun process(rawEntities: List<AppEntity>): List<ProcessedGroup> = coroutineScope {
        rawEntities
            .groupBy { it.packageName }
            .map { (packageName, entities) ->
                async(Dispatchers.IO) {
                    // 1. 并行去重 (IO密集)
                    val deduplicated = deduplicateEntities(entities)
                    // 2. 并行获取系统信息 (IPC/IO密集)
                    val installedInfo = InstalledAppInfo.buildByPackageName(packageName)

                    ProcessedGroup(packageName, deduplicated, installedInfo)
                }
            }.awaitAll()
    }

    /**
     * 判断会话类型 (用于 UI 展示逻辑)
     */
    fun determineSessionType(
        groups: List<ProcessedGroup>,
        rawEntities: List<AppEntity>
    ): SessionTypeInfo {
        val allEntities = groups.flatMap { it.entities }

        // 1. 判断源文件数量
        val sourcePaths = allEntities.mapNotNull { it.data.sourcePath() }.distinct()
        val isFromSingleFile = sourcePaths.size == 1

        // 2. 获取主容器类型
        val firstType = allEntities.firstOrNull()?.sourceType

        // 3. 判断是否为多应用模式
        val isMultiPackage = groups.size > 1
        val hasMultipleBases = !isMultiPackage && allEntities.count { it is AppEntity.BaseEntity } > 1

        // 特殊逻辑：如果是混合模块，强制不视为多应用模式，保持整体性
        val isMixedModule = firstType == DataType.MIXED_MODULE_APK || firstType == DataType.MIXED_MODULE_ZIP

        val isMultiAppSession = !isMixedModule && (isMultiPackage || hasMultipleBases)

        // 4. 修正最终的容器类型展示
        val finalContainerType = when {
            isMultiAppSession && firstType == DataType.MULTI_APK_ZIP -> DataType.MULTI_APK_ZIP
            isMultiAppSession -> DataType.MULTI_APK
            else -> firstType ?: DataType.APK
        }

        return SessionTypeInfo(isMultiAppSession, finalContainerType, isFromSingleFile)
    }

    /**
     * 签名校验状态辅助方法
     */
    fun checkSignature(
        baseEntity: AppEntity.BaseEntity?,
        installedInfo: InstalledAppInfo?
    ): SignatureMatchStatus {
        return when {
            installedInfo == null -> SignatureMatchStatus.NOT_INSTALLED
            baseEntity?.signatureHash.isNullOrBlank() || installedInfo.signatureHash.isNullOrBlank() ->
                SignatureMatchStatus.UNKNOWN_ERROR

            baseEntity.signatureHash == installedInfo.signatureHash ->
                SignatureMatchStatus.MATCH

            else ->
                SignatureMatchStatus.MISMATCH
        }
    }

    //Internal: 去重逻辑
    private suspend fun deduplicateEntities(entities: List<AppEntity>): List<AppEntity> = coroutineScope {
        val bases = entities.filterIsInstance<AppEntity.BaseEntity>()
        // 只有一个 Base 就不需要去重计算，直接返回
        if (bases.size <= 1) return@coroutineScope entities

        // 计算指纹
        val hashedBases = bases.map { entity ->
            async { calculateFingerprint(entity) to entity }
        }.awaitAll()

        // 根据指纹去重
        val distinctBases = hashedBases
            .distinctBy { it.first } // pair.first is hash
            .map { it.second }

        val others = entities.filter { it !is AppEntity.BaseEntity }
        distinctBases + others
    }

    private fun calculateFingerprint(entity: AppEntity.BaseEntity): String {
        return when (val data = entity.data) {
            is DataEntity.FileEntity -> {
                // 全文件哈希 (较慢但准确)
                File(data.path).calculateSHA256() ?: "${data.path}_${entity.versionCode}"
            }

            is DataEntity.ZipFileEntity -> {
                // Zip Entry 优化：只用 CRC + Size，不解压，速度极快
                try {
                    ZipFile(data.parent.path).use { zip ->
                        zip.getEntry(data.name)?.let { "${it.crc}|${it.size}" }
                    } ?: "${data.name}_${entity.versionCode}"
                } catch (e: Exception) {
                    "${data.name}_${entity.versionCode}"
                }
            }

            else -> "${entity.packageName}_${entity.versionCode}"
        }
    }
}