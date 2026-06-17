package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChatSession
import com.example.data.model.ChatMessage
import com.example.data.model.PendingAction
import com.example.data.model.AppSetting

@Database(
    entities = [
        ChatSession::class,
        ChatMessage::class,
        PendingAction::class,
        AppSetting::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RisoDatabase : RoomDatabase() {
    abstract fun risoDao(): RisoDao

    companion object {
        @Volatile
        private var INSTANCE: RisoDatabase? = null

        fun getDatabase(context: Context): RisoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RisoDatabase::class.java,
                    "riso_local_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
