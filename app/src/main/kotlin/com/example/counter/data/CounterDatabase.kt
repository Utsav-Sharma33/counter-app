package com.example.counter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CounterEntity::class], version = 2, exportSchema = true)
abstract class CounterDatabase : RoomDatabase() {
    abstract fun counterDao(): CounterDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE counters ADD COLUMN normalizedName TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    """
                    UPDATE counters
                    SET normalizedName = CASE
                        WHEN TRIM(name) = '' THEN '__legacy_empty__:' || id
                        ELSE LOWER(TRIM(name))
                    END
                    """.trimIndent()
                )
                // Preserve all legacy rows, including any duplicate names that may
                // have been created by concurrent operations in the old app.
                database.execSQL(
                    """
                    UPDATE counters
                    SET normalizedName = normalizedName || '#legacy-' || id
                    WHERE id NOT IN (
                        SELECT MIN(id)
                        FROM counters
                        GROUP BY LOWER(TRIM(name))
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_counters_normalizedName " +
                        "ON counters(normalizedName)"
                )
            }
        }

        @Volatile
        private var INSTANCE: CounterDatabase? = null

        fun getDatabase(context: Context): CounterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CounterDatabase::class.java,
                    "counter_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
