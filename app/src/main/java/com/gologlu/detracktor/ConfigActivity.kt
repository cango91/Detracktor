package com.gologlu.detracktor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gologlu.detracktor.data.CleaningRule
import com.gologlu.detracktor.data.PatternType
import com.gologlu.detracktor.data.RulePriority
import com.gologlu.detracktor.data.SecurityConfig
import com.gologlu.detracktor.ui.theme.DetracktorTheme
import com.gologlu.detracktor.utils.RegexValidator

class ConfigActivity : ComponentActivity() {
    
    private lateinit var configManager: ConfigManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        configManager = ConfigManager(this)
        
        enableEdgeToEdge()
        setContent {
            DetracktorTheme {
                ConfigScreen(
                    configManager = configManager,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    configManager: ConfigManager,
    onBackPressed: () -> Unit
) {
    var config by remember { mutableStateOf(configManager.loadConfig()) }
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<CleaningRule?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!config.removeAllParams) {
                FloatingActionButton(
                    onClick = { showAddRuleDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Rule")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Cleaning Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Remove all parameters",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (config.removeAllParams) "All query parameters will be removed" 
                                          else "Use rules below",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = config.removeAllParams,
                                onCheckedChange = { checked ->
                                    config = config.copy(removeAllParams = checked)
                                    configManager.saveConfig(config)
                                }
                            )
                        }
                    }
                }
            }
            
