package com.example.data.database

import androidx.room.*
import com.example.data.model.DailyCheckin
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyCheckinDao {
    @Query("SELECT * FROM daily_checkins ORDER BY date DESC")
    fun getAllCheckins(): Flow<List<DailyCheckin>>

    @Query("SELECT * FROM daily_checkins WHERE date = :date LIMIT 1")
    suspend fun getCheckinForDate(date: String): DailyCheckin?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckin(checkin: DailyCheckin)

    @Query("DELETE FROM daily_checkins WHERE date = :date")
    suspend fun deleteCheckinForDate(date: String)
}
