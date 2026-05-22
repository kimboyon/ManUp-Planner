package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.DailyCheckin
import com.example.data.model.Routine
import com.example.data.model.RoutineCompletion
import com.example.data.repository.HealthRepository
import com.example.network.GeminiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(
    private val context: Context,
    private val repository: HealthRepository
) : ViewModel() {

    private val prefs = context.getSharedPreferences("manup_user_preferences", Context.MODE_PRIVATE)

    // Authentication States
    val isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val loggedInProvider = MutableStateFlow(prefs.getString("logged_in_provider", "NONE") ?: "NONE")
    val userName = MutableStateFlow(prefs.getString("user_name", "익명 사용자") ?: "익명 사용자")
    val userAge = MutableStateFlow(prefs.getInt("user_age", 48))
    val userProfileUri = MutableStateFlow(prefs.getString("user_profile_uri", "") ?: "")

    // Subscription Tier: "FREE_TRIAL" (무료체험), "BASIC" (월 1만원), "PREMIUM" (월 3만원)
    val subscriptionTier = MutableStateFlow(prefs.getString("subscription_tier", "FREE_TRIAL") ?: "FREE_TRIAL")

    // Dynamic error/status dialog states
    val actionLimitErrorMsg = MutableStateFlow<String?>(null)

    // Primary Database States
    val allCheckins: StateFlow<List<DailyCheckin>> = repository.allCheckins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRoutines: StateFlow<List<Routine>> = repository.activeRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCompletions: StateFlow<List<RoutineCompletion>> = repository.allCompletions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Interactive States
    val showCheckinDialog = MutableStateFlow(false)
    val checkinDate = MutableStateFlow(getTodayDateString())
    
    // AI Routine Loading States
    val isAiLoading = MutableStateFlow(false)
    val aiErrorMsg = MutableStateFlow<String?>(null)

    init {
        // Populate default/standard routines if none exist or if updated routines are missing
        viewModelScope.launch {
            repository.activeRoutines.collect { list ->
                val hasNewRoutineType = list.any { it.title.contains("마음챙김") || it.title.contains("케겔") || it.title.contains("건강식") }
                if (list.isEmpty() || !hasNewRoutineType) {
                    populateDefaultRoutines()
                }
            }
        }
    }

    private suspend fun populateDefaultRoutines() {
        val defaults = listOf(
            Routine(
                title = "하체 서킷 스쿼트 15회",
                description = "하체 자극은 전신 호르몬 분비 및 혈류 개선과 코어 힘 강화에 가장 뛰어난 과학적 기반 운동입니다.",
                category = "muscle"
            ),
            Routine(
                title = "점심 야외 산책 20분",
                description = "햇빛을 쬐며 세로토닌 합성을 촉진해, 갱년기 불면증 완화와 밤 시간의 멜라토닌 분비를 돕습니다.",
                category = "sleep"
            ),
            Routine(
                title = "토마토 & 아연 활력 건강식",
                description = "[식단] 중장년 남성 전립선 건강에 탁월한 라이코펜이 풍부한 익힌 토마토와 성호르몬 합성을 촉진하는 아연(견과류, 굴 등) 중심의 활력 식사를 섭취합니다.",
                category = "energy"
            ),
            Routine(
                title = "4초 마음챙김 박스 호흡",
                description = "[마음챙김] 불안감, 집중력 저하 및 중년기 신체 변화로 인한 초조함을 직관적으로 완화하고 자율신경계를 정돈하는 호흡 회복 훈련입니다.",
                category = "mood"
            ),
            Routine(
                title = "골반저근 강화운동(케겔) 10회",
                description = "[비뇨생식 피지크] 하반신 골반 아래 근육을 집중 수축 이완하여 갱년기 비뇨 생식 활력 저하 방지 및 전립선 혈류 공급을 유도합니다.",
                category = "libido"
            ),
            Routine(
                title = "가까운 이들과 5분 소통",
                description = "[사회·정신적 연대] 중장년기의 사회적 고립감을 덜고 감정을 솔직하게 교류하여 스트레스 호르몬인 코르티솔 분비를 신속히 감쇄시킵니다.",
                category = "focus"
            )
        )
        // Avoid duplicate insertion by matching existing titles
        val existingTitles = activeRoutines.value.map { it.title }.toSet()
        for (item in defaults) {
            if (!existingTitles.contains(item.title)) {
                repository.insertRoutine(item)
            }
        }
    }

    fun getTodayDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }

    // Save Daily Check-in
    fun saveCheckin(
        date: String,
        sleepQuality: Int,
        energy: Int,
        mood: Int,
        focus: Int,
        brainFog: Int,
        libido: Int,
        muscleStrength: Int,
        note: String
    ) {
        viewModelScope.launch {
            val checkin = DailyCheckin(
                date = date,
                timestamp = System.currentTimeMillis(),
                sleepQuality = sleepQuality,
                energy = energy,
                mood = mood,
                focus = focus,
                brainFog = brainFog,
                libido = libido,
                muscleStrength = muscleStrength,
                note = note
            )
            repository.insertCheckin(checkin)
            showCheckinDialog.value = false
        }
    }

    // Toggle Routine Check-Off
    fun toggleRoutineCompletion(routineId: Int, date: String, isCompleted: Boolean) {
        viewModelScope.launch {
            if (isCompleted) {
                // Remove completion
                repository.deleteCompletion(routineId, date)
            } else {
                // Add completion
                val completion = RoutineCompletion(
                    routineId = routineId,
                    date = date,
                    completedAt = System.currentTimeMillis()
                )
                repository.insertCompletion(completion)
            }
        }
    }

    // Delete a specific routine entirely
    fun deleteRoutine(id: Int) {
        viewModelScope.launch {
            repository.deleteRoutine(id)
        }
    }

    // Generate customized AI routine based on the user's condition
    fun requestAiRoutine() {
        viewModelScope.launch {
            val checkins = allCheckins.value
            if (checkins.isEmpty()) {
                aiErrorMsg.value = "AI 루틴을 분석생성하려면 최소 1회 이상 '30초 체크인' 건강 기록이 필요합니다. 상단 '오늘의 컨디션 기록' 단추를 이용해주세요!"
                return@launch
            }

            isAiLoading.value = true
            aiErrorMsg.value = null

            // Construct health summary for context
            val latest = checkins.first()
            val summary = """
                최종 기록일: ${latest.date}
                - 수면 품질: ${latest.sleepQuality}/5
                - 활력 에너지: ${latest.energy}/5
                - 감정 기분: ${latest.mood}/5
                - 집중력 상태: ${latest.focus}/5
                - 뇌안개(두뇌 명료도): ${latest.brainFog}/5 (높을수록 뇌안개 심함)
                - 활력/성욕 지수: ${latest.libido}/5
                - 근력감(신체 활성도): ${latest.muscleStrength}/5
                - 추가 증상 노트: "${latest.note}"
            """.trimIndent()

            GeminiClient.generateCustomRoutine(
                userMetricsSummary = summary,
                onSuccess = { routinesText ->
                    viewModelScope.launch {
                        // Deactivate previous AI routines to avoid cluttering
                        val active = activeRoutines.value
                        for (r in active) {
                            if (r.isAiGenerated) {
                                repository.deleteRoutine(r.id)
                            }
                        }

                        // Parse and Insert new AI routines
                        for (raw in routinesText) {
                            val parts = raw.split("|")
                            val title = parts.getOrNull(0)?.trim() ?: "AI 추천 건강 수칙"
                            val desc = parts.getOrNull(1)?.trim() ?: "컨디션 정상화를 위한 추천 요령입니다."
                            
                            val routine = Routine(
                                title = title,
                                description = desc,
                                category = "ai",
                                isAiGenerated = true
                            )
                            repository.insertRoutine(routine)
                        }
                        isAiLoading.value = false
                    }
                },
                onError = { err ->
                    aiErrorMsg.value = err
                    isAiLoading.value = false
                }
            )
        }
    }

    // Populate Mock 4-Week Data so the charts, heatmaps, and Doctor Consultation preparation sheets show immediate, beautiful data!
    fun populateSampleData() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            // Delete existing to start clean
            val existing = allCheckins.value
            for (c in existing) {
                repository.deleteCheckinForDate(c.date)
            }

            val random = Random()
            
            // Loop back 28 days
            for (i in 27 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val dateStr = sdf.format(calendar.time)
                
                // Simulate periodic metrics
                // 45-55 age male condition decline with minor fluctuations
                val sleep = random.nextInt(3) + 2 // 2 to 4
                val energy = random.nextInt(3) + 2 // 2 to 4
                val mood = random.nextInt(3) + 2 // 2 to 4
                val focus = random.nextInt(3) + 2 // 2 to 4
                val brainFog = random.nextInt(3) + 1 // 1 to 3
                val libido = random.nextInt(3) + 1 // 1 to 3
                val muscle = random.nextInt(3) + 2 // 2 to 4
                
                val note = if (i % 7 == 0) "만성 피로감 심함, 아침 기상이 어려움" else if (i % 5 == 0) "하체 근력 저하 및 활력 다운 우려됨" else ""

                val checkin = DailyCheckin(
                    date = dateStr,
                    timestamp = calendar.timeInMillis,
                    sleepQuality = sleep,
                    energy = energy,
                    mood = mood,
                    focus = focus,
                    brainFog = brainFog,
                    libido = libido,
                    muscleStrength = muscle,
                    note = note
                )
                repository.insertCheckin(checkin)

                // Fill periodic routine completions
                val routines = activeRoutines.value
                for (r in routines) {
                    if (random.nextDouble() > 0.4) {
                        repository.insertCompletion(
                            RoutineCompletion(
                                routineId = r.id,
                                date = dateStr,
                                completedAt = calendar.timeInMillis
                            )
                        )
                    }
                }
            }
        }
    }

    // User Authentication Methods
    fun login(provider: String, name: String, age: Int = 48) {
        viewModelScope.launch {
            prefs.edit().apply {
                putBoolean("is_logged_in", true)
                putString("logged_in_provider", provider)
                putString("user_name", name)
                putInt("user_age", age)
                putString("subscription_tier", "FREE_TRIAL") // starts at Free Trial
                apply()
            }
            isLoggedIn.value = true
            loggedInProvider.value = provider
            userName.value = name
            userAge.value = age
            subscriptionTier.value = "FREE_TRIAL"
        }
    }

    fun logout() {
        viewModelScope.launch {
            prefs.edit().apply {
                putBoolean("is_logged_in", false)
                putString("logged_in_provider", "NONE")
                putString("user_name", "익명 사용자")
                putString("subscription_tier", "FREE_TRIAL")
                putString("user_profile_uri", "")
                apply()
            }
            isLoggedIn.value = false
            loggedInProvider.value = "NONE"
            userName.value = "익명 사용자"
            subscriptionTier.value = "FREE_TRIAL"
            userProfileUri.value = ""
            
            // Clear checkins block error message
            actionLimitErrorMsg.value = null
        }
    }

    fun updateSubscription(tier: String) {
        viewModelScope.launch {
            prefs.edit().putString("subscription_tier", tier).apply()
            subscriptionTier.value = tier
            actionLimitErrorMsg.value = null
        }
    }

    fun updateProfileImage(uriString: String) {
        viewModelScope.launch {
            prefs.edit().putString("user_profile_uri", uriString).apply()
            userProfileUri.value = uriString
        }
    }

    fun updateProfileInfo(name: String, age: Int) {
        viewModelScope.launch {
            prefs.edit().apply {
                putString("user_name", name)
                putInt("user_age", age)
                apply()
            }
            userName.value = name
            userAge.value = age
        }
    }

    // Comprehensive Check-in & Routine execution guard
    fun canPerformAction(actionType: String, isAlreadyCompleted: Boolean = false): Boolean {
        if (isAlreadyCompleted) return true // deleting/undoing is always allowed

        val todayStr = getTodayDateString()
        val tier = subscriptionTier.value
        
        // Count today's total active events
        val todayCheckinsCount = allCheckins.value.count { it.date == todayStr }
        val todayCompletionsCount = allCompletions.value.count { it.date == todayStr }
        val todayTotalActions = todayCheckinsCount + todayCompletionsCount

        when (actionType) {
            "CHECKIN" -> {
                if (tier == "FREE_TRIAL") {
                    val totalCheckins = allCheckins.value.size
                    if (totalCheckins >= 10) {
                        actionLimitErrorMsg.value = "무료 체험판의 10회 기록 한도를 초과했습니다!\n지속적인 맨업 관리를 위해 베이직 또는 프리미엄 구독으로 손쉽게 전환해 보세요."
                        return false
                    }
                } else if (tier == "BASIC") {
                    if (todayTotalActions >= 3) {
                        actionLimitErrorMsg.value = "베이직 요금제 일일 3회 이용 요건 정원에 도달했습니다.\n리포트 무제한 작성 및 AI 처방 제안 분석을 위해 '프리미엄(월 3만원)'으로 업그레이드해 보세요."
                        return false
                    }
                } else if (tier == "PREMIUM") {
                    if (todayTotalActions >= 10) {
                        actionLimitErrorMsg.value = "프리미엄 등급 일일 최고 10회 동작 한도에 도달했습니다.\n오늘 하루도 빈틈없는 최고 수준의 컨디션 회복 훈련에 성공하셨습니다!"
                        return false
                    }
                }
            }
            "ROUTINE" -> {
                if (tier == "FREE_TRIAL") {
                    // check-in limit applies but routine completions are free, lets allow them unless they are past trial
                    val totalCheckins = allCheckins.value.size
                    if (totalCheckins >= 10) {
                        actionLimitErrorMsg.value = "무료 체험판 7일 한도 및 10회 기록 한도가 만료되었습니다.\n안전한 자가 분석 습관 유지를 위해 유료 회원으로 가입하세요."
                        return false
                    }
                } else if (tier == "BASIC") {
                    if (todayTotalActions >= 3) {
                        actionLimitErrorMsg.value = "베이직 요금제 일일 완료 및 등록 한도(오늘 3회)를 도달했습니다.\n프리미엄 플랜(월 3만원)으로 강화하시어 당당한 하루 완료 지표를 확장하세요."
                        return false
                    }
                } else if (tier == "PREMIUM") {
                    if (todayTotalActions >= 10) {
                        actionLimitErrorMsg.value = "프리미엄 등급의 일일 10회 완수 강도 제한에 도달했습니다. 충분한 휴식을 추천합니다."
                        return false
                    }
                }
            }
            "AI_ANALYSIS" -> {
                if (tier == "FREE_TRIAL") {
                    actionLimitErrorMsg.value = "AI 연동 건강 진단 제안 기능은 '프리미엄(월 3만원)' 전용 기능입니다.\n체험판 상태에서는 이용이 불가능합니다."
                    return false
                } else if (tier == "BASIC") {
                    actionLimitErrorMsg.value = "베이직 멤버십(월 1만원)은 리포트 작성 기능이 포함되나, 'AI 실시간 건강 분석 수칙 제안'은 제외됩니다.\n프리미엄 멤버십으로 전환해 보세요."
                    return false
                }
            }
        }
        return true
    }
}

class MainViewModelFactory(
    private val context: Context,
    private val repository: HealthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
