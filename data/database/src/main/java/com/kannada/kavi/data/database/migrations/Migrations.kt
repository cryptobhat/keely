package com.kannada.kavi.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations for KaviDatabase.
 *
 * Each migration represents a schema change from one version to another.
 * Migrations ensure that existing user data is preserved when updating the database schema.
 *
 * Migration Strategy:
 * - Always test migrations with MigrationTest before releasing
 * - Export schemas to data/database/schemas/ for reference
 * - Never remove old migrations (users may skip versions)
 * - Document the reason for each migration
 *
 * Current Version: 1
 * Future migrations will be added here as the database schema evolves.
 */
object Migrations {

    /**
     * Example migration from version 1 to version 2.
     * This is a template for future migrations.
     *
     * Usage:
     * ```kotlin
     * Room.databaseBuilder(...)
     *     .addMigrations(MIGRATION_1_2)
     *     .build()
     * ```
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Example: Add a new column to user_typed_words table
            // database.execSQL("ALTER TABLE user_typed_words ADD COLUMN context TEXT")

            // Example: Create a new table
            // database.execSQL("""
            //     CREATE TABLE IF NOT EXISTS user_dictionary (
            //         id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            //         word TEXT NOT NULL,
            //         definition TEXT,
            //         created_at INTEGER NOT NULL
            //     )
            // """)

            // Example: Create an index
            // database.execSQL("CREATE INDEX IF NOT EXISTS index_user_dictionary_word ON user_dictionary(word)")
        }
    }

    /**
     * Example migration from version 2 to version 3.
     * Add more migrations as needed when the schema changes.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Future migration logic here
        }
    }

    /**
     * All migrations list for easy registration.
     * Add new migrations to this array as they are created.
     */
    val ALL_MIGRATIONS = arrayOf<Migration>(
        // MIGRATION_1_2,  // Uncomment when implementing version 2
        // MIGRATION_2_3,  // Uncomment when implementing version 3
    )
}

/**
 * Migration Guidelines:
 *
 * 1. ALTER TABLE - Add columns:
 *    database.execSQL("ALTER TABLE table_name ADD COLUMN column_name TYPE DEFAULT default_value")
 *
 * 2. CREATE TABLE:
 *    database.execSQL("CREATE TABLE IF NOT EXISTS ...")
 *
 * 3. CREATE INDEX:
 *    database.execSQL("CREATE INDEX IF NOT EXISTS index_name ON table_name(column_name)")
 *
 * 4. DROP INDEX:
 *    database.execSQL("DROP INDEX IF EXISTS index_name")
 *
 * 5. Rename Table (requires recreation):
 *    - Create new table with new name
 *    - Copy data from old table
 *    - Drop old table
 *
 * 6. Modify Column (requires table recreation):
 *    - Create temporary table with new schema
 *    - Copy data from old table
 *    - Drop old table
 *    - Rename temporary table
 *
 * Example of complex migration (table recreation):
 * ```kotlin
 * // Create new table
 * database.execSQL("CREATE TABLE user_typed_words_new (...)")
 *
 * // Copy data
 * database.execSQL("""
 *     INSERT INTO user_typed_words_new (id, word, frequency, last_used, language)
 *     SELECT id, word, frequency, last_used, language FROM user_typed_words
 * """)
 *
 * // Drop old table
 * database.execSQL("DROP TABLE user_typed_words")
 *
 * // Rename new table
 * database.execSQL("ALTER TABLE user_typed_words_new RENAME TO user_typed_words")
 *
 * // Recreate indexes
 * database.execSQL("CREATE INDEX ...")
 * ```
 */
