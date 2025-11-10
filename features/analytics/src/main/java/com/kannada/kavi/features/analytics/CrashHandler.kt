package com.kannada.kavi.features.analytics

import android.content.Context
import android.os.Build
import java.io.PrintWriter
import java.io.StringWriter

/**
 * CrashHandler - Global Exception Handler
 * 
 * Catches uncaught exceptions and reports them to Crashlytics.
 * This ensures all crashes are tracked, even if they're not explicitly caught.
 * 
 * Usage:
 * ```kotlin
 * CrashHandler.install(applicationContext)
 * ```
 */
object CrashHandler : Thread.UncaughtExceptionHandler {
    
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var analyticsManager: AnalyticsManager? = null
    
    /**
     * Install the crash handler
     * Should be called in Application.onCreate()
     * 
     * @param context Application context
     */
    fun install(context: Context) {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        analyticsManager = AnalyticsManager.getInstance(context)
    }
    
    /**
     * Handle uncaught exception
     */
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            // Log crash information
            analyticsManager?.let { manager ->
                val crashInfo = mapOf(
                    "thread_name" to thread.name,
                    "thread_id" to thread.id.toString(),
                    "device_model" to Build.MODEL,
                    "device_manufacturer" to Build.MANUFACTURER,
                    "android_version" to Build.VERSION.SDK_INT.toString(),
                    "android_release" to Build.VERSION.RELEASE,
                    "stack_trace" to getStackTrace(exception)
                )
                
                manager.recordCrash(exception, crashInfo)
                manager.log("Uncaught exception in thread: ${thread.name}")
            }
        } catch (e: Exception) {
            // If crash reporting fails, at least log to system
            android.util.Log.e("CrashHandler", "Failed to report crash", e)
        } finally {
            // Call default handler to show crash dialog
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
    
    /**
     * Get stack trace as string
     */
    private fun getStackTrace(exception: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        exception.printStackTrace(pw)
        return sw.toString()
    }
}

