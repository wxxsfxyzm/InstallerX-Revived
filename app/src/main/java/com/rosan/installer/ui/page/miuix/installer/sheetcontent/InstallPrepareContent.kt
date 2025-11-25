package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Manufacturer
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.entity.InstalledAppInfo
import com.rosan.installer.data.app.model.entity.SignatureMatchStatus
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixNavigationItemWidget
import com.rosan.installer.ui.theme.miuixSheetCardColorDark
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InstallPrepareContent(
    colorScheme: ColorScheme,
    isDarkMode: Boolean,
    installer: InstallerRepo,
    viewModel: InstallerViewModel,
    onCancel: () -> Unit,
    onInstall: () -> Unit
) {
    val context = LocalContext.current
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val currentPackage = installer.analysisResults.find { it.packageName == currentPackageName }
    val displayIcons by viewModel.displayIcons.collectAsState()
    val settings = viewModel.viewSettings

    var isExpanded by remember { mutableStateOf(false) }

    if (currentPackage == null) {
        LoadingContent(statusText = stringResource(id = R.string.loading))
        return
    }

    val allEntities = (if (installer.analysisResults.size > 1) {
        currentPackage.appEntities.filter { it.selected }
    } else {
        currentPackage.appEntities
    }).map { it.app }

    val primaryEntity = allEntities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
        ?: allEntities.filterIsInstance<AppEntity.ModuleEntity>().firstOrNull()
        ?: allEntities.sortedBest().firstOrNull()

    if (primaryEntity == null) {
        LoadingContent(statusText = "No main app entity found")
        return
    }

    val containerType = primaryEntity.sourceType
    val entityToInstall = allEntities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
    val displayIcon = if (currentPackageName != null) displayIcons[currentPackageName] else null

    val errorColor = MaterialTheme.colorScheme.error
    val tertiaryColor = MiuixTheme.colorScheme.primary

    val (warningMessages, buttonTextId) = remember(currentPackage, entityToInstall) {
        val oldInfo = currentPackage.installedAppInfo
        val signatureStatus = currentPackage.signatureMatchStatus
        val warnings = mutableListOf<Pair<String, Color>>()
        var finalButtonTextId = R.string.install
        if (entityToInstall != null) {
            if (oldInfo == null) {
                finalButtonTextId = R.string.install
            } else {
                when {
                    entityToInstall.versionCode > oldInfo.versionCode -> finalButtonTextId = R.string.upgrade
                    entityToInstall.versionCode < oldInfo.versionCode -> {
                        warnings.add(context.getString(R.string.installer_prepare_type_downgrade) to errorColor)
                        finalButtonTextId = R.string.install_anyway
                    }

                    oldInfo.isArchived -> finalButtonTextId = R.string.unarchive
                    else -> finalButtonTextId = R.string.reinstall
                }
            }
        }
        if (primaryEntity.sourceType == DataType.APK || primaryEntity.sourceType == DataType.APKS)
            when (signatureStatus) {
                SignatureMatchStatus.MISMATCH -> {
                    warnings.add(0, context.getString(R.string.installer_prepare_signature_mismatch) to errorColor)
                    finalButtonTextId = R.string.install_anyway
                }

                SignatureMatchStatus.UNKNOWN_ERROR -> {
                    warnings.add(0, context.getString(R.string.installer_prepare_signature_unknown) to tertiaryColor)
                }

                else -> {}
            }
        val newMinSdk = entityToInstall?.minSdk?.toIntOrNull()
        if (newMinSdk != null && newMinSdk > Build.VERSION.SDK_INT) {
            warnings.add(0, context.getString(R.string.installer_prepare_sdk_incompatible) to errorColor)
        }
        Pair(warnings, finalButtonTextId)
    }

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val displayLabel = when (primaryEntity) {
            is AppEntity.BaseEntity -> primaryEntity.label ?: primaryEntity.packageName
            is AppEntity.ModuleEntity -> primaryEntity.name
            else -> primaryEntity.packageName
        }
        item {
            AppInfoSlot(
                icon = displayIcon,
                label = displayLabel,
                packageName = primaryEntity.packageName
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            WarningTextBlock(warnings = warningMessages)
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardColors(
                    color = if (isDynamicColor) colorScheme.surfaceContainer else
                        if (isDarkMode) miuixSheetCardColorDark else Color.White,
                    contentColor = MiuixTheme.colorScheme.onSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (primaryEntity) {
                        is AppEntity.BaseEntity -> {
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_name_label,
                                newValue = primaryEntity.versionName,
                                oldValue = currentPackage.installedAppInfo?.versionName,
                                isArchived = currentPackage.installedAppInfo?.isArchived ?: false
                            )
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_code_label,
                                newValue = primaryEntity.versionCode.toString(),
                                oldValue = currentPackage.installedAppInfo?.versionCode?.toString(),
                                isDowngrade = if (currentPackage.installedAppInfo != null) primaryEntity.versionCode < currentPackage.installedAppInfo.versionCode else false,
                                isArchived = currentPackage.installedAppInfo?.isArchived ?: false
                            )
                            SDKComparison(
                                entityToInstall = primaryEntity,
                                preInstallAppInfo = currentPackage.installedAppInfo,
                                installer = installer
                            )
                            if (RsConfig.currentManufacturer == Manufacturer.OPPO || RsConfig.currentManufacturer == Manufacturer.ONEPLUS) {
                                AnimatedVisibility(visible = settings.showOPPOSpecial && primaryEntity.sourceType == DataType.APK) {
                                    primaryEntity.minOsdkVersion?.let {
                                        AdaptiveInfoRow(
                                            labelResId = R.string.installer_package_minOsdkVersion_label,
                                            newValue = it,
                                            oldValue = null,
                                            isArchived = false
                                        )
                                    }
                                }
                            }
                        }

                        is AppEntity.ModuleEntity -> {
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_name_label,
                                newValue = primaryEntity.version,
                                oldValue = null,
                                isArchived = false
                            )
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_code_label,
                                newValue = primaryEntity.versionCode.toString(),
                                oldValue = null,
                                isArchived = false
                            )
                            AnimatedVisibility(visible = installer.config.displaySdk) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AdaptiveInfoRow(
                                        labelResId = R.string.installer_module_author_label,
                                        newValue = primaryEntity.author,
                                        oldValue = null,
                                        isArchived = false
                                    )
                                }
                            }
                        }

                        else -> null
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardColors(
                        color = if (isDynamicColor) colorScheme.surfaceContainer else
                            if (isDarkMode) miuixSheetCardColorDark else Color.White,
                        contentColor = MiuixTheme.colorScheme.onSurface
                    )
                ) {
                    Column {
                        // Permissions List
                        if (containerType == DataType.APK)
                            MiuixNavigationItemWidget(
                                title = stringResource(R.string.permission_list),
                                description = stringResource(R.string.permission_list_desc),
                                insideMargin = PaddingValues(12.dp),
                                onClick = { viewModel.dispatch(InstallerViewAction.ShowMiuixPermissionList) },
                            )

                        // Install Options
                        if (installer.config.authorizer != ConfigEntity.Authorizer.Dhizuku ||
                            installer.config.authorizer != ConfigEntity.Authorizer.None
                        )
                            MiuixNavigationItemWidget(
                                title = stringResource(R.string.config_label_install_options),
                                description = stringResource(R.string.config_label_install_options_desc),
                                insideMargin = PaddingValues(12.dp),
                                onClick = { viewModel.dispatch(InstallerViewAction.InstallExtendedMenu) }
                            )

                        // Select Splits
                        val hasSplits = currentPackage.appEntities.size > 1
                        if (hasSplits) {
                            MiuixNavigationItemWidget(
                                title = stringResource(R.string.installer_select_split),
                                description = stringResource(R.string.installer_select_split_desc),
                                insideMargin = PaddingValues(12.dp),
                                onClick = { viewModel.dispatch(InstallerViewAction.InstallChoice) },
                            )
                        }
                    }
                }
            }
        }

        val canInstallBaseEntity = (primaryEntity as? AppEntity.BaseEntity)?.let {
            it.minSdk?.toIntOrNull()?.let { sdk -> sdk <= Build.VERSION.SDK_INT } ?: true
        } ?: false

        val canInstallModuleEntity = (primaryEntity as? AppEntity.ModuleEntity)?.let {
            settings.enableModuleInstall
        } ?: false

        val canInstall = canInstallBaseEntity || canInstallModuleEntity

        val showExpandButton = canInstallBaseEntity && settings.showExtendedMenu

        if (showExpandButton)
            item {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    text = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand)
                )
            }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCancel,
                    text = stringResource(R.string.cancel),
                    modifier = Modifier.weight(1f),
                )
                if (canInstall) {
                    TextButton(
                        onClick = onInstall,
                        text = stringResource(buttonTextId),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private val InfoRowSpacing = 8.dp

@Composable
private fun AdaptiveInfoRow(
    @StringRes labelResId: Int,
    newValue: String,
    oldValue: String?,
    isDowngrade: Boolean = false,
    isArchived: Boolean
) {
    val showComparison = oldValue != null && newValue != oldValue
    val oldTextContent = if (isArchived) stringResource(R.string.old_version_archived) else oldValue.orEmpty()

    SubcomposeLayout { constraints ->
        val label = @Composable {
            Text(
                text = stringResource(labelResId),
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.SemiBold
            )
        }

        val valueContentSingleLine = @Composable {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showComparison) {
                    Text(oldTextContent, style = MiuixTheme.textStyles.body2)
                    Icon(
                        imageVector = AppIcons.ArrowIndicator,
                        contentDescription = "to",
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(16.dp)
                    )
                    Text(newValue, style = MiuixTheme.textStyles.body2)
                } else {
                    Text(newValue, style = MiuixTheme.textStyles.body2)
                }
            }
        }

        val labelPlaceable = subcompose("label", label).first().measure(constraints)
        val valuePlaceable = subcompose("valueContent", valueContentSingleLine).first().measure(constraints)

        val totalWidth = labelPlaceable.width + InfoRowSpacing.roundToPx() + valuePlaceable.width
        val shouldWrap = totalWidth > constraints.maxWidth

        if (shouldWrap && showComparison) {
            val oldValueText = @Composable { Text(oldTextContent, style = MiuixTheme.textStyles.body2) }
            val newValueTextWithArrow = @Composable {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.ArrowIndicator,
                        contentDescription = "to",
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(16.dp)
                    )
                    Text(newValue, style = MiuixTheme.textStyles.body2)
                }
            }

            val oldTextPlaceable = subcompose("oldTextWrap", oldValueText).first().measure(constraints)
            val newTextWithArrowPlaceable = subcompose("newTextWrap", newValueTextWithArrow).first().measure(constraints)

            val firstRowHeight = maxOf(labelPlaceable.height, oldTextPlaceable.height)
            val secondRowHeight = newTextWithArrowPlaceable.height
            val totalHeight = firstRowHeight + InfoRowSpacing.roundToPx() + secondRowHeight

            layout(constraints.maxWidth, totalHeight) {
                labelPlaceable.placeRelative(0, Alignment.CenterVertically.align(labelPlaceable.height, firstRowHeight))
                oldTextPlaceable.placeRelative(
                    constraints.maxWidth - oldTextPlaceable.width,
                    Alignment.CenterVertically.align(oldTextPlaceable.height, firstRowHeight)
                )

                val secondRowYOffset = firstRowHeight + InfoRowSpacing.roundToPx()
                newTextWithArrowPlaceable.placeRelative(
                    constraints.maxWidth - newTextWithArrowPlaceable.width,
                    secondRowYOffset + Alignment.CenterVertically.align(newTextWithArrowPlaceable.height, secondRowHeight)
                )
            }
        } else {
            val height = maxOf(labelPlaceable.height, valuePlaceable.height)
            layout(constraints.maxWidth, height) {
                labelPlaceable.placeRelative(0, Alignment.CenterVertically.align(labelPlaceable.height, height))
                valuePlaceable.placeRelative(
                    constraints.maxWidth - valuePlaceable.width,
                    Alignment.CenterVertically.align(valuePlaceable.height, height)
                )
            }
        }
    }
}

