package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gologlu.detracktor.runtime.android.presentation.types.RuleMatchSummary

/**
 * Enhanced rules summary component displaying rule matches as vertical bullets
 * instead of concatenated text. Provides better visual organization.
 */
@Composable
fun RulesSummary(
    matchedRules: List<RuleMatchSummary>,
    modifier: Modifier = Modifier
) {
    if (matchedRules.isEmpty()) return
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rules-summary"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Text(
                text = "${matchedRules.size} rule${if (matchedRules.size != 1) "s" else ""} matched",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("rules-header")
            )
            
            // Rule items as vertical bullets
            matchedRules.forEachIndexed { index, rule ->
                RuleBulletItem(
                    rule = rule,
                    index = index
                )
            }
        }
    }
}

/**
 * Individual rule match item displayed as a bullet point
 */
@Composable
private fun RuleBulletItem(
    rule: RuleMatchSummary,
    index: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rule-item-$index"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Bullet point indicator
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Rule matched",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(16.dp)
                .testTag("rule-bullet-$index")
        )
        
        // Rule content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Domain and description
            Text(
                text = rule.domain,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("rule-domain-$index")
            )
            
            if (rule.description.isNotBlank()) {
                Text(
                    text = rule.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.testTag("rule-description-$index")
                )
            }
            
            // Matched parameters
            if (rule.matchedParams.isNotEmpty()) {
                MatchedParametersChips(
                    matchedParams = rule.matchedParams,
                    ruleIndex = index
                )
            }
        }
    }
}

/**
 * Display matched parameters as small chips
 */
@Composable
private fun MatchedParametersChips(
    matchedParams: List<String>,
    ruleIndex: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Matched:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.testTag("matched-label-$ruleIndex")
        )
        
        // Wrap parameters in multiple rows if needed
        val chunkedParams = matchedParams.chunked(3) // Max 3 per row
        chunkedParams.forEachIndexed { rowIndex, rowParams ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.testTag("matched-row-$ruleIndex-$rowIndex")
            ) {
                rowParams.forEach { param ->
                    Text(
                        text = param,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("matched-param-$param")
                    )
                }
            }
        }
    }
}

/**
 * Compact rules summary for when space is limited
 */
@Composable
fun CompactRulesSummary(
    matchedRules: List<RuleMatchSummary>,
    modifier: Modifier = Modifier
) {
    if (matchedRules.isEmpty()) return
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
            .testTag("compact-rules-summary"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bullet indicator
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("compact-bullet")
        )
        
        // Summary text
        Text(
            text = "${matchedRules.size} rule${if (matchedRules.size != 1) "s" else ""} matched",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("compact-text")
        )
        
        // Total matched parameters count
        val totalMatched = matchedRules.sumOf { it.matchedParams.size }
        if (totalMatched > 0) {
            Text(
                text = "($totalMatched matched)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.testTag("compact-matched-count")
            )
        }
    }
}
