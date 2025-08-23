package com.example.shareuntracked

import android.service.quicksettings.TileService
import android.service.quicksettings.Tile

/**
 * Quick Settings tile for URL cleaning
 */
class QuickSettingsTileService : TileService() {
    
    private lateinit var urlCleanerService: UrlCleanerService
    
    override fun onCreate() {
        super.onCreate()
        urlCleanerService = UrlCleanerService(this)
    }
    
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }
    
    override fun onClick() {
        super.onClick()
        
        // Clean clipboard URL when tile is clicked
        urlCleanerService.cleanClipboardUrl()
        
        // Update tile state
        updateTile()
    }
    
    private fun updateTile() {
        qsTile?.let { tile ->
            tile.label = "Clean URL"
            tile.contentDescription = "Clean URL from clipboard"
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }
}
