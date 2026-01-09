package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HUI
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    viewModel: AppsViewModel = koinViewModel()
) {
    val displayedApps by viewModel.displayedApps.collectAsState(initial = emptyList())
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    var appToUninstall by remember { mutableStateOf<ApplicationInfo?>(null) }
    var appToExport by remember { mutableStateOf<ApplicationInfo?>(null) }

    val context = LocalContext.current

    val exportApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive"),
        onResult = { uri -> appToExport?.let { viewModel.onApkExportUriReceived(uri, it) } }
    )

    LaunchedEffect(Unit) {
        viewModel.events.collect {
            when (it) {
                is AppsViewEvent.ShowToast -> HUI.showToast(it.message, it.args?.toString() ?: "")
                is AppsViewEvent.ExportApk -> {
                    appToExport = displayedApps.find { app -> app.packageName == it.fileName.removeSuffix(".apk") }
                    exportApkLauncher.launch(it.fileName)
                }
                is AppsViewEvent.ShowUninstallDialog -> appToUninstall = it.appInfo
            }
        }
    }

    appToUninstall?.let {
        AlertDialog(
            onDismissRequest = { appToUninstall = null },
            title = { Text(it.loadLabel(context.packageManager).toString()) },
            text = { Text(stringResource(R.string.msg_uninstall)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmUninstall(it); appToUninstall = null }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { appToUninstall = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search") }
                        )
                    } else {
                        Text(stringResource(R.string.title_apps))
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = { isSearchActive = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        AppsMenu(viewModel)
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier.padding(paddingValues),
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.loadApps() }
        ) {
            if (isRefreshing && displayedApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 240.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedApps, key = { it.packageName }) { app ->
                        AppListItem(appInfo = app, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppListItem(
    appInfo: ApplicationInfo,
    viewModel: AppsViewModel
) {
    val pm = LocalContext.current.packageManager
    val isChecked = HailData.isChecked(appInfo.packageName)
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .combinedClickable(onClick = {}, onLongClick = { menuExpanded = true }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = appInfo.loadLabel(pm).toString())
                Text(text = appInfo.packageName, style = MaterialTheme.typography.bodySmall)
            }
            Checkbox(
                checked = isChecked,
                onCheckedChange = { 
                    if (it) HailData.addCheckedApp(appInfo.packageName) else HailData.removeCheckedApp(appInfo.packageName)
                    viewModel.loadApps()
                }
            )
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.action_details)) }, onClick = {
                HUI.startActivity(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, HPackages.packageUri(appInfo.packageName))
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_export_clipboard)) }, onClick = {
                HUI.copyText(appInfo.packageName)
                HUI.showToast(R.string.msg_text_copied, appInfo.packageName)
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_extract_apk)) }, onClick = {
                viewModel.exportApk(appInfo)
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_uninstall)) }, onClick = {
                viewModel.uninstallApp(appInfo)
                menuExpanded = false
            })
        }
    }
}

@Composable
private fun AppsMenu(viewModel: AppsViewModel) {
    var menuExpanded by remember { mutableStateOf(false) }
    val sortOption by viewModel.sortOption.collectAsState()
    val filterOptions by viewModel.filterOptions.collectAsState()

    Box {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            // Sort options
            DropdownMenuItem(text = { Text(stringResource(R.string.sort_by_name)) }, onClick = { viewModel.onSortChanged(HailData.SORT_NAME); menuExpanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.sort_by_install_time)) }, onClick = { viewModel.onSortChanged(HailData.SORT_INSTALL); menuExpanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.sort_by_update_time)) }, onClick = { viewModel.onSortChanged(HailData.SORT_UPDATE); menuExpanded = false })
            
            // Filter options
            DropdownMenuItem(text = { Text(stringResource(R.string.filter_user_apps)) }, onClick = { viewModel.onFilterChanged(HailData.FILTER_USER_APPS, !filterOptions.getOrDefault(HailData.FILTER_USER_APPS, false)); menuExpanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.filter_system_apps)) }, onClick = { viewModel.onFilterChanged(HailData.FILTER_SYSTEM_APPS, !filterOptions.getOrDefault(HailData.FILTER_SYSTEM_APPS, false)); menuExpanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.filter_frozen_apps)) }, onClick = { viewModel.onFilterChanged(HailData.FILTER_FROZEN_APPS, !filterOptions.getOrDefault(HailData.FILTER_FROZEN_APPS, false)); menuExpanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.filter_unfrozen_apps)) }, onClick = { viewModel.onFilterChanged(HailData.FILTER_UNFROZEN_APPS, !filterOptions.getOrDefault(HailData.FILTER_UNFROZEN_APPS, false)); menuExpanded = false })
        }
    }
}
