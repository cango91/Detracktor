package com.gologlu.detracktor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.gologlu.detracktor.data.CleaningResult
import com.gologlu.detracktor.ui.theme.DetracktorTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var urlCleanerService: UrlCleanerService

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        urlCleanerService = UrlCleanerService(this)
        
        // Handle the intent if this activity was launched with a URL to clean
        handleIntent(intent)
        
        enableEdgeToEdge()
        setContent {
            DetracktorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        urlCleanerService = urlCleanerService,
                        onCleanClipboard = { cleanClipboard() },
                        onOpenConfig = { openConfigActivity() }
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    handleShareIntent(intent)
                }
            }
            "com.gologlu.detracktor.CLEAN_CLIPBOARD" -> {
                // Handle clipboard cleaning request
                cleanClipboard()
                finish()
            }
            Intent.ACTION_MAIN -> {
                // Normal app launch - show UI
            }
            else -> {
                // Handle other intents like shortcuts or tiles
                urlCleanerService.cleanClipboardUrl()
                finish()
            }
        }
    }
    
    private fun handleShareIntent(intent: Intent) {
        val url = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (url.isNullOrEmpty()) {
            // No text in share intent
            showToast(CleaningResult.CLIPBOARD_EMPTY)
            finish()
            return
        }
        
        if (!isValidHttpUrl(url)) {
            // Not a valid URL - don't interfere with the share intent
            showToast(CleaningResult.NOT_A_URL)
            finish()
            return
        }
        
        val cleanedUrl = urlCleanerService.cleanUrl(url)
        
        if (cleanedUrl != url) {
            // URL was cleaned, offer to share the cleaned version
            showShareDialog(cleanedUrl, url)
        } else {
            // No change - return to original share intent instead of canceling
            showToast(CleaningResult.NO_CHANGE)
            // Create share intent with original URL to continue the share flow
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            val chooserIntent = Intent.createChooser(shareIntent, "Share URL")
            startActivity(chooserIntent)
            finish()
        }
    }
    
    private fun showShareDialog(cleanedUrl: String, originalUrl: String) {
        // Create share intent with cleaned URL (no clipboard copy for share intents)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, cleanedUrl)
        }
        
        // Show chooser to let user pick target app
        val chooserIntent = Intent.createChooser(shareIntent, "Share cleaned URL")
        startActivity(chooserIntent)
        
        // Show toast to indicate cleaning happened
        showToast(CleaningResult.CLEANED_AND_COPIED)
        
        // Close this activity
        finish()
    }
    
    private fun isValidHttpUrl(url: String): Boolean {
        return try {
            val parsedUrl = java.net.URL(url)
            parsedUrl.protocol == "http" || parsedUrl.protocol == "https"
        } catch (e: Exception) {
            false
        }
    }
    
    private fun copyToClipboard(text: String) {
        val clipData = android.content.ClipData.newPlainText("Cleaned URL", text)
        val clipboardManager = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboardManager.setPrimaryClip(clipData)
    }
    
    private fun cleanClipboard() {
        val result = urlCleanerService.cleanClipboardUrl()
        // Show toast feedback like in share intent processing
        showToast(result)
    }
    
    private fun showToast(result: CleaningResult) {
        // Safety check: don't show toast if activity is finishing or destroyed
        if (isFinishing || isDestroyed) {
            android.util.Log.d("MainActivity", "Skipping toast - activity is finishing/destroyed")
            return
        }
        
        val message = when (result) {
            CleaningResult.CLIPBOARD_EMPTY -> "Clipboard empty"
            CleaningResult.NOT_A_URL -> "Not a URL"
            CleaningResult.NO_CHANGE -> "No change"
            CleaningResult.CLEANED_AND_COPIED -> "Cleaned"
        }
        
        try {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error showing toast: $message", e)
        }
    }
    
    private fun openConfigActivity() {
        val intent = Intent(this, ConfigActivity::class.java)
        startActivity(intent)
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    urlCleanerService: UrlCleanerService,
    onCleanClipboard: () -> Unit,
    onOpenConfig: () -> Unit
) {
    var clipboardAnalysis by remember { mutableStateOf<com.gologlu.detracktor.data.ClipboardAnalysis?>(null) }
    
    // Update clipboard analysis periodically
    LaunchedEffect(Unit) {
        while (true) {
            clipboardAnalysis = urlCleanerService.analyzeClipboardContent()
            kotlinx.coroutines.delay(1000) // Check every second
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Detracktor",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Clean URLs by removing tracking parameters",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        // Clipboard Preview Card
        clipboardAnalysis?.let { analysis ->
            ClipboardPreviewCard(analysis = analysis)
        }
        
        Button(
            onClick = onCleanClipboard,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Clean Clipboard URL")
        }
        
        Button(
            onClick = onOpenConfig,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Settings")
        }
        
        Text(
            text = "You can also use the Share menu from other apps",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ClipboardPreviewCard(analysis: com.gologlu.detracktor.data.ClipboardAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Clipboard Content",
                style = MaterialTheme.typography.titleMedium
            )
            
            if (!analysis.isValidUrl) {
                Text(
                    text = "Clipboard content is not a valid URI",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = analysis.originalUrl,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                // Show URL with highlighted parameters
                Text(
                    text = "URL Preview:",
                    style = MaterialTheme.typography.labelMedium
                )
                
                Text(
                    text = buildUrlWithHighlights(analysis),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                if (analysis.hasChanges) {
                    Text(
                        text = "Parameters to remove: ${analysis.parametersToRemove.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                    
                    analysis.matchingRule?.let { rule ->
                        Text(
                            text = "Matching rule: $rule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = "No parameters to remove",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Green
                    )
                }
            }
        }
    }
}

@Composable
fun buildUrlWithHighlights(analysis: com.gologlu.detracktor.data.ClipboardAnalysis) = buildAnnotatedString {
    val url = analysis.originalUrl
    val uri = android.net.Uri.parse(url)
    
    // Add base URL
    val baseUrl = "${uri.scheme}://${uri.host}${uri.path ?: ""}"
    append(baseUrl)
    
    // Add query parameters with highlighting
    val queryParams = uri.queryParameterNames
    if (queryParams.isNotEmpty()) {
        append("?")
        queryParams.forEachIndexed { index, param ->
            if (index > 0) append("&")
            
            val value = uri.getQueryParameter(param) ?: ""
            val paramString = if (value.isNotEmpty()) "$param=$value" else param
            
            if (analysis.parametersToRemove.contains(param)) {
                // Highlight parameters to be removed in red
                withStyle(style = SpanStyle(color = Color.Red)) {
                    append(paramString)
                }
            } else {
                // Keep parameters in normal color
                append(paramString)
            }
        }
    }
}
