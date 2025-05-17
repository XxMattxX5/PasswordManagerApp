package com.example.password_manager.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
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

class FolderActivity:BaseActivity() {

    private lateinit var folderId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder)

        folderId = intent.getStringExtra("FOLDER_ID") ?: throw IllegalArgumentException("FOLDER_ID is missing")

        fetchFolderFromBackend(folderId)

        val updateFolderBtn = findViewById<Button>(R.id.button4)
        val deleteFolderBtn = findViewById<Button>(R.id.button5)

        updateFolderBtn.setOnClickListener {
            val name = findViewById<EditText>(R.id.editTextText2).text.toString()

            sendUpdateFolderRequest(folderId,name)
        }

        deleteFolderBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this folder?")
                .setPositiveButton("Delete") { _, _ ->
                    deleteFolder(folderId)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            if (AuthManager.isLogged == null) {
                val valid = AuthManager.validateToken(this@FolderActivity)
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

    private fun fetchFolderFromBackend(id: String) {
        val authToken = AuthManager.getToken(this)
        val baseUrl = BuildConfig.BASE_URL

        val folderNameInput = findViewById<EditText>(R.id.editTextText2)

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$baseUrl/passwords/folder/$id/")
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val name = parseResponse(body)

                        withContext(Dispatchers.Main) {
                            folderNameInput.setText(name)
                        }

                    }
                } else {
                    if (response.code == 401) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@FolderActivity, "Session Expired", Toast.LENGTH_SHORT).show()
                            AuthManager.logout(this@FolderActivity)
                        }
                    }

                    Log.e("PasswordActivity", "Error response: ${response.code}")


                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun parseResponse(body: String): String {
        val json = JSONObject(body)
        val data = json.getJSONObject("data")

        return data.getString("name")
    }

    private fun sendUpdateFolderRequest(
        id:String,
        name: String,
        ) {
        val btnCreate = findViewById<Button>(R.id.button5)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnCreate.isEnabled = false
        progressBar.visibility = View.VISIBLE

        val baseUrl = BuildConfig.BASE_URL
        val client = OkHttpClient()
        val authToken = AuthManager.getToken(this)

        val formBodyBuilder = FormBody.Builder()
            .add("name", name)

        val formBody = formBodyBuilder.build()

        val request = Request.Builder()
            .url("$baseUrl/passwords/folder/$id/")
            .addHeader("Authorization", "Bearer $authToken")
            .put(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@FolderActivity, "Failed To Update: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@FolderActivity, "Folder updated successfully!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@FolderActivity, PasswordListActivity::class.java)
                        startActivity(intent)

                        finish()
                    }
                } else if (response.code == 401) {
                    runOnUiThread {
                        Toast.makeText(this@FolderActivity, "Session Expired", Toast.LENGTH_SHORT).show()
                        AuthManager.logout(this@FolderActivity)
                    }
                } else {
                    val errorBody = response.body?.string()
                    runOnUiThread {
                        Toast.makeText(this@FolderActivity, "Failed to update folder: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun deleteFolder(id: String) {
        val authToken = AuthManager.getToken(this)
        val baseUrl = BuildConfig.BASE_URL


        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$baseUrl/passwords/folder/$id/")
            .addHeader("Authorization", "Bearer $authToken")
            .delete()
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FolderActivity, "Folder Deleted", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@FolderActivity, PasswordListActivity::class.java)
                        startActivity(intent)

                        finish()
                    }


                } else {
                    if (response.code == 401) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@FolderActivity, "Session Expired", Toast.LENGTH_SHORT).show()
                            AuthManager.logout(this@FolderActivity)
                        }
                    }

                    Log.e("FolderActivity", "Error response: ${response.code}")


                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}