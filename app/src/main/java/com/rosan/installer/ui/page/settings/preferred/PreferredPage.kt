package com.rosan.installer.ui.page.settings.preferred

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.data.app.model.impl.DSRepoImpl
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.util.ConfigUtil
import com.rosan.installer.ui.theme.none
import com.rosan.installer.ui.widget.setting.BaseWidget
import com.rosan.installer.ui.widget.setting.DropDownMenuWidget
import com.rosan.installer.ui.widget.setting.LabelWidget
import com.rosan.installer.util.help
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferredPage(
    navController: NavController,
    windowInsets: WindowInsets,
    viewModel: PreferredViewModel = koinViewModel()
) {
    LaunchedEffect(true) {
        viewModel.dispatch(PreferredViewAction.Init)
    }

    val snackBarHostState = remember {
        SnackbarHostState()
    }
    Scaffold(
        modifier = Modifier
            .windowInsetsPadding(windowInsets)
            .fillMaxSize(),
        contentWindowInsets = WindowInsets.none,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.preferred))
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            item { LabelWidget(stringResource(R.string.global)) }
            item { DataAuthorizerWidget(viewModel) }
            item { DataCustomizeAuthorizerWidget(viewModel) }
            item { DataInstallModeWidget(viewModel) }
            item { LabelWidget(label = stringResource(id = R.string.basic)) }
            item { DefaultInstaller(snackBarHostState, true) }
            item { DefaultInstaller(snackBarHostState, false) }
            item { ClearCache() }
            item { LabelWidget(label = stringResource(id = R.string.more)) }
            item { UserTerms() }
            item { PrivacyPolicy() }
        }
    }
}

fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

@Composable
fun DataAuthorizerWidget(viewModel: PreferredViewModel) {
    val authorizer = viewModel.state.authorizer
    val data = mapOf(
        ConfigEntity.Authorizer.None to stringResource(R.string.config_authorizer_none),
        ConfigEntity.Authorizer.Root to stringResource(R.string.config_authorizer_root),
        ConfigEntity.Authorizer.Shizuku to stringResource(R.string.config_authorizer_shizuku),
        ConfigEntity.Authorizer.Dhizuku to stringResource(R.string.config_authorizer_dhizuku),
        ConfigEntity.Authorizer.Customize to stringResource(R.string.config_authorizer_customize),
    )
    DropDownMenuWidget(
        icon = Icons.TwoTone.Memory,
        title = stringResource(R.string.config_authorizer),
        description = if (data.containsKey(authorizer)) data[authorizer] else null,
        choice = data.keys.toList().indexOf(authorizer),
        data = data.values.toList(),
    ) {
        data.keys.toList().getOrNull(it)?.let {
            viewModel.dispatch(PreferredViewAction.ChangeGlobalAuthorizer(it))
        }
    }
}

@Composable
fun DataCustomizeAuthorizerWidget(viewModel: PreferredViewModel) {
    if (!viewModel.state.authorizerCustomize) return
    val customizeAuthorizer = viewModel.state.customizeAuthorizer
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .focusable(),
        leadingIcon = {
            Icon(imageVector = Icons.TwoTone.Terminal, contentDescription = null)
        },
        label = {
            Text(stringResource(R.string.config_customize_authorizer))
        },
        value = customizeAuthorizer,
        onValueChange = { viewModel.dispatch(PreferredViewAction.ChangeGlobalCustomizeAuthorizer(it)) },
        maxLines = 8,
    )
}

@Composable
fun DataInstallModeWidget(viewModel: PreferredViewModel) {
    val installMode = viewModel.state.installMode
    val data = mapOf(
        ConfigEntity.InstallMode.Dialog to stringResource(R.string.config_install_mode_dialog),
        ConfigEntity.InstallMode.AutoDialog to stringResource(R.string.config_install_mode_auto_dialog),
        ConfigEntity.InstallMode.Notification to stringResource(R.string.config_install_mode_notification),
        ConfigEntity.InstallMode.AutoNotification to stringResource(R.string.config_install_mode_auto_notification),
        ConfigEntity.InstallMode.Ignore to stringResource(R.string.config_install_mode_ignore),
    )
    DropDownMenuWidget(
        icon = Icons.TwoTone.Downloading,
        title = stringResource(R.string.config_install_mode),
        description = if (data.containsKey(installMode)) data[installMode] else null,
        choice = data.keys.toList().indexOf(installMode),
        data = data.values.toList(),
    ) {
        data.keys.toList().getOrNull(it)?.let {
            viewModel.dispatch(PreferredViewAction.ChangeGlobalInstallMode(it))
        }
    }
}

