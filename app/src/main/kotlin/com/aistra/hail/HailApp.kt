package com.aistra.hail

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.services.AutoFreezeService
import com.aistra.hail.ui.apps.AppsViewModel
import com.aistra.hail.ui.auth.AuthViewModel
import com.aistra.hail.ui.home.HomeViewModel
import com.aistra.hail.ui.settings.SettingsViewModel
import com.aistra.hail.utils.HDhizuku
import io.insertkoin.android.ext.koin.androidApplication
import io.insertkoin.android.ext.koin.androidContext
import io.insertkoin.core.context.startKoin
import io.insertkoin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel

val appModule = module {
    viewModel { HomeViewModel(androidApplication()) }
    viewModel { AuthViewModel() }
    viewModel { SettingsViewModel(androidApplication()) }
    viewModel { AppsViewModel(androidApplication()) }
}

class HailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        app = this

        startKoin {
            androidContext(this@HailApp)
            modules(appModule)
        }

        if (HailData.workingMode.startsWith(HailData.DHIZUKU)) HDhizuku.init()
    }

    fun setAutoFreezeService(autoFreezeAfterLock: Boolean = HailData.autoFreezeAfterLock, context: Context = app) {
        val start = autoFreezeAfterLock && HailData.checkedList.any {
            it.packageName != packageName && it.applicationInfo != null && !AppManager.isAppFrozen(it.packageName) && !it.whitelisted
        }
        val intent = Intent(app, AutoFreezeService::class.java)
        if (start) {
            setAutoFreezeServiceEnabled(true)
            ContextCompat.startForegroundService(context, intent)
        } else {
            stopService(intent)
            setAutoFreezeServiceEnabled(false)
        }
    }

    fun setAutoFreezeServiceEnabled(enabled: Boolean) {
        packageManager.setComponentEnabledSetting(
            ComponentName(app, AutoFreezeService::class.java),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    fun setAppTheme(theme: String) {
        HailData.appTheme = theme
    }

    companion object {
        lateinit var app: HailApp private set
    }
}