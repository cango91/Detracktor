package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gologlu.detracktor.runtime.android.presentation.types.RuleMatchSummary
import com.gologlu.detracktor.runtime.android.presentation.types.WarningDisplayData

/**
 * Collapsible warning panel component for enhanced warning display.
 * Shows warning count in header and detailed information when expanded.
 */
@Composable
fun WarningPanel(
    warningData: WarningDisplayData,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!warningData.hasWarnings) return
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("warning-panel"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // Warning header - always visible
            WarningHeader(
                warningCount = warningData.warningCount,
                isExpanded = warningData.isExpanded,
                onClick = onToggleExpanded
            )
            
            // Warning content - collapsible
            AnimatedVisibility(
                visible = warningData.isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                WarningContent(warningData = warningData)
            }
        }
    }
}

/**
 * Warning panel header with warning count and expand/collapse button
 */
@Composable
private fun WarningHeader(
    warningCount: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp)
            .testTag("warning-header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("warning-icon")
            )
            Text(
                text = "$warningCount warning${if (warningCount != 1) "s" else ""} detected",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("warning-count")
            )
        }
        
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("warning-expand-icon")
        )
    }
}

/**
 * Warning panel content showing detailed warning information
 */
@Composable
private fun WarningContent(warningData: WarningDisplayData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .testTag("warning-content"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Credentials warning
        if (warningData.hasCredentials) {
            WarningItem(
                title = "Embedded Credentials",
                description = "This URL contains embedded username/password credentials that may be visible to others.",
                severity = WarningSeverity.HIGH
            )
        }
        
        // Sensitive parameters warning
        if (warningData.sensitiveParams.isNotEmpty()) {
            WarningItem(
                title = "Sensitive Parameters",
                description = "Found sensitive parameters: ${warningData.sensitiveParams.joinToString(", ")}",
                severity = WarningSeverity.MEDIUM
            )
        }
        
    }
}

/**
 * Individual warning item with title, description and severity indicator
 */
@Composable
private fun WarningItem(
    title: String,
    description: String,
    severity: WarningSeverity
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = severity.backgroundColor,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
            .testTag("warning-item"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Severity indicator
        Text(
            text = severity.indicator,
            style = MaterialTheme.typography.bodySmall,
            color = severity.textColor,
            modifier = Modifier.testTag("warning-severity")
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = severity.textColor,
                modifier = Modifier.testTag("warning-title")
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = severity.textColor.copy(alpha = 0.8f),
                modifier = Modifier.testTag("warning-description")
            )
        }
    }
}

/**
 * Warning severity levels with associated colors and indicators
 */
private enum class WarningSeverity(
    val indicator: String,
    val backgroundColorProvider: @Composable () -> Color,
    val textColorProvider: @Composable () -> Color
) {
    HIGH(
        indicator = "🔴",
        backgroundColorProvider = { MaterialTheme.colorScheme.errorContainer },
        textColorProvider = { MaterialTheme.colorScheme.onErrorContainer }
    ),
    MEDIUM(
        indicator = "🟡",
        backgroundColorProvider = { MaterialTheme.colorScheme.secondaryContainer },
        textColorProvider = { MaterialTheme.colorScheme.onSecondaryContainer }
    ),
    LOW(
        indicator = "🔵",
        backgroundColorProvider = { MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) },
        textColorProvider = { MaterialTheme.colorScheme.onPrimaryContainer }
    );
    
    val backgroundColor: Color @Composable get() = backgroundColorProvider()
    val textColor: Color @Composable get() = textColorProvider()
}
