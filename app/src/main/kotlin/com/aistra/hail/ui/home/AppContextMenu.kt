package com.aistra.hail.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContextMenu(
    appInfo: AppInfo,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val isFrozen = AppManager.isAppFrozen(appInfo.packageName)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = appInfo.name.toString(),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            MenuItem(
                icon = Icons.Default.AcUnit,
                text = if (isFrozen) stringResource(R.string.action_unfreeze) else stringResource(R.string.action_freeze),
                onClick = { viewModel.toggleFreezeState(appInfo) }
            )
            MenuItem(
                icon = Icons.Default.MoreTime,
                text = stringResource(R.string.action_deferred_task),
                onClick = { viewModel.showDeferredTaskDialog(appInfo) }
            )
            MenuItem(
                icon = if (appInfo.pinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                text = if (appInfo.pinned) stringResource(R.string.action_unpin) else stringResource(R.string.action_pin),
                onClick = { viewModel.pinApp(appInfo) }
            )
            MenuItem(
                icon = Icons.Default.Security,
                text = if (appInfo.whitelisted) stringResource(R.string.action_remove_whitelist) else stringResource(R.string.action_whitelist),
                onClick = { viewModel.toggleWhitelist(appInfo) }
            )
            MenuItem(
                icon = Icons.Default.Label,
                text = stringResource(R.string.action_tag_set),
                onClick = { viewModel.showTagAssignmentDialog(appInfo) }
            )
            MenuItem(
                icon = Icons.Default.PushPin,
                text = stringResource(R.string.action_add_pin_shortcut),
                onClick = { viewModel.createShortcut(appInfo) }
            )
            MenuItem(
                icon = Icons.Default.IosShare,
                text = stringResource(R.string.action_export_clipboard),
                onClick = { viewModel.exportAppsToClipboard(listOf(appInfo)) }
            )
            MenuItem(
                icon = Icons.Default.Delete,
                text = stringResource(R.string.action_remove_home),
                onClick = { viewModel.removeApp(appInfo) }
            )
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text)
    }
}
