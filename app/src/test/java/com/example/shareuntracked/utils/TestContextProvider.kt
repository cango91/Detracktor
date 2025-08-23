package com.example.shareuntracked.utils

import android.content.ClipboardManager
import android.content.Context
import android.content.res.AssetManager
import org.mockito.Mockito.*
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Test utility class for creating properly configured mock Android contexts
 * and system services for unit testing.
 */
class TestContextProvider {
    
    /**
     * Creates a mock Android Context with basic configuration
     */
    fun createMockContext(): Context {
        val mockContext = mock(Context::class.java)
        val mockFilesDir = mock(File::class.java)
        
        `when`(mockContext.filesDir).thenReturn(mockFilesDir)
        `when`(mockFilesDir.absolutePath).thenReturn("/mock/files")
        
        return mockContext
    }
    
    /**
     * Creates a mock AssetManager that returns the provided JSON content
     * when opening "default_rules.json"
     */
    fun createMockAssetManager(jsonContent: String): AssetManager {
        val mockAssetManager = mock(AssetManager::class.java)
        val inputStream = ByteArrayInputStream(jsonContent.toByteArray())
        
        `when`(mockAssetManager.open("default_rules.json")).thenReturn(inputStream)
        
        return mockAssetManager
    }
    
    /**
     * Creates a mock ClipboardManager for testing clipboard operations
     */
    fun createMockClipboardManager(): ClipboardManager {
        return mock(ClipboardManager::class.java)
    }
    
    /**
     * Sets up a mock context with assets containing the provided JSON content
     */
    fun createMockContextWithAssets(jsonContent: String): Context {
        val mockContext = createMockContext()
        val mockAssetManager = createMockAssetManager(jsonContent)
        
        `when`(mockContext.assets).thenReturn(mockAssetManager)
        
        return mockContext
    }
    
    /**
     * Creates a mock context with clipboard manager
     */
    fun createMockContextWithClipboard(): Context {
        val mockContext = createMockContext()
        val mockClipboardManager = createMockClipboardManager()
        
        `when`(mockContext.getSystemService(Context.CLIPBOARD_SERVICE))
            .thenReturn(mockClipboardManager)
        
        return mockContext
    }
    
    /**
     * Creates a fully configured mock context with both assets and clipboard
     */
    fun createFullMockContext(jsonContent: String): Context {
        val mockContext = createMockContext()
        val mockAssetManager = createMockAssetManager(jsonContent)
        val mockClipboardManager = createMockClipboardManager()
        
        `when`(mockContext.assets).thenReturn(mockAssetManager)
        `when`(mockContext.getSystemService(Context.CLIPBOARD_SERVICE))
            .thenReturn(mockClipboardManager)
        
        return mockContext
    }
}
