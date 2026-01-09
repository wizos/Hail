package com.aistra.hail.ui.apps

import android.app.Application
import android.content.pm.ApplicationInfo
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.FuzzySearch
import com.aistra.hail.utils.HFiles
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HUI
import com.aistra.hail.utils.NineKeySearch
import com.aistra.hail.utils.PinyinSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream

sealed class AppsViewEvent {
    data class ShowToast(val message: Int, val args: Any? = null) : AppsViewEvent()
    data class ExportApk(val fileName: String) : AppsViewEvent()
    data class ShowUninstallDialog(val appInfo: ApplicationInfo) : AppsViewEvent()
}

class AppsViewModel(application: Application) : AndroidViewModel(application) {

    private val _allApps = MutableStateFlow<List<ApplicationInfo>>(emptyList())
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(HailData.sortBy)
    val sortOption = _sortOption.asStateFlow()

    private val _filterOptions = MutableStateFlow(
        mapOf(
            HailData.FILTER_USER_APPS to true,
            HailData.FILTER_SYSTEM_APPS to false,
            HailData.FILTER_FROZEN_APPS to false,
            HailData.FILTER_UNFROZEN_APPS to false,
        )
    )
    val filterOptions = _filterOptions.asStateFlow()

    val displayedApps = combine(
        _allApps, _searchQuery, _sortOption, _filterOptions
    ) { apps, query, sort, filters ->
        var finalList = apps

        // Filtering logic...

        // Sorting logic...

        finalList
    }

    private val _events = MutableSharedFlow<AppsViewEvent>()
    val events = _events.asSharedFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _allApps.value = withContext(Dispatchers.IO) {
                HPackages.getInstalledApplications()
            }
            _isRefreshing.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortChanged(sort: String) {
        _sortOption.value = sort
        HailData.sortBy = sort
    }

    fun onFilterChanged(filter: String, isEnabled: Boolean) {
        val newFilters = _filterOptions.value.toMutableMap()
        if (filter == HailData.FILTER_USER_APPS && isEnabled) {
            newFilters[HailData.FILTER_SYSTEM_APPS] = false
        } else if (filter == HailData.FILTER_SYSTEM_APPS && isEnabled) {
            newFilters[HailData.FILTER_USER_APPS] = false
        }
        newFilters[filter] = isEnabled
        _filterOptions.value = newFilters

        if (filter == HailData.FILTER_USER_APPS || filter == HailData.FILTER_SYSTEM_APPS) {
            HailData.changeAppsFilter(filter, isEnabled)
            HailData.changeAppsFilter(if (filter == HailData.FILTER_USER_APPS) HailData.FILTER_SYSTEM_APPS else HailData.FILTER_USER_APPS, !isEnabled)
        } else {
            HailData.changeAppsFilter(filter, isEnabled)
        }
    }

    fun exportApk(appInfo: ApplicationInfo) {
        viewModelScope.launch {
            _events.emit(AppsViewEvent.ExportApk("${appInfo.loadLabel(getApplication<Application>().packageManager)}.apk"))
        }
    }
    
    fun onApkExportUriReceived(uri: Uri?, appInfo: ApplicationInfo) {
        if (uri == null) return
        viewModelScope.launch {
             runCatching {
                withContext(Dispatchers.IO) {
                    FileInputStream(appInfo.sourceDir).use { source ->
                        getApplication<Application>().contentResolver.openOutputStream(uri, "rwt").use { target ->
                            if (target == null) return@withContext
                            HFiles.copy(source, target)
                        }
                    }
                }
            }.onSuccess {
                _events.emit(AppsViewEvent.ShowToast(R.string.msg_extract_apk, uri.toString()))
            }.onFailure {
                _events.emit(AppsViewEvent.ShowToast(R.string.operation_failed, it.localizedMessage ?: "Unknown"))
            }
        }
    }

    fun uninstallApp(appInfo: ApplicationInfo) {
        viewModelScope.launch {
            when {
                HPackages.isAppUninstalled(appInfo.packageName) -> _events.emit(AppsViewEvent.ShowToast(R.string.app_not_installed))
                appInfo.packageName == getApplication<Application>().packageName -> {
                    // Handle self-uninstall logic
                }
                HailData.workingMode == HailData.MODE_DEFAULT -> AppManager.uninstallApp(appInfo.packageName)
                else -> _events.emit(AppsViewEvent.ShowUninstallDialog(appInfo))
            }
        }
    }
    
    fun confirmUninstall(appInfo: ApplicationInfo) {
        viewModelScope.launch {
            if (AppManager.uninstallApp(appInfo.packageName)) {
                loadApps()
            }
        }
    }
}