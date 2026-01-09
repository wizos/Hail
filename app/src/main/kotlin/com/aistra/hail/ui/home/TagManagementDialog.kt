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
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo

@Composable
fun TagManagementDialog(
    viewModel: HomeViewModel,
    tags: List<Pair<String, Int>>,
    selectedApps: List<AppInfo>,
    onDismiss: () -> Unit
) {
    if (selectedApps.isEmpty()) {
        onDismiss()
        return
    }

    val initialStates = remember(tags, selectedApps) {
        tags.map { (_, tagId) ->
            when (selectedApps.count { tagId in it.tagIdList }) {
                selectedApps.size -> ToggleableState.On
                0 -> ToggleableState.Off
                else -> ToggleableState.Indeterminate
            }
        }
    }

    val tagStates = remember {
        val states = tags.mapIndexed { index, tag -> tag.second to initialStates[index] }
        states.toMutableStateMap()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_tag_set)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                tags.forEachIndexed { index, tag ->
                    val (tagName, tagId) = tag
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val currentState = tagStates[tagId]!!
                                val initialState = initialStates[index]
                                tagStates[tagId] = if (initialState == ToggleableState.Indeterminate) {
                                    when (currentState) {
                                        ToggleableState.On -> ToggleableState.Off
                                        ToggleableState.Off -> ToggleableState.Indeterminate
                                        ToggleableState.Indeterminate -> ToggleableState.On
                                    }
                                } else {
                                    if (currentState == ToggleableState.On) ToggleableState.Off
                                    else ToggleableState.On
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TriStateCheckbox(
                            state = tagStates[tagId]!!,
                            onClick = null,
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = tagName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.applyTagsToSelectedApps(tagStates)
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
