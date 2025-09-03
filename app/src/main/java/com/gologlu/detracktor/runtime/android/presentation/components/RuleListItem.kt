package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gologlu.detracktor.application.types.UrlRule
import com.gologlu.detracktor.runtime.android.presentation.types.getDescription
import com.gologlu.detracktor.runtime.android.presentation.ui.theme.DetracktorTheme

/**
 * Individual rule display component with edit/delete actions
 */
@Composable
fun RuleListItem(
    rule: UrlRule,
    index: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rule-item-$index"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rule ${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("rule-title-$index")
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit-rule-$index")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit rule",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("delete-rule-$index")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete rule",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Rule description
            Text(
                text = rule.getDescription(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("rule-description-$index")
            )
            
            // Rule details
            RuleDetailsSection(rule = rule, index = index)
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Rule") },
            text = { 
                Text("Are you sure you want to delete this rule? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm-delete-$index")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.testTag("cancel-delete-$index")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("delete-dialog-$index")
        )
    }
}

@Composable
private fun RuleDetailsSection(
    rule: UrlRule,
    index: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Host pattern
        RuleDetailRow(
            label = "Host:",
            value = getHostDisplayText(rule),
            testTag = "rule-host-$index"
        )
        
        // Remove patterns
        if (rule.then.remove.isNotEmpty()) {
            RuleDetailRow(
                label = "Removes:",
                value = rule.then.remove.joinToString(", ") { it.pattern },
                testTag = "rule-removes-$index"
            )
        }
        
        // Schemes
        rule.when_.schemes?.let { schemes ->
            RuleDetailRow(
                label = "Schemes:",
                value = schemes.joinToString(", ") { it.uppercase() },
                testTag = "rule-schemes-$index"
            )
        }
        
        // Warning settings
        rule.then.warn?.let { warn ->
            val warningParts = mutableListOf<String>()
            if (warn.warnOnEmbeddedCredentials == true) {
                warningParts.add("credentials")
            }
            warn.sensitiveParams?.let { params ->
                if (params.isNotEmpty()) {
                    warningParts.add("sensitive params: ${params.joinToString(", ")}")
                }
            }
            if (warningParts.isNotEmpty()) {
                RuleDetailRow(
                    label = "Warns on:",
                    value = warningParts.joinToString(", "),
                    testTag = "rule-warnings-$index"
                )
            }
        }
    }
}

@Composable
private fun RuleDetailRow(
    label: String,
    value: String,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.widthIn(min = 60.dp)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .testTag(testTag),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Get display text for host pattern with proper multiple domain support
 */
private fun getHostDisplayText(rule: UrlRule): String {
    return when (val domains = rule.when_.host.domains) {
        is com.gologlu.detracktor.application.types.Domains.Any -> "All domains (*)"
        is com.gologlu.detracktor.application.types.Domains.ListOf -> {
            val domainList = domains.values.filter { it.isNotBlank() }
            if (domainList.isEmpty()) return "No domains specified"
            
            val subdomainPrefix = when (rule.when_.host.subdomains) {
                is com.gologlu.detracktor.application.types.Subdomains.Any -> "*."
                is com.gologlu.detracktor.application.types.Subdomains.None -> ""
                is com.gologlu.detracktor.application.types.Subdomains.OneOf -> {
                    val labels = rule.when_.host.subdomains.labels.filter { it.isNotBlank() }
                    if (labels.isEmpty()) "" else "${labels.joinToString("|")}."
                }
                null -> ""
            }
            
            // Format domains with subdomain prefix
            val formattedDomains = domainList.map { domain ->
                if (subdomainPrefix.isNotEmpty()) {
                    "$subdomainPrefix$domain"
                } else {
                    domain
                }
            }
            
            // Show multiple domains appropriately
            when {
                formattedDomains.size == 1 -> formattedDomains.first()
                formattedDomains.size <= 3 -> formattedDomains.joinToString(", ")
                else -> "${formattedDomains.take(2).joinToString(", ")}, +${formattedDomains.size - 2} more"
            }
        }
    }
}

/**
 * Compact version of rule list item for smaller spaces
 */
@Composable
fun CompactRuleListItem(
    rule: UrlRule,
    index: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("compact-rule-item-$index"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = rule.getDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("compact-rule-description-$index")
                )
                
                Text(
                    text = getHostDisplayText(rule),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("compact-rule-host-$index")
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("compact-edit-rule-$index")
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit rule",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("compact-delete-rule-$index")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete rule",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
