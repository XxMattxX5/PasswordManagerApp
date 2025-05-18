package com.example.password_manager.ui

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import com.example.password_manager.R
import com.example.password_manager.model.PasswordFolder
import com.example.password_manager.model.PasswordSummary
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.password_manager.adapter.PasswordAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.example.password_manager.BuildConfig
import com.example.password_manager.utils.AuthManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.text.TextWatcher
import android.content.Intent
import android.view.View.GONE
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import androidx.lifecycle.lifecycleScope

class PasswordListActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PasswordAdapter
    private var searchJob: Job? = null
    private var selectedFolderId: String? = null
    private var selectedFolderName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_list)

        val createPasswordButton = findViewById<ImageView>(R.id.imageView5)
        val createFolderButton = findViewById<ImageView>(R.id.imageView4)

        createPasswordButton.setOnClickListener {
            val intent = Intent(this@PasswordListActivity, CreatePasswordActivity::class.java)

            // Adds folder id and name to intent if not null
            selectedFolderId?.let { folderId ->
                intent.putExtra("SELECTED_FOLDER_ID", folderId)
            }
            selectedFolderName?.let { folderName ->
                intent.putExtra("SELECTED_FOLDER_NAME", folderName)
            }

            startActivity(intent)
        }

        createFolderButton.setOnClickListener {
            val intent = Intent(this@PasswordListActivity, CreateFolderActivity::class.java)
            startActivity(intent)
        }


        recyclerView = findViewById(R.id.passwordsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter with empty lists first
        adapter = PasswordAdapter(
            emptyList(),
            emptyList(),
            onPasswordClick = { password ->
                
            },
            onFolderClick = { folder ->
                if (selectedFolderId == folder.id) {
                    selectedFolderId = null
                    selectedFolderName = null
                } else {
                    selectedFolderId = folder.id
                    selectedFolderName = folder.name
                }




            }
        )
        recyclerView.adapter = adapter

        fetchPasswordsFromBackend()

        // On search change request is send to backend for passwords list
        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(500) // debounce delay of 500ms
                    val query = s.toString().trim()
                    fetchPasswordsFromBackend(query)
                }
            }
        })
    }

    // On resume user's auth is checked
    // If user is no longer logged in they are navigated hom
    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            if (AuthManager.isLogged == null) {
                val valid = AuthManager.validateToken(this@PasswordListActivity)
                if (!valid) {
                    navigateToHome()
                }
            } else if (AuthManager.isLogged != true) {
                navigateToHome()
            }
        }
    }

    // Navigates user to home page
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Sends request to backend to fetch a list of passwords and folders associated with user
    private fun fetchPasswordsFromBackend(query: String = "") {
        val authToken = AuthManager.getToken(this)
        val baseUrl = BuildConfig.BASE_URL

        // Append "q" query parameter if query is not empty
        val url = if (query.isNotEmpty()) {
            "$baseUrl/passwords/list/?q=${Uri.encode(query)}"
        } else {
            "$baseUrl/passwords/list/"
        }

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()

                // if success password list is populated using password adapter
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val (folders, standalonePasswords) = parseResponse(body)

                        withContext(Dispatchers.Main) {
                            adapter.updateData(folders, standalonePasswords)
                        }

                    }
                } else {
                    if (response.code == 401) {
                        // If response is 401 user is logged out and navigated home
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@PasswordListActivity, "Session Expired", Toast.LENGTH_SHORT).show()
                            AuthManager.logout(this@PasswordListActivity)
                        }
                    }

                    Log.e("PasswordListActivity", "Error response: ${response.code}")

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    // Parses the response for the fetch password request
    private fun parseResponse(jsonString: String): Pair<List<PasswordFolder>, List<PasswordSummary>> {
        val folders = mutableListOf<PasswordFolder>()
        val standalonePasswords = mutableListOf<PasswordSummary>()

        val jsonObject = JSONObject(jsonString)

        val foldersJsonArray = jsonObject.getJSONArray("folders")
        for (i in 0 until foldersJsonArray.length()) {
            val folderObj = foldersJsonArray.getJSONObject(i)
            val folderId = folderObj.getInt("id").toString()
            val folderName = folderObj.getString("name")

            val passwordsJson = folderObj.getJSONArray("passwords")
            val passwordsList = mutableListOf<PasswordSummary>()
            for (j in 0 until passwordsJson.length()) {
                val pwdObj = passwordsJson.getJSONObject(j)
                val pwdId = pwdObj.getInt("id").toString()
                val pwdName = pwdObj.getString("name")

                passwordsList.add(PasswordSummary(pwdId, pwdName))
            }

            folders.add(PasswordFolder(folderId, folderName, passwordsList))
        }

        val passwordsJsonArray = jsonObject.getJSONArray("passwords")
        for (i in 0 until passwordsJsonArray.length()) {
            val pwdObj = passwordsJsonArray.getJSONObject(i)
            val pwdId = pwdObj.getInt("id").toString()
            val pwdName = pwdObj.getString("name")
            standalonePasswords.add(PasswordSummary(pwdId, pwdName))
        }

        return Pair(folders, standalonePasswords)
    }
}