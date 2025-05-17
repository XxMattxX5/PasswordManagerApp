package com.example.password_manager.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.example.password_manager.R
import com.example.password_manager.BuildConfig
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.FormBody
import okhttp3.Callback
import java.io.IOException
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.password_manager.utils.AuthManager
import com.example.password_manager.utils.EncryptionManager
import kotlinx.coroutines.launch
import okhttp3.Response

class CreatePasswordActivity: BaseActivity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_password)

        // Get the folder name from intent extras
        val folderName = intent.getStringExtra("SELECTED_FOLDER_NAME")
        val folderId = intent.getStringExtra("SELECTED_FOLDER_ID")

        // Find the TextView in your layout
        val folderInfoTextView = findViewById<TextView>(R.id.textView18)

        // Set the text or hide if folderName is null/empty
        if (!folderName.isNullOrEmpty()) {
            folderInfoTextView.text = "Creating password in $folderName"
            folderInfoTextView.visibility = View.VISIBLE
        } else {
            folderInfoTextView.visibility = View.GONE
        }

        val btnCreate = findViewById<Button>(R.id.button5)

        val passwordVisible = booleanArrayOf(false)

        btnCreate.setOnClickListener {
            val accountName = findViewById<EditText>(R.id.editTextText3).text.toString()
            val username = findViewById<EditText>(R.id.editTextText4).text.toString()
            val password = findViewById<EditText>(R.id.editTextTextPassword2).text.toString()

            sendCreatePasswordRequest(accountName,username,password,folderId)

        }

        val editTextPassword = findViewById<EditText>(R.id.editTextTextPassword2)

        editTextPassword.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                val drawable = editTextPassword.compoundDrawables[drawableEnd]
                if (drawable != null) {
                    val bounds = drawable.bounds
                    val x = event.x.toInt()
                    val width = editTextPassword.width

                    if (x >= width - bounds.width() - editTextPassword.paddingEnd) {
                        passwordVisible[0] = !passwordVisible[0]
                        if (passwordVisible[0]) {
                            editTextPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                            editTextPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.visibility_eye, 0)
                        } else {
                            editTextPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                            editTextPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.visibility_off_eye, 0)
                        }
                        editTextPassword.setSelection(editTextPassword.text.length)
                        editTextPassword.performClick()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            if (AuthManager.isLogged == null) {
                val valid = AuthManager.validateToken(this@CreatePasswordActivity)
                if (!valid) {
                    navigateToHome()
                }
            } else if (AuthManager.isLogged != true) {
                navigateToHome()
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun sendCreatePasswordRequest(
        accountName: String,
        username: String,
        password: String,
        folderId: String? // nullable
    ) {
        val btnCreate = findViewById<Button>(R.id.button5)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnCreate.isEnabled = false
        progressBar.visibility = View.VISIBLE

        val baseUrl = BuildConfig.BASE_URL
        val client = OkHttpClient()
        val authToken = AuthManager.getToken(this)

        val encryptionKey = EncryptionManager.retrieveDerivedKey(this)

        val encryptedPassword: String = if (encryptionKey != null) {
            EncryptionManager.encryptPassword(password, encryptionKey)
        } else {
            AuthManager.logout(this)
            Toast.makeText(this@CreatePasswordActivity, "Error occurred during password encryption", Toast.LENGTH_SHORT).show()
            return
        }

        val formBodyBuilder = FormBody.Builder()
            .add("accountName", accountName)
            .add("username", username)
            .add("password", encryptedPassword)

        // Only add folderId if not null
        folderId?.let {
            formBodyBuilder.add("folderId", it)
        }

        val formBody = formBodyBuilder.build()

        val request = Request.Builder()
            .url("$baseUrl/passwords/create/")
            .addHeader("Authorization", "Bearer $authToken")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CreatePasswordActivity, "Creation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnCreate.isEnabled = true
                    progressBar.visibility = View.GONE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    btnCreate.isEnabled = true
                    progressBar.visibility = View.GONE
                }

                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@CreatePasswordActivity, "Password created successfully!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@CreatePasswordActivity, PasswordListActivity::class.java)
                        startActivity(intent)

                        finish()  // Close the activity or navigate away
                    }
                } else if (response.code == 401) {
                    runOnUiThread {
                        Toast.makeText(this@CreatePasswordActivity, "Session Expired", Toast.LENGTH_SHORT).show()
                        AuthManager.logout(this@CreatePasswordActivity)
                    }
                } else {
                    val errorBody = response.body?.string()
                    runOnUiThread {
                        Toast.makeText(this@CreatePasswordActivity, "Failed to create password: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}