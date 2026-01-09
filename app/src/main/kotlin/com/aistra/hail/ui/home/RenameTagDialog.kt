package com.aistra.hail.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.R

@Composable
fun RenameTagDialog(
    tag: Pair<String, Int>,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(tag.first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_tag_set)) },
        text = {
            TextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(stringResource(R.string.tag)) }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.renameTag(tag, newName)
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tag.second != 0) { // Default tag cannot be removed
                    TextButton(
                        onClick = {
                            viewModel.removeTag(tag)
                        }
                    ) {
                        Text(stringResource(R.string.action_tag_remove))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
    )
}
