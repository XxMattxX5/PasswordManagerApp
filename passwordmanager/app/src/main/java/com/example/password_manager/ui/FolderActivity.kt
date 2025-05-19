package com.example.password_manager.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
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

        // Gets folder id from the intent
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


    // Sends Request to the backend fetch folder using id
    private fun fetchFolderFromBackend(id: String) {
        val authToken = AuthManager.getToken(this)
        val baseUrl = BuildConfig.BASE_URL

        val loadingProgress = findViewById<ProgressBar>(R.id.progressBar2)
        val folderContent = findViewById<LinearLayout>(R.id.layout2)

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
                        // Logs out user and navigates home if code is 401
                        withContext(Dispatchers.Main) {

                            AuthManager.logout(this@FolderActivity, true)

                        }
                    }

                    Log.e("PasswordActivity", "Error response: ${response.code}")

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            withContext(Dispatchers.Main) {
                loadingProgress.visibility = View.GONE
                folderContent.visibility = View.VISIBLE
            }
        }
    }

    // Parse response of fetch folder request
    private fun parseResponse(body: String): String {
        val json = JSONObject(body)
        val data = json.getJSONObject("data")

        return data.getString("name")
    }

    // Sends a request to backend to update the folder of the given id
    private fun sendUpdateFolderRequest(
        id:String,
        name: String,
        ) {
        val btnCreate = findViewById<Button>(R.id.button4)
        val deleteButton = findViewById<Button>(R.id.button5)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Sets page into a processing state
        btnCreate.isEnabled = false
        deleteButton.isEnabled = false
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
                    deleteButton.isEnabled = true
                    btnCreate.isEnabled = true
                    progressBar.visibility = View.GONE
                }
            }

            // Handles response from update folder request
            override fun onResponse(call: Call, response: Response) {

                // Takes page out of processing state
                runOnUiThread {
                    deleteButton.isEnabled = true
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
                    // Logouts user and navigates them home if response code is 401
                    runOnUiThread {

                        AuthManager.logout(this@FolderActivity, true)
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

    // Sends request to backend to delete the folder with the given id
    private fun deleteFolder(id: String) {
        val authToken = AuthManager.getToken(this)
        val baseUrl = BuildConfig.BASE_URL

        val btnCreate = findViewById<Button>(R.id.button4)
        val deleteButton = findViewById<Button>(R.id.button5)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Sets page into processing state
        btnCreate.isEnabled = false
        deleteButton.isEnabled = false
        progressBar.visibility = View.VISIBLE


        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$baseUrl/passwords/folder/$id/")
            .addHeader("Authorization", "Bearer $authToken")
            .delete()
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            // Takes page out of processing state
            withContext(Dispatchers.Main) {
                deleteButton.isEnabled = true
                btnCreate.isEnabled = true
                progressBar.visibility = View.GONE
            }

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    // Sends user home after if folder deletion is successful
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FolderActivity, "Folder Deleted", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@FolderActivity, PasswordListActivity::class.java)
                        startActivity(intent)

                        finish()
                    }
                } else {
                    if (response.code == 401) {
                        // Logs user out and navigates them home if response code is 401
                        withContext(Dispatchers.Main) {
                            AuthManager.logout(this@FolderActivity, true)
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