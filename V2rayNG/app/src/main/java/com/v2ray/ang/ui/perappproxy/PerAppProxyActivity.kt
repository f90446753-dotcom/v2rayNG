package com.v2ray.ang.ui.perappproxy

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppDropdownMenuItems
import com.v2ray.ang.ui.compose.ConfirmDialog
import com.v2ray.ang.ui.compose.ZakiAppRow
import com.v2ray.ang.ui.compose.ZakiCard
import com.v2ray.ang.ui.compose.ZakiIconButton
import com.v2ray.ang.ui.compose.ZakiLine
import com.v2ray.ang.ui.compose.ZakiPurple
import com.v2ray.ang.ui.compose.ZakiPurpleSoft
import com.v2ray.ang.ui.compose.ZakiSurface
import com.v2ray.ang.ui.compose.ZakiSwitchRow
import com.v2ray.ang.ui.compose.ZakiTopBar
import com.v2ray.ang.ui.compose.zakiEnter
import com.v2ray.ang.util.Utils

private enum class PerAppMenuAction(@StringRes val labelRes: Int) {
    SelectAll(R.string.menu_item_select_all),
    InvertSelection(R.string.menu_item_invert_selection),
    SelectProxyApps(R.string.menu_item_select_proxy_app),
    ImportSelection(R.string.menu_item_import_proxy_app),
    ExportSelection(R.string.menu_item_export_proxy_app)
}

class PerAppProxyActivity : BaseComponentActivity() {

    private val viewModel: PerAppProxyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadApps(this)
    }

    @Composable
    override fun ScreenContent() {
        val apps by viewModel.displayedApps.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val blacklist by viewModel.blacklist.collectAsStateWithLifecycle()
        val perAppProxyEnabled by viewModel.perAppProxyEnabled.collectAsStateWithLifecycle()
        val bypassApps by viewModel.bypassApps.collectAsStateWithLifecycle()

        PerAppProxyScreen(
            apps = apps,
            isLoading = isLoading,
            blacklist = blacklist,
            perAppProxyEnabled = perAppProxyEnabled,
            bypassApps = bypassApps,
            onBackClick = { finish() },
            onPerAppProxyChanged = { viewModel.setPerAppProxyEnabled(it) },
            onBypassAppsChanged = { viewModel.setBypassAppsEnabled(it) },
            onToggleApp = { viewModel.toggle(it) },
            onSearch = { viewModel.filterApps(it) },
            onSelectAll = { viewModel.selectAll() },
            onInvertSelection = { viewModel.invertSelection() },
            onSelectProxyAuto = { viewModel.selectProxyAppAuto(this) },
            onImportProxyApp = {
                val content = Utils.getClipboard(applicationContext)
                viewModel.importProxyApp(content, this)
            },
            onExportProxyApp = {
                val export = viewModel.exportProxyApp()
                Utils.setClipboard(applicationContext, export)
                toastSuccess(R.string.toast_success)
            }
        )
    }
}

@Composable
fun PerAppProxyScreen(
    apps: List<AppInfo>,
    isLoading: Boolean,
    blacklist: Set<String>,
    perAppProxyEnabled: Boolean,
    bypassApps: Boolean,
    onBackClick: () -> Unit,
    onPerAppProxyChanged: (Boolean) -> Unit,
    onBypassAppsChanged: (Boolean) -> Unit,
    onToggleApp: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onSelectProxyAuto: () -> Unit,
    onImportProxyApp: () -> Unit,
    onExportProxyApp: () -> Unit
) {
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showInfoDialog by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        onSearch(searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ZakiTopBar(
            title = "Per-App settings",
            onBack = onBackClick,
            actions = {
                ZakiIconButton(
                    iconRes = R.drawable.ic_zaki_search,
                    contentDescription = "Search",
                    onClick = {
                        if (showSearch) {
                            searchQuery = ""
                            onSearch("")
                        }
                        showSearch = !showSearch
                    },
                    tint = if (showSearch) ZakiPurple else MaterialTheme.colorScheme.onSurface
                )
                ZakiIconButton(
                    iconRes = R.drawable.ic_about_24dp,
                    contentDescription = "Information",
                    onClick = { showInfoDialog = true }
                )
                Box {
                    ZakiIconButton(
                        iconRes = R.drawable.ic_zaki_more,
                        contentDescription = "More",
                        onClick = { showMenu = true }
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = ZakiSurface,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        AppDropdownMenuItems(PerAppMenuAction.entries, { it.labelRes }) { action ->
                            showMenu = false
                            when (action) {
                                PerAppMenuAction.SelectAll -> onSelectAll()
                                PerAppMenuAction.InvertSelection -> onInvertSelection()
                                PerAppMenuAction.SelectProxyApps -> onSelectProxyAuto()
                                PerAppMenuAction.ImportSelection -> onImportProxyApp()
                                PerAppMenuAction.ExportSelection -> onExportProxyApp()
                            }
                        }
                    }
                }
            }
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = ZakiPurple,
                trackColor = Color.Transparent
            )
        } else {
            Spacer(Modifier.height(2.dp))
        }

        ZakiCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .zakiEnter(delayMs = 0)
        ) {
            Column {
                ZakiSwitchRow(
                    title = stringResource(R.string.per_app_proxy_settings_enable),
                    checked = perAppProxyEnabled,
                    onCheckedChange = onPerAppProxyChanged
                )
                HorizontalDivider(color = ZakiLine, modifier = Modifier.padding(horizontal = 18.dp))
                ZakiSwitchRow(
                    title = stringResource(R.string.switch_bypass_apps_mode),
                    checked = bypassApps,
                    onCheckedChange = onBypassAppsChanged
                )
            }
        }

        AnimatedVisibility(visible = showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onSearch(it)
                },
                singleLine = true,
                placeholder = { Text("Search apps") },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZakiPurple,
                    unfocusedBorderColor = ZakiLine,
                    focusedContainerColor = ZakiPurpleSoft.copy(alpha = 0.5f),
                    unfocusedContainerColor = ZakiPurpleSoft.copy(alpha = 0.5f),
                    cursorColor = ZakiPurple
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 24.dp)
        ) {
            items(items = apps, key = { it.packageName }) { app ->
                ZakiAppRow(
                    appName = app.appName,
                    packageName = app.packageName,
                    checked = blacklist.contains(app.packageName),
                    onToggle = { onToggleApp(app.packageName) }
                )
            }
            item {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }

    if (showInfoDialog) {
        ConfirmDialog(
            message = stringResource(R.string.summary_pref_per_app_proxy),
            dismissText = null,
            onConfirm = {},
            onDismiss = { showInfoDialog = false },
        )
    }
}
