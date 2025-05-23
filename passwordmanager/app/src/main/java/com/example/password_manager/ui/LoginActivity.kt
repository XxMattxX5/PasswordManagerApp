package com.example.password_manager.ui
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import com.example.password_manager.R
import okhttp3.*
import java.io.IOException
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.password_manager.BuildConfig
import org.json.JSONObject
import org.json.JSONException
import com.example.password_manager.utils.AuthManager
import com.example.password_manager.utils.EncryptionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


class LoginActivity: BaseActivity() {

    override fun shouldEnforceAuth(): Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)  // your layout with editTextPassword

        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)

        val passwordVisible = booleanArrayOf(false)

        val btnSignIn = findViewById<Button>(R.id.button)
        btnSignIn.setOnClickListener {
            performLogin()
        }


        // Checks visibility of password and changes visibility icon on touch
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
//                        editTextPassword.performClick()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

    }

    // On Resume auth status is checked
    // If user is logged in they are navigated to the passwordList page
    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            if (AuthManager.isLogged == null) {
                val valid = AuthManager.validateToken(this@LoginActivity)
                if (valid) {
                    navigateToPasswordList()
                }
            } else if (AuthManager.isLogged == true) {
                navigateToPasswordList()
            }
        }
    }

    // Navigates user to passwordList page
    private fun navigateToPasswordList() {
        val intent = Intent(this, PasswordListActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun performLogin() {
        val username = findViewById<EditText>(R.id.editTextUsername).text.toString().lowercase()
        val password = findViewById<EditText>(R.id.editTextPassword).text.toString()

        // Make network request
        sendLoginRequest(username, password)
    }

    // Sends a login request to the backend API
    private fun sendLoginRequest(username: String, password: String) {
        val btnSignIn = findViewById<Button>(R.id.button)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Sets page into processing state
        btnSignIn.isEnabled = false
        progressBar.visibility = View.VISIBLE

        val baseUrl = BuildConfig.BASE_URL
        val client = OkHttpClient()

        val formBody = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url("$baseUrl/auth/login/")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            // Handles response from login request
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread{
                    btnSignIn.isEnabled = true
                    progressBar.visibility = View.GONE
                }

                // If successful access token and encryption are stored
                // and user is navigated to passwordList page
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val json = JSONObject(body)
                            val token = json.getString("access")
                            val encryptSalt = json.getString("encryption_salt")

                            // Saves Token and updates login status
                            AuthManager.saveToken(this@LoginActivity, token)
                            AuthManager.isLogged = true

                            // Creates and stores encryption key
                            CoroutineScope(Dispatchers.IO).launch {
                                val key = EncryptionManager.deriveKey(
                                    password,
                                    encryptSalt.toByteArray(Charsets.UTF_8)
                                )
                                EncryptionManager.storeDerivedKey(this@LoginActivity, key)
                            }

                            // Outputs success message and navigates user to passwordList page
                            runOnUiThread {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Login successful!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val intent =
                                    Intent(this@LoginActivity, PasswordListActivity::class.java)
                                startActivity(intent)
                                finish()

                            }

                        } catch (e: JSONException) {
                            e.printStackTrace()
                            runOnUiThread {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Response parsing error",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
                    // Displays account locked and lock until time
                    val errorBody = response.body?.string()
                    if (response.code == 403 && errorBody != null) {
                        try {

                            val message = errorBody.trim().removeSurrounding("\"")
                            runOnUiThread {
                                Toast.makeText(
                                    this@LoginActivity,
                                    message,
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                        } catch (e: JSONException) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Login failed: Could not parse server response.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@LoginActivity,
                                "Login failed: username or password are incorrect",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }
            }
        })
    }

}