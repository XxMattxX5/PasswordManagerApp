package com.example.password_manager.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec





object EncryptionManager {

    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 1

    private const val PREFS_NAME = "secure_prefs"


    // Creates a encryption key given master password and encryption salt
    fun deriveKey(password: String, salt: ByteArray, iterations: Int = 100_000, keyLength: Int = 256): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    // Stores encryption key in android secure storage
    fun storeDerivedKey(context: Context, key: SecretKey) {
        val prefs = getPrefs(context) // your EncryptedSharedPreferences
        val encodedKey = Base64.encodeToString(key.encoded, Base64.DEFAULT)
        prefs.edit() { putString("derived_key", encodedKey) }
    }

    // Retrieves encryption key from storage
    fun retrieveDerivedKey(context: Context): SecretKey? {
        val prefs = getPrefs(context)
        val encodedKey = prefs.getString("derived_key", null) ?: return null
        val keyBytes = Base64.decode(encodedKey, Base64.DEFAULT)
        return SecretKeySpec(keyBytes, "AES")
    }

    // Encrypts plainText using the provided AES key, returns Base64 encoded cipher text including IV
    fun encryptPassword(plainText: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(AES_MODE)

        // Generate random IV
        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)

        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Prepend IV to encrypted bytes (needed for decryption)
        val combined = iv + encryptedBytes

        // Return Base64 string
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    // Decrypts Base64 encoded cipher text (which includes IV), returns original plain text
    fun decryptPassword(encryptedBase64: String, key: SecretKey): String {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)

        // Extract IV and encrypted bytes
        val iv = combined.copyOfRange(0, IV_LENGTH_BYTE)
        val encryptedBytes = combined.copyOfRange(IV_LENGTH_BYTE, combined.size)

        val cipher = Cipher.getInstance(AES_MODE)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    // Creates entry in android secure storage
    private fun getPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Deletes stored encryption key
    fun deleteStoredKey(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit() { remove("derived_key") }
    }
}