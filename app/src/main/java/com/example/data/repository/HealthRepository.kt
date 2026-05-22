package com.example.data.repository

import com.example.data.database.DailyCheckinDao
import com.example.data.database.RoutineDao
import com.example.data.model.DailyCheckin
import com.example.data.model.Routine
import com.example.data.model.RoutineCompletion
import kotlinx.coroutines.flow.Flow

class HealthRepository(
    private val dailyCheckinDao: DailyCheckinDao,
    private val routineDao: RoutineDao
) {
    val allCheckins: Flow<List<DailyCheckin>> = dailyCheckinDao.getAllCheckins()
    val activeRoutines: Flow<List<Routine>> = routineDao.getActiveRoutines()
    val allCompletions: Flow<List<RoutineCompletion>> = routineDao.getAllCompletions()

    suspend fun getCheckinForDate(date: String): DailyCheckin? {
        return dailyCheckinDao.getCheckinForDate(date)
    }

    suspend fun insertCheckin(checkin: DailyCheckin) {
        dailyCheckinDao.insertCheckin(checkin)
    }

    suspend fun deleteCheckinForDate(date: String) {
        dailyCheckinDao.deleteCheckinForDate(date)
    }

    suspend fun insertRoutine(routine: Routine): Long {
        return routineDao.insertRoutine(routine)
    }

    suspend fun deactivateRoutine(id: Int) {
        routineDao.deactivateRoutine(id)
    }

    suspend fun deleteRoutine(id: Int) {
        routineDao.deleteRoutine(id)
    }

    suspend fun insertCompletion(completion: RoutineCompletion) {
        routineDao.insertCompletion(completion)
    }

    suspend fun deleteCompletion(routineId: Int, date: String) {
        routineDao.deleteCompletion(routineId, date)
    }

    fun getCompletionsForDate(date: String): Flow<List<RoutineCompletion>> {
        return routineDao.getCompletionsForDate(date)
    }
}
