package com.mountaincrab.logrhythm

import android.app.Application
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mountaincrab.logrhythm.data.local.ALL_MIGRATIONS
import com.mountaincrab.logrhythm.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Room migration tests — run on the JVM via Robolectric (no device needed).
 *
 *   ./gradlew :app:testDebugUnitTest
 *
 * Two layers of protection:
 *
 * 1. Schema validation (MigrationTestHelper) — verifies the SQL in each
 *    migration produces the schema Room expects from the @Entity classes.
 *    Catches column type mismatches, missing columns, wrong constraints, etc.
 *
 * 2. Driver signature check (reflection) — verifies every migration overrides
 *    migrate(SQLiteConnection), which is required when using BundledSQLiteDriver.
 *    The base class throws NotImplementedError if this is not overridden, but
 *    MigrationTestHelper uses SupportSQLiteOpenHelper internally and would
 *    silently pass even with the wrong signature.
 *
 * Schema files must be committed alongside migration code. Generate with:
 *   ./gradlew :app:compileDebugKotlinAndroid
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    // --- Schema validation tests ---

    /**
     * Full migration chain from v5 (oldest schema on file) to current version.
     */
    @Test
    fun migrateAllFromV5() {
        helper.createDatabase(DB_NAME, 5).close()
        helper.runMigrationsAndValidate(DB_NAME, AppDatabase.CURRENT_VERSION, true, *ALL_MIGRATIONS)
    }

    /**
     * v6→v7 adds syncStatus/updatedAt to tag tables; existing rows survive.
     */
    @Test
    fun migrate6To7_tagColumnsAddedCorrectly() {
        helper.createDatabase(DB_NAME, 6).apply {
            execSQL("INSERT INTO poop_tags (id, name, isDeleted, sortOrder, createdAt) VALUES ('t1', 'urgent', 0, 1, 1000000)")
            execSQL("INSERT INTO note_tags  (id, name, isDeleted, sortOrder, createdAt) VALUES ('t2', 'mood',   0, 1, 1000000)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 7, true, *ALL_MIGRATIONS)

        db.query("SELECT syncStatus, updatedAt FROM poop_tags WHERE id = 't1'").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals("PENDING", c.getString(0))
            assertEquals(0L, c.getLong(1))
        }
        db.query("SELECT syncStatus, updatedAt FROM note_tags WHERE id = 't2'").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals("PENDING", c.getString(0))
            assertEquals(0L, c.getLong(1))
        }
        db.close()
    }

    // --- Driver signature test ---

    /**
     * Verifies every migration overrides migrate(SQLiteConnection).
     *
     * MigrationTestHelper uses SupportSQLiteOpenHelper internally and calls
     * migrate(SupportSQLiteDatabase), so the schema tests above would pass
     * even if this method is missing. This reflection test catches the gap:
     * if migrate(SQLiteConnection) is not declared on the migration class,
     * the production app (which uses BundledSQLiteDriver) will crash with
     * NotImplementedError the first time a migration runs.
     */
    @Test
    fun allMigrations_overrideSQLiteConnectionMigrate() {
        for (migration in ALL_MIGRATIONS) {
            val method = try {
                migration.javaClass.getDeclaredMethod("migrate", SQLiteConnection::class.java)
            } catch (e: NoSuchMethodException) {
                null
            }
            assertNotNull(
                "Migration ${migration.startVersion}→${migration.endVersion} must override " +
                    "migrate(SQLiteConnection) for BundledSQLiteDriver. " +
                    "Only migrate(SupportSQLiteDatabase) was found.",
                method,
            )
        }
    }

    companion object {
        private const val DB_NAME = "migration_test"
    }
}
