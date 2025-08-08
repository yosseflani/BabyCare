package com.example.babycare

data class Baby(
    var id: String = "",
    val name: String = "",
    val birthDate: String = "",
    val weight: Double = 0.0,
    val height: Double = 0.0,
    val allergies: String = "",
    val medications: String = "",
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
