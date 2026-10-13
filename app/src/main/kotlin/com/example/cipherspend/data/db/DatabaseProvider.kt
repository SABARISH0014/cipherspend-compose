package com.example.cipherspend.data.db

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.MessageDigest
import java.security.SecureRandom

object DatabaseProvider {
    private const val TAG = "DatabaseProvider"
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

    fun rekeyDatabase(context: Context, newPassphraseSeed: String) {
        synchronized(this) {
            val newKeyBytes = MessageDigest.getInstance("SHA-256").digest(newPassphraseSeed.toByteArray())
            val newKeyEncoded = Base64.encodeToString(newKeyBytes, Base64.NO_WRAP)
            val hexKey = newKeyBytes.joinToString("") { "%02x".format(it) }

            val activeDb = instance ?: getDatabase(context)
            try {
                val writableDb = activeDb.openHelper.writableDatabase
                writableDb.execSQL("PRAGMA rekey = \"x'$hexKey'\";")
                Log.d(TAG, "SQLCipher database rekeyed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to rekey SQLCipher database: ${e.message}", e)
            }

            savePassphrase(context, newKeyEncoded)

            try {
                if (activeDb.isOpen) {
                    activeDb.close()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error closing db after rekey: ${e.message}")
            }
            instance = null
        }
    }

    private fun savePassphrase(context: Context, keyEncoded: String) {
        try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val sharedPreferences = EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences.edit().putString(KEY_DB_PASSPHRASE, keyEncoded).commit()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save rekeyed passphrase to EncryptedSharedPreferences: ${e.message}")
        }

        try {
            context.applicationContext.getSharedPreferences("cipher_fallback_vault", Context.MODE_PRIVATE)
                .edit().putString(KEY_DB_PASSPHRASE, keyEncoded).commit()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save rekeyed passphrase to fallback vault: ${e.message}")
        }
    }

    fun nuclearWipe(context: Context) {
        synchronized(this) {
            instance?.let {
                if (it.isOpen) {
                    it.close()
                }
            }
            instance = null
        }

        // Completely delete the Room database file
        context.applicationContext.deleteDatabase(DB_NAME)

        // Clear EncryptedSharedPreferences (wiping the DB passphrase)
        try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val sharedPreferences = EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences.edit().clear().commit()
        } catch (e: Exception) {
            // In case Keystore is altered or unavailable
        }

        // Clear fallback vault
        try {
            context.applicationContext.getSharedPreferences("cipher_fallback_vault", Context.MODE_PRIVATE)
                .edit().clear().commit()
        } catch (e: Exception) {
            // Ignored
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
