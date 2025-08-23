package com.gologlu.detracktor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
                    urlCleanerService.processIntent(intent)
                    // Close the activity after processing
                    finish()
                }
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
    
    private fun cleanClipboard() {
        urlCleanerService.cleanClipboardUrl()
    }
    
    private fun openConfigActivity() {
        val intent = Intent(this, ConfigActivity::class.java)
        startActivity(intent)
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onCleanClipboard: () -> Unit,
    onOpenConfig: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Detracktor",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Clean URLs by removing tracking parameters",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
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
            text = "You can also:\n• Add Quick Settings tile\n• Use Share menu\n• Add home screen widget",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}
