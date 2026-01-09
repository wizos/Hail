package com.aistra.hail.ui.settings

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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.R

@Composable
fun AddShortcutDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf(0) }
    val shortcutEntries = stringArrayResource(R.array.pin_shortcut_entries)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_add_pin_shortcut)) },
        text = {
            Column {
                shortcutEntries.forEachIndexed { index, text ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = index }
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
                viewModel.addPinShortcut(shortcutEntries[selectedOption])
                onDismiss()
            }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
    )
}
