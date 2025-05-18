package com.example.password_manager.ui


import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.example.password_manager.R
import com.example.password_manager.utils.AuthManager
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    override fun onStart() {
        super.onStart()

        // Checks user auth to determine where to send them to upon enter app
        lifecycleScope.launch {
            if (AuthManager.isLogged == null) {
                val valid = AuthManager.validateToken(this@MainActivity)
                if (valid) {
                    val intent = Intent(this@MainActivity, PasswordListActivity::class.java)
                    startActivity(intent)

                    finish()
                } else {
                    val intent = Intent(this@MainActivity, HomeActivity::class.java)
                    startActivity(intent)

                    finish()
                }
            } else if (AuthManager.isLogged == true) {
                val intent = Intent(this@MainActivity, PasswordListActivity::class.java)
                startActivity(intent)

                finish()
            } else {
                val intent = Intent(this@MainActivity, HomeActivity::class.java)
                startActivity(intent)

                finish()
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}