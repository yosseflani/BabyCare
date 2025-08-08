package com.example.babycare

data class ScheduleWithBaby(
    val id: String = "",
    val babyId: String = "",
    val title: String = "",
    val description: String = "",
    val time: String = "",
    val frequency: String = "",
    val babyName: String = "",
    val isDone: Boolean = false
)
