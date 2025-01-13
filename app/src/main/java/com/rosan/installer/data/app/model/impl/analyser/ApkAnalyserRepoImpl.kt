package com.rosan.installer.data.app.model.impl.analyser

import android.content.res.ApkAssets
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.repo.AnalyserRepo
import com.rosan.installer.data.reflect.repo.ReflectRepo
import com.rosan.installer.data.res.model.impl.AxmlTreeRepoImpl
import com.rosan.installer.data.res.repo.AxmlTreeRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.FileDescriptor

object ApkAnalyserRepoImpl : AnalyserRepo, KoinComponent {
    private val reflect = get<ReflectRepo>()

    override suspend fun doWork(
        config: ConfigEntity,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val apps = mutableListOf<AppEntity>()
        data.forEach { apps.addAll(doWork(config, it)) }
        return apps
    }

    private fun doWork(config: ConfigEntity, data: DataEntity): List<AppEntity> {
        return when (data) {
            is DataEntity.FileEntity -> doFileWork(config, data)
            is DataEntity.FileDescriptorEntity -> doFileDescriptorWork(config, data)
            else -> throw Exception("can't analyse this entity: $data")
        }
    }

    private fun <R> useResources(block: (resources: Resources) -> R): R {
        val resources = createResources()
        return resources.assets.use {
            block.invoke(resources)
        }
    }

    private fun createResources(): Resources {
        val resources = Resources.getSystem()
        val constructor = reflect.getDeclaredConstructor(AssetManager::class.java)
            ?: return resources
        val assetManager = constructor.newInstance() as AssetManager
        return Resources(assetManager, resources.displayMetrics, resources.configuration)
    }

    private fun doFileWork(config: ConfigEntity, data: DataEntity.FileEntity): List<AppEntity> {
        val path = data.path
        return useResources { resources ->
            setAssetPath(resources.assets, arrayOf(ApkAssets.loadFromPath(path)))
            listOf(loadAppEntity(resources, resources.newTheme(), data))
        }
    }

    private fun doFileDescriptorWork(
        config: ConfigEntity, data: DataEntity.FileDescriptorEntity
    ): List<AppEntity> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) throw Exception("FileDescriptor Analyser only work on Android P or greater")
        val fileDescriptor =
            data.getFileDescriptor() ?: throw Exception("can't get fd from '$data'")
        return useResources { resources ->
            setAssetPath(resources.assets, arrayOf(loadFromFd(fileDescriptor)))
            listOf(loadAppEntity(resources, resources.newTheme(), data))
        }
    }

    private fun addAssetPath(assetManager: AssetManager, path: String) {
        val addAssetPathMtd = reflect.getDeclaredMethod(
            AssetManager::class.java, "addAssetPath", String::class.java
        )!!
        addAssetPathMtd.isAccessible = true
        val cookie = addAssetPathMtd.invoke(assetManager, path) as Int
        if (cookie == 0) throw Exception("the cookie of the added asset, or 0 on failure.")
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private fun setAssetPath(assetManager: AssetManager, assets: Array<ApkAssets>) {
        val setApkAssetsMtd = reflect.getDeclaredMethod(
            AssetManager::class.java,
            "setApkAssets",
            Array<ApkAssets>::class.java,
            Boolean::class.java
        )!!
        setApkAssetsMtd.isAccessible = true
        setApkAssetsMtd.invoke(assetManager, assets, true)
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private fun loadFromFd(fileDescriptor: FileDescriptor): ApkAssets {
        val friendlyName = "${fileDescriptor.hashCode()}.apk"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ApkAssets.loadFromFd(
            fileDescriptor, friendlyName, 0, null
        )
        else ApkAssets.loadFromFd(fileDescriptor, friendlyName, true, false)
    }

    private fun loadAppEntity(
        resources: Resources, theme: Resources.Theme?, data: DataEntity
    ): AppEntity {
        var packageName: String? = null
        var splitName: String? = null
        var versionCode: Long = -1
        var versionName = ""
        var label: String? = null
        var icon: Drawable? = null
        var roundIcon: Drawable? = null
        AxmlTreeRepoImpl(resources.assets.openXmlResourceParser("AndroidManifest.xml")).register("/manifest") {
            packageName = getAttributeValue(null, "package")
            splitName = getAttributeValue(null, "split")
            val versionCodeMajor = getAttributeIntValue(
                AxmlTreeRepo.ANDROID_NAMESPACE, "versionCodeMajor", 0
            ).toLong()
            val versionCodeMinor = getAttributeIntValue(
                AxmlTreeRepo.ANDROID_NAMESPACE, "versionCode", 0
            ).toLong()
            versionCode = versionCodeMajor shl 32 or (versionCodeMinor and 0xffffffffL)
            versionName =
                getAttributeValue(AxmlTreeRepo.ANDROID_NAMESPACE, "versionName") ?: versionName
        }.register("/manifest/application") {
            label = when (val resId =
                getAttributeResourceValue(AxmlTreeRepo.ANDROID_NAMESPACE, "label", -1)) {
                -1 -> getAttributeValue(AxmlTreeRepo.ANDROID_NAMESPACE, "label") ?: label
                0 -> null
                else -> resources.getString(resId)
            }
            icon = when (val resId =
                getAttributeResourceValue(AxmlTreeRepo.ANDROID_NAMESPACE, "icon", -1)) {
                -1 -> null
                0 -> null
                else -> ResourcesCompat.getDrawable(resources, resId, theme)
            }
            roundIcon = when (val resId = getAttributeResourceValue(
                AxmlTreeRepo.ANDROID_NAMESPACE, "roundIcon", -1
            )) {
                -1 -> null
                0 -> null
                else -> ResourcesCompat.getDrawable(resources, resId, theme)
            }
        }.map { }
        if (packageName.isNullOrEmpty()) throw Exception("can't get the package from this package")
        return if (splitName.isNullOrEmpty()) AppEntity.BaseEntity(
            packageName = packageName!!,
            data = data,
            versionCode = versionCode,
            versionName = versionName,
            label = label,
            icon = roundIcon ?: icon
        ) else AppEntity.SplitEntity(
            packageName = packageName!!,
            data = data,
            splitName = splitName!!
        )
    }
}