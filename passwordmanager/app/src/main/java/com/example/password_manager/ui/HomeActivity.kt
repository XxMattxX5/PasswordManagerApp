package com.example.password_manager.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.example.password_manager.R
import com.example.password_manager.utils.AuthManager
import kotlinx.coroutines.launch


class HomeActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val signInButton = findViewById<Button>(R.id.button2)
        val signUpButton = findViewById<Button>(R.id.button1)

        signInButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        signUpButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }


    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            if (AuthManager.isLogged == null) {
                val valid = AuthManager.validateToken(this@HomeActivity)
                if (valid) {
                    navigateToPasswordList()
                }
            } else if (AuthManager.isLogged == true) {
                navigateToPasswordList()
            }
        }
    }

    private fun navigateToPasswordList() {
        val intent = Intent(this, PasswordListActivity::class.java)
        startActivity(intent)
        finish()
    }

}