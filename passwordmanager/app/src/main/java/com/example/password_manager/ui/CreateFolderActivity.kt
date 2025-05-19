package com.example.password_manager.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.password_manager.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import com.example.password_manager.BuildConfig
import com.example.password_manager.utils.AuthManager
import kotlinx.coroutines.launch

class CreateFolderActivity:BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_folder)

        val createFolderButton = findViewById<Button>(R.id.button4)

        createFolderButton.setOnClickListener {
            val folderName = findViewById<EditText>(R.id.editTextText2).text.toString()
            createFolder(folderName)
        }

    }

    // Sends Request to backend API to create a new folder
    private fun createFolder(folderName: String) {
        val btnCreate = findViewById<Button>(R.id.button4)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Puts page into processing state
        btnCreate.isEnabled = false
        progressBar.visibility = View.VISIBLE

        val baseUrl = BuildConfig.BASE_URL
        val client = OkHttpClient()

        val json = """
        {
            "folderName": "$folderName"
        }
    """.trimIndent()

        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val token = AuthManager.getToken(this)

        val request = Request.Builder()
            .url("$baseUrl/passwords/folder/create/")
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle error
                runOnUiThread {
                    Toast.makeText(applicationContext, "Request failed", Toast.LENGTH_SHORT).show()
                    btnCreate.isEnabled = true
                    progressBar.visibility = View.GONE
                }
            }

            // Handles response from the create folder request
            override fun onResponse(call: Call, response: Response) {
                // Takes page out of processing state
                runOnUiThread {
                    btnCreate.isEnabled = true
                    progressBar.visibility = View.GONE
                }
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            applicationContext,
                            "Folder created successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(this@CreateFolderActivity, PasswordListActivity::class.java)
                        startActivity(intent)
                        finish()

                    } else if (response.code == 401) {
                        // Logs user out and navigates home if code is 401
                        AuthManager.logout(this@CreateFolderActivity,true)

                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Failed to create folder",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }
}