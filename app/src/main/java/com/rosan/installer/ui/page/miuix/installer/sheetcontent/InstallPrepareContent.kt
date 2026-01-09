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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Manufacturer
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.InstalledAppInfo
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.app.model.enums.SignatureMatchStatus
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixInstallerTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixNavigationItemWidget
import com.rosan.installer.ui.theme.LocalIsDark
import com.rosan.installer.ui.theme.miuixSheetCardColorDark
import com.rosan.installer.ui.util.formatSize
import com.rosan.installer.ui.util.isGestureNavigation
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
    installer: InstallerRepo,
    viewModel: InstallerViewModel,
    appInfo: AppInfoState,
    onCancel: () -> Unit,
    onInstall: () -> Unit
) {
    val isDarkMode = LocalIsDark.current
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val currentPackage = installer.analysisResults.find { it.packageName == currentPackageName }
    val settings = viewModel.viewSettings

    var isExpanded by remember { mutableStateOf(false) }

    if (currentPackage == null) {
        LoadingContent(statusText = stringResource(id = R.string.loading))
        return
    }

    val allEntities = currentPackage.appEntities
        .filter { it.selected } // Always include selected entities
        .map { it.app }

    val selectedEntities = currentPackage.appEntities
        .filter { it.selected }
        .map { it.app }
    val rawBaseEntity = currentPackage.appEntities
        .map { it.app }
        .filterIsInstance<AppEntity.BaseEntity>()
        .firstOrNull()
    val effectivePrimaryEntity = selectedEntities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
        ?: selectedEntities.filterIsInstance<AppEntity.ModuleEntity>().firstOrNull()
        ?: appInfo.primaryEntity
    if (effectivePrimaryEntity == null) {
        LoadingContent(statusText = "No main app entity found")
        return
    }

    val entityToInstall = allEntities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
    val totalSelectedSize = allEntities.sumOf { it.size }

    val isModuleSelected = selectedEntities.any { it is AppEntity.ModuleEntity }
    val isPureSplit = effectivePrimaryEntity is AppEntity.SplitEntity
    val isBundleSplitUpdate = effectivePrimaryEntity is AppEntity.BaseEntity &&
            entityToInstall == null &&
            selectedEntities.isNotEmpty() &&
            !isModuleSelected
    val isSplitUpdateMode = (isBundleSplitUpdate || isPureSplit) && currentPackage.installedAppInfo != null

    val errorColor = MaterialTheme.colorScheme.error
    val tertiaryColor = MiuixTheme.colorScheme.primary

    val downgradeWarning = stringResource(R.string.installer_prepare_type_downgrade)
    val sigMismatchWarning = stringResource(R.string.installer_prepare_signature_mismatch)
    val sigUnknownWarning = stringResource(R.string.installer_prepare_signature_unknown)
    val sdkIncompatibleWarning = stringResource(R.string.installer_prepare_sdk_incompatible)
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
                        warnings.add(downgradeWarning to errorColor)
                        finalButtonTextId = R.string.install_anyway
                    }
                    // If it's a "Ghost" app (Uninstalled but data kept), treat it as a Reinstall
                    oldInfo.isUninstalled -> finalButtonTextId = R.string.reinstall
                    // If it's Archived, treat it as Unarchive
                    oldInfo.isArchived -> finalButtonTextId = R.string.unarchive
                    else -> finalButtonTextId = R.string.reinstall
                }
            }
        }
        if (!isSplitUpdateMode && (effectivePrimaryEntity.sourceType == DataType.APK || effectivePrimaryEntity.sourceType == DataType.APKS))
            when (signatureStatus) {
                SignatureMatchStatus.MISMATCH -> {
                    warnings.add(0, sigMismatchWarning to errorColor)
                    finalButtonTextId = R.string.install_anyway
                }

                SignatureMatchStatus.UNKNOWN_ERROR -> {
                    warnings.add(0, sigUnknownWarning to tertiaryColor)
                }

                else -> {}
            }
        val newMinSdk = entityToInstall?.minSdk?.toIntOrNull()
        if (newMinSdk != null && newMinSdk > Build.VERSION.SDK_INT) {
            warnings.add(0, sdkIncompatibleWarning to errorColor)
        }
        Pair(warnings, finalButtonTextId)
    }

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { AppInfoSlot(appInfo = appInfo) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { MiuixWarningTextBlock(warnings = warningMessages) }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardColors(
                    color = if (isDynamicColor) MiuixTheme.colorScheme.surfaceContainer else
                        if (isDarkMode) miuixSheetCardColorDark else Color.White,
                    contentColor = MiuixTheme.colorScheme.onSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (effectivePrimaryEntity) {
                        is AppEntity.BaseEntity -> {
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_name_label,
                                newValue = effectivePrimaryEntity.versionName,
                                oldValue = currentPackage.installedAppInfo?.versionName,
                                isUninstalled = currentPackage.installedAppInfo?.isUninstalled ?: false,
                                isArchived = currentPackage.installedAppInfo?.isArchived ?: false
                            )
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_code_label,
                                newValue = effectivePrimaryEntity.versionCode.toString(),
                                oldValue = currentPackage.installedAppInfo?.versionCode?.toString(),
                                isUninstalled = currentPackage.installedAppInfo?.isUninstalled ?: false,
                                isArchived = currentPackage.installedAppInfo?.isArchived ?: false
                            )
                            SDKComparison(
                                entityToInstall = effectivePrimaryEntity,
                                preInstallAppInfo = currentPackage.installedAppInfo,
                                installer = installer
                            )

                            AnimatedVisibility(visible = installer.config.displaySize && effectivePrimaryEntity.size > 0) {
                                val oldSize = currentPackage.installedAppInfo?.packageSize ?: 0L
                                val oldSizeStr = if (oldSize > 0 && !isSplitUpdateMode) oldSize.formatSize() else null
                                val newSizeStr = totalSelectedSize.formatSize()

                                AdaptiveInfoRow(
                                    labelResId = R.string.installer_package_size_label,
                                    newValue = newSizeStr,
                                    oldValue = oldSizeStr
                                )
                            }

                            if (RsConfig.currentManufacturer == Manufacturer.OPPO || RsConfig.currentManufacturer == Manufacturer.ONEPLUS) {
                                AnimatedVisibility(visible = settings.showOPPOSpecial && effectivePrimaryEntity.sourceType == DataType.APK) {
                                    effectivePrimaryEntity.minOsdkVersion?.let {
                                        AdaptiveInfoRow(
                                            labelResId = R.string.installer_package_minOsdkVersion_label,
                                            newValue = it,
                                            oldValue = null
                                        )
                                    }
                                }
                            }
                        }

                        is AppEntity.ModuleEntity -> {
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_name_label,
                                newValue = effectivePrimaryEntity.version,
                                oldValue = null
                            )
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_code_label,
                                newValue = effectivePrimaryEntity.versionCode.toString(),
                                oldValue = null
                            )
                            AnimatedVisibility(visible = installer.config.displaySdk) {
                                AdaptiveInfoRow(
                                    labelResId = R.string.installer_module_author_label,
                                    newValue = effectivePrimaryEntity.author,
                                    oldValue = null
                                )
                            }
                            AnimatedVisibility(visible = installer.config.displaySize) {
                                val newSizeStr = totalSelectedSize.formatSize()
                                AdaptiveInfoRow(
                                    labelResId = R.string.installer_package_size_label,
                                    newValue = newSizeStr,
                                    oldValue = null
                                )
                            }
                        }

                        is AppEntity.SplitEntity -> {
                            // Split Name
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_split_name_label,
                                newValue = effectivePrimaryEntity.splitName,
                                oldValue = null
                            )

                            // SDK Comparison (If splits define min/target SDK)
                            SDKComparison(
                                entityToInstall = effectivePrimaryEntity,
                                preInstallAppInfo = currentPackage.installedAppInfo,
                                installer = installer
                            )

                            // Size
                            AnimatedVisibility(visible = installer.config.displaySize) {
                                val newSizeStr = totalSelectedSize.formatSize()
                                AdaptiveInfoRow(
                                    labelResId = R.string.installer_package_size_label,
                                    newValue = newSizeStr,
                                    oldValue = null // Don't compare with full app size, meaningless
                                )
                            }
                        }

                        else -> {}
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = (rawBaseEntity != null) && isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardColors(
                        color = if (isDynamicColor) MiuixTheme.colorScheme.surfaceContainer else
                            if (isDarkMode) miuixSheetCardColorDark else Color.White,
                        contentColor = MiuixTheme.colorScheme.onSurface
                    )
                ) {
                    Column {
                        // Permissions List
                        if (rawBaseEntity?.permissions?.isNotEmpty() == true)
                            MiuixNavigationItemWidget(
                                title = stringResource(R.string.permission_list),
                                description = stringResource(R.string.permission_list_desc),
                                insideMargin = PaddingValues(12.dp),
                                onClick = { viewModel.dispatch(InstallerViewAction.ShowMiuixPermissionList) },
                            )

                        // Install Options
                        if (installer.config.authorizer != ConfigEntity.Authorizer.Dhizuku &&
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

        val isInvalidSplitInstall = currentPackage.installedAppInfo == null &&
                entityToInstall == null &&
                selectedEntities.any { it is AppEntity.SplitEntity }

        item {
            AnimatedVisibility(
                visible = isInvalidSplitInstall,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                MiuixInstallerTipCard(text = stringResource(R.string.installer_splits_invalid_tip))
            }
        }

        item {
            AnimatedVisibility(
                visible = isSplitUpdateMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                MiuixInstallerTipCard(text = stringResource(R.string.installer_splits_only_tip))
            }
        }

        item {
            AnimatedVisibility(
                visible = (effectivePrimaryEntity is AppEntity.ModuleEntity) &&
                        effectivePrimaryEntity.description.isNotBlank() &&
                        installer.config.displaySdk,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                MiuixInstallerTipCard((effectivePrimaryEntity as AppEntity.ModuleEntity).description)
            }
        }

        val canInstallBaseEntity = (effectivePrimaryEntity as? AppEntity.BaseEntity)?.let { base ->
            if (entityToInstall != null) {
                // Installing Base: Check SDK
                base.minSdk?.toIntOrNull()?.let { sdk -> sdk <= Build.VERSION.SDK_INT } ?: true
            } else {
                // Bundle Split Update: Allowed if installed
                isSplitUpdateMode
            }
        } ?: false

        val canInstallModuleEntity = (effectivePrimaryEntity as? AppEntity.ModuleEntity)?.let {
            settings.enableModuleInstall
        } ?: false

        val canInstallSplitEntity = (effectivePrimaryEntity as? AppEntity.SplitEntity)?.let {
            currentPackage.installedAppInfo != null
        } ?: false

        val canInstall = canInstallBaseEntity || canInstallModuleEntity || canInstallSplitEntity

        // Even if we can't install (e.g. because Base is deselected), we might want to expand the menu to fix the selection.
        // We show the button if rawBaseEntity exists (Bundle/APK) and settings allow it.
        val showExpandButton = rawBaseEntity != null && settings.showExtendedMenu

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
                    .navigationBarsPadding()
                    .padding(top = 24.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCancel,
                    text = stringResource(R.string.cancel),
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onInstall,
                    enabled = canInstall,
                    text = stringResource(buttonTextId),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.weight(1f)
                )
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
    isUninstalled: Boolean = false,
    isArchived: Boolean = false
) {
    val showComparison = oldValue != null && newValue != oldValue
    val oldTextContent = when {
        isArchived -> stringResource(R.string.old_version_archived)
        isUninstalled -> stringResource(R.string.old_version_uninstalled)
        else -> oldValue.orEmpty()
    }

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
private fun MiuixWarningTextBlock(warnings: List<Pair<String, Color>>) {
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
                    isUninstalled = preInstallAppInfo?.isUninstalled ?: false,
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
    isUninstalled: Boolean = false,
    isArchived: Boolean = false,
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

                val oldText = when {
                    isUninstalled -> stringResource(R.string.old_version_uninstalled)
                    isArchived -> stringResource(R.string.old_version_archived)
                    else -> oldSdk
                }

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