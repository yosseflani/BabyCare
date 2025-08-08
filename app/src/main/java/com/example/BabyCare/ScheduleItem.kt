package com.example.babycare

data class ScheduleItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val frequencyHours: Int = 0,
    val time: String = "",
    val babyId: String = ""
)
