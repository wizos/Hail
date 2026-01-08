package com.aistra.hail.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.ActivityMainBinding
import com.aistra.hail.extensions.*
import com.aistra.hail.ui.auth.AuthManager
import com.aistra.hail.ui.auth.CalculatorView
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HUI
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import androidx.core.content.edit

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {
    lateinit var fab: ExtendedFloatingActionButton
    lateinit var appbar: AppBarLayout

    private var isAuthenticated = false
    private var isInBackground = false
    private var calculatorOverlay: FrameLayout? = null
    private lateinit var mainContent: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val binding = initView()

        mainContent = binding.root

        // 如果不需要认证，直接显示
        if (!AuthManager.isAuthEnabled()) {
            isAuthenticated = true
            return
        }

        // 检查息屏后是否需要重新认证
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val needReauth = if (AuthManager.needReauthOnScreenOff()) {
            sp.getBoolean("need_reauth", false)
        } else false

        if (needReauth) {
            sp.edit().putBoolean("need_reauth", false).apply()
            // 息屏后需要重新认证
        }

        if (HailData.authType == HailData.AUTH_TYPE_PASSWORD) {
            // 密码认证模式
            createCalculatorOverlay(binding)

            // 如果未认证或需要重新认证，显示计算器
            if (!isAuthenticated) {
                calculatorOverlay?.isVisible = true
                mainContent.isVisible = false
            }
        } else {
            // 生物识别认证模式
            binding.root.isVisible = false
            performBiometricAuth(binding)
        }
    }

    private fun createCalculatorOverlay(binding: ActivityMainBinding) {
        calculatorOverlay = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            val calculator = CalculatorView(this@MainActivity)
            calculator.setOnPasswordEntered { password ->
                android.util.Log.d("PasswordDebug", "输入密码: '$password'")
                android.util.Log.d("PasswordDebug", "存储密码: '${HailData.authPassword}'")
                android.util.Log.d("PasswordDebug", "是否相等: ${password == HailData.authPassword}")

                if (AuthManager.authenticatePassword(password)) {
                    isAuthenticated = true
                    this@MainActivity.calculatorOverlay?.isVisible = false
                    calculator.display.text = ""
                    binding.root.isVisible = true
                    HUI.showToast("认证成功")
                } else {
                    calculator.clearInput()
                    HUI.showToast("密码错误")
                }
            }
            addView(calculator)
        }

        // 添加到window
        addContentView(
            calculatorOverlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun performBiometricAuth(binding: ActivityMainBinding) {
        AuthManager.authenticateBiometric(
            this,
            onSuccess = {
                isAuthenticated = true
                binding.root.isVisible = true
            },
            onError = { error ->
                HUI.showToast(error)
                finishAndRemoveTask()
            }
        )
    }

    private fun initView() = ActivityMainBinding.inflate(layoutInflater).apply {
        setContentView(root)
        setSupportActionBar(appBarMain.toolbar)
        fab = appBarMain.fab
        appbar = appBarMain.appBarLayout

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.addOnDestinationChangedListener(this@MainActivity)
        val appBarConfiguration = AppBarConfiguration.Builder(
            R.id.nav_home, R.id.nav_apps, R.id.nav_settings, R.id.nav_about
        ).build()
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNav?.setupWithNavController(navController)
        navRail?.setupWithNavController(navController)

        val isRtl = isRtl
        val isLandscape = isLandscape
        appBarMain.appBarLayout.applyDefaultInsetter {
            paddingRelative(isRtl, start = !isLandscape, end = true, top = true)
        }
        bottomNav?.applyDefaultInsetter { paddingRelative(isRtl, start = true, end = true, bottom = true) }
        navRail?.applyDefaultInsetter { paddingRelative(isRtl, start = true, top = true, bottom = true) }
        fab.applyDefaultInsetter { marginRelative(isRtl, end = true, bottom = isLandscape) }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.let { MenuCompat.setGroupDividerEnabled(it, true) }
        return super.onCreateOptionsMenu(menu)
    }

    fun ownerRemoveDialog() {
        MaterialAlertDialogBuilder(this).setTitle(R.string.title_remove_owner).setMessage(R.string.msg_remove_owner)
            .setPositiveButton(R.string.action_continue) { _, _ ->
                HPolicy.setOrganizationName()
                HPolicy.removeDeviceOwner()
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    override fun onStop() {
        super.onStop()
        // 标记进入后台
        if (AuthManager.needReauthOnResume()) {
            isInBackground = true
        }
    }

    override fun onResume() {
        super.onResume()
        // 场景1：从后台恢复，需要重新认证
        if (isInBackground && isAuthenticated && AuthManager.needReauthOnResume()) {
            isAuthenticated = false
            reAuthenticate()
            isInBackground = false
            return
        }

        // 场景2：检查息屏标记
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        if (sp.getBoolean("need_reauth", false)) {
            sp.edit { putBoolean("need_reauth", false) }
            if (isAuthenticated) {
                isAuthenticated = false
                reAuthenticate()
            }
        }

        isInBackground = false
    }

    private fun reAuthenticate() {
        if (HailData.authType == HailData.AUTH_TYPE_PASSWORD) {
            // 确保遮罩层存在
            if (calculatorOverlay == null) {
                createCalculatorOverlay(ActivityMainBinding.bind(mainContent))
            }
            calculatorOverlay?.isVisible = true
            mainContent.isVisible = false
        } else {
            mainContent.isVisible = false
            performBiometricAuth(ActivityMainBinding.bind(mainContent))
        }
    }

    override fun onDestinationChanged(
        controller: NavController, destination: NavDestination, arguments: Bundle?
    ) {
        fab.tag = destination.id == R.id.nav_home
        if (fab.tag == true) fab.show() else fab.hide()
    }
}