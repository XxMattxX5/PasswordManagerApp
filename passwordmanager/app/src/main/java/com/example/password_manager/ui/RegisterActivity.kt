package com.example.password_manager.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.example.password_manager.R
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import okhttp3.Callback
import okhttp3.Response
import com.example.password_manager.BuildConfig
import com.example.password_manager.utils.AuthManager
import kotlinx.coroutines.launch


class RegisterActivity: BaseActivity() {
    override fun shouldEnforceAuth(): Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val createAccountButton = findViewById<Button>(R.id.button3)

        createAccountButton.setOnClickListener {
            val username = findViewById<EditText>(R.id.editTextText).text.toString()
            val email = findViewById<EditText>(R.id.editTextTextEmail).text.toString()
            val password = findViewById<EditText>(R.id.editTextTextPassword).text.toString()
            val passwordConfirm = findViewById<EditText>(R.id.editTextTextPassword1).text.toString()

            registerUser(username, email, password, passwordConfirm)

        }
        val passwordVisible = booleanArrayOf(false)
        val passwordConfirmVisible = booleanArrayOf(false)

        val editTextPassword = findViewById<EditText>(R.id.editTextTextPassword)
        val editTextPassword2 = findViewById<EditText>(R.id.editTextTextPassword1)

        // Changes the visibility of the password field and its associated visibility icon on touch
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

        // Changes the visibility of the confirm password field and its associated visibility icon on touch
        editTextPassword2.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                val drawable = editTextPassword2.compoundDrawables[drawableEnd]
                if (drawable != null) {
                    val bounds = drawable.bounds
                    val x = event.x.toInt()
                    val width =editTextPassword2.width

                    if (x >= width - bounds.width() - editTextPassword2.paddingEnd) {
                        passwordConfirmVisible[0] = !passwordConfirmVisible[0]
                        if (passwordConfirmVisible[0]) {
                            editTextPassword2.transformationMethod = HideReturnsTransformationMethod.getInstance()
                            editTextPassword2.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.visibility_eye, 0)
                        } else {
                            editTextPassword2.transformationMethod = PasswordTransformationMethod.getInstance()
                            editTextPassword2.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.visibility_off_eye, 0)
                        }
                        editTextPassword2.setSelection(editTextPassword2.text.length)
//                        editTextPassword2.performClick()

                        return@setOnTouchListener true
                    }
                }
            }

            false
        }


    }

    // On resume user's auth status is checked
    // If user is no longer logged in they are navigated hom
    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            if (AuthManager.isLogged == null) {
                val valid = AuthManager.validateToken(this@RegisterActivity)
                if (valid) {
                    navigateToPasswordList()
                }
            } else if (AuthManager.isLogged == true) {
                navigateToPasswordList()
            }
        }
    }

    // Navigates user to home page
    private fun navigateToPasswordList() {
        val intent = Intent(this, PasswordListActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Sends a request to backend API to create new user
    private fun registerUser(username: String, email: String, password: String, passwordConfirm: String) {
        val createAccountButton = findViewById<Button>(R.id.button3)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Sets page into processing state
        createAccountButton.isEnabled = false
        progressBar.visibility = View.VISIBLE

        val baseUrl = BuildConfig.BASE_URL
        val client = OkHttpClient()

        val json = JSONObject()
        json.put("username", username)
        json.put("email", email)
        json.put("password", password)
        json.put("password_confirm", passwordConfirm)

        val mediaType = "application/json; charset=utf-8".toMediaType()

        val requestBody = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$baseUrl/auth/register/") // change this to your actual endpoint
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle network error
                Log.e("Register", "Network error", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Show success message to user
                    Handler(Looper.getMainLooper()).post {
                        progressBar.visibility = View.GONE
                        createAccountButton.isEnabled = true
                        Toast.makeText(this@RegisterActivity, "Account Created!", Toast.LENGTH_LONG)
                            .show()
                    }
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                    startActivity(intent)

                    finish()

                } else {
                    // Gets errors from response body and displays them to user
                    val errorBody = response.body?.string()
                    val errorJson = JSONObject(errorBody ?: "{}")

                    val errors = mutableListOf<String>()

                    if (errorJson.has("username")) {
                        val msg = errorJson.getJSONArray("username").join(", ")
                        errors.add("Username: $msg")
                    }

                    if (errorJson.has("email")) {
                        val msg = errorJson.getJSONArray("email").join(", ")
                        errors.add("Email: $msg")
                    }

                    if (errorJson.has("password")) {
                        val msg = errorJson.getJSONArray("password").join(", ")
                        errors.add("Password: $msg")
                    }

                    if (errorJson.has("password_confirm")) {
                        val msg = errorJson.getJSONArray("password_confirm").join(", ")
                        errors.add("Password Confirm: $msg")
                    }

                    // Show validation errors to the user in a Toast
                    Handler(Looper.getMainLooper()).post {
                        progressBar.visibility = View.GONE
                        createAccountButton.isEnabled = true
                        Toast.makeText(
                            this@RegisterActivity,
                            errors.joinToString("\n"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }


            }
        })

    }

}