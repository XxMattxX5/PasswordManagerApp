package com.example.password_manager.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.password_manager.ui.HomeActivity
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import com.example.password_manager.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async



object AuthManager {

    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_TOKEN = "ACCESS_TOKEN"

    var isLogged: Boolean? = null

    @Volatile
    private var validationJob: Deferred<Boolean>? = null

    // Saves access token to android secure store
    fun saveToken(context: Context, token: String) {
        getPrefs(context).edit() { putString(KEY_TOKEN, token) }
    }

    // Gets token from storage
    fun getToken(context: Context): String? {
        return getPrefs(context).getString(KEY_TOKEN, null)
    }

    // Checks if a token is present in storage
    fun isLoggedIn(context: Context): Boolean {
        return getToken(context) != null
    }

    // Deletes access token and encryption key from storage logging out user and navigating them to home page
    fun logout(context: Context, wasLoggedOutDueToExpiration: Boolean = false) {

        getPrefs(context).edit() { remove(KEY_TOKEN) }
        EncryptionManager.deleteStoredKey(context)
        isLogged = false



        val isAppInForeground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(
            Lifecycle.State.STARTED)

        if (isAppInForeground) {
            if (wasLoggedOutDueToExpiration == true) {
                Toast.makeText(context, "Session Expired", Toast.LENGTH_SHORT).show()
            }

            val intent = Intent(context, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)

            if (context is Activity) {
                context.finish()
            }
        }
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

    // Sends request to backend to validate access token
    // Ensures only 1 validation request is sent at a time
    suspend fun validateToken(context: Context): Boolean {

        validationJob?.let {
            return it.await()
        }

        // Otherwise, create a new job for validation
        val newJob = CoroutineScope(Dispatchers.IO).async {
            val url = BuildConfig.BASE_URL
            val token = getToken(context) ?: return@async false

            val client = OkHttpClient()
            val request = Request.Builder()
                .url("$url/auth/validate/")
                .addHeader("Authorization", "Bearer $token")
                .build()

            return@async try {
                val response = client.newCall(request).execute()
                isLogged = response.isSuccessful
                response.isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                isLogged = false
                false
            }
        }

        validationJob = newJob

        try {
            return newJob.await()
        } finally {
            // Clear the job after completion so future calls can trigger a new validation
            validationJob = null
        }
    }




}