package com.example.password_manager.ui

import android.app.Application
import android.widget.Toast
import com.example.password_manager.utils.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.jakewharton.threetenabp.AndroidThreeTen

class MyApp : Application() {
    private var authCheckJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        startPeriodicAuthCheck()
        AndroidThreeTen.init(this)
    }

    private fun startPeriodicAuthCheck() {
        if (authCheckJob?.isActive == true) return

        authCheckJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(300_000)
                val valid = AuthManager.validateToken(applicationContext)

                if (!valid) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Session expired", Toast.LENGTH_SHORT).show()
                        AuthManager.logout(applicationContext)
                    }
                    break
                }
            }
        }
    }
}