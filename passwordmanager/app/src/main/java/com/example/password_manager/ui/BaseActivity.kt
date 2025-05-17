package com.example.password_manager.ui

import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.password_manager.R
import com.example.password_manager.utils.AuthManager
import kotlinx.coroutines.launch


abstract class BaseActivity : AppCompatActivity() {

    override fun setContentView(layoutResID: Int) {
        // Inflate a base layout with a top bar
        val fullView = layoutInflater.inflate(R.layout.activity_base, null)
        val activityContainer: FrameLayout = fullView.findViewById(R.id.activity_content)
        layoutInflater.inflate(layoutResID, activityContainer, true)
        super.setContentView(fullView)

        val logoutButton = findViewById<Button>(R.id.logoutButton)

        logoutButton.setOnClickListener {
            AuthManager.logout(this)
            updateTopBarVisibility()
        }

        lifecycleScope.launch {
            setLogoutVisibility()
        }
    }

    private suspend fun setLogoutVisibility() {
        if (AuthManager.isLogged == null) {
            AuthManager.validateToken(this@BaseActivity)
            updateTopBarVisibility()

        } else {
            updateTopBarVisibility()
        }
    }


    private fun updateTopBarVisibility() {
        val isLoggedIn = isUserLoggedIn()
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        if (isLoggedIn) {
            logoutButton.visibility = View.VISIBLE

        } else {
            logoutButton.visibility = View.GONE
        }
    }

    fun isUserLoggedIn(): Boolean {
        // Return cached value if available, else false
        return AuthManager.isLogged ?: false
    }

}