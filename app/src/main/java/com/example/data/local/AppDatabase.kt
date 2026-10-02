package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Album
import com.example.data.model.Document
import com.example.data.model.HiddenAlbum
import com.example.data.model.HiddenHiddenItem
import com.example.data.model.HiddenItem
import com.example.data.model.HiddenTrashItem
import com.example.data.model.PrivateMedia
import com.example.data.model.SecurityActivityLog
import com.example.data.model.TrashItem

@Database(
    entities = [
        Document::class,
        PrivateMedia::class,
        Album::class,
        TrashItem::class,
        HiddenItem::class,
        HiddenAlbum::class,
        HiddenTrashItem::class,
        SecurityActivityLog::class,
        HiddenHiddenItem::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun privateMediaDao(): PrivateMediaDao
    abstract fun albumDao(): AlbumDao
    abstract fun trashDao(): TrashDao
    abstract fun hiddenDao(): HiddenDao
    abstract fun securityLogDao(): SecurityLogDao
    abstract fun hiddenHiddenDao(): HiddenHiddenDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "secure_vault.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
