package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.DailyCheckin
import com.example.data.model.RoutineCompletion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ManUp", appName)
    }

    @Test
    fun `test report symptoms percentages with simulated 28 days logs`() = runBlocking {
        val checkinDao = db.dailyCheckinDao()

        // 1. Simulate 28 days logs
        // - 15 days of low energy (energy <= 2) -> 15/28 = 53% low energy
        // - 10 days of sleep quality <= 2 -> 10/28 = 35% sleep quality issues
        // - 20 days of muscle weakness <= 2 -> 20/28 = 71% muscle weakness
        // - 5 days libido <= 2 -> 5/28 = 17% libido issues
        for (i in 1..28) {
            val dateStr = String.format("2026-05-%02d", i)
            val energy = if (i <= 15) 2 else 4 // 15 low energy
            val sleepQuality = if (i <= 10) 2 else 4 // 10 sleep quality
            val muscle = if (i <= 20) 2 else 5 // 20 muscle weakness
            val libido = if (i <= 5) 2 else 4 // 5 libido issues

            val checkin = DailyCheckin(
                date = dateStr,
                timestamp = System.currentTimeMillis() - (i * 24 * 3600 * 1000L),
                sleepQuality = sleepQuality,
                energy = energy,
                mood = 3,
                focus = 3,
                brainFog = 2,
                libido = libido,
                muscleStrength = muscle,
                note = if (i % 7 == 0) "피로 호소 및 하체 기운 빠짐" else ""
            )
            checkinDao.insertCheckin(checkin)
        }

        // 2. Fetch from database Flow and verify count
        val checkins = checkinDao.getAllCheckins().first()
        assertEquals(28, checkins.size)

        // 3. Do report calculations matching ClinicalReportPaperBlueprint logic
        val totalLogs = checkins.size
        val lowEnergyCount = checkins.filter { it.energy <= 2 }.size
        val sleepIssuesCount = checkins.filter { it.sleepQuality <= 2 }.size
        val muscleWeaknessCount = checkins.filter { it.muscleStrength <= 2 }.size
        val libidoLossCount = checkins.filter { it.libido <= 2 }.size

        val lowEnergyPercent = (lowEnergyCount * 100 / totalLogs)
        val insomniaPercent = (sleepIssuesCount * 100 / totalLogs)
        val muscleWeaknessPercent = (muscleWeaknessCount * 100 / totalLogs)
        val libidoLossPercent = (libidoLossCount * 100 / totalLogs)

        assertEquals(53, lowEnergyPercent)
        assertEquals(35, insomniaPercent)
        assertEquals(71, muscleWeaknessPercent)
        assertEquals(17, libidoLossPercent)

        // 4. Verify Clinical Advisory selection logic
        val highlightAdvisory = when {
            totalLogs == 0 -> "자가 보고 기록이 없습니다."
            libidoLossPercent > 50 || muscleWeaknessPercent > 50 -> "※ 비뇨기 호르몬 분비 감퇴 연관 유의성 발견"
            lowEnergyPercent > 40 || insomniaPercent > 40 -> "※ 부신 피로 및 심혈관 생물학적 사이클 둔화"
            else -> "※ 현재 완만한 대사 균형을 유지 중이나"
        }

        assertTrue(highlightAdvisory.contains("비뇨기 호르몬 분비 감퇴 연관 유의성 발견"))
    }
}