            if (!config.removeAllParams) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cleaning Rules",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedButton(
                                    onClick = {
                                        configManager.resetToDefault()
                                        config = configManager.loadConfig()
                                    }
                                ) {
                                    Text("Reset to Default")
                                }
                            }
                            
                            Text(
                                text = "Rules define which parameters to remove with hierarchical priority matching",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
                
                items(config.rules.sortedBy { it.priority.level }) { rule ->
                    RuleCard(
                        rule = rule,
                        onEdit = { editingRule = it },
                        onDelete = { ruleToDelete ->
                            config = config.copy(rules = config.rules - ruleToDelete)
                            configManager.saveConfig(config)
                        },
                        onToggleEnabled = { ruleToToggle ->
                            val updatedRules = config.rules.map { 
                                if (it == ruleToToggle) it.copy(enabled = !it.enabled) else it 
                            }
                            config = config.copy(rules = updatedRules)
                            configManager.saveConfig(config)
                        }
                    )
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "How to Use",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = """
                                • Share a URL to this app
                                • Use the Quick Settings tile
                                • Add the home screen widget
                                • Open the app and tap "Clean Clipboard URL"
                                
                                Features:
                                • Hierarchical rule matching (exact hosts override wildcards)
                                • International domain name support
                                • Regex and path-specific patterns
                                • Rule priorities and validation
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
    
    if (showAddRuleDialog) {
        RuleEditDialog(
            rule = null,
            onDismiss = { showAddRuleDialog = false },
            onSave = { newRule ->
                config = config.copy(rules = config.rules + newRule)
                configManager.saveConfig(config)
                showAddRuleDialog = false
            }
        )
    }
    
    editingRule?.let { rule ->
        RuleEditDialog(
            rule = rule,
            onDismiss = { editingRule = null },
            onSave = { updatedRule ->
                val updatedRules = config.rules.map { 
                    if (it == rule) updatedRule else it 
                }
                config = config.copy(rules = updatedRules)
                configManager.saveConfig(config)
                editingRule = null
            }
        )
    }
}

@Composable
fun RuleCard(
    rule: CleaningRule,
    onEdit: (CleaningRule) -> Unit,
    onDelete: (CleaningRule) -> Unit,
    onToggleEnabled: (CleaningRule) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.hostPattern,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (rule.enabled) MaterialTheme.colorScheme.onSurface 
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getPriorityDisplayName(rule.priority),
                            style = MaterialTheme.typography.labelSmall,
                            color = getPriorityColor(rule.priority)
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = getPatternTypeDisplayName(rule.patternType),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rule.enabled,
                        onCheckedChange = { onToggleEnabled(rule) }
                    )
                    IconButton(onClick = { onEdit(rule) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
            
            Text(
                text = "Removes: ${rule.params.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            rule.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Rule") },
            text = { Text("Are you sure you want to delete this rule for ${rule.hostPattern}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(rule)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RuleEditDialog(
    rule: CleaningRule?,
    onDismiss: () -> Unit,
    onSave: (CleaningRule) -> Unit
) {
    var hostPattern by remember { mutableStateOf(rule?.hostPattern ?: "") }
    var params by remember { mutableStateOf(rule?.params?.joinToString(", ") ?: "") }
    var priority by remember { mutableStateOf(rule?.priority ?: RulePriority.EXACT_HOST) }
    var patternType by remember { mutableStateOf(rule?.patternType ?: PatternType.WILDCARD) }
    var enabled by remember { mutableStateOf(rule?.enabled ?: true) }
    var description by remember { mutableStateOf(rule?.description ?: "") }
    var showPriorityDropdown by remember { mutableStateOf(false) }
    var showPatternTypeDropdown by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (rule == null) "Add New Rule" else "Edit Rule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = hostPattern,
                    onValueChange = { 
                        hostPattern = it
                        validationError = null
                    },
                    label = { Text("Host Pattern") },
                    placeholder = { Text("example.com or *.example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = validationError != null
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = params,
                    onValueChange = { params = it },
                    label = { Text("Parameters to Remove") },
                    placeholder = { Text("utm_source, utm_medium, fbclid") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Priority Dropdown
                Box {
                    OutlinedTextField(
                        value = getPriorityDisplayName(priority),
                        onValueChange = { },
                        label = { Text("Priority") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            TextButton(onClick = { showPriorityDropdown = true }) {
                                Text("Change")
                            }
                        }
                    )
                    
                    DropdownMenu(
                        expanded = showPriorityDropdown,
                        onDismissRequest = { showPriorityDropdown = false }
                    ) {
                        RulePriority.values().forEach { priorityOption ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(getPriorityDisplayName(priorityOption))
                                        Text(
                                            text = getPriorityDescription(priorityOption),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    priority = priorityOption
                                    showPriorityDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Pattern Type Dropdown
                Box {
                    OutlinedTextField(
                        value = getPatternTypeDisplayName(patternType),
                        onValueChange = { },
                        label = { Text("Pattern Type") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            TextButton(onClick = { showPatternTypeDropdown = true }) {
                                Text("Change")
                            }
                        }
                    )
                    
                    DropdownMenu(
                        expanded = showPatternTypeDropdown,
                        onDismissRequest = { showPatternTypeDropdown = false }
                    ) {
                        PatternType.values().forEach { typeOption ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(getPatternTypeDisplayName(typeOption))
                                        Text(
                                            text = getPatternTypeDescription(typeOption),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    patternType = typeOption
                                    showPatternTypeDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Brief description of this rule") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rule Enabled")
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }
                
                validationError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val validation = validateRule(hostPattern, params, patternType)
                            if (validation != null) {
                                validationError = validation
                            } else {
                                val paramsList = params.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                val newRule = CleaningRule(
                                    hostPattern = hostPattern.trim(),
                                    params = paramsList,
                                    priority = priority,
                                    patternType = patternType,
                                    enabled = enabled,
                                    description = description.takeIf { it.isNotBlank() }
                                )
                                onSave(newRule)
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun getPriorityColor(priority: RulePriority): Color {
    return when (priority) {
        RulePriority.EXACT_HOST -> Color(0xFF4CAF50)
        RulePriority.SUBDOMAIN_WILDCARD -> Color(0xFF2196F3)
        RulePriority.PATH_SPECIFIC -> Color(0xFFFF9800)
        RulePriority.GLOBAL_WILDCARD -> Color(0xFF9C27B0)
    }
}

fun getPriorityDisplayName(priority: RulePriority): String {
    return when (priority) {
        RulePriority.EXACT_HOST -> "Exact Host"
        RulePriority.SUBDOMAIN_WILDCARD -> "Subdomain Wildcard"
        RulePriority.PATH_SPECIFIC -> "Path Specific"
        RulePriority.GLOBAL_WILDCARD -> "Global Wildcard"
    }
}

fun getPriorityDescription(priority: RulePriority): String {
    return when (priority) {
        RulePriority.EXACT_HOST -> "Matches exact domain (example.com)"
        RulePriority.SUBDOMAIN_WILDCARD -> "Matches subdomains (*.example.com)"
        RulePriority.PATH_SPECIFIC -> "Matches specific paths (example.com/path/*)"
        RulePriority.GLOBAL_WILDCARD -> "Matches all domains (*)"
    }
}

fun getPatternTypeDisplayName(patternType: PatternType): String {
    return when (patternType) {
        PatternType.EXACT -> "Exact Match"
        PatternType.WILDCARD -> "Wildcard"
        PatternType.REGEX -> "Regular Expression"
        PatternType.PATH_PATTERN -> "Path Pattern"
    }
}

fun getPatternTypeDescription(patternType: PatternType): String {
    return when (patternType) {
        PatternType.EXACT -> "Exact string matching"
        PatternType.WILDCARD -> "Simple wildcard patterns (*.domain.com)"
        PatternType.REGEX -> "Full regular expression support"
        PatternType.PATH_PATTERN -> "URL path pattern matching"
    }
}

fun validateRule(hostPattern: String, params: String, patternType: PatternType): String? {
    if (hostPattern.isBlank()) {
        return "Host pattern cannot be empty"
    }
    
    if (params.isBlank()) {
        return "Parameters cannot be empty"
    }
    
    // Secure validation for regex patterns using RegexValidator
    if (patternType == PatternType.REGEX) {
        val regexValidator = RegexValidator()
        val securityConfig = SecurityConfig()
        
        val validationResult = regexValidator.validateRegexSafety(hostPattern)
        
        if (!validationResult.isSafe) {
            return when {
                validationResult.errorMessage != null -> "Invalid regex: ${validationResult.errorMessage}"
                validationResult.riskLevel == com.gologlu.detracktor.utils.RegexRiskLevel.HIGH -> "⚠️ High ReDoS Risk - pattern may cause performance issues"
                validationResult.riskLevel == com.gologlu.detracktor.utils.RegexRiskLevel.CRITICAL -> "⚠️ Critical ReDoS Risk - pattern rejected for security"
                else -> "Unsafe regex pattern detected"
            }
        }
        
        // Additional validation suggestions
        if (validationResult.suggestions.isNotEmpty()) {
            return "Suggestion: ${validationResult.suggestions.first()}"
        }
    }
    
    // Basic validation for wildcard patterns
    if (patternType == PatternType.WILDCARD && hostPattern.contains("*")) {
        if (!hostPattern.startsWith("*.") && hostPattern != "*") {
            return "Wildcard patterns should start with '*.' or be just '*'"
        }
    }
    
    // Validate pattern length
    if (hostPattern.length > 500) {
        return "Pattern too long (max 500 characters)"
    }
    
    return null
}
