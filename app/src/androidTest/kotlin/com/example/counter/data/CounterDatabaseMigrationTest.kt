package com.example.counter.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CounterDatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "counter-migration-test"

    @Before
    fun cleanBefore() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun cleanAfter() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationPreservesRowsAndMakesLegacyDuplicateKeysUnique() = runBlocking {
        val legacy = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        legacy.execSQL(
            """
            CREATE TABLE counters (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                count INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        legacy.execSQL(
            "INSERT INTO counters (id, name, count, createdAt, updatedAt) VALUES " +
                "(1, 'Water', 12, 100, 100), " +
                "(2, ' water ', 7, 200, 200), " +
                "(3, 'Steps', 25, 300, 300)"
        )
        legacy.version = 1
        legacy.close()

        val database = Room.databaseBuilder(
            context,
            CounterDatabase::class.java,
            databaseName
        )
            .addMigrations(CounterDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        try {
            val counters = database.counterDao().getAllCountersOnce()
            assertEquals(3, counters.size)
            assertEquals(12, counters.first { it.id == 1 }.count)
            assertEquals(7, counters.first { it.id == 2 }.count)
            assertEquals(25, counters.first { it.id == 3 }.count)
            assertNotEquals(
                counters.first { it.id == 1 }.normalizedName,
                counters.first { it.id == 2 }.normalizedName
            )
            assertEquals("steps", counters.first { it.id == 3 }.normalizedName)
        } finally {
            database.close()
        }
    }
}
