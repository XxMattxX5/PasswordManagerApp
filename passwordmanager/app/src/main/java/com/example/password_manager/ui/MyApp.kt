package com.example.password_manager.ui

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.WorkerParameters
import com.example.password_manager.utils.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.jakewharton.threetenabp.AndroidThreeTen

class MyApp : Application(), DefaultLifecycleObserver {
    private var authCheckJob: Job? = null

    override fun onCreate() {
        super<Application>.onCreate()
        AndroidThreeTen.init(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        // App is foregrounded — start check if not running
        startPeriodicAuthCheck()
    }

    // Starts periodically check auth every 5 minutes
    private fun startPeriodicAuthCheck() {
        if (authCheckJob?.isActive == true) return

        authCheckJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(300_000)

                if (AuthManager.isLogged == false) continue

                val valid = AuthManager.validateToken(applicationContext)

                if (!valid) {
                    withContext(Dispatchers.Main) {
                        AuthManager.logout(applicationContext, true)
                    }
                    break
                }
            }
        }
    }


}