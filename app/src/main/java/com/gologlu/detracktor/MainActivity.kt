package com.gologlu.detracktor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gologlu.detracktor.data.AnnotatedUrlSegment
import com.gologlu.detracktor.data.SegmentType
import com.gologlu.detracktor.data.UrlAnalysis
import kotlinx.coroutines.delay

/**
 * Simplified main activity with smart partial-blur URL rendering.
 * Supports both clipboard monitoring mode and share intent pass-through mode.
 */
class MainActivity : ComponentActivity() {
    
    private lateinit var urlCleanerService: UrlCleanerService
    private var isPassThroughMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        urlCleanerService = UrlCleanerService(this)
        
        // Check if launched via share intent (pass-through mode)
        isPassThroughMode = intent?.action == Intent.ACTION_SEND && 
                           intent.type == "text/plain"
        
        setContent {
            DetracktorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isPassThroughMode) {
                        PassThroughMode(intent)
                    } else {
                        ClipboardMonitorMode()
                    }
                }
            }
        }
    }
    
    @Composable
    private fun ClipboardMonitorMode() {
        var currentAnalysis by remember { mutableStateOf<UrlAnalysis?>(null) }
        var isMonitoring by remember { mutableStateOf(false) }
        val context = LocalContext.current
        
        LaunchedEffect(isMonitoring) {
            if (isMonitoring) {
                while (isMonitoring) {
                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = clipboardManager.primaryClip
                    
                    if (clipData != null && clipData.itemCount > 0) {
                        val clipText = clipData.getItemAt(0).text?.toString()
                        if (!clipText.isNullOrBlank() && isValidUrl(clipText)) {
                            val analysis = urlCleanerService.analyzeClipboardContent(clipText)
                            if (analysis != currentAnalysis) {
                                currentAnalysis = analysis
                            }
                        }
                    }
                    
                    delay(1000) // Check clipboard every second
                }
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Detracktor",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Text(
                text = "Privacy-focused URL cleaning",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Monitor toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = isMonitoring,
                    onCheckedChange = { isMonitoring = it }
                )
                Text(
                    text = if (isMonitoring) "Monitoring clipboard" else "Start monitoring",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Analysis display
            currentAnalysis?.let { analysis ->
                UrlAnalysisCard(analysis = analysis)
            } ?: run {
                if (isMonitoring) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Waiting for URL in clipboard...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Service stats
            ServiceStatsCard()
        }
    }
    
    @Composable
    private fun PassThroughMode(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        var analysis by remember { mutableStateOf<UrlAnalysis?>(null) }
        
        LaunchedEffect(sharedText) {
            if (!sharedText.isNullOrBlank() && isValidUrl(sharedText)) {
                analysis = urlCleanerService.analyzeClipboardContent(sharedText)
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "URL Cleaning",
                style = MaterialTheme.typography.headlineMedium
            )
            
            // Analysis display
            analysis?.let { urlAnalysis ->
                UrlAnalysisCard(analysis = urlAnalysis)
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            copyToClipboard(urlAnalysis.cleanedUrl)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Copy Cleaned URL")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            finish()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done")
                    }
                }
            } ?: run {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No valid URL found in shared content",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    private fun UrlAnalysisCard(analysis: UrlAnalysis) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Original URL with smart blurring
                Text(
                    text = "Original URL:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                SelectionContainer {
                    Text(
                        text = buildAnnotatedUrlString(analysis.segments),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                
                // Cleaned URL
                if (analysis.originalUrl != analysis.cleanedUrl) {
                    HorizontalDivider()
                    
                    Text(
                        text = "Cleaned URL:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    SelectionContainer {
                        Text(
                            text = analysis.cleanedUrl,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Warnings
                if (analysis.hasEmbeddedCredentials) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "⚠️ This URL contains embedded credentials",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                // Matching rules
                if (analysis.matchingRules.isNotEmpty()) {
                    Text(
                        text = "Matching rules: ${analysis.matchingRules.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    @Composable
    private fun ServiceStatsCard() {
        val stats = remember { urlCleanerService.getServiceStats() }
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Service Status",
                    style = MaterialTheme.typography.labelMedium
                )
                
                Text(
                    text = "Rules: ${stats.enabledRules}/${stats.totalRules} enabled",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "Cache: ${stats.cacheSize} entries",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    
    private fun buildAnnotatedUrlString(segments: List<AnnotatedUrlSegment>) = buildAnnotatedString {
        segments.forEach { segment ->
            withStyle(
                style = SpanStyle(
                    color = when (segment.type) {
                        SegmentType.PROTOCOL -> Color(0xFF4CAF50)
                        SegmentType.CREDENTIALS -> Color(0xFFFF5722)
                        SegmentType.HOST -> Color(0xFF2196F3)
                        SegmentType.PATH -> Color(0xFF9C27B0)
                        SegmentType.PARAM_NAME -> Color(0xFF607D8B)
                        SegmentType.PARAM_VALUE -> if (segment.shouldBlur) Color(0xFFFF9800) else Color(0xFF795548)
                        SegmentType.SEPARATOR -> Color(0xFF9E9E9E)
                    },
                    textDecoration = if (segment.shouldBlur) TextDecoration.LineThrough else null
                )
            ) {
                append(if (segment.shouldBlur) "●".repeat(segment.text.length.coerceAtMost(10)) else segment.text)
            }
        }
    }
    
    private fun isValidUrl(text: String): Boolean {
        return text.startsWith("http://") || text.startsWith("https://") || text.startsWith("ftp://")
    }
    
    private fun copyToClipboard(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Cleaned URL", text)
        clipboardManager.setPrimaryClip(clipData)
    }
}

@Composable
private fun DetracktorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}
