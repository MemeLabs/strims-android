package gg.strims.android.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PrivateMessage::class], version = 1, exportSchema = false)
abstract class PrivateMessagesDatabase: RoomDatabase() {

    abstract fun privateMessagesDao(): PrivateMessagesDao

    companion object {
        @Volatile
        private var INSTANCE: PrivateMessagesDatabase? = null

        fun getDatabase(context: Context): PrivateMessagesDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }

            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrivateMessagesDatabase::class.java,
                    "private_messages_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}