package com.rosan.installer.data.app.model.entity

import android.graphics.drawable.Drawable

sealed class AppEntity {
    abstract val packageName: String

    abstract val name: String

    abstract val targetSdk: String?

    abstract val minSdk: String?

    data class BaseEntity(
        override val packageName: String,
        val data: DataEntity,
        val versionCode: Long,
        val versionName: String,
        val label: String?,
        val icon: Drawable?,
        override val targetSdk: String?,
        override val minSdk: String?,
        // Get from AndroidManifest.xml
        val permissions: List<String>? = null,
        // Set by ExtendedMenu
        val permissionsToGrant: MutableList<String>? = null
    ) : AppEntity() {
        override val name = "base.apk"
    }

    data class SplitEntity(
        override val packageName: String,
        val data: DataEntity,
        val splitName: String,
        override val targetSdk: String?,
        override val minSdk: String?,
    ) : AppEntity() {
        override val name = "$splitName.apk"
    }

    data class DexMetadataEntity(
        override val packageName: String,
        val data: DataEntity,
        val dmName: String,
        override val targetSdk: String?,
        override val minSdk: String?,
    ) : AppEntity() {
        override val name = "base.dm"
    }
}