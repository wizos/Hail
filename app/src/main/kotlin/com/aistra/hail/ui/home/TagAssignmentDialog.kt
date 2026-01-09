package com.aistra.hail.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo

@Composable
fun TagAssignmentDialog(
    appInfo: AppInfo,
    viewModel: HomeViewModel,
    tags: List<Pair<String, Int>>,
    onDismiss: () -> Unit
) {
    var selectedTagIds by remember { mutableStateOf(appInfo.tagIdList.toList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_tag_set)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                tags.forEach { (tagName, tagId) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                selectedTagIds = if (tagId in selectedTagIds) {
                                    selectedTagIds - tagId
                                } else {
                                    selectedTagIds + tagId
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tagId in selectedTagIds,
                            onCheckedChange = null // Handled by Row click
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(tagName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                viewModel.updateTagsForApp(appInfo, selectedTagIds)
            }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        modifier = Modifier.padding(vertical = 16.dp)
    )
}