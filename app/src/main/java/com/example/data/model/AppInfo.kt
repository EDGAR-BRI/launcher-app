package com.example.data.model

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String = "",
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false
)
