package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val category: String, // "sleep", "energy", "mood", "focus", "brainflag", "libido", "muscle"
    val isAiGenerated: Boolean = false,
    val isActive: Boolean = true
)
