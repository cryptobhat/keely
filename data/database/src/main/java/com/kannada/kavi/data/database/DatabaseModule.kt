package com.kannada.kavi.data.database

import android.content.Context
import androidx.room.Room
import com.kannada.kavi.data.database.dao.AnalyticsDao
import com.kannada.kavi.data.database.dao.ClipboardDao
import com.kannada.kavi.data.database.dao.UserHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 *
 * This module provides:
 * - KaviDatabase singleton instance
 * - All DAO instances (UserHistoryDao, ClipboardDao, AnalyticsDao)
 *
 * The database is created with:
 * - Schema export enabled for migration testing
 * - Migration support (add migrations as database evolves)
 * - Fallback to destructive migration during development (remove in production)
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the singleton instance of KaviDatabase.
     *
     * Configuration:
     * - Schema location: app_root/data/database/schemas/
     * - Migration strategy: Destructive migration for v1 (development only)
     * - Future migrations should be added via .addMigrations()
     *
     * @param context Application context
     * @return KaviDatabase singleton
     */
    @Provides
    @Singleton
    fun provideKaviDatabase(
        @ApplicationContext context: Context
    ): KaviDatabase {
        return Room.databaseBuilder(
            context,
            KaviDatabase::class.java,
            KaviDatabase.DATABASE_NAME
        )
            // Add migrations here as database schema evolves
            // Example: .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            // .addMigrations(Migrations.MIGRATION_1_2)

            // Fallback to destructive migration for development
            // IMPORTANT: Remove this in production or add proper migrations
            .fallbackToDestructiveMigration()

            // Enable multi-instance invalidation for testing
            .enableMultiInstanceInvalidation()

            .build()
    }

    /**
     * Provides UserHistoryDao for dependency injection.
     * Used by UserHistoryRepository to access user typing history.
     */
    @Provides
    @Singleton
    fun provideUserHistoryDao(database: KaviDatabase): UserHistoryDao {
        return database.userHistoryDao()
    }

    /**
     * Provides ClipboardDao for dependency injection.
     * Used by ClipboardRepository to access clipboard history.
     */
    @Provides
    @Singleton
    fun provideClipboardDao(database: KaviDatabase): ClipboardDao {
        return database.clipboardDao()
    }

    /**
     * Provides AnalyticsDao for dependency injection.
     * Used by AnalyticsRepository to queue analytics events.
     */
    @Provides
    @Singleton
    fun provideAnalyticsDao(database: KaviDatabase): AnalyticsDao {
        return database.analyticsDao()
    }
}
