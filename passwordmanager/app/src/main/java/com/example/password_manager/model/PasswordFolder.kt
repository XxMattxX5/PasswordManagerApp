package com.example.password_manager.model

data class PasswordFolder (
    val id: String,
    val name: String,
    val passwords: List<PasswordSummary>,
    var isExpanded: Boolean = false
)