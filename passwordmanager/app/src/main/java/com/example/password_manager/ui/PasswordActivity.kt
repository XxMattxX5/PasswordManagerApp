package com.example.password_manager.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.password_manager.BuildConfig
import com.example.password_manager.R
import com.example.password_manager.utils.AuthManager
import com.example.password_manager.utils.EncryptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class PasswordActivity: BaseActivity() {

    private lateinit var passwordId: String

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password)

        passwordId = intent.getStringExtra("PASSWORD_ID") ?: throw IllegalArgumentException("PASSWORD_ID is missing")

        fetchPasswordFromBackend(passwordId)

        val updatePasswordBtn = findViewById<Button>(R.id.button5)
        val deletePasswordBtn = findViewById<Button>(R.id.button6)

        updatePasswordBtn.setOnClickListener {

            val accountName = findViewById<EditText>(R.id.editTextText3).text.toString()
            val username = findViewById<EditText>(R.id.editTextText4).text.toString()
            val password = findViewById<EditText>(R.id.editTextTextPassword2).text.toString()

            sendUpdatePasswordRequest(passwordId,accountName,username,password)

        }

        deletePasswordBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this password?")
                .setPositiveButton("Delete") { _, _ ->
                    deletePassword(passwordId)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        val passwordInput = findViewById<EditText>(R.id.editTextTextPassword2)

        val passwordVisible = booleanArrayOf(false)

        passwordInput.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                val drawable = passwordInput.compoundDrawables[drawableEnd]
                if (drawable != null) {
                    val bounds = drawable.bounds
                    val x = event.x.toInt()
                    val width = passwordInput.width

                    if (x >= width - bounds.width() - passwordInput.paddingEnd) {
                        passwordVisible[0] = !passwordVisible[0]
                        if (passwordVisible[0]) {
                            passwordInput.transformationMethod = HideReturnsTransformationMethod.getInstance()
                            passwordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.visibility_eye, 0)
                        } else {
                            passwordInput.transformationMethod = PasswordTransformationMethod.getInstance()
                            passwordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.visibility_off_eye, 0)
                        }
                        passwordInput.setSelection(passwordInput.text.length)
                        passwordInput.performClick()
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
                val valid = AuthManager.validateToken(this@PasswordActivity)
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

    private fun fetchPasswordFromBackend(id: String) {
        val authToken = AuthManager.getToken(this)
        val baseUrl = BuildConfig.BASE_URL

        val accountNameInput = findViewById<EditText>(R.id.editTextText3)
        val usernameInput = findViewById<EditText>(R.id.editTextText4)
        val passwordInput = findViewById<EditText>(R.id.editTextTextPassword2)

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$baseUrl/passwords/$id/")
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        var (name, username, password) = parseResponse(body)

                        val encryptionKey = EncryptionManager.retrieveDerivedKey(this@PasswordActivity)


                        if (encryptionKey != null) {
                            password = EncryptionManager.decryptPassword(password, encryptionKey)
                        } else {
                            Toast.makeText(this@PasswordActivity, "Error occurred during password decryption", Toast.LENGTH_SHORT).show()
                            AuthManager.logout(this@PasswordActivity)
                        }

                        withContext(Dispatchers.Main) {
                            accountNameInput.setText(name)
                            usernameInput.setText(username)
                            passwordInput.setText(password)
                        }

                    }
                } else {
                    if (response.code == 401) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@PasswordActivity, "Session Expired", Toast.LENGTH_SHORT).show()
                            AuthManager.logout(this@PasswordActivity)
                        }
                    }

                    Log.e("PasswordActivity", "Error response: ${response.code}")


                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun parseResponse(body: String): Triple<String, String, String> {
        val json = JSONObject(body)
        val data = json.getJSONObject("data")

        val name = data.getString("name")
        val username = data.getString("username")
        val password = data.getString("password")

        return Triple(name, username, password)
    }


    private fun sendUpdatePasswordRequest(
        id:String,
        accountName: String,
        username: String,
        password: String,

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
            Toast.makeText(this@PasswordActivity, "Error occurred during password encryption", Toast.LENGTH_SHORT).show()
            return
        }

        val formBodyBuilder = FormBody.Builder()
            .add("name", accountName)
            .add("username", username)
            .add("password", encryptedPassword)


        val formBody = formBodyBuilder.build()

        val request = Request.Builder()
            .url("$baseUrl/passwords/$id/")
            .addHeader("Authorization", "Bearer $authToken")
            .put(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@PasswordActivity, "Failed To Update: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@PasswordActivity, "Password updated successfully!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@PasswordActivity, PasswordListActivity::class.java)
                        startActivity(intent)

                        finish()
                    }
                } else if (response.code == 401) {
                    runOnUiThread {
                        Toast.makeText(this@PasswordActivity, "Session Expired", Toast.LENGTH_SHORT).show()
                        AuthManager.logout(this@PasswordActivity)
                    }
                } else {
                    val errorBody = response.body?.string()
                    runOnUiThread {
                        Toast.makeText(this@PasswordActivity, "Failed to update password: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun deletePassword(id: String) {
        val authToken = AuthManager.getToken(this)
        val baseUrl = BuildConfig.BASE_URL


        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$baseUrl/passwords/$id/")
            .addHeader("Authorization", "Bearer $authToken")
            .delete()
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@PasswordActivity, "Password Deleted", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@PasswordActivity, PasswordListActivity::class.java)
                            startActivity(intent)

                            finish()
                        }


                } else {
                    if (response.code == 401) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@PasswordActivity, "Session Expired", Toast.LENGTH_SHORT).show()
                            AuthManager.logout(this@PasswordActivity)
                        }
                    }

                    Log.e("PasswordActivity", "Error response: ${response.code}")


                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}