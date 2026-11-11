package com.example.counter.data

import android.content.ContentValues
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

                // Normalize in Kotlin so migration behavior matches new writes for
                // non-ASCII names as well as ASCII names.
                val seenNames = mutableSetOf<String>()
                database.query("SELECT id, name FROM counters ORDER BY id").use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow("id")
                    val nameColumn = cursor.getColumnIndexOrThrow("name")
                    while (cursor.moveToNext()) {
                        val id = cursor.getInt(idColumn)
                        val name = cursor.getString(nameColumn)
                        val normalized = CounterNameNormalizer.normalize(name)
                        val uniqueKey = if (seenNames.add(normalized)) {
                            normalized
                        } else {
                            "$normalized#legacy-$id"
                        }
                        val values = ContentValues().apply {
                            put("normalizedName", uniqueKey)
                        }
                        database.update(
                            "counters",
                            androidx.sqlite.db.SupportSQLiteDatabase.CONFLICT_ABORT,
                            values,
                            "id = ?",
                            arrayOf(id.toString())
                        )
                    }
                }

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
