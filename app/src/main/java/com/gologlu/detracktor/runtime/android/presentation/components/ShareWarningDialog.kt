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
import com.gologlu.detracktor.runtime.android.presentation.types.WarningDisplayData

/**
 * Dialog for share-intent warnings with "don't warn again" checkbox.
 * Shows warnings prominently and allows users to choose between sharing or copying,
 * with an option to suppress future warnings for share-intent flows.
 */
@Composable
fun ShareWarningDialog(
    cleanedUrl: String,
    warningData: WarningDisplayData,
    onShare: (dontWarnAgain: Boolean) -> Unit,
    onCopy: (dontWarnAgain: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dontWarnAgain by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.share_warning_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.share_warning_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Warning display
                DialogWarningDisplay(
                    warningData = warningData,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                // "Don't warn again" checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = dontWarnAgain,
                        onCheckedChange = { dontWarnAgain = it }
                    )
                    Text(
                        text = stringResource(R.string.dont_warn_again),
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
                    onClick = { onCopy(dontWarnAgain) }
                ) {
                    Text(stringResource(R.string.copy))
                }
                TextButton(
                    onClick = { onShare(dontWarnAgain) }
                ) {
                    Text(stringResource(R.string.share))
                }
            }
        },
        modifier = modifier
    )
}
