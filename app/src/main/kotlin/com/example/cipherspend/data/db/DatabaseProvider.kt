package com.example.cipherspend.data.db

import android.content.Context
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.SecureRandom
import android.util.Base64

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null
    private const val DB_NAME = "cipherspend_encrypted.db"
    private const val PREFS_NAME = "cipher_secure_vault"
    private const val KEY_DB_PASSPHRASE = "db_passphrase_key"

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: buildDatabase(context.applicationContext).also { instance = it }
        }
    }

    private fun buildDatabase(context: Context): AppDatabase {
        System.loadLibrary("sqlcipher")
        val passphrase = getOrCreatePassphrase(context)
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DB_NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    private fun getOrCreatePassphrase(context: Context): ByteArray {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            var storedKey = sharedPreferences.getString(KEY_DB_PASSPHRASE, null)
            if (storedKey == null) {
                val randomBytes = ByteArray(32)
                SecureRandom().nextBytes(randomBytes)
                storedKey = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
                sharedPreferences.edit().putString(KEY_DB_PASSPHRASE, storedKey).apply()
            }
            Base64.decode(storedKey, Base64.NO_WRAP)
        } catch (e: Exception) {
            // Fallback for hardware keystore edge cases
            val prefs = context.getSharedPreferences("cipher_fallback_vault", Context.MODE_PRIVATE)
            var storedKey = prefs.getString(KEY_DB_PASSPHRASE, null)
            if (storedKey == null) {
                val randomBytes = ByteArray(32)
                SecureRandom().nextBytes(randomBytes)
                storedKey = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
                prefs.edit().putString(KEY_DB_PASSPHRASE, storedKey).apply()
            }
            Base64.decode(storedKey, Base64.NO_WRAP)
        }
    }
}
