// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer

import androidx.lifecycle.viewModelScope
import com.rosan.installer.data.session.repository.InstallerSessionRepositoryImpl
import com.rosan.installer.domain.engine.model.install.InstallPhase
import com.rosan.installer.domain.engine.model.install.SessionMode
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.PackageAnalysisResult
import com.rosan.installer.domain.engine.model.packageinfo.PackageIdentityStatus
import com.rosan.installer.domain.engine.model.packageinfo.SignatureMatchStatus
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.engine.usecase.GetAppIconColorUseCase
import com.rosan.installer.domain.engine.usecase.GetAppIconUseCase
import com.rosan.installer.domain.engine.usecase.GetAppLabelUseCase
import com.rosan.installer.domain.privileged.usecase.GetAvailableUsersUseCase
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.domain.settings.model.preferences.AppPreferences
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Exercises public selection actions, including stale UI events and bulk selection.
 * Package selection restores the file choices recorded before clearing the package.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InstallerSelectionTest {
    @Test
    fun `restoring mixed APK success uses the selected app regardless of module order`() {
        for (moduleFirst in listOf(true, false)) {
            fixture { scope ->
                val app = base("fusehide.apk", true, "io.github.example.fusehide")
                val module = SelectInstallEntity(
                    AppEntity.ModuleEntity(
                        id = "zygisk_fusehide",
                        name = "FuseHide module",
                        version = "1",
                        versionCode = 1,
                        author = "",
                        description = "",
                        data = app.app.data,
                        size = 0,
                        sourceType = DataType.MIXED_MODULE_APK,
                    ),
                    false,
                )
                val appGroup = group(app, packageName = app.app.packageName)
                val moduleGroup = group(module, packageName = module.app.packageName)
                session.progress.tryEmit(ProgressEntity.InstallSuccess)
                load(if (moduleFirst) listOf(moduleGroup, appGroup) else listOf(appGroup, moduleGroup))
                scope.runCurrent()

                assertEquals(InstallerStage.InstallSuccess, vm.uiState.value.stage)
                assertEquals(app.app.packageName, vm.uiState.value.currentPackageName)
                val restoredPackage = vm.uiState.value.analysisResults
                    .single { it.packageName == vm.uiState.value.currentPackageName }
                assertSame(app.app, restoredPackage.appEntities.single { it.selected }.app)
            }
        }
    }

    @Test
    fun `restoring single app progress skips every unselected package`() {
        val stages = listOf(
            ProgressEntity.Installing() to InstallerStage.Installing(0f, 1, 1, null, InstallPhase.WRITING),
            ProgressEntity.InstallWaitingUnknownSource to InstallerStage.InstallWaitingUnknownSource,
            ProgressEntity.InstallFailed to InstallerStage.InstallFailed,
            ProgressEntity.InstallSuccess to InstallerStage.InstallSuccess,
        )
        for ((progress, expectedStage) in stages) {
            fixture { scope ->
                val results = (0 until 16).map { index ->
                    val packageName = "example.app$index"
                    group(base("app$index.apk", index == 12, packageName), packageName = packageName)
                }
                session.progress.tryEmit(progress)
                load(results)
                scope.runCurrent()

                assertEquals(expectedStage, vm.uiState.value.stage)
                assertEquals("example.app12", vm.uiState.value.currentPackageName)
                assertSame(results, session.analysisResults)
            }
        }
    }

    @Test
    fun `reattaching after single app installation retains the selected package`() = fixture { scope ->
        val results = listOf(
            group(base("unselected.apk", false)),
            group(base("selected.apk", true, "selected.app"), packageName = "selected.app"),
        )
        session.progress.tryEmit(ProgressEntity.Installing())
        load(results)
        scope.runCurrent()
        assertEquals("selected.app", vm.uiState.value.currentPackageName)

        session.progress.tryEmit(ProgressEntity.InstallSuccess)
        scope.runCurrent()
        assertEquals("selected.app", vm.uiState.value.currentPackageName)

        vm.dispatch(InstallerViewAction.CollectSession(session))
        scope.runCurrent()
        assertEquals(InstallerStage.InstallSuccess, vm.uiState.value.stage)
        assertEquals("selected.app", vm.uiState.value.currentPackageName)
    }

    @Test
    fun `batch progress keeps the current selected package when it succeeds`() = fixture { scope ->
        val results = (0 until 6).map { index ->
            val packageName = "example.app$index"
            group(base("app$index.apk", index % 2 == 1, packageName), packageName = packageName)
        }
        session.progress.tryEmit(ProgressEntity.Installing(current = 2, total = 3))
        load(results)
        scope.runCurrent()
        assertEquals("example.app3", vm.uiState.value.currentPackageName)

        session.progress.tryEmit(ProgressEntity.InstallSuccess)
        scope.runCurrent()
        assertEquals("example.app3", vm.uiState.value.currentPackageName)
    }

    @Test
    fun `restoring empty success does not invent a package`() = fixture { scope ->
        session.progress.tryEmit(ProgressEntity.InstallSuccess)
        load(emptyList())
        scope.runCurrent()
        assertEquals(InstallerStage.InstallSuccess, vm.uiState.value.stage)
        assertEquals(null, vm.uiState.value.currentPackageName)
    }

    @Test
    fun `multi selection toggles only the target for every initial selection combination`() = fixture {
        for (targetSelected in listOf(false, true)) {
            for (siblingSelected in listOf(false, true)) {
                val target = base("one", targetSelected)
                val sibling = base("two", siblingSelected)
                load(listOf(group(target, sibling)))
                toggle(target, multi = true)
                assertEquals(listOf(!targetSelected, siblingSelected), flags())
                assertSame(sibling, session.analysisResults.single().appEntities[1])
            }
        }
    }

    @Test
    fun `single selection selects an alternative or clears the selected alternative`() = fixture {
        for (targetSelected in listOf(false, true)) {
            for (siblingSelected in listOf(false, true)) {
                val target = base("one", targetSelected)
                load(listOf(group(target, base("two", siblingSelected))))
                toggle(target, multi = false)
                assertEquals(listOf(!targetSelected, false), flags())
            }
        }
    }

    @Test
    fun `empty results empty group and unknown package do not change selection`() = fixture {
        val target = base("one", false)
        for (results in listOf(emptyList(), listOf(group()), listOf(group(target)))) {
            load(results)
            vm.dispatch(InstallerViewAction.ToggleSelection("missing", target, false))
            assertEquals(results, session.analysisResults)
        }
        load(listOf(group()))
        toggle(target, multi = false)
        assertEquals(emptyList(), flags())
    }

    @Test
    fun `selection does not change another package or the input snapshot`() = fixture {
        val target = base("one", false)
        val other = group(base("other", true, "other.package"), packageName = "other.package")
        val original = listOf(group(target), other)
        load(original)
        toggle(target, multi = false)
        assertEquals(false, original.first().appEntities.single().selected)
        assertSame(other, session.analysisResults[1])
        assertEquals(true, session.analysisResults.first().appEntities.single().selected)
    }

    @Test
    fun `distinct files with the same package and version remain separately selectable`() = fixture {
        val first = base("one", false)
        val second = base("two", false)
        load(listOf(group(first, second)))
        toggle(second, multi = true)
        assertEquals(listOf(false, true), flags())
    }

    @Test
    fun `sequential multi selection accepts untouched entities from the original snapshot`() = fixture {
        val entries = (1..20).map { base("file$it", false) }
        load(listOf(group(*entries.toTypedArray())))
        entries.forEach { toggle(it, multi = true) }
        assertEquals(List(20) { true }, flags())
    }

    @Test
    fun `base toggle preserves independently selected split and dex metadata`() = fixture {
        val target = base("base", false)
        val split = SelectInstallEntity(
            AppEntity.SplitEntity(
                packageName = "example",
                data = DataEntity.FileEntity("config.apk"),
                splitName = "config.en",
                targetSdk = null,
                minSdk = null,
                arch = null,
                size = 0,
            ),
            true,
        )
        val dex = SelectInstallEntity(
            AppEntity.DexMetadataEntity(
                packageName = "example",
                data = DataEntity.FileEntity("base.dm"),
                dmName = "base.dm",
                targetSdk = null,
                minSdk = null,
                size = 0,
            ),
            false,
        )
        load(listOf(group(target, split, dex)))
        toggle(target, multi = true)
        assertEquals(listOf(true, true, false), flags())
    }

    @Test
    fun `stale single selection does not undo the current selection`() = fixture {
        val stale = base("one", false)
        load(listOf(group(stale)))
        toggle(stale, multi = true)
        toggle(stale, multi = false)
        assertEquals(listOf(true), flags())
    }

    @Test
    fun `copied selection wrapper still identifies the current app`() = fixture {
        val target = base("one", false)
        load(listOf(group(target)))
        toggle(target.copy(), multi = true)
        assertEquals(listOf(true), flags())
    }

    @Test
    fun `signature issues are recalculated only when signature checking was performed`() = fixture {
        for (checked in listOf(false, true)) {
            val target = base("unsigned", false)
            load(listOf(group(target).copy(signatureCheckPerformed = checked)))
            toggle(target, multi = true)
            assertEquals(checked, session.analysisResults.single().signatureAnalysis.hasIssues)
            val selected = session.analysisResults.single().appEntities.single()
            toggle(selected, multi = true)
            assertEquals(false, session.analysisResults.single().signatureAnalysis.hasIssues)
        }
    }

    @Test
    fun `mixed type choice selects only the requested type across packages`() = fixture {
        for (asModule in listOf(false, true)) {
            val app = base("base", true)
            val module = SelectInstallEntity(
                AppEntity.ModuleEntity(
                    id = "module",
                    name = "Module",
                    version = "1",
                    versionCode = 1,
                    author = "",
                    description = "",
                    data = DataEntity.FileEntity("module.zip"),
                    size = 0,
                ),
                true,
            )
            load(listOf(group(app), group(module, packageName = "module")))
            vm.dispatch(InstallerViewAction.SelectMixedModuleType(asModule))
            assertEquals(listOf(!asModule, asModule), session.analysisResults.flatMap { it.appEntities }.map { it.selected })
        }
    }

    @Test
    fun `missing requested mixed type leaves selection unchanged`() = fixture {
        val results = listOf(group(base("one", true)))
        load(results)
        vm.dispatch(InstallerViewAction.SelectMixedModuleType(true))
        assertEquals(results, session.analysisResults)
    }

    @Test
    fun `selected module can be toggled without modifying an app package`() = fixture {
        val module = SelectInstallEntity(
            AppEntity.ModuleEntity(
                id = "module",
                name = "Module",
                version = "1",
                versionCode = 1,
                author = "",
                description = "",
                data = DataEntity.FileEntity("module.zip"),
                size = 0,
            ),
            true,
        )
        val appGroup = group(base("app", true))
        load(listOf(appGroup, group(module, packageName = "module")))
        toggle(module, multi = true)
        assertSame(appGroup, session.analysisResults.first())
        assertEquals(false, session.analysisResults.last().appEntities.single().selected)
    }

    @Test
    fun `foreign target cannot clear a selected alternative in the same package`() = fixture {
        val selected = base("same-name", true)
        load(listOf(group(selected)))
        val snapshot = session.analysisResults
        toggle(base("same-name", false), multi = false)
        assertSame(snapshot, session.analysisResults)
        assertEquals(listOf(true), flags())
    }

    @Test
    fun `bulk selection is idempotent and preserves module selections across packages`() = fixture {
        for (moduleSelected in listOf(false, true)) {
            val module = SelectInstallEntity(
                AppEntity.ModuleEntity(
                    id = "module",
                    name = "Module",
                    version = "1",
                    versionCode = 1,
                    author = "",
                    description = "",
                    data = DataEntity.FileEntity("module.zip"),
                    size = 0,
                ),
                moduleSelected,
            )
            val moduleGroup = group(module, packageName = "module")
            load(listOf(group(base("one", false), base("two", true)), moduleGroup))
            for (selected in listOf(true, false)) {
                vm.dispatch(InstallerViewAction.SetApkSelection(selected))
                assertEquals(listOf(selected, selected), session.analysisResults.first().appEntities.map { it.selected })
                assertSame(moduleGroup, session.analysisResults.last())
                val snapshot = session.analysisResults
                vm.dispatch(InstallerViewAction.SetApkSelection(selected))
                assertSame(snapshot, session.analysisResults)
            }
        }
    }

    @Test
    fun `bulk selection includes split and dex and refreshes signature issues`() = fixture {
        val split = SelectInstallEntity(
            AppEntity.SplitEntity(
                packageName = "example",
                data = DataEntity.FileEntity("config.apk"),
                splitName = "config.en",
                targetSdk = null,
                minSdk = null,
                arch = null,
                size = 0,
            ),
            false,
        )
        val dex = SelectInstallEntity(
            AppEntity.DexMetadataEntity(
                packageName = "example",
                data = DataEntity.FileEntity("base.dm"),
                dmName = "base.dm",
                targetSdk = null,
                minSdk = null,
                size = 0,
            ),
            false,
        )
        load(listOf(group(base("one", false), split, dex).copy(signatureCheckPerformed = true)))
        vm.dispatch(InstallerViewAction.SetApkSelection(true))
        assertEquals(listOf(true, true, true), flags())
        assertEquals(true, session.analysisResults.single().signatureAnalysis.hasIssues)
        vm.dispatch(InstallerViewAction.SetApkSelection(false))
        assertEquals(listOf(false, false, false), flags())
        assertEquals(false, session.analysisResults.single().signatureAnalysis.hasIssues)
    }

    @Test
    fun `bulk selection with no entries does not publish a replacement`() = fixture {
        for (results in listOf(emptyList(), listOf(group()))) {
            load(results)
            for (selected in listOf(true, false)) {
                vm.dispatch(InstallerViewAction.SetApkSelection(selected))
                assertSame(results, session.analysisResults)
            }
        }
    }

    @Test
    fun `package clear and restore retains each split and dex choice`() = fixture {
        val entries = listOf(
            base("base", true),
            split("config.en", true),
            split("config.fr", false),
            SelectInstallEntity(
                AppEntity.DexMetadataEntity(
                    packageName = "example",
                    data = DataEntity.FileEntity("base.dm"),
                    dmName = "base.dm",
                    targetSdk = null,
                    minSdk = null,
                    size = 0,
                ),
                true,
            ),
        )
        load(listOf(group(*entries.toTypedArray()).copy(signatureCheckPerformed = true)))
        repeat(2) {
            vm.dispatch(InstallerViewAction.TogglePackageSelection("example", session.analysisResults.single().appEntities.first()))
            assertEquals(listOf(false, false, false, false), flags())
            assertEquals(false, session.analysisResults.single().signatureAnalysis.hasIssues)
            vm.dispatch(InstallerViewAction.TogglePackageSelection("example", session.analysisResults.single().appEntities.first()))
            assertEquals(listOf(true, true, false, true), flags())
            assertEquals(true, session.analysisResults.single().signatureAnalysis.hasIssues)
        }
    }

    @Test
    fun `initial package selection retains parser split choices`() = fixture {
        load(listOf(group(base("base", false), split("en", true), split("fr", false))))
        vm.dispatch(InstallerViewAction.TogglePackageSelection("example", session.analysisResults.single().appEntities.first()))
        assertEquals(listOf(true, true, false), flags())
    }

    @Test
    fun `package restore cannot reuse a different analysis with the same names`() = fixture {
        val original = base("base", true)
        load(listOf(group(original, split("en", true))))
        vm.dispatch(InstallerViewAction.TogglePackageSelection("example", original))
        val replacement = base("base", false)
        load(listOf(group(replacement, split("en", false))))
        vm.dispatch(InstallerViewAction.TogglePackageSelection("example", original))
        assertEquals(listOf(false, false), flags())
        vm.dispatch(InstallerViewAction.TogglePackageSelection("example", replacement))
        assertEquals(listOf(true, false), flags())
    }

    @Test
    fun `individual edits after a package clear supersede the remembered selection`() = fixture {
        val original = base("base", true)
        load(listOf(group(original, split("en", true), split("fr", false))))
        vm.dispatch(InstallerViewAction.TogglePackageSelection("example", original))
        toggle(session.analysisResults.single().appEntities[2], multi = true)
        vm.dispatch(InstallerViewAction.TogglePackageSelection("example", session.analysisResults.single().appEntities.first()))
        assertEquals(listOf(true, false, true), flags())
    }

    @Test
    fun `package actions reject version groups and stale checked states`() = fixture {
        val original = base("base", true)
        val versions = listOf(group(original, base("alternative", false)))
        load(versions)
        vm.dispatch(InstallerViewAction.TogglePackageSelection("example", original))
        assertSame(versions, session.analysisResults)
        load(listOf(group(original, split("en", true))))
        vm.dispatch(InstallerViewAction.TogglePackageSelection("example", original))
        val cleared = session.analysisResults
        vm.dispatch(InstallerViewAction.TogglePackageSelection("example", original))
        assertSame(cleared, session.analysisResults)
    }

    private fun split(name: String, selected: Boolean) = SelectInstallEntity(
        AppEntity.SplitEntity(
            packageName = "example",
            data = DataEntity.FileEntity(name),
            splitName = name,
            targetSdk = null,
            minSdk = null,
            arch = null,
            size = 0,
        ),
        selected,
    )

    private fun fixture(block: Harness.(TestScope) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val harness = Harness()
        try {
            block(harness, this)
            runCurrent()
            assertEquals(harness.session.analysisResults, harness.vm.uiState.value.analysisResults)
        } finally {
            harness.vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private class Harness {
        val session = InstallerSessionRepositoryImpl("selection-test") {}
        val vm = InstallerViewModel(
            session = session,
            appSettingsRepo = stub { name, _ ->
                when (name) {
                    "getPreferencesFlow" -> flowOf(testPreferences())
                    "getBoolean" -> flowOf(false)
                    else -> error("Unexpected settings call: $name")
                }
            },
            getAvailableUsers = GetAvailableUsersUseCase(stub()),
            getAppIcon = GetAppIconUseCase(stub()),
            getAppIconColor = GetAppIconColorUseCase(stub()),
            getAppLabel = GetAppLabelUseCase(
                stub { name, _ ->
                    check(name == "getAppLabel")
                    null
                },
            ),
            deviceCapabilityProvider = stub { name, _ ->
                check(name == "isSystemApp")
                true
            },
            installedPackageSignatureProvider = stub(),
        )

        fun load(results: List<PackageAnalysisResult>) {
            session.analysisResults = results
            vm.dispatch(InstallerViewAction.CollectSession(session))
        }

        fun toggle(target: SelectInstallEntity, multi: Boolean) {
            vm.dispatch(InstallerViewAction.ToggleSelection(target.app.packageName, target, multi))
        }

        fun flags() = session.analysisResults.single().appEntities.map { it.selected }
    }

    private fun base(name: String, selected: Boolean, packageName: String = "example") = SelectInstallEntity(
        AppEntity.BaseEntity(
            packageName = packageName, sharedUserId = null, data = DataEntity.FileEntity(name),
            versionCode = 1, versionName = "1", label = null, icon = null, name = name,
            targetSdk = null, minSdk = null, size = 0,
        ),
        selected,
    )

    private fun group(vararg entities: SelectInstallEntity, packageName: String = "example") = PackageAnalysisResult(
        packageName = packageName,
        sessionMode = SessionMode.Batch,
        appEntities = entities.toList(),
        installedAppInfo = null,
        signatureCheckPerformed = false,
        signatureMatchStatus = SignatureMatchStatus.NOT_INSTALLED,
        identityStatus = PackageIdentityStatus.NOT_APPLICABLE,
    )

    companion object {
        private fun testPreferences() = AppPreferences(
            authorizer = com.rosan.installer.domain.settings.model.config.Authorizer.None,
            alwaysUseRootInSystem = false,
            customizeAuthorizer = "",
            hideIdenticalInstallComparisons = false,
            showDialogInstallExtendedMenu = false,
            expandDialogTemporarySettingsByDefault = false,
            showSmartSuggestion = false,
            disableNotificationForDialogInstall = false,
            showDialogWhenPressingNotification = false,
            closeSessionCountDown = 0,
            notificationSuccessAutoClearSeconds = 0,
            versionCompareInSingleLine = false,
            sdkCompareInMultiLine = false,
            showOPPOSpecial = false,
            checkAppSignature = false,
            checkSplitPackageSignatures = false,
            showSignatureInfoOnMatch = false,
            showSignatureDetails = false,
            installerRequireBiometricAuth = com.rosan.installer.domain.settings.model.config.BiometricAuthMode.entries.first(),
            uninstallerRequireBiometricAuth = false,
            showLiveActivity = false,
            useMiIsland = false,
            useMiIslandBypassRestriction = false,
            useMiIslandOuterGlow = false,
            useMiIslandBlockingIntervalMs = 0,
            autoSilentInstall = false,
            longClickBackgroundInstall = false,
            tryMultipleAuthorizersOnInstall = false,
            smartAuthorizerCandidates = emptyList(),
            showMiuixUI = false,
            preferSystemIcon = false,
            showLauncherIcon = false,
            userSetLSPosedActive = false,
            detectXposedModule = false,
            quickOpenLSPosed = false,
            managedInstallerPackages = emptyList(),
            managedBlacklistPackages = emptyList(),
            managedSharedUserIdBlacklist = emptyList(),
            managedSharedUserIdExemptedPackages = emptyList(),
            uninstallFlags = 0,
            networkSourceMode = com.rosan.installer.domain.settings.model.config.NetworkSourceMode.entries.first(),
            networkSourceModeWarningAcknowledged = false,
            allowInternetAccess = false,
            githubUpdateChannel = com.rosan.installer.domain.settings.model.preferences.GithubUpdateChannel.entries.first(),
            customGithubProxyUrl = "",
            labRootEnableModuleFlash = false,
            labRootShowModuleArt = false,
            labRootMode = com.rosan.installer.domain.settings.model.preferences.RootMode.entries.first(),
            labHttpProfile = com.rosan.installer.domain.settings.model.preferences.HttpProfile.entries.first(),
            labHttpSaveFile = false,
            labSetInstallRequester = false,
            labTapIconToShare = false,
            labShowFilePath = false,
            labShowInstallInitiator = false,
            labInstallWithoutUserAction = false,
            labRespectPlatformInstallPolicy = false,
            enableFileLogging = false,
            themeMode = com.rosan.installer.domain.settings.model.preferences.theme.ThemeMode.entries.first(),
            paletteStyle = com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle.entries.first(),
            colorSpec = com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec.entries.first(),
            useDynamicColor = false,
            useMiuixMonet = false,
            useAppleFloatingBar = false,
            seedColorInt = 0,
            useDynColorFollowPkgIcon = false,
            useDynColorFollowPkgIconForLiveActivity = false,
            useBlur = false,
            predictiveBackAnimation = com.rosan.installer.domain.settings.model.preferences.PredictiveBackAnimation.entries.first(),
            predictiveBackExitDirection = com.rosan.installer.domain.settings.model.preferences.PredictiveBackExitDirection.entries.first(),
        )

        // No relaxed defaults: unrelated dependency access must fail rather than mask a regression.
        private inline fun <reified T> stub(
            crossinline answer: (String, Array<out Any?>?) -> Any? = { name, _ -> error("Unexpected dependency call: $name") },
        ): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
            answer(method.name, args)
        } as T
    }
}
