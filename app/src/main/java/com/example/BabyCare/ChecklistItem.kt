package com.example.babycare

data class ChecklistItem(
    val id: String = "",
    val babyId: String = "",
    val date: String = "",
    val title: String = "",
    var description: String? = null,
    var time: String = "",
    var isCompleted: Boolean = false
)
