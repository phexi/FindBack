package com.itemfinder.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ItemEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN location TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate table to satisfy Room schema hash validation
                // The TypeConverter handles List<String> <-> JSON TEXT transparently
                // Convert existing single-path data to JSON array format
                db.execSQL("""
                    CREATE TABLE items_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        itemPhotoPath TEXT NOT NULL,
                        locationPhotoPath TEXT NOT NULL DEFAULT '',
                        location TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO items_new (id, name, itemPhotoPath, locationPhotoPath, location, createdAt)
                    SELECT id, name, itemPhotoPath,
                        CASE
                            WHEN locationPhotoPath IS NOT NULL AND locationPhotoPath != '' AND locationPhotoPath NOT LIKE '[%'
                            THEN '[\"' || REPLACE(locationPhotoPath, '\', '\\') || '\"]'
                            WHEN locationPhotoPath IS NOT NULL AND locationPhotoPath != ''
                            THEN locationPhotoPath
                            ELSE '[]'
                        END,
                        location, createdAt
                    FROM items
                """)
                db.execSQL("DROP TABLE items")
                db.execSQL("ALTER TABLE items_new RENAME TO items")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "itemfinder_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
