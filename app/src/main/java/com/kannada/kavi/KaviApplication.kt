package com.kannada.kavi

import android.app.Application
import com.kannada.kavi.features.analytics.AnalyticsManager
import com.kannada.kavi.features.analytics.CrashHandler

/**
 * KaviApplication - Application class for Kavi Keyboard
 * 
 * Initializes global components like analytics and crash reporting.
 * This is called when the app starts, before any activities or services.
 */
class KaviApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize crash handler to catch uncaught exceptions
        CrashHandler.install(this)
        
        // Initialize analytics manager
        // This ensures Firebase is initialized early
        AnalyticsManager.getInstance(this)
    }
}




