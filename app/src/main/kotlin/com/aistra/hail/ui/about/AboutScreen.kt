package com.aistra.hail.ui.about

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HUI
import java.text.SimpleDateFormat

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val installTime = remember {
        try {
            HPackages.getUnhiddenPackageInfoOrNull(context.packageName)!!.firstInstallTime
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    var openLicenseDialog by remember { mutableStateOf(false) }
    if (openLicenseDialog) {
        LicenseDialog { openLicenseDialog = false }
    }

    var openDonateDialog by remember { mutableStateOf(false) }
    if (openDonateDialog) {
        DonateDialog { openDonateDialog = false }
    }

    Column(
        modifier = Modifier.verticalScroll(state = rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
        Card(
            onClick = { HUI.openLink(HailData.URL_WHY_FREE_SOFTWARE) },
            modifier = Modifier
                .height(dimensionResource(R.dimen.header_height))
                .padding(horizontal = dimensionResource(R.dimen.padding_medium))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_extra_small), Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White, CircleShape),
                    contentScale = ContentScale.None
                )
                Text(
                    text = stringResource(R.string.app_name), style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.app_slogan), style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
        OutlinedCard(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium))) {
            ClickableItem(
                icon = Icons.Outlined.Update, title = R.string.label_version, desc = HailData.VERSION
            ) { HUI.openLink(HailData.URL_RELEASES) }
            ClickableItem(
                icon = Icons.Outlined.InstallMobile,
                title = R.string.label_time,
                desc = SimpleDateFormat.getDateInstance().format(installTime)
            ) { HUI.showToast("\uD83E\uDD76\uD83D\uDCA8\uD83D\uDC09") }
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
        OutlinedCard(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium))) {
            ClickableItem(
                icon = Icons.AutoMirrored.Filled.Send, title = R.string.action_telegram
            ) { HUI.openLink(HailData.URL_TELEGRAM) }
            ClickableItem(
                icon = Icons.Outlined.Group, title = R.string.action_qq
            ) { HUI.openLink(HailData.URL_QQ) }
            ClickableItem(
                icon = Icons.Outlined.LocalMall, title = R.string.action_fdroid
            ) { HUI.openLink(HailData.URL_FDROID) }
            ClickableItem(
                icon = Icons.Outlined.CardGiftcard, title = R.string.action_donate, onClick = { openDonateDialog = true }
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
        OutlinedCard(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium))) {
            ClickableItem(
                icon = Icons.Outlined.Code, title = R.string.action_github
            ) { HUI.openLink(HailData.URL_GITHUB) }
            ClickableItem(
                icon = Icons.Outlined.Translate, title = R.string.action_translate
            ) { HUI.openLink(HailData.URL_TRANSLATE) }
            ClickableItem(
                icon = Icons.Outlined.Description, title = R.string.action_licenses
            ) { openLicenseDialog = true }
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
    }
}

@Composable
private fun ClickableItem(
    icon: ImageVector, @StringRes title: Int, desc: String? = null, onClick: () -> Unit
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick),
    verticalAlignment = Alignment.CenterVertically
) {
    Icon(
        imageVector = icon, contentDescription = null, modifier = Modifier.padding(
            horizontal = dimensionResource(R.dimen.padding_medium),
            vertical = dimensionResource(if (desc == null) R.dimen.padding_medium else R.dimen.padding_large)
        )
    )
    Column {
        Text(text = stringResource(title), style = MaterialTheme.typography.bodyLarge)
        if (desc != null) Text(text = desc, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LicenseDialog(onDismiss: () -> Unit) {
    val context = LocalResources.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.action_licenses)) },
        text = {
            SelectionContainer {
                Text(
                    text = buildAnnotatedString {
                        val lines = context.openRawResource(R.raw.licenses).bufferedReader().readLines()
                        lines.forEach {
                            if (it.isNotBlank()) {
                                // Link handling with annotations will be done here
                                append(it.substringBefore(": "))
                            }
                            if (it != lines.last()) append("\n\n")
                        }
                    }, modifier = Modifier.verticalScroll(state = rememberScrollState())
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(text = stringResource(android.R.string.ok)) } }
    )
}