@Composable
private fun WarningTextBlock(warnings: List<Pair<String, Color>>) {
    AnimatedVisibility(visible = warnings.isNotEmpty()) {
        Column(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            warnings.forEach { (text, color) ->
                Text(
                    text = text,
                    color = color,
                    style = MiuixTheme.textStyles.body2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SDKComparison(
    entityToInstall: AppEntity,
    preInstallAppInfo: InstalledAppInfo?,
    installer: InstallerRepo
) {
    AnimatedVisibility(visible = installer.config.displaySdk) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Target SDK
            entityToInstall.targetSdk?.let { newTargetSdk ->
                SdkInfoRow(
                    labelResId = R.string.installer_package_target_sdk_label,
                    newSdk = newTargetSdk,
                    oldSdk = preInstallAppInfo?.targetSdk?.toString(),
                    isArchived = preInstallAppInfo?.isArchived ?: false,
                    type = "target"
                )
            }
            // Min SDK
            entityToInstall.minSdk?.let { newMinSdk ->
                SdkInfoRow(
                    labelResId = R.string.installer_package_min_sdk_label,
                    newSdk = newMinSdk,
                    oldSdk = preInstallAppInfo?.minSdk?.toString(),
                    isArchived = preInstallAppInfo?.isArchived ?: false,
                    type = "min"
                )
            }
        }
    }
}

@Composable
private fun SdkInfoRow(
    @StringRes labelResId: Int,
    newSdk: String,
    oldSdk: String?,
    isArchived: Boolean,
    type: String // "min" or "target"
) {
    val newSdkInt = newSdk.toIntOrNull()
    val oldSdkInt = oldSdk?.toIntOrNull()
    val showComparison = oldSdkInt != null && newSdkInt != null && newSdkInt != oldSdkInt

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label to the left.
        Text(
            text = stringResource(labelResId),
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.SemiBold
        )

        // Label to the right.
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showComparison) {
                // val isDowngrade = newSdkInt < oldSdkInt
                // val isIncompatible = type == "min" && newSdkInt > Build.VERSION.SDK_INT
                // val color = if (isDowngrade || isIncompatible) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

                val oldText = if (isArchived) stringResource(R.string.old_version_archived) else oldSdk

                Text(text = oldText, style = MiuixTheme.textStyles.body2)

                Icon(
                    imageVector = AppIcons.ArrowIndicator,
                    contentDescription = "to",
                    // tint = color,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(16.dp)
                )

                Text(text = newSdk/*, color = color*/, style = MiuixTheme.textStyles.body2)
            } else {
                val isIncompatible = type == "min" && newSdkInt != null && newSdkInt > Build.VERSION.SDK_INT
                val color = if (isIncompatible) MaterialTheme.colorScheme.error else Color.Unspecified

                Text(text = newSdk, color = color, style = MiuixTheme.textStyles.body2)
            }
        }
    }
}