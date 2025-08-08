package com.example.babycare

data class Note(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val datetime: String = "",
    var isCompleted: Boolean = false,
    val babyId: String = ""
)
