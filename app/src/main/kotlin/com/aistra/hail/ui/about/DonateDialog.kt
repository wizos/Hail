package com.aistra.hail.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HUI

@Composable
fun DonateDialog(onDismiss: () -> Unit) {
    var selectedOption by remember { mutableStateOf(0) }
    val paymentEntries = stringArrayResource(R.array.donate_payment_entries)
    val context = LocalContext.current

    var showWeChatQr by remember { mutableStateOf(false) }
    var showBilibiliInfo by remember { mutableStateOf(false) }

    if (showWeChatQr) {
        AlertDialog(
            onDismissRequest = { showWeChatQr = false },
            title = { Text(stringResource(R.string.title_donate)) },
            text = { Image(painter = painterResource(R.mipmap.qr_wechat), contentDescription = "WeChat QR Code") },
            confirmButton = {
                TextButton(onClick = {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.tencent.mm")
                    intent?.putExtra("LauncherUI.From.Scaner.Shortcut", true)
                    if (intent != null) context.startActivity(intent) else HUI.showToast(R.string.app_not_installed)
                    showWeChatQr = false
                }) {
                    Text(stringResource(R.string.donate_wechat_scan))
                }
            },
            dismissButton = { TextButton(onClick = { showWeChatQr = false }) { Text(stringResource(android.R.string.cancel)) } }
        )
    }

    if (showBilibiliInfo) {
        AlertDialog(
            onDismissRequest = { showBilibiliInfo = false },
            title = { Text(stringResource(R.string.title_donate)) },
            text = { Text(stringResource(R.string.donate_bilibili_msg)) },
            confirmButton = {
                TextButton(onClick = { HUI.openLink(HailData.URL_BILIBILI); showBilibiliInfo = false }) {
                    Text(stringResource(R.string.donate_bilibili_space))
                }
            },
            dismissButton = { TextButton(onClick = { showBilibiliInfo = false }) { Text(stringResource(R.string.donate_bilibili_cancel)) } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_donate)) },
        text = {
            Column {
                paymentEntries.forEachIndexed { index, text ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { 
                                selectedOption = index
                             }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOption == index),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (selectedOption) {
                    0 -> {
                        if (!HUI.openLink(HailData.URL_ALIPAY_API)) {
                            HUI.openLink(HailData.URL_ALIPAY)
                        }
                    }
                    1 -> showWeChatQr = true
                    2 -> showBilibiliInfo = true
                    3 -> HUI.openLink(HailData.URL_LIBERAPAY)
                    4 -> HUI.openLink(HailData.URL_PAYPAL)
                }
                onDismiss()
            }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
    )
}