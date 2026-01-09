package com.aistra.hail.ui.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HUI
import java.security.MessageDigest
import androidx.core.content.edit

object AuthManager {

    fun isAuthEnabled(): Boolean {
        return HailData.biometricLogin
    }

    fun needReauthOnResume(): Boolean {
        return HailData.reauthOnResume && isAuthEnabled()
    }

    fun needReauthOnScreenOff(): Boolean {
        return HailData.reauthOnScreenOff && isAuthEnabled()
    }

    fun authenticateBiometric(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val biometricManager = BiometricManager.from(activity)
        if (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("生物识别不可用")
            return
        }

        val biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    HUI.showToast("认证失败")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.action_biometric))
            .setSubtitle(activity.getString(R.string.msg_biometric))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun authenticatePassword(input: String): Boolean {
        val stored = HailData.authPassword

//        android.util.Log.d("PasswordDebug", "输入密码: '$password'")
        if (stored.isEmpty()) return false
        // 简化：直接比较，不再hash存储（可选，也可以继续使用hash）
        return input == stored
    }

    fun setPassword(password: String): Boolean {
        // 简化：直接明文存储（可选，也可以使用hash）
        val editor = androidx.preference.PreferenceManager.getDefaultSharedPreferences(com.aistra.hail.HailApp.app).edit()
        editor.putString(HailData.AUTH_PASSWORD, password)
        val success = editor.commit()  // commit() 返回是否保存成功

        if (!success) {
            android.util.Log.e("AuthManager", "密码保存失败")
        }

        // 验证一下是否真的保存成功
        val saved = androidx.preference.PreferenceManager.getDefaultSharedPreferences(com.aistra.hail.HailApp.app)
            .getString(HailData.AUTH_PASSWORD, "")
        android.util.Log.d("AuthManager", "保存的密码: '$saved'")

        return success

    }

    // 保留hash方法，如果需要加密存储可以启用
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun validatePasswordFormat(password: String): Boolean {
        // 只允许 0-9，纯数字
        return password.all { it.isDigit() } && password.isNotEmpty()
    }
}