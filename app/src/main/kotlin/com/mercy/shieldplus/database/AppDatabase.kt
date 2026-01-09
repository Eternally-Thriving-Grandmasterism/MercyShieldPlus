package com.mercyshieldplus.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mercyshieldplus.util.SecurePassphraseManager
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [IntegrityReportEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun integrityDao(): IntegrityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Derive passphrase from Keystore-backed secure storage
                val passphrase = SecurePassphraseManager.getPassphrase(context.applicationContext)

                val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase))

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mercyshieldplus_database_encrypted"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()  // Dev mercy; production migrations
                    .build()

                // Zeroize passphrase char[] after DB open (best effort)
                passphrase.fill('\u0000')

                INSTANCE = instance
                instance
            }
        }
    }
}
