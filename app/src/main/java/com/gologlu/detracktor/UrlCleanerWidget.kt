package com.gologlu.detracktor

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Home screen widget for URL cleaning
 */
class UrlCleanerWidget : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update all widget instances
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == ACTION_CLEAN_URL) {
            // Handle widget click
            val urlCleanerService = UrlCleanerService(context)
            urlCleanerService.cleanClipboardUrl()
        }
    }
    
    companion object {
        private const val ACTION_CLEAN_URL = "com.example.detracktor.CLEAN_URL"
        
        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // Create RemoteViews for the widget layout
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            
            // Create intent for widget click
            val intent = Intent(context, UrlCleanerWidget::class.java).apply {
                action = ACTION_CLEAN_URL
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Set click listener for the widget
            views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)
            
            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
