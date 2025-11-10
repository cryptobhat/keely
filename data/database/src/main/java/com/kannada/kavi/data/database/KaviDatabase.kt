package com.kannada.kavi.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kannada.kavi.data.database.converters.TypeConverters as KaviTypeConverters
import com.kannada.kavi.data.database.dao.AnalyticsDao
import com.kannada.kavi.data.database.dao.ClipboardDao
import com.kannada.kavi.data.database.dao.UserHistoryDao
import com.kannada.kavi.data.database.entities.AnalyticsEventEntity
import com.kannada.kavi.data.database.entities.ClipboardItemEntity
import com.kannada.kavi.data.database.entities.UserTypedWordEntity

/**
 * Main Room database for the Kavi Kannada Keyboard.
 *
 * This database stores:
 * - User typing history for improved suggestions
 * - Clipboard history for persistence across sessions
 * - Analytics events for offline queueing before Firebase sync
 *
 * Database version: 1
 * Schema export is enabled for proper migration testing.
 *
 * @see UserTypedWordEntity User's typed words with frequency tracking
 * @see ClipboardItemEntity Clipboard items with pin support
 * @see AnalyticsEventEntity Analytics events with sync status
 */
@Database(
    entities = [
        UserTypedWordEntity::class,
        ClipboardItemEntity::class,
        AnalyticsEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(KaviTypeConverters::class)
abstract class KaviDatabase : RoomDatabase() {

    /**
     * Data Access Object for user typing history.
     * Used by SuggestionEngine to learn and predict user's typing patterns.
     */
    abstract fun userHistoryDao(): UserHistoryDao

    /**
     * Data Access Object for clipboard history.
     * Provides persistent clipboard storage across keyboard sessions.
     */
    abstract fun clipboardDao(): ClipboardDao

    /**
     * Data Access Object for analytics events.
     * Queues analytics events locally before syncing to Firebase.
     */
    abstract fun analyticsDao(): AnalyticsDao

    companion object {
        /**
         * Database name used when creating the database instance.
         */
        const val DATABASE_NAME = "kavi_database"

        /**
         * Database version for migrations.
         * Increment this when schema changes require migration.
         */
        const val DATABASE_VERSION = 1
    }
}
