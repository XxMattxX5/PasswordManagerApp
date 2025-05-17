package com.example.password_manager.model

sealed class ListItem {
    data class FolderItem(val folder: PasswordFolder) : ListItem()
    data class PasswordItem(val password: PasswordSummary) : ListItem()
}