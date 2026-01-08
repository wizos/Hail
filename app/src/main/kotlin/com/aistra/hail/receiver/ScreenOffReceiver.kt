package com.aistra.hail.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import com.aistra.hail.ui.auth.AuthManager
import com.aistra.hail.work.HWork

class ScreenOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            HWork.setAutoFreeze(true)
            // 在 onReceive 中添加
            if (AuthManager.needReauthOnScreenOff()) {
                // 标记需要重新认证
                PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putBoolean("need_reauth", true)
                    .apply()
            }
        }
    }
}