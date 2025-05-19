package com.example.password_manager.ui

import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.password_manager.R
import com.example.password_manager.utils.AuthManager
import kotlinx.coroutines.launch

import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding


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

        val topBar = findViewById<LinearLayout>(R.id.topBar)
        val originalTopPadding = topBar.paddingTop

        WindowCompat.setDecorFitsSystemWindows(window, false)

        ViewCompat.setOnApplyWindowInsetsListener(fullView) { view, insets ->

            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            // Add top padding for status bar
            topBar.updatePadding(top = originalTopPadding + systemInsets.top)

            // Add bottom padding only if keyboard is visible
            val bottomPadding = if (insets.isVisible(WindowInsetsCompat.Type.ime())) imeInsets.bottom else systemInsets.bottom

            view.updatePadding(
                bottom = bottomPadding,
                left = systemInsets.left,
                right = systemInsets.right
            )

            // Return insets so system can still do default behavior
            insets
        }
        
    }

    // Ensures login status is stored in the isLogged variable before updating top bar
    private suspend fun setLogoutVisibility() {
        if (AuthManager.isLogged == null) {
            AuthManager.validateToken(this@BaseActivity)

        }
        updateTopBarVisibility()
    }

    // Updates top bar visibility based on user's login status
    private fun updateTopBarVisibility() {
        val isLoggedIn = isUserLoggedIn()
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        if (isLoggedIn) {
            logoutButton.visibility = View.VISIBLE

        } else {
            logoutButton.visibility = View.GONE
        }
    }

    // Gets user login status
    private fun isUserLoggedIn(): Boolean {
        // Return cached value if available, else false
        return AuthManager.isLogged ?: false
    }

}