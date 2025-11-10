package com.kannada.kavi.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * MigrationTest - Database Migration Tests
 *
 * Tests schema migrations to ensure data integrity across app updates.
 *
 * WHAT ARE MIGRATIONS?
 * ====================
 * When you update your app and change the database schema (add/remove columns, tables, etc.),
 * you need migrations to convert existing user data from old schema to new schema.
 *
 * Without migrations:
 * - User loses all data on app update
 * - App crashes if schema doesn't match
 *
 * With migrations:
 * - User data is preserved
 * - Smooth app updates
 *
 * TEST CATEGORIES:
 * ================
 * 1. Schema Validation
 *    - Version 1 schema matches expected structure
 *    - All tables created correctly
 *    - All indexes created correctly
 *
 * 2. Future Migration Tests (when schema changes)
 *    - Migration 1->2 preserves data
 *    - Migration 2->3 adds new fields with defaults
 *    - etc.
 *
 * HOW TO USE THIS FILE:
 * =====================
 * 1. When you bump database version (e.g., 1 -> 2):
 *    - Add a Migration_1_2 object in Migrations.kt
 *    - Add a test here: testMigration1To2()
 *
 * 2. In the test:
 *    - Create DB with old schema
 *    - Insert sample data
 *    - Run migration
 *    - Verify data is preserved and new schema is correct
 *
 * EXAMPLE:
 * ========
 * ```kotlin
 * @Test
 * fun testMigration1To2() {
 *     // Create DB version 1
 *     helper.createDatabase(TEST_DB, 1).apply {
 *         execSQL("INSERT INTO user_typed_words (word, frequency) VALUES ('test', 5)")
 *         close()
 *     }
 *
 *     // Run migration to version 2
 *     helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
 *
 *     // Verify data
 *     getMigratedRoomDatabase().apply {
 *         val word = userHistoryDao().getWord("test")
 *         assertThat(word).isNotNull()
 *         assertThat(word?.frequency).isEqualTo(5)
 *         close()
 *     }
 * }
 * ```
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    companion object {
        private const val TEST_DB = "migration-test"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KaviDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    // ========================================
    // SCHEMA VALIDATION TESTS (Version 1)
    // ========================================

    @Test
    @Throws(IOException::class)
    fun testSchemaVersion1_allTablesCreated() {
        // Given/When - Create database version 1
        val db = helper.createDatabase(TEST_DB, 1)

        // Then - Verify all tables exist
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table'")
        val tableNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            tableNames.add(cursor.getString(0))
        }
        cursor.close()

        // Should have our 3 tables (+ system tables)
        assertThat(tableNames).contains("user_typed_words")
        assertThat(tableNames).contains("clipboard_items")
        assertThat(tableNames).contains("analytics_events")

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun testSchemaVersion1_userTypedWordsTable() {
        // Given/When
        val db = helper.createDatabase(TEST_DB, 1)

        // Insert test data
        db.execSQL(
            """
            INSERT INTO user_typed_words (word, frequency, last_used, language)
            VALUES ('ಕನ್ನಡ', 10, 12345, 'kannada')
            """
        )

        // Then - Verify data can be read back
        val cursor = db.query("SELECT * FROM user_typed_words WHERE word = 'ಕನ್ನಡ'")
        assertThat(cursor.moveToFirst()).isTrue()
        assertThat(cursor.getColumnIndex("id")).isAtLeast(0)
        assertThat(cursor.getColumnIndex("word")).isAtLeast(0)
        assertThat(cursor.getColumnIndex("frequency")).isAtLeast(0)
        assertThat(cursor.getColumnIndex("last_used")).isAtLeast(0)
        assertThat(cursor.getColumnIndex("language")).isAtLeast(0)

        val word = cursor.getString(cursor.getColumnIndex("word"))
        val frequency = cursor.getInt(cursor.getColumnIndex("frequency"))
        assertThat(word).isEqualTo("ಕನ್ನಡ")
        assertThat(frequency).isEqualTo(10)

        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun testSchemaVersion1_clipboardItemsTable() {
        // Given/When
        val db = helper.createDatabase(TEST_DB, 1)

        // Insert test data
        db.execSQL(
            """
            INSERT INTO clipboard_items (id, text, timestamp, is_pinned, source_app, content_type)
            VALUES ('test-id', 'Test text', 12345, 0, 'TestApp', 'TEXT')
            """
        )

        // Then - Verify data can be read back
        val cursor = db.query("SELECT * FROM clipboard_items WHERE id = 'test-id'")
        assertThat(cursor.moveToFirst()).isTrue()
        assertThat(cursor.getColumnIndex("id")).isAtLeast(0)
        assertThat(cursor.getColumnIndex("text")).isAtLeast(0)
        assertThat(cursor.getColumnIndex("timestamp")).isAtLeast(0)
        assertThat(cursor.getColumnIndex("is_pinned")).isAtLeast(0)
        assertThat(cursor.getColumnIndex("source_app")).isAtLeast(0)
        assertThat(cursor.getColumnIndex("content_type")).isAtLeast(0)

        val text = cursor.getString(cursor.getColumnIndex("text"))
        val isPinned = cursor.getInt(cursor.getColumnIndex("is_pinned"))
        assertThat(text).isEqualTo("Test text")
        assertThat(isPinned).isEqualTo(0)

        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun testSchemaVersion1_analyticsEventsTable() {
        // Given/When
        val db = helper.createDatabase(TEST_DB, 1)

        // Insert test data
        db.execSQL(
            """
            INSERT INTO analytics_events (event_name, timestamp, properties, is_synced, retry_count)
            VALUES ('test_event', 12345, '{}', 0, 0)
            """
        )

        // Then - Verify data can be read back
        val cursor = db.query("SELECT * FROM analytics_events WHERE event_name = 'test_event'")
        assertThat(cursor.moveToFirst()).isTrue()
        assertThat(cursor.getColumnIndex("id")).isAtLeast(0)
        assertThat(cursor.getColumnIndex("event_name")).isAtLeast(0)
        assertThat(cursor.getColumnIndex("timestamp")).isAtLeast(0)
        assertThat(cursor.getColumnIndex("properties")).isAtLeast(0)
        assertThat(cursor.getColumnIndex("is_synced")).isAtLeast(0)
        assertThat(cursor.getColumnIndex("retry_count")).isAtLeast(0)

        val eventName = cursor.getString(cursor.getColumnIndex("event_name"))
        val retryCount = cursor.getInt(cursor.getColumnIndex("retry_count"))
        assertThat(eventName).isEqualTo("test_event")
        assertThat(retryCount).isEqualTo(0)

        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun testSchemaVersion1_indexesCreated() {
        // Given/When
        val db = helper.createDatabase(TEST_DB, 1)

        // Then - Verify indexes exist
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='index'")
        val indexNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            indexNames.add(cursor.getString(0))
        }
        cursor.close()

        // User typed words indexes
        assertThat(indexNames).contains("index_user_typed_words_word")
        assertThat(indexNames).contains("index_user_typed_words_last_used")
        assertThat(indexNames).contains("index_user_typed_words_frequency")
        assertThat(indexNames).contains("index_user_typed_words_language_frequency")

        // Clipboard items indexes
        assertThat(indexNames).contains("index_clipboard_items_timestamp")
        assertThat(indexNames).contains("index_clipboard_items_is_pinned")

        // Analytics events indexes
        assertThat(indexNames).contains("index_analytics_events_is_synced")
        assertThat(indexNames).contains("index_analytics_events_timestamp")
        assertThat(indexNames).contains("index_analytics_events_retry_count")

        db.close()
    }

    // ========================================
    // DATA INTEGRITY TESTS
    // ========================================

    @Test
    @Throws(IOException::class)
    fun testUniqueConstraint_userTypedWords() {
        // Given
        val db = helper.createDatabase(TEST_DB, 1)

        // When - Insert word twice
        db.execSQL("INSERT INTO user_typed_words (word, frequency, last_used, language) VALUES ('ಕನ್ನಡ', 5, 1000, 'kannada')")

        try {
            // This should fail due to unique constraint on 'word'
            db.execSQL("INSERT INTO user_typed_words (word, frequency, last_used, language) VALUES ('ಕನ್ನಡ', 10, 2000, 'kannada')")
            throw AssertionError("Expected unique constraint violation")
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            // Expected - unique constraint should prevent duplicate
            assertThat(e.message).contains("UNIQUE")
        }

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun testPrimaryKey_clipboardItems() {
        // Given
        val db = helper.createDatabase(TEST_DB, 1)

        // When - Insert item with same ID twice
        db.execSQL("INSERT INTO clipboard_items (id, text, timestamp, is_pinned, content_type) VALUES ('same-id', 'Text 1', 1000, 0, 'TEXT')")

        try {
            // This should fail due to primary key constraint
            db.execSQL("INSERT INTO clipboard_items (id, text, timestamp, is_pinned, content_type) VALUES ('same-id', 'Text 2', 2000, 0, 'TEXT')")
            throw AssertionError("Expected primary key constraint violation")
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            // Expected - primary key should prevent duplicate
            assertThat(e.message).contains("PRIMARY KEY")
        }

        db.close()
    }

    // ========================================
    // FUTURE MIGRATION TESTS
    // ========================================

    /**
     * Template for future migration tests.
     *
     * When you create version 2 of the database:
     * 1. Uncomment this test
     * 2. Create MIGRATION_1_2 in Migrations.kt
     * 3. Update the test with actual migration logic
     */
    /*
    @Test
    @Throws(IOException::class)
    fun testMigration1To2() {
        // Given - Create DB version 1 with sample data
        helper.createDatabase(TEST_DB, 1).apply {
            // Insert sample data in version 1 schema
            execSQL("INSERT INTO user_typed_words (word, frequency, last_used, language) VALUES ('test', 5, 1000, 'kannada')")
            execSQL("INSERT INTO clipboard_items (id, text, timestamp, is_pinned, content_type) VALUES ('clip1', 'Text', 1000, 0, 'TEXT')")
            execSQL("INSERT INTO analytics_events (event_name, timestamp, properties, is_synced, retry_count) VALUES ('event1', 1000, '{}', 0, 0)")
            close()
        }

        // When - Run migration to version 2
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Then - Verify data is preserved and new schema is correct
        // Add your verification logic here based on what changed in version 2

        db.close()
    }
    */

    /**
     * Template for testing migration from version 2 to 3
     */
    /*
    @Test
    @Throws(IOException::class)
    fun testMigration2To3() {
        // Given - Create DB version 2 with sample data
        helper.createDatabase(TEST_DB, 2).apply {
            // Insert data in version 2 schema
            close()
        }

        // When - Run migration to version 3
        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        // Then - Verify migration
        db.close()
    }
    */

    /**
     * Test that migration chain works (e.g., 1 -> 2 -> 3)
     */
    /*
    @Test
    @Throws(IOException::class)
    fun testMigrationChain1To3() {
        // Given - Create DB version 1
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO user_typed_words (word, frequency, last_used, language) VALUES ('test', 5, 1000, 'kannada')")
            close()
        }

        // When - Run all migrations in sequence
        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            MIGRATION_1_2,
            MIGRATION_2_3
        )

        // Then - Verify data survived all migrations
        // Add verification logic

        db.close()
    }
    */

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Get Room database after migration for high-level verification
     */
    private fun getMigratedRoomDatabase(): KaviDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.databaseBuilder(
            context,
            KaviDatabase::class.java,
            TEST_DB
        )
            .addMigrations(/* Add your migrations here when you have them */)
            .build()

        // Trigger Room to validate schema
        helper.closeWhenFinished(database)
        return database
    }
}

/**
 * MIGRATION GUIDE
 * ===============
 *
 * When you need to change the database schema:
 *
 * 1. **Increment version in KaviDatabase.kt**
 *    ```kotlin
 *    @Database(
 *        entities = [...],
 *        version = 2, // Changed from 1
 *        exportSchema = true
 *    )
 *    ```
 *
 * 2. **Create migration in Migrations.kt**
 *    ```kotlin
 *    val MIGRATION_1_2 = object : Migration(1, 2) {
 *        override fun migrate(database: SupportSQLiteDatabase) {
 *            // Add new column example:
 *            database.execSQL("ALTER TABLE user_typed_words ADD COLUMN new_field TEXT")
 *        }
 *    }
 *    ```
 *
 * 3. **Add migration to DatabaseModule.kt**
 *    ```kotlin
 *    .addMigrations(MIGRATION_1_2)
 *    ```
 *
 * 4. **Write test in this file**
 *    - Uncomment template above
 *    - Insert data in old schema
 *    - Run migration
 *    - Verify data preserved and new schema correct
 *
 * 5. **Test thoroughly**
 *    - Run all migration tests
 *    - Test on real device with existing data
 *    - Check exported schema files match
 *
 * COMMON MIGRATION OPERATIONS:
 * ============================
 * - Add column: `ALTER TABLE table_name ADD COLUMN column_name TYPE DEFAULT value`
 * - Drop column: Create new table, copy data, drop old, rename new
 * - Add index: `CREATE INDEX index_name ON table_name(column_name)`
 * - Drop index: `DROP INDEX index_name`
 * - Rename table: `ALTER TABLE old_name RENAME TO new_name`
 *
 * TIPS:
 * =====
 * - Always test migrations with real user data patterns
 * - Test migration chains (1->2->3, not just 1->3)
 * - Keep old schema exports for reference
 * - Document why each migration was needed
 * - Consider data size (migrations run on UI thread on old Android versions)
 */
