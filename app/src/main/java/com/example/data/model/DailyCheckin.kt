package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_checkins")
data class DailyCheckin(
    @PrimaryKey
    val date: String, // format: "YYYY-MM-DD"
    val timestamp: Long,
    val sleepQuality: Int, // 1 to 5
    val energy: Int,       // 1 to 5
    val mood: Int,         // 1 to 5
    val focus: Int,        // 1 to 5
    val brainFog: Int,     // 1 to 5 (1 = clean, 5 = severe)
    val libido: Int,       // 1 to 5
    val muscleStrength: Int, // 1 to 5
    val note: String = ""
)
