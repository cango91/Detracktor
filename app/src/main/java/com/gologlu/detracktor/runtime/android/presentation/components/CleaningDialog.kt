package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import com.gologlu.detracktor.R
import com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction

/**
 * Dialog for manual URL cleaning with "remember my choice" checkbox.
 * Allows users to choose between sharing or copying the cleaned URL,
 * with an option to remember their choice for future cleanings.
 */
@Composable
fun CleaningDialog(
    cleanedUrl: String,
    onShare: (rememberChoice: Boolean) -> Unit,
    onCopy: (rememberChoice: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rememberChoice by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.url_cleaned),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.url_cleaned_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // "Remember my choice" checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = { rememberChoice = it }
                    )
                    Text(
                        text = stringResource(R.string.remember_my_choice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = { onCopy(rememberChoice) }
                ) {
                    Text(stringResource(R.string.copy))
                }
                TextButton(
                    onClick = { onShare(rememberChoice) }
                ) {
                    Text(stringResource(R.string.share))
                }
            }
        },
        modifier = modifier
    )
}
