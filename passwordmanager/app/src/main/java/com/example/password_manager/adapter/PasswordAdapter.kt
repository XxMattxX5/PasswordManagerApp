package com.example.password_manager.adapter

import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.password_manager.model.ListItem
import com.example.password_manager.model.PasswordFolder
import com.example.password_manager.model.PasswordSummary
import android.view.LayoutInflater
import android.widget.TextView
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.password_manager.R
import com.example.password_manager.ui.FolderActivity
import com.example.password_manager.ui.PasswordActivity


class PasswordAdapter(
    private var folders: List<PasswordFolder>,
    private var standalonePasswords: List<PasswordSummary>,
    private val onPasswordClick: (PasswordSummary) -> Unit,
    private val onFolderClick: (PasswordFolder) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<ListItem> = listOf()
    private var expandedFolder:  String? = null
    private val passwordNestedMap = mutableMapOf<String, Boolean>()
    private var lastClickedFolderId: String? = null

    init {
        updateItems()
    }

    // ViewType Constants
    companion object {
        private const val TYPE_FOLDER = 0
        private const val TYPE_PASSWORD = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.FolderItem -> TYPE_FOLDER
            is ListItem.PasswordItem -> TYPE_PASSWORD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Inflate layouts based on type
        return if (viewType == TYPE_FOLDER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
            FolderViewHolder(view, onFolderClick)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_password, parent, false)
            PasswordViewHolder(view, onPasswordClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.FolderItem -> {
                val folder = item.folder
                (holder as FolderViewHolder).bind(folder)

            }

        is ListItem.PasswordItem -> {
            val password = item.password
            val isNested = passwordNestedMap[password.id] ?: false
            (holder as PasswordViewHolder).bind(password, isNested)

        }
    }
}


    override fun getItemCount(): Int = items.size

    inner class FolderViewHolder(
        itemView: View,
        private val onFolderClick: (PasswordFolder) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(folder: PasswordFolder) {
            val folderName = itemView.findViewById<TextView>(R.id.folderNameTextView)
            folderName.text = folder.name

            val isExpand = expandedFolder == folder.id
            val backgroundColor = if (isExpand)
                ContextCompat.getColor(itemView.context, R.color.selected_background_folder_name)
            else
                ContextCompat.getColor(itemView.context, android.R.color.transparent)

            itemView.setBackgroundColor(backgroundColor)

            val context = itemView.context

            itemView.setOnClickListener {

                // Expand/collapse logic
                if (expandedFolder == folder.id) {
                    expandedFolder = null
                } else {
                    expandedFolder = folder.id
                }
                lastClickedFolderId = if (lastClickedFolderId == folder.id) {
                    Toast.makeText(context, "Folder no longer selected", Toast.LENGTH_SHORT).show()
                    null

                } else {
                    Toast.makeText(context, "Selected folder: ${folder.name}", Toast.LENGTH_SHORT).show()
                    folder.id
                }

                updateItems()

                // Notify external listener
                onFolderClick(folder)
            }
            itemView.setOnLongClickListener {
                val intent = Intent(context, FolderActivity::class.java)
                intent.putExtra("FOLDER_ID", folder.id)

                context.startActivity(intent)

                true
            }
        }
    }

    fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()


    inner class PasswordViewHolder(itemView: View, private val onPasswordClick: (PasswordSummary) -> Unit) : RecyclerView.ViewHolder(itemView) {
        fun bind(password: PasswordSummary, isNested: Boolean) {
            val passwordName = itemView.findViewById<TextView>(R.id.accountNameTextView)
            passwordName.text = password.name



            val context = itemView.context  // get context here
            val paddingStart = if (isNested) 25.dpToPx(context) else 45
            itemView.setPadding(
                paddingStart,
                itemView.paddingTop,
                itemView.paddingRight,
                itemView.paddingBottom
            )

            val isInSelectedFolder = isNested && lastClickedFolderId != null &&
                    (folders.find { it.id == lastClickedFolderId }?.passwords?.any { it.id == password.id } == true)

            val backgroundColor = if (isInSelectedFolder)
                ContextCompat.getColor(context, R.color.selected_background_folder_passwords)
            else
                ContextCompat.getColor(context, android.R.color.transparent)

            itemView.setBackgroundColor(backgroundColor)

            itemView.setOnClickListener {
                onPasswordClick(password)

                val intent = Intent(context, PasswordActivity::class.java)
                intent.putExtra("PASSWORD_ID", password.id)

                context.startActivity(intent)


                updateItems()

            }
        }
    }


    fun updateItems() {
        val newItems = mutableListOf<ListItem>()
        passwordNestedMap.clear()

        for (folder in folders) {
            newItems.add(ListItem.FolderItem(folder))
            if (expandedFolder == folder.id) {
                for (password in folder.passwords) {
                    newItems.add(ListItem.PasswordItem(password))
                    passwordNestedMap[password.id] = true  // Mark as nested
                }
            }
        }

        for (password in standalonePasswords) {
            newItems.add(ListItem.PasswordItem(password))
            passwordNestedMap[password.id] = false  // Not nested
        }

        items = newItems
        notifyDataSetChanged()
    }

    fun updateData(folders: List<PasswordFolder>, standalonePasswords: List<PasswordSummary>) {
        this.folders = folders
        this.standalonePasswords = standalonePasswords
        updateItems()  // existing function that rebuilds the items list
    }
}