@Composable
fun DefaultInstaller(snackBarHostState: SnackbarHostState, lock: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exception: Throwable by remember {
        mutableStateOf(Throwable())
    }
    var showException by remember {
        mutableStateOf(false)
    }

    fun workIt() {
        synchronized(scope) {
            scope.launch(Dispatchers.IO) {
                val exceptionOrNull = kotlin.runCatching {
                    DSRepoImpl.doWork(ConfigUtil.getByPackageName(null), lock)
                }.exceptionOrNull()
                exceptionOrNull?.printStackTrace()

                snackBarHostState.currentSnackbarData?.dismiss()
                if (exceptionOrNull == null) snackBarHostState.showSnackbar(
                    context.getString(
                        if (lock) R.string.lock_default_installer_success
                        else R.string.unlock_default_installer_success
                    )
                )
                else {
                    val result = snackBarHostState.showSnackbar(
                        context.getString(
                            if (lock) R.string.lock_default_installer_failed
                            else R.string.unlock_default_installer_failed
                        ),
                        context.getString(R.string.details),
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        exception = exceptionOrNull
                        showException = true
                    }
                }
            }
        }
    }

    BaseWidget(
        icon = if (lock) Icons.TwoTone.Favorite else Icons.TwoTone.FavoriteBorder,
        title =
        stringResource(if (lock) R.string.lock_default_installer else R.string.unlock_default_installer),
        description =
        stringResource(if (lock) R.string.lock_default_installer_dsp else R.string.unlock_default_installer_dsp),
        onClick = {
            workIt()
        }
    ) {}
    if (!showException) return
    AlertDialog(onDismissRequest = {
        showException = false
    }, title = {
        Text(stringResource(if (lock) R.string.lock_default_installer_failed else R.string.unlock_default_installer_failed))
    }, text = {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onErrorContainer) {
            LazyColumn(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .fillMaxWidth()
                    .padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(exception.help(), fontWeight = FontWeight.Bold)
                }
                item {
                    SelectionContainer {
                        Text(exception.stackTraceToString().trim())
                    }
                }
            }
        }
    }, confirmButton = {
        TextButton(onClick = {
            showException = false
            workIt()
        }) {
            Text(stringResource(R.string.retry))
        }
    }, dismissButton = {
        TextButton(onClick = {
            showException = false
        }) {
            Text(stringResource(R.string.cancel))
        }
    })
}

@Composable
fun ClearCache() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inProgress by remember {
        mutableStateOf(false)
    }
    BaseWidget(
        icon = Icons.TwoTone.ClearAll,
        title = stringResource(id = R.string.clear_cache),
        onClick = {
            if (inProgress) return@BaseWidget
            inProgress = true
            scope.launch(Dispatchers.IO) {
                val paths = listOfNotNull(
                    context.externalCacheDir?.absolutePath
                )

                fun clearFile(file: File) {
                    if (!file.exists()) return
                    if (file.isDirectory) file.listFiles()?.forEach {
                        clearFile(it)
                    }
                    file.delete()
                }

                paths.forEach {
                    clearFile(File(it))
                }
                inProgress = false
            }
        }
    ) {}
}

@Composable
fun UserTerms() {
    val context = LocalContext.current
    BaseWidget(
        icon = Icons.TwoTone.Gavel,
        title = stringResource(id = R.string.user_terms),
        onClick = {
            openUrl(context, "https://iamr0s.github.io/InstallerXDocs/terms")
        }
    ) {}
}

@Composable
fun PrivacyPolicy() {
    val context = LocalContext.current
    BaseWidget(
        icon = Icons.TwoTone.PrivacyTip,
        title = stringResource(id = R.string.privacy_policy),
        onClick = {
            openUrl(context, "https://iamr0s.github.io/InstallerXDocs/privacy")
        }
    ) {}
}