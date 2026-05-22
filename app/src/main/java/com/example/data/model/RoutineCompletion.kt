package com.example.data.model

import androidx.room.Entity

@Entity(
    tableName = "routine_completions",
    primaryKeys = ["routineId", "date"]
)
data class RoutineCompletion(
    val routineId: Int,
    val date: String, // format: "YYYY-MM-DD"
    val completedAt: Long
)
