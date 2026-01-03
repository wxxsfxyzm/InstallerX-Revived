package com.rosan.installer.data.app.model.entity

import android.graphics.drawable.Drawable
import com.rosan.installer.build.model.entity.Architecture
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.app.util.FilterType
import com.rosan.installer.data.app.util.SplitType

sealed class AppEntity {
    abstract val packageName: String
    abstract val name: String
    abstract val data: DataEntity
    abstract val targetSdk: String?
    abstract val minSdk: String?
    abstract val arch: Architecture?
    abstract val size: Long
    abstract val sourceType: DataType?

    data class BaseEntity(
        override val packageName: String,
        val sharedUserId: String?,
        override val data: DataEntity,
        val versionCode: Long,
        val versionName: String,
        val label: String?,
        val icon: Drawable?,
        override val name: String = "base.apk",
        override val targetSdk: String?,
        override val minSdk: String?,
        // Only available for oppo apk
        val minOsdkVersion: String? = null,
        override val arch: Architecture? = null,
        override val size: Long = data.getSize(),
        override val sourceType: DataType? = null,
        // Get from AndroidManifest.xml
        val permissions: List<String>? = null,
        val signatureHash: String? = null,
        val fileHash: String? = null
    ) : AppEntity()

    data class SplitEntity(
        override val packageName: String,
        override val data: DataEntity,
        val splitName: String,
        override val targetSdk: String?,
        override val minSdk: String?,
        override val arch: Architecture?,
        override val size: Long = data.getSize(),
        override val sourceType: DataType? = null,
        // Split Type: Used for UI grouping (determines under which header it's displayed)
        val type: SplitType = SplitType.FEATURE,
        // Filter Type: Used for installation selection strategies (how to filter)
        // The default is NONE, meaning this Split has no special hardware/language constraints
        // and can be installed as long as the user wants it.
        val filterType: FilterType = FilterType.NONE,
        // Extracted config value ("zh", "xhdpi", "arm64-v8a")
        val configValue: String? = null
    ) : AppEntity() {
        override val name = "$splitName.apk"
    }

    data class DexMetadataEntity(
        override val packageName: String,
        override val data: DataEntity,
        val dmName: String,
        override val targetSdk: String?,
        override val minSdk: String?,
        override val arch: Architecture? = null,
        override val size: Long = data.getSize(),
        override val sourceType: DataType? = null,
    ) : AppEntity() {
        override val name = "base.dm"
    }

    data class ModuleEntity(
        val id: String,
        override val name: String,
        val version: String,
        val versionCode: Long,
        val author: String,
        val description: String,
        override val data: DataEntity,
        override val size: Long = data.getSize(),
        override val sourceType: DataType? = null
    ) : AppEntity() {
        override val packageName: String
            get() = id
        override val targetSdk: String? = null
        override val minSdk: String? = null
        override val arch: Architecture? = null
    }
}