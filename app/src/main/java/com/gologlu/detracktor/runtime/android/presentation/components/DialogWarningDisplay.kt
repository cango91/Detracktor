package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gologlu.detracktor.R
import com.gologlu.detracktor.runtime.android.presentation.types.WarningDisplayData

/**
 * Simplified warning display component for use in dialogs.
 * Shows warnings in a compact format without expansion functionality.
 */
@Composable
fun DialogWarningDisplay(
    warningData: WarningDisplayData,
    modifier: Modifier = Modifier
) {
    if (!warningData.hasWarnings) return
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Warning header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(R.string.content_description_warning),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.warnings_detected),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        
        // Warning details
        Column(
            modifier = Modifier.padding(start = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (warningData.hasCredentials) {
                Text(
                    text = stringResource(R.string.warning_credentials_detected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (warningData.sensitiveParams.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.warning_sensitive_params_detected,
                        warningData.sensitiveParams.joinToString(", ")
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
