package com.mercyshieldplus.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [IntegrityReportEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun integrityDao(): IntegrityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Passphrase mercy — derive from Android Keystore or hardcode dev; production use secure source
        private const val PASSPHRASE = "mercy_shield_plus_eternal_quantum_fortress_2026"  // Change to secure random/device-bound

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphraseBytes = PASSPHRASE.toCharArray()
                val factory = SupportFactory(SQLiteDatabase.getBytes(passphraseBytes))

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mercyshieldplus_database_encrypted"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()  // Dev mercy; add migrations production
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
