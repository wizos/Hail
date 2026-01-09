package com.aistra.hail.ui.home

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.ui.main.AppDestinations
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val tags by viewModel.tags.collectAsState()
    val appsByTag by viewModel.appsByTag.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val isMultiSelectActive by viewModel.isMultiSelectActive.collectAsState()
    val selectedApps by viewModel.selectedApps.collectAsState()
    val isTagDialogVisible by viewModel.isTagDialogVisible.collectAsState()
    val appForDeferredTask by viewModel.appForDeferredTask.collectAsState()
    val appForTagAssignment by viewModel.appForTagAssignment.collectAsState()
    val isAddTagDialogVisible by viewModel.isAddTagDialogVisible.collectAsState()
    val tagToManage by viewModel.tagToManage.collectAsState()
    val pagerState = rememberPagerState(pageCount = { tags.size })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val selectedAppForMenu by viewModel.selectedAppForMenu.collectAsState()

    var currentVisibleApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    val currentTagId = tags.getOrNull(pagerState.currentPage)?.second ?: 0

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest {
            when (it) {
                is HomeViewEvent.LaunchIntent -> context.startActivity(it.intent)
                is HomeViewEvent.ShowToast -> {
                    val text = it.args?.let { args -> context.getString(it.message, args as Int) } ?: context.getString(it.message)
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (isTagDialogVisible) {
        TagManagementDialog(
            viewModel = viewModel,
            tags = tags,
            selectedApps = selectedApps,
            onDismiss = { viewModel.dismissTagDialog() }
        )
    }

    appForDeferredTask?.let {
        DeferredTaskDialog(
            appInfo = it,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissDeferredTaskDialog() }
        )
    }

    appForTagAssignment?.let {
        TagAssignmentDialog(
            appInfo = it,
            viewModel = viewModel,
            tags = tags,
            onDismiss = { viewModel.dismissTagAssignmentDialog() }
        )
    }

    if (isAddTagDialogVisible) {
        InputDialog(
            title = stringResource(R.string.action_tag_add),
            hint = stringResource(R.string.tag),
            onDismiss = { viewModel.dismissAddTagDialog() },
            onConfirm = { viewModel.addNewTag(it) }
        )
    }

    tagToManage?.let {
        RenameTagDialog(
            tag = it,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissTagManagementDialog() }
        )
    }

    selectedAppForMenu?.let {
        AppContextMenu(
            appInfo = it,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissContextMenu() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when {
                        isMultiSelectActive -> Text(stringResource(R.string.msg_selected, selectedApps.size))
                        isSearchActive -> TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search") }
                        )
                        else -> Text(stringResource(R.string.app_name))
                    }
                },
                navigationIcon = {
                    when {
                        isMultiSelectActive -> IconButton(onClick = { viewModel.toggleMultiSelect() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit multi-select")
                        }
                        isSearchActive -> IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        else -> { /* No navigation icon */ }
                    }
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    when {
                        isMultiSelectActive -> {
                            IconButton(onClick = { viewModel.selectAll(currentVisibleApps) }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                            }
                            MultiSelectMenu(viewModel)
                        }
                        isSearchActive -> { /* No actions in search mode */ }
                        else -> {
                            IconButton(onClick = { viewModel.toggleSearch() }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { viewModel.toggleMultiSelect() }) {
                                Icon(Icons.Default.Checklist, contentDescription = "Multi-select")
                            }
                            GlobalOptionsMenu(navController, viewModel, currentVisibleApps, currentTagId)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSearchActive && !isMultiSelectActive) {
                FloatingActionButton(onClick = { viewModel.freezeCurrentApps(currentVisibleApps) }) {
                    Icon(Icons.Default.Add, contentDescription = "Freeze current apps")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (isSearchActive) {
                currentVisibleApps = appsByTag.values.flatten().distinctBy { it.packageName }
                AppGrid(
                    apps = currentVisibleApps,
                    selectedApps = selectedApps,
                    onAppClick = viewModel::onAppClick,
                    onAppLongClick = viewModel::onAppLongClick
                )
            } else {
                if (tags.size > 1) {
                    SecondaryTabRow(selectedTabIndex = pagerState.currentPage) {
                        for ((index, tag) in tags.withIndex()) {
                            Tab(
                                selected = pagerState.currentPage == index,
                                text = { Text(tag.first.toString()) },
                                modifier = Modifier.combinedClickable(
                                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                                    onLongClick = { viewModel.onTagLongPress(tag) }
                                )
                            )
                        }
//                        tags.forEachIndexed { index, tag ->
//                            Tab(
//                                selected = pagerState.currentPage == index,
//                                text = { Text(tag.first) },
//                                modifier = Modifier.combinedClickable(
//                                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
//                                    onLongClick = { viewModel.onTagLongPress(tag) }
//                                )
//                            )
//                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val tagId = tags.getOrNull(page)?.second
                    val apps = if (tagId != null) appsByTag[tagId] ?: emptyList() else emptyList()
                    currentVisibleApps = apps
                    AppGrid(
                        apps = apps,
                        selectedApps = selectedApps,
                        onAppClick = viewModel::onAppClick,
                        onAppLongClick = viewModel::onAppLongClick
                    )
                }
            }
        }
    }
}

@Composable
private fun GlobalOptionsMenu(navController: NavController, viewModel: HomeViewModel, currentVisibleApps: List<AppInfo>, currentTagId: Int) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.title_apps)) }, onClick = {
                navController.navigate(AppDestinations.APPS)
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.title_settings)) }, onClick = {
                navController.navigate(AppDestinations.SETTINGS)
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_import_clipboard)) }, onClick = {
                viewModel.importFromClipboard(currentTagId)
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_import_frozen)) }, onClick = {
                viewModel.importFrozenApps(currentTagId)
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_export_current)) }, onClick = {
                viewModel.exportAppsToClipboard(currentVisibleApps)
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_export_all)) }, onClick = {
                viewModel.exportAppsToClipboard(viewModel.appsByTag.value.values.flatten())
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_freeze_current)) }, onClick = {
                viewModel.freezeCurrentApps(currentVisibleApps)
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_unfreeze_current)) }, onClick = {
                viewModel.unfreezeCurrentApps(currentVisibleApps)
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_freeze_all)) }, onClick = {
                viewModel.freezeAllApps(false)
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_unfreeze_all)) }, onClick = {
                viewModel.unfreezeAllApps()
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_freeze_non_whitelisted)) }, onClick = {
                viewModel.freezeAllApps(true)
                menuExpanded = false
            })
        }
    }
}

