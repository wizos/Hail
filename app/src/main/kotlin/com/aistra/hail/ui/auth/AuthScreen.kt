package com.aistra.hail.ui.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aistra.hail.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = koinViewModel(),
    onAuthenticated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.startAuthentication()
    }

    when (val state = uiState) {
        is AuthState.Authenticated -> {
            LaunchedEffect(Unit) {
                onAuthenticated()
            }
        }
        AuthState.BiometricPrompt -> {
            val activity = context as FragmentActivity
            val biometricManager = BiometricManager.from(activity)
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                val executor = ContextCompat.getMainExecutor(activity)
                val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        viewModel.onBiometricAuthResult(true, null)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        viewModel.onBiometricAuthResult(false, errString.toString())
                    }
                })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(activity.getString(R.string.action_biometric))
                    .setSubtitle(activity.getString(R.string.msg_biometric))
                    .setNegativeButtonText(activity.getString(android.R.string.cancel))
                    .build()
                
                biometricPrompt.authenticate(promptInfo)
            } else {
                // Fallback or show error if biometric is not available
                viewModel.onBiometricAuthResult(false, "生物识别不可用")
            }
        }
        is AuthState.PasswordEntry -> {
            PasswordScreen(viewModel = viewModel, error = state.error)
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
