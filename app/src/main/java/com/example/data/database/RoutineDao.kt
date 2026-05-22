package com.example.data.database

import androidx.room.*
import com.example.data.model.Routine
import com.example.data.model.RoutineCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY id ASC")
    fun getAllRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE isActive = 1 ORDER BY id ASC")
    fun getActiveRoutines(): Flow<List<Routine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: Routine): Long

    @Query("UPDATE routines SET isActive = 0 WHERE id = :id")
    suspend fun deactivateRoutine(id: Int)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: Int)

    // Completion Handlers
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: RoutineCompletion)

    @Query("DELETE FROM routine_completions WHERE routineId = :routineId AND date = :date")
    suspend fun deleteCompletion(routineId: Int, date: String)

    @Query("SELECT * FROM routine_completions WHERE date = :date")
    fun getCompletionsForDate(date: String): Flow<List<RoutineCompletion>>

    @Query("SELECT * FROM routine_completions ORDER BY date DESC")
    fun getAllCompletions(): Flow<List<RoutineCompletion>>
}