@Composable
private fun MultiSelectMenu(viewModel: HomeViewModel) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.action_freeze)) }, onClick = {
                viewModel.setFreezeStateForSelected(true)
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_unfreeze)) }, onClick = {
                viewModel.setFreezeStateForSelected(false)
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_tag_set)) }, onClick = {
                viewModel.showTagDialog()
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_export_clipboard)) }, onClick = {
                viewModel.exportAppsToClipboard(viewModel.selectedApps.value)
                menuExpanded = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_remove_home)) }, onClick = {
                viewModel.removeSelectedApps()
                menuExpanded = false
            })
        }
    }
}

@Composable
private fun AppGrid(apps: List<AppInfo>, selectedApps: List<AppInfo>, onAppClick: (AppInfo) -> Unit, onAppLongClick: (AppInfo) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 80.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            val isSelected = app in selectedApps
            AppGridItem(
                appInfo = app,
                isSelected = isSelected,
                onClick = { onAppClick(app) },
                onLongClick = { onAppLongClick(app) }
            )
        }
    }
}

@Composable
private fun AppGridItem(appInfo: AppInfo, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = shape
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppIcon(
            appInfo = appInfo,
            modifier = Modifier
                .fillMaxSize(0.6f)
                .aspectRatio(1f)
        )
        Text(
            text = appInfo.name.toString(),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}