package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.model.DailyCheckin
import com.example.data.model.Routine
import com.example.data.model.RoutineCompletion
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class TabItem {
    HOME_ROUTINE,
    PATTERN_ANALYSIS,
    HOSPITAL_REPORT,
    MY_PAGE
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val loggedInProvider by viewModel.loggedInProvider.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val subscriptionTier by viewModel.subscriptionTier.collectAsStateWithLifecycle()
    val actionLimitErrorMsg by viewModel.actionLimitErrorMsg.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(TabItem.HOME_ROUTINE) }
    
    val checkins by viewModel.allCheckins.collectAsStateWithLifecycle()
    val routines by viewModel.activeRoutines.collectAsStateWithLifecycle()
    val completions by viewModel.allCompletions.collectAsStateWithLifecycle()
    val showCheckin by viewModel.showCheckinDialog.collectAsStateWithLifecycle()

    if (!isLoggedIn) {
        OnboardingWelcomeScreen(viewModel)
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = DeepForest,
                    modifier = Modifier
                        .border(0.5.dp, SageCardElevated, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = activeTab == TabItem.HOME_ROUTINE,
                        onClick = { activeTab = TabItem.HOME_ROUTINE },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("홈 & 루틴", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepForest,
                            selectedTextColor = SandGold,
                            unselectedIconColor = TextMutedGreen,
                            unselectedTextColor = TextMutedGreen,
                            indicatorColor = SandGold
                        ),
                        modifier = Modifier.testTag("nav_home")
                    )
                    NavigationBarItem(
                        selected = activeTab == TabItem.PATTERN_ANALYSIS,
                        onClick = { activeTab = TabItem.PATTERN_ANALYSIS },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Trends") },
                        label = { Text("4주 분석", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepForest,
                            selectedTextColor = SandGold,
                            unselectedIconColor = TextMutedGreen,
                            unselectedTextColor = TextMutedGreen,
                            indicatorColor = SandGold
                        ),
                        modifier = Modifier.testTag("nav_trends")
                    )
                    NavigationBarItem(
                        selected = activeTab == TabItem.HOSPITAL_REPORT,
                        onClick = { activeTab = TabItem.HOSPITAL_REPORT },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "Report") },
                        label = { Text("병원 리포트", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepForest,
                            selectedTextColor = SandGold,
                            unselectedIconColor = TextMutedGreen,
                            unselectedTextColor = TextMutedGreen,
                            indicatorColor = SandGold
                        ),
                        modifier = Modifier.testTag("nav_report")
                    )
                    NavigationBarItem(
                        selected = activeTab == TabItem.MY_PAGE,
                        onClick = { activeTab = TabItem.MY_PAGE },
                        icon = { Icon(Icons.Default.Person, contentDescription = "My Page") },
                        label = { Text("마이페이지", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepForest,
                            selectedTextColor = SandGold,
                            unselectedIconColor = TextMutedGreen,
                            unselectedTextColor = TextMutedGreen,
                            indicatorColor = SandGold
                        ),
                        modifier = Modifier.testTag("nav_mypage")
                    )
                }
            },
            containerColor = DeepForest
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "ScreenTransition"
                ) { targetTab ->
                    when (targetTab) {
                        TabItem.HOME_ROUTINE -> HomeRoutineScreen(viewModel, checkins, routines, completions)
                        TabItem.PATTERN_ANALYSIS -> PatternAnalysisScreen(viewModel, checkins, completions)
                        TabItem.HOSPITAL_REPORT -> HospitalReportScreen(viewModel, checkins, completions)
                        TabItem.MY_PAGE -> MyPageScreen(viewModel, checkins, completions)
                    }
                }

                // Slide-up check-in form overlay
                if (showCheckin) {
                    CheckinDialog(
                        onDismiss = { viewModel.showCheckinDialog.value = false },
                        onSave = { sleep, energy, mood, focus, fog, libido, muscle, note ->
                            viewModel.saveCheckin(viewModel.checkinDate.value, sleep, energy, mood, focus, fog, libido, muscle, note)
                        },
                        initialDate = viewModel.checkinDate.value
                    )
                }
            }
        }
    }

    // Limit error modal popup with pricing choices
    if (actionLimitErrorMsg != null) {
        Dialog(
            onDismissRequest = { viewModel.actionLimitErrorMsg.value = null }
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DeepForest,
                border = BorderStroke(1.dp, SandGold),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Subscription Limit",
                        tint = SandGold,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "멤버십 이용 한도 알림",
                        color = SandGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        actionLimitErrorMsg ?: "",
                        color = TextWhite,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateSubscription("BASIC")
                                viewModel.actionLimitErrorMsg.value = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SandGold, contentColor = DeepForest),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Text("베이직 연간 가입 (월 10,000원)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        
                        Button(
                            onClick = {
                                viewModel.updateSubscription("PREMIUM")
                                viewModel.actionLimitErrorMsg.value = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SandGold, contentColor = DeepForest),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Text("프리미엄 무제한 가입 (월 30,000원)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.actionLimitErrorMsg.value = null },
                            border = BorderStroke(1.dp, SageCardElevated),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Text("취소", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==================== TAB 1: HOME & ROUTINES ====================
@Composable
fun HomeRoutineScreen(
    viewModel: MainViewModel,
    checkins: List<DailyCheckin>,
    routines: List<Routine>,
    completions: List<RoutineCompletion>
) {
    val todayStr = viewModel.getTodayDateString()
    val todayCheckin = checkins.find { it.date == todayStr }
    val isCompletedToday = todayCheckin != null

    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val aiError by viewModel.aiErrorMsg.collectAsStateWithLifecycle()

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val loggedInProvider by viewModel.loggedInProvider.collectAsStateWithLifecycle()
    val subscriptionTier by viewModel.subscriptionTier.collectAsStateWithLifecycle()

    // Calculate Completion Streaks & Achievements
    val completedTodayCount = completions.filter { it.date == todayStr }.size
    val totalActiveRoutines = routines.size
    var selectedRoutineForHistory by remember { mutableStateOf<Routine?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Membership Profile Area
        item {
            var showBillingDialog by remember { mutableStateOf(false) }
            
            val tierName = when (subscriptionTier) {
                "FREE_TRIAL" -> "무료 체험판 (체험 이용)"
                "BASIC" -> "베이직 멤버십 (월 1만원)"
                "PREMIUM" -> "프리미엄 멤버십 (월 3만원)"
                else -> "일반 회원"
            }
            
            val limitIndicator = when (subscriptionTier) {
                "FREE_TRIAL" -> {
                    val remaining = (10 - checkins.size).coerceAtLeast(0)
                    "남은 무료 체크인 횟수: ${remaining}/10회"
                }
                "BASIC" -> {
                    "베이직 일일 한도: ${completedTodayCount + (if (isCompletedToday) 1 else 0)}/3회"
                }
                "PREMIUM" -> {
                    "프리미엄 일일 한도: ${completedTodayCount + (if (isCompletedToday) 1 else 0)}/10회"
                }
                else -> ""
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SageCard.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, SandGold.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().testTag("profile_billing_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(SandGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (loggedInProvider == "GOOGLE") Icons.Default.AccountCircle else if (loggedInProvider == "KAKAO") Icons.Default.Face else Icons.Default.Person,
                                    tint = DeepForest,
                                    modifier = Modifier.size(24.dp),
                                    contentDescription = "Avatar"
                                )
                            }
                            Column {
                                Text(userName, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .background(SandGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            if (loggedInProvider == "GOOGLE") "Google 연동됨" else if (loggedInProvider == "KAKAO") "카카오 연동됨" else "비회원",
                                            color = SandGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text("연령: ${viewModel.userAge.collectAsStateWithLifecycle().value}세", color = TextMutedGreen, fontSize = 11.sp)
                                }
                            }
                        }

                        // Logout text link
                        Text(
                            "로그아웃",
                            color = CoralRed.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { viewModel.logout() }
                                .padding(6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = SageCardElevated, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("현재 멤버십 등급", color = TextMutedGreen, fontSize = 11.sp)
                            Text(tierName, color = SandGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(limitIndicator, color = TextWhite, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showBillingDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SageCardElevated,
                                contentColor = SandGold
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = "Card", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("등급 변경/구독", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (showBillingDialog) {
                Dialog(onDismissRequest = { showBillingDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = DeepForest,
                        border = BorderStroke(1.5.dp, SandGold),
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("맨업(ManUp) 프리미엄 플랜 구독 정보", color = SandGold, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("비교 분석 및 한계 없는 자기관리 시스템", color = TextWhite, fontSize = 11.sp)
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Plan 1: Free Trial
                            BillingPlanView(
                                title = "1. 무료 체험판 (Free Trial)",
                                desc = "가입 후 최대 10회 기록 한도 제공\n• 7대 지표 수동 기록 전용\n• 병원 진단용 리포트 조회 불가\n• AI 맞춤 조언 및 처방 기능 제한",
                                price = "무료체험 (10회 한도 완료 시 즉시 정지)",
                                isCurrent = subscriptionTier == "FREE_TRIAL",
                                onSelect = {
                                    viewModel.updateSubscription("FREE_TRIAL")
                                    showBillingDialog = false
                                }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Plan 2: Basic
                            BillingPlanView(
                                title = "2. 베이직 멤버십 (환자 보고서 해제)",
                                desc = "하루 최대 3회 완료 및 체크인 기록 가능\n• 30일 누적 최대 90회 제공\n• 비뇨의학과 상담 지참용 진단 리포트 무제한 제공(생성 활성화)\n• AI 맞춤 진단 기능 제외",
                                price = "월 10,000원",
                                isCurrent = subscriptionTier == "BASIC",
                                onSelect = {
                                    viewModel.updateSubscription("BASIC")
                                    showBillingDialog = false
                                }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Plan 3: Premium
                            BillingPlanView(
                                title = "3. 프리미엄 멤버십 (AI 분석 및 무제한)",
                                desc = "하루 최대 10회 완료 및 체크인 기록 가능\n• 30일 누적 최대 300회 제공\n• 비뇨의학과 상담 지참용 진단 리포트 무제한 제공\n• Gemini AI 맞춤 성 호르몬/신체 회복 루틴 실시간 처방 무제한 제공!",
                                price = "월 30,000원",
                                isCurrent = subscriptionTier == "PREMIUM",
                                onSelect = {
                                    viewModel.updateSubscription("PREMIUM")
                                    showBillingDialog = false
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            TextButton(onClick = { showBillingDialog = false }) {
                                Text("닫기", color = TextMutedGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Welcome and Core positioning Branding Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SageCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SageCardElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "4050 남성을 위한 컨디션 케어",
                                color = TextMutedGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ManUp 플래너",
                                color = SandGold,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        
                        // Developer populate data helper
                        Button(
                            onClick = { viewModel.populateSampleData() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SageCardElevated,
                                contentColor = SandGold
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("demo_data_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Demo", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("데모 4주 데이터 채우기", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "피로·수면 부족·무력감을 매일 30초 기록하여 과학적인 생활 자가 회복 루틴을 훈련하세요. 내부 갱년기 분석 진단 엔진이 당신의 변화 추이를 모니터링합니다.",
                        color = TextWhite,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // 30 Seconds Check-in Trigger Widget
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isCompletedToday) SageCardElevated else SageCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.5.dp, 
                    if (isCompletedToday) SandGold else SageCardElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (viewModel.canPerformAction("CHECKIN")) {
                            viewModel.checkinDate.value = todayStr
                            viewModel.showCheckinDialog.value = true 
                        }
                    }
                    .testTag("checkin_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                if (isCompletedToday) SandGold else SageCardElevated, 
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCompletedToday) Icons.Default.CheckCircle else Icons.Default.Add,
                            contentDescription = "Check-in Status",
                            tint = if (isCompletedToday) DeepForest else SandGold,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isCompletedToday) "오늘 자가 기록 완료" else "오늘의 30초 컨디션 기록하기",
                            color = TextWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isCompletedToday) "체크인 완료! 하단 4주 분석 및 리포트에서 통계를 보세요." else "수면, 근력감, 집중력 등 핵심 필수 성인 남성 7대 지표 자가 분석",
                            color = TextMutedGreen,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Go",
                        tint = TextMutedGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Today's Routine List section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = "Routine", tint = SandGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "오늘 실행 컨디션 루틴 ($completedTodayCount / $totalActiveRoutines)",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (routines.isNotEmpty()) {
                    Text(
                        text = "길게 탭해 삭제",
                        color = TextMutedGreen,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (routines.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(SageCard, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, SageCardElevated), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("불러올 건강 루틴이 없습니다. 하단에서 AI 맞춤 수칙을 받아보세요.", color = TextMutedGreen, fontSize = 12.sp)
                }
            }
        } else {
            val chunkedRoutines = routines.chunked(2)
            items(chunkedRoutines) { rowRoutines ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowRoutines.forEach { routine ->
                        val isCompleted = completions.any { it.routineId == routine.id && it.date == todayStr }
                        val categoryEmoji = when (routine.category.lowercase()) {
                            "muscle" -> "💪"
                            "sleep" -> "💤"
                            "energy" -> "🥗"
                            "mood" -> "🧘"
                            "libido" -> "🔥"
                            "focus" -> "👥"
                            else -> "✨"
                        }
                        val categoryLabel = when (routine.category.lowercase()) {
                            "muscle" -> "근력 운동"
                            "sleep" -> "야외 수면"
                            "energy" -> "건강 식단"
                            "mood" -> "마음챙김"
                            "libido" -> "비뇨 생식"
                            "focus" -> "스트레스/소통"
                            else -> "일반 수칙"
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCompleted) SageCardElevated else SageCard
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = if (isCompleted) SandGold else SageCardElevated
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { 
                                    selectedRoutineForHistory = routine
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(SandGold.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(text = categoryEmoji, fontSize = 11.sp)
                                            Text(text = categoryLabel, color = SandGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteRoutine(routine.id) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Routine",
                                            tint = CoralRed.copy(alpha = 0.5f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = routine.title,
                                    color = if (isCompleted) SandGold else TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = routine.description,
                                    color = TextMutedGreen,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.height(28.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isCompleted) SandGold.copy(alpha = 0.15f) else SageCardElevated,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.CalendarToday,
                                        contentDescription = "Status",
                                        tint = if (isCompleted) SandGold else TextMutedGreen,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isCompleted) "오늘 실천 완료" else "기록하기 📅",
                                        color = if (isCompleted) SandGold else TextMutedGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    if (rowRoutines.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // AI Custom Routine Generator Block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SageCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SageCardElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = "AI Routine", tint = SandGold, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("자가 데이터 연동형 AI 맞춤 루틴 진단", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "기록된 7대 남성 지수를 기반으로 Gemini AI 코치가 식습관, 운동, 정신 명료성을 회복하기 위한 타겟팅 1~3가지 규칙을 설계합니다.",
                        color = TextMutedGreen,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isAiLoading) {
                        CircularProgressIndicator(color = SandGold, modifier = Modifier.size(28.dp))
                    } else {
                        Button(
                            onClick = { 
                                if (viewModel.canPerformAction("AI_ANALYSIS")) {
                                    viewModel.requestAiRoutine() 
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SandGold, contentColor = DeepForest),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("ai_routine_button")
                        ) {
                            Text("Gemini AI 맞춤 건강 제안 처방 받기", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    aiError?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(it, color = CoralRed, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Disclaimer regarding regulatory terms
                    Surface(
                        color = SageCardElevated.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = SandGold, modifier = Modifier.size(16.dp))
                            Text(
                                text = "본 기능은 웰빙 증진 가이드이며, 비뇨의학과 등 전문 의료 행위나 전문 호르몬 치료 처방을 예측 및 대체할 수 없습니다. 안전한 웰빙 건강 보조 도구로써 참고하세요.",
                                color = TextMutedGreen.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedRoutineForHistory != null) {
        RoutineHistoryCheckDialog(
            routine = selectedRoutineForHistory!!,
            viewModel = viewModel,
            completions = completions,
            onDismiss = { selectedRoutineForHistory = null }
        )
    }
}

@Composable
fun RoutineHistoryCheckDialog(
    routine: Routine,
    viewModel: MainViewModel,
    completions: List<RoutineCompletion>,
    onDismiss: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dispDaySdf = SimpleDateFormat("M월 d일 (E)", Locale.KOREAN)
    
    // Generate last 7 days (including today)
    val days = remember {
        val list = mutableListOf<Triple<String, String, Boolean>>()
        val cal = Calendar.getInstance()
        for (i in 0 until 7) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(cal.time)
            val label = when (i) {
                0 -> "오늘 - ${dispDaySdf.format(cal.time)}"
                1 -> "어제 - ${dispDaySdf.format(cal.time)}"
                2 -> "그저께 - ${dispDaySdf.format(cal.time)}"
                else -> dispDaySdf.format(cal.time)
            }
            list.add(Triple(dateStr, label, i == 0))
        }
        list
    }

    val categoryEmoji = when (routine.category.lowercase()) {
        "muscle" -> "💪"
        "sleep" -> "💤"
        "energy" -> "🥗"
        "mood" -> "🧘"
        "libido" -> "🔥"
        "focus" -> "👥"
        else -> "✨"
    }

    val categoryLabel = when (routine.category.lowercase()) {
        "muscle" -> "하체 근력 강화"
        "sleep" -> "수면 및 숙면 케어"
        "energy" -> "식단 및 신체 활력"
        "mood" -> "마음챙김 및 스트레스 완화"
        "libido" -> "비뇨 생식 활력 기운"
        "focus" -> "정신적 연대 및 소통"
        else -> "AI 맞춤 조언"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DeepForest,
            border = BorderStroke(1.5.dp, SandGold),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header details with large icon / emoji
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(SandGold.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, SandGold.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = categoryEmoji, fontSize = 32.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .background(SandGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(categoryLabel, color = SandGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = routine.title,
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = routine.description,
                    color = TextMutedGreen,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))
                
                Divider(color = SageCardElevated, thickness = 1.dp)
                
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "📅 일자별 실천 여부 체크 (최근 7일)",
                    color = SandGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 10.dp)
                )

                // Render list of 7 days
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    days.forEach { (dateStr, label, isToday) ->
                        val isCompletedOnDay = completions.any { it.routineId == routine.id && it.date == dateStr }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCompletedOnDay) SageCardElevated else SageCard
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isCompletedOnDay) SandGold.copy(alpha = 0.4f) else SageCardElevated
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (viewModel.canPerformAction("ROUTINE", isCompletedOnDay)) {
                                        viewModel.toggleRoutineCompletion(routine.id, dateStr, isCompletedOnDay)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isToday) Icons.Default.CalendarToday else Icons.Default.History,
                                        contentDescription = "Date",
                                        tint = if (isToday) SandGold else TextMutedGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        color = if (isToday) TextWhite else TextWhite.copy(alpha = 0.85f),
                                        fontSize = 13.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                                    )
                                }

                                // Large toggle checkbox
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (isCompletedOnDay) SandGold else SageCardElevated,
                                            CircleShape
                                        )
                                        .clickable {
                                            if (viewModel.canPerformAction("ROUTINE", isCompletedOnDay)) {
                                                viewModel.toggleRoutineCompletion(routine.id, dateStr, isCompletedOnDay)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCompletedOnDay) Icons.Default.CheckCircle else Icons.Default.Circle,
                                        contentDescription = if (isCompletedOnDay) "Completed" else "Incomplete",
                                        tint = if (isCompletedOnDay) DeepForest else TextMutedGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SageCardElevated, contentColor = SandGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("확인 및 닫기", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


// ==================== TAB 2: PATTERN ANALYSIS (HEATMAP & GRAPHS) ====================
@Composable
fun PatternAnalysisScreen(
    viewModel: MainViewModel,
    checkins: List<DailyCheckin>,
    completions: List<RoutineCompletion>
) {
    var chartIndexScope by remember { mutableStateOf(0) } // 0 = 신체 활력(수면+에너지+근력), 1 = 신경성(기분+집중+뇌안개+성욕)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("28일 실천 및 인지 패턴 분석", color = SandGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("기록 강도와 습관 완수 기록에 입각한 4주간 종합 바이오리듬 정밀 정보", color = TextMutedGreen, fontSize = 12.sp)
        }

        // HEATMAP GRID (28 Days Tracker)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SageCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SageCardElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("4주 기록 히트맵 실천도 (최근 28일)", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("각 사각형은 날짜를 표시하며, 금색 원 크기는 당일 습관 완수 개수입니다.", color = TextMutedGreen, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    HeatmapGrid(checkins = checkins, completions = completions)

                    Spacer(modifier = Modifier.height(12.dp))
                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(10.dp).background(SageCardElevated, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("기록 없음", color = TextMutedGreen, fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.size(10.dp).background(SageCardElevated.copy(alpha = 1.5f), RoundedCornerShape(2.dp)).border(1.dp, TextWhite.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("기록 완료", color = TextMutedGreen, fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.size(8.dp).background(SandGold, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("습관 달성 완료", color = TextMutedGreen, fontSize = 10.sp)
                    }
                }
            }
        }

        // TREND OVER TIME CHART (Custom Line Drawing)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SageCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SageCardElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (chartIndexScope == 0) "피지컬 스타미나 지수 흐름" else "메디 멘탈 케어 추이",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Selector
                        Row(
                            modifier = Modifier
                                .background(SageCardElevated, RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {
                            Text(
                                "신체활력",
                                color = if (chartIndexScope == 0) DeepForest else TextMutedGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (chartIndexScope == 0) SandGold else Color.Transparent)
                                    .clickable { chartIndexScope = 0 }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Text(
                                "정신피로",
                                color = if (chartIndexScope == 1) DeepForest else TextMutedGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (chartIndexScope == 1) SandGold else Color.Transparent)
                                    .clickable { chartIndexScope = 1 }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (chartIndexScope == 0) "수면품질 + 신체의 힘 + 기초활력 가중 수치를 기반으로 계산" 
                               else "스트레스 기분지수 + 집중도 + 뇌안개(두뇌 명료도 가중치 역추산) 수치",
                        color = TextMutedGreen, 
                        fontSize = 11.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    if (checkins.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("비교 분석할 지난 데이터 로그가 없습니다.", color = TextMutedGreen, fontSize = 12.sp)
                        }
                    } else {
                        // Custom Canvas Line Graph
                        BiorythmLineGraph(
                            checkins = checkins,
                            scopeType = chartIndexScope
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeatmapGrid(checkins: List<DailyCheckin>, completions: List<RoutineCompletion>) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance()
    
    // Generate dates lists for last 28 days
    val datesList = remember(checkins, completions) {
        val list = mutableListOf<String>()
        val tempCal = Calendar.getInstance()
        for (i in 27 downTo 0) {
            tempCal.time = Date()
            tempCal.add(Calendar.DAY_OF_YEAR, -i)
            list.add(sdf.format(tempCal.time))
        }
        list
    }

    // Grid layout: 4 columns x 7 rows. Column index is Week (Week 1, Week 2, Week 3, Week 4)
    // Row is Mon, Tue, Wed, Thu, Fri, Sat, Sun.
    // To make it easy to draw, let's draw it as direct columns!
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val daysOfWeekLabel = listOf("월", "화", "수", "목", "금", "토", "일")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Spacer(modifier = Modifier.size(16.dp)) // Header corner space
            daysOfWeekLabel.forEach { dayName ->
                Box(
                    modifier = Modifier.height(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(dayName, color = TextMutedGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Draw 4 columns (weeks)
        for (w in 0..3) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Header (Week 1 to Week 4)
                Text("주차 ${w + 1}", color = TextMutedGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                for (d in 0..6) {
                    val dateIndex = w * 7 + d
                    val dateStr = datesList.getOrNull(dateIndex)
                    
                    val dayCheckin = dateStr?.let { dStr -> checkins.find { it.date == dStr } }
                    val hasLog = dayCheckin != null
                    val completedRoutinesOnThisDay = dateStr?.let { dStr -> completions.filter { it.date == dStr }.size } ?: 0

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (hasLog) SageCardElevated else SageCardElevated.copy(alpha = 0.25f)
                            )
                            .border(
                                width = if (hasLog) 1.dp else 0.5.dp,
                                color = if (hasLog) TextWhite.copy(alpha = 0.4f) else SageCardElevated.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (completedRoutinesOnThisDay > 0) {
                            val dotSize = (4 + (completedRoutinesOnThisDay * 2)).coerceAtMost(14).dp
                            Box(
                                modifier = Modifier
                                    .size(dotSize)
                                    .background(SandGold, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BiorythmLineGraph(checkins: List<DailyCheckin>, scopeType: Int) {
    // We reverse checkins to print them chronologically (older is left, newer is right)
    val chronologicalLogs = remember(checkins) {
        checkins.take(14).reversed() // Show last 14 logs for density cleanliness
    }

    val maxVal = 5f
    val minVal = 1f

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        val width = size.width
        val height = size.height
        val stepX = width / (chronologicalLogs.size - 1).coerceAtLeast(1)

        val points = mutableListOf<Offset>()
        for (idx in chronologicalLogs.indices) {
            val log = chronologicalLogs[idx]
            val score = if (scopeType == 0) {
                // Physical: average of sleep (1-5), energy (1-5), muscle (1-5)
                (log.sleepQuality + log.energy + log.muscleStrength) / 3f
            } else {
                // Mental: mood (1-5) + focus (1-5) + inverted brainFog (6 - brainFog) + libido (1-5) / 4
                (log.mood + log.focus + (6f - log.brainFog) + log.libido) / 4f
            }

            // Normalization: y coord rises downward in computer coordinates, so invert.
            val normY = (score - minVal) / (maxVal - minVal)
            val yPos = height - (normY * height)
            val xPos = idx * stepX
            points.add(Offset(xPos, yPos))
        }

        // Draw grid lines
        val gridCount = 4
        for (g in 0..gridCount) {
            val gY = height * (g / gridCount.toFloat())
            drawLine(
                color = Color(0xFF1B2721),
                start = Offset(0f, gY),
                end = Offset(width, gY),
                strokeWidth = 1f
            )
        }

        // Draw line connection brush paths
        if (points.isNotEmpty()) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(points.first().x, points.first().y)
                for (pIdx in 1 until points.size) {
                    lineTo(points[pIdx].x, points[pIdx].y)
                }
            }

            // Draw line
            drawPath(
                path = path,
                color = SandGold,
                style = Stroke(width = 4f)
            )

            // Draw circular handles or nodes
            points.forEach { pt ->
                drawCircle(
                    color = DeepForest,
                    radius = 8f,
                    center = pt
                )
                drawCircle(
                    color = SandGold,
                    radius = 5f,
                    center = pt
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    
    // Bottom dates labeling
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (chronologicalLogs.isNotEmpty()) {
            val startLabel = chronologicalLogs.first().date.substring(5)
            val endLabel = chronologicalLogs.last().date.substring(5)
            Text(text = "시작($startLabel)", color = TextMutedGreen, fontSize = 11.sp)
            Text(text = "안정화 흐름 추이", color = TextMutedGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = "현재($endLabel)", color = TextMutedGreen, fontSize = 11.sp)
        }
    }
}


// ==================== TAB 3: HOSPITAL CONSULTATION CLINIC-PREP REPORT ====================
@Composable
fun HospitalReportScreen(
    viewModel: MainViewModel,
    checkins: List<DailyCheckin>,
    completions: List<RoutineCompletion>
) {
    var showShareNotification by remember { mutableStateOf(false) }
    val subscriptionTier by viewModel.subscriptionTier.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("병원 비뇨의학과 상담 지참용 진단 보조 파일", color = SandGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("본 자격요건 리포트는 의사와의 검진 상담 시간을 최고 배율로 단축하고 자가보고 데이터를 공식 제출하기 위한 A4 융합 규격 문서입니다.", color = TextMutedGreen, fontSize = 12.sp)
        }

        if (subscriptionTier == "FREE_TRIAL") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SageCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, SandGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(SandGold.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked Report",
                                tint = SandGold,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "병원 지참용 리포트 잠김",
                            color = SandGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "무료 체험판(Free Trial) 등급에서는 비뇨의학과 전문 상담 리포트 조회가 제한됩니다.\n\n베이직 또는 프리미엄 멤버십으로 업그레이드하시면 4주 패턴 분석 정밀 진단서 및 A4 규격 지참 서류를 다운로드할 수 있습니다.",
                            color = TextWhite,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.updateSubscription("BASIC") },
                            colors = ButtonDefaults.buttonColors(containerColor = SandGold, contentColor = DeepForest),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("upgrade_basic_button")
                        ) {
                            Text("베이직 멤버십 시작 (월 10,000원)", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.updateSubscription("PREMIUM") },
                            colors = ButtonDefaults.buttonColors(containerColor = SageCardElevated, contentColor = SandGold),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SandGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("upgrade_premium_button")
                        ) {
                            Text("프리미엄 멤버십 시작 (월 30,000원)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Action Buttons: Share / Save / Test Mock Trigger
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showShareNotification = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SandGold, contentColor = DeepForest),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("A4 규격 PDF로 공유하기", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Clinical Report Card Render
            item {
                ClinicalReportPaperBlueprint(
                    checkins = checkins,
                    completions = completions
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showShareNotification) {
        AlertDialog(
            onDismissRequest = { showShareNotification = false },
            title = { Text("리포트 파일 공유 완료", color = SandGold, fontWeight = FontWeight.Bold) },
            text = { Text("28일 통계 기반 환자 상태 요약본이 'ManUp_상담준비자료_자가제출용.pdf' 형식 파일로 모바일 기기 저장 하거나 의사에게 공유할 준비가 완료되었습니다.\n\n(참고: 본 PDF는 의료법을 준수하여 환자의 순수 자가기록 정리본으로, 처방전이 아닙니다.)", color = TextWhite, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { showShareNotification = false }) {
                    Text("확인", color = SandGold, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SageCard,
            tonalElevation = 6.dp
        )
    }
}

// A4 style clinical paper card
@Composable
fun ClinicalReportPaperBlueprint(
    checkins: List<DailyCheckin>,
    completions: List<RoutineCompletion>
) {
    // Calculations of TDS indexes
    val totalLogs = checkins.size
    val lowEnergyCount = checkins.filter { it.energy <= 2 }.size
    val sleepIssuesCount = checkins.filter { it.sleepQuality <= 2 }.size
    val focusDeclineCount = checkins.filter { it.focus <= 2 }.size
    val muscleWeaknessCount = checkins.filter { it.muscleStrength <= 2 }.size
    val libraryBrainFogCount = checkins.filter { it.brainFog >= 4 }.size
    val libidoLossCount = checkins.filter { it.libido <= 2 }.size

    val lowEnergyPercent = if (totalLogs > 0) (lowEnergyCount * 100 / totalLogs) else 0
    val insomniaPercent = if (totalLogs > 0) (sleepIssuesCount * 100 / totalLogs) else 0
    val focusPercent = if (totalLogs > 1) (focusDeclineCount * 100 / totalLogs) else 0
    val muscleWeaknessPercent = if (totalLogs > 0) (muscleWeaknessCount * 100 / totalLogs) else 0
    val libidoLossPercent = if (totalLogs > 0) (libidoLossCount * 100 / totalLogs) else 0

    val writtenNotes = checkins.filter { it.note.isNotEmpty() }.take(5)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White), // White paper contrast
        shape = RoundedCornerShape(4.dp), // Crisp sharp paper look
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFFC7A259), RoundedCornerShape(4.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Text(
                text = "MANUP CLINICAL DISCOVERY PREPARATION REPORT",
                color = Color(0xFF1B2721),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "비뇨의학과/내분비내과 의사 상담용 비처방 자가기술 보고서",
                color = Color(0xFF555555),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFF1B2721), thickness = 2.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Metadata information table
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("• 대상자 식별 : 익명 환자 (4050 성인남성)", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("• 분석 기한 : 최근 28일 트래커 로그", color = Color.DarkGray, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("• 기록 횟수 : $totalLogs 회", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("• 완수 통계 : ${completions.size} 건 축적", color = Color.DarkGray, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFFDDDDDD), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: Symptom Frequency Analysis
            Text("1. 지표 가중 누적 결손 관찰 분석 (%)", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SymptomPaperRow("심각한 활력 저하 및 무기력 체감 비율", lowEnergyPercent, "만성 피로 호소 기준")
                SymptomPaperRow("수면 장애 및 아침 기상 둔화 빈도", insomniaPercent, "숙면 부실 보고 기준")
                SymptomPaperRow("신체 활력 및 하체 낙맥(근력 감소) 이상징후", muscleWeaknessPercent, "체 근육 소실 기준")
                SymptomPaperRow("감퇴 성욕 및 생식 호르몬 저하 지표 빈도", libidoLossPercent, "순수 활력 다운 기준")
            }

            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = Color(0xFFDDDDDD), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Clinical Summary Insight
            Text("2. 전임상 자가기록 요약 및 전문의 지참 메모", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9F9F9), RoundedCornerShape(6.dp))
                    .padding(14.dp)
            ) {
                Column {
                    val highlightAdvisory = when {
                        totalLogs == 0 -> "자가 보고 기록이 없습니다. '30초 체크인' 데이터를 쌓으면 혈당, 대사 증상 가중치를 판정 보조합니다."
                        libidoLossPercent > 50 || muscleWeaknessPercent > 50 -> "※ 비뇨기 호르몬 분비 감퇴 연관 유의성 발견: 최근 28일 중 50%를 상회하여 근력 저하 및 생식기 활력 정체가 보조 관찰됩니다. 남성갱년기 증후군(TDS) 혈액 검사를 의사에게 건의하기에 충분합니다."
                        lowEnergyPercent > 40 || insomniaPercent > 40 -> "※ 부신 피로 및 심혈관 생물학적 사이클 둔화: 수면 박탈 및 급격한 에너지 다운이 동시 관찰되어, 멜라토닌 및 성장 호르몬 이상 유동성이 수면 다원 가이드를 요청하도록 추천하는 단계입니다."
                        else -> "※ 현재 완만한 대사 균형을 유지 중이나, 주기적인 실천 루틴 습관 강화를 통해 남성 컨디션 사이클이 더욱 증강되어 회복력 기반을 다질 수 있습니다."
                    }

                    Text(
                        text = highlightAdvisory,
                        color = if (libidoLossPercent > 50 || lowEnergyPercent > 40) Color(0xFFC2185B) else Color.DarkGray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFFDDDDDD), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: Subjective Patient Feedback Timeline
            Text("3. 환자 자가 작성 상세 주관 기록 기록지 (최신 5선)", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            if (writtenNotes.isEmpty()) {
                Text("– 미세 증상 기록이 기재되지 않았습니다.", color = Color.Gray, fontSize = 11.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    writtenNotes.forEach { item ->
                        Text(
                            text = "• [${item.date}] \"${item.note}\"",
                            color = Color.DarkGray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = Color(0xFF1B2721), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom clinic advisory stamp
            Text(
                text = "의사 안내문: 본 자진보고서는 환자의 매일 연속 자가 모니터링 축적 기록입니다. 신체 및 호르몬 균형(TDS/PADAM) 진단과 생활 보조 영양 교정 처치를 처방하기에 앞서 신뢰성 있는 환자 기초 통계 보조 정보 카드로 활용하십시오.",
                color = Color.Gray,
                fontSize = 9.sp,
                lineHeight = 13.sp,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SymptomPaperRow(title: String, percentage: Int, subtitle: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.Gray, fontSize = 9.sp)
            }
            Text("$percentage %", color = if (percentage > 40) Color.Red else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(3.dp))
        // Simulated progress bar representing ratio
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color(0xFFEEEEEE), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (percentage / 100f).coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(if (percentage > 40) Color(0xFFE57373) else Color(0xFF1B2721), RoundedCornerShape(2.dp))
            )
        }
    }
}


// ==================== CHECK-IN OVERLAY / DIALOG ====================
@Composable
fun CheckinDialog(
    onDismiss: () -> Unit,
    onSave: (sleep: Int, energy: Int, mood: Int, focus: Int, fog: Int, libido: Int, muscle: Int, note: String) -> Unit,
    initialDate: String
) {
    var sleepValue by remember { mutableStateOf(3f) }
    var energyValue by remember { mutableStateOf(3f) }
    var moodValue by remember { mutableStateOf(3f) }
    var focusValue by remember { mutableStateOf(3f) }
    var brainFogValue by remember { mutableStateOf(3f) }
    var libidoValue by remember { mutableStateOf(3f) }
    var muscleValue by remember { mutableStateOf(3f) }
    var noteText by remember { mutableStateOf("") }

    var stepIndex by remember { mutableStateOf(0) } // 0 = 피지컬 활력, 1 = 신경&멘탈, 2 = 최종 작성

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp)),
            color = DeepForest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("30초 남성 바이오 루프 체크인", color = SandGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("입력 기준일: $initialDate", color = TextMutedGreen, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextWhite)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                // Progress segments indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(
                                    if (index <= stepIndex) SandGold else SageCardElevated,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Scrollable slider parameters
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    when (stepIndex) {
                        0 -> {
                            Text("Step 1: 신체 물리 에너지 지표 (1-5)", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))

                            MetricSliderCard("수면 품질", sleepValue, "숙면 기상이 매우 상쾌함", "수면 끊김 & 피로 심함") { sleepValue = it }
                            Spacer(modifier = Modifier.height(14.dp))
                            MetricSliderCard("활력 에너지", energyValue, "피로가 없는 하루 완성", "아침 무기력 동반") { energyValue = it }
                            Spacer(modifier = Modifier.height(14.dp))
                            MetricSliderCard("근력 및 코어 활성도", muscleValue, "탄탄하고 운동 소화 높음", "근감소 및 신체 쇠퇴 기미") { muscleValue = it }
                        }
                        1 -> {
                            Text("Step 2: 인지 정신 및 성 호르몬 지표 (1-5)", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))

                            MetricSliderCard("뇌안개 지수 (인기)", brainFogValue, "뇌가 맑음(명료도 높음)", "두뇌 가물가물하고 멍함") { brainFogValue = it }
                            Spacer(modifier = Modifier.height(14.dp))
                            MetricSliderCard("집중력 & 결정력", focusValue, "정교한 비즈니스 몰두함", "쉽게 지치고 집중 붕괴") { focusValue = it }
                            Spacer(modifier = Modifier.height(14.dp))
                            MetricSliderCard("생식기 활력 증강 (libido)", libidoValue, "아침 활력 및 성욕 개선", "비활성화 및 정체 정서") { libidoValue = it }
                        }
                        2 -> {
                            Text("Step 3: 감정 기분 및 추가 증상 비망록 (선택)", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))

                            MetricSliderCard("당일 감정선 (기분)", moodValue, "여유롭고 지치지 않는 평온", "불안 및 예민한 정서") { moodValue = it }
                            Spacer(modifier = Modifier.height(16.dp))

                            Text("추가 자가보고 기재지 (아침 발기 유무, 안면 홍조, 만성 통증 등 작성)", color = TextMutedGreen, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            TextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                placeholder = { Text("피로누적 부위에 관한 상세 고백 또는 처방 수강 노트를 기재하세요.", fontSize = 12.sp, color = TextMutedGreen) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = SageCard,
                                    unfocusedContainerColor = SageCard,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedIndicatorColor = SandGold,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (stepIndex > 0) {
                        Button(
                            onClick = { stepIndex -= 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = SageCardElevated, contentColor = TextWhite)
                        ) {
                            Text("이전")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (stepIndex < 2) {
                        Button(
                            onClick = { stepIndex += 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = SandGold, contentColor = DeepForest)
                        ) {
                            Text("다음 인터뷰")
                        }
                    } else {
                        Button(
                            onClick = {
                                onSave(
                                    sleepValue.toInt(),
                                    energyValue.toInt(),
                                    moodValue.toInt(),
                                    focusValue.toInt(),
                                    brainFogValue.toInt(),
                                    libidoValue.toInt(),
                                    muscleValue.toInt(),
                                    noteText
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SandGold, contentColor = DeepForest),
                            modifier = Modifier.testTag("save_checkin_button")
                        ) {
                            Text("기록 완료하고 저장")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricSliderCard(title: String, score: Float, bestLabel: String, worstLabel: String, onValueChange: (Float) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SageCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, SageCardElevated)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("값: ${score.toInt()}", color = SandGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = score,
                onValueChange = onValueChange,
                valueRange = 1f..5f,
                steps = 3, // Shows dots for 1, 2, 3, 4, 5
                colors = SliderDefaults.colors(
                    thumbColor = SandGold,
                    activeTrackColor = SandGold,
                    inactiveTrackColor = SageCardElevated,
                    activeTickColor = DeepForest,
                    inactiveTickColor = SandGold
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(worstLabel, color = TextMutedGreen, fontSize = 10.sp)
                Text(bestLabel, color = TextMutedGreen, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun OnboardingWelcomeScreen(viewModel: MainViewModel) {
    var isSimulatingLogin by remember { mutableStateOf(false) }
    var loginProviderText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepForest)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant Emblem icon (representing vitality/strength)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(SandGold.copy(alpha = 0.1f), CircleShape)
                    .border(1.5.dp, SandGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "ManUp Logo",
                    tint = SandGold,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ManUp (맨업)",
                color = SandGold,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "4050 남성 맞춤 건강 지표 및 실천 루틴 관리",
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Informative cards of advantages
            Card(
                colors = CardDefaults.cardColors(containerColor = SageCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, SageCardElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureCheckRow("30초 바이오 활성 체크인", "아침 피로감, 집중도, 수면, 생식 활력 기운 축적")
                    FeatureCheckRow("비뇨의학 비처방 진단 리포트", "의사 상담 시간을 10배 단축하는 지참용 기록 자동 정리")
                    FeatureCheckRow("회복 증진 건강 행동 수칙", "하반신 근육 증강 운동, 스트레스 제어 실천 리스트")
                    FeatureCheckRow("Gemini AI 실시간 맞춤 처방", "적체된 바이오 지표 이상기복에 따른 생활 팁 추천")
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            if (isSimulatingLogin) {
                CircularProgressIndicator(color = SandGold, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(loginProviderText, color = SandGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else {
                Text(
                    "간편 가입 및 체험 계정으로 시작",
                    color = TextMutedGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Google Authentication Button
                Button(
                    onClick = {
                        isSimulatingLogin = true
                        loginProviderText = "Google 계정 동기화 요청 중..."
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(1000)
                            viewModel.login("GOOGLE", "홍지훈 (51세)", 51)
                            isSimulatingLogin = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF333333)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("google_login_button")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Google Logo",
                                tint = Color(0xFFEA4335),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text("Google 계정으로 계속하기", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Kakao Authentication Button
                Button(
                    onClick = {
                        isSimulatingLogin = true
                        loginProviderText = "카카오 인증 서버 연동 중..."
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(1000)
                            viewModel.login("KAKAO", "임석호 (47세)", 47)
                            isSimulatingLogin = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE500), contentColor = Color(0xFF3C1E1E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("kakao_login_button")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF3C1E1E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("K", color = Color(0xFFFEE500), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text("카카오톡 계정으로 계속하기", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Guest option (FREE_TRIAL)
                TextButton(
                    onClick = {
                        viewModel.login("NONE", "게스트 체험 회원", 45)
                    },
                    modifier = Modifier.testTag("guest_login_button")
                ) {
                    Text(
                        "비회원으로 먼저 둘러보기 (무료체험)",
                        color = SandGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureCheckRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Eligible",
            tint = SandGold,
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Column {
            Text(title, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextMutedGreen, fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
fun BillingPlanView(
    title: String,
    desc: String,
    price: String,
    isCurrent: Boolean,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) SageCardElevated else SageCard
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isCurrent) 1.5.dp else 0.5.dp,
            color = if (isCurrent) SandGold else SageCardElevated
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = if (isCurrent) SandGold else TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .background(SandGold, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("사용 중", color = DeepForest, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, color = TextMutedGreen, fontSize = 10.sp, lineHeight = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(price, color = SandGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MyPageScreen(
    viewModel: MainViewModel,
    checkins: List<DailyCheckin>,
    completions: List<RoutineCompletion>
) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userAge by viewModel.userAge.collectAsStateWithLifecycle()
    val userProfileUri by viewModel.userProfileUri.collectAsStateWithLifecycle()
    val subscriptionTier by viewModel.subscriptionTier.collectAsStateWithLifecycle()
    val loggedInProvider by viewModel.loggedInProvider.collectAsStateWithLifecycle()

    var isEditingProfile by remember { mutableStateOf(false) }
    var tempName by remember(userName) { mutableStateOf(userName) }
    var tempAge by remember(userAge) { mutableStateOf(userAge) }

    var showPresetPicker by remember { mutableStateOf(false) }

    val totalCheckins = checkins.size
    val totalCompletions = completions.size

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateProfileImage(uri.toString())
        }
    }

    val presetAvatars = listOf(
        "🧔" to "클래식 맨업",
        "🏌️" to "활력 필드 골퍼",
        "🏃" to "러너 파워 지망",
        "🧘" to "마음챙김 마스터",
        "💪" to "근력 벌크업",
        "✨" to "오리지널 시그니처"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepForest)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Title Header
        item {
            Column {
                Text(
                    text = "마이페이지",
                    color = TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "내 프로필 관리, 실천 이력 확인 및 요금제 설정",
                    color = TextMutedGreen,
                    fontSize = 12.sp
                )
            }
        }

        // 1. PROFILE CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SageCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SageCardElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Image
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(SageCardElevated)
                            .border(2.dp, SandGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfileUri.isNotEmpty() && !userProfileUri.startsWith("preset:")) {
                            AsyncImage(
                                model = userProfileUri,
                                contentDescription = "Profile Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Render presets or fallback icon
                            val presetEmoji = presetAvatars.find { "preset:${it.first}" == userProfileUri }?.first ?: "🧔"
                            Text(text = presetEmoji, fontSize = 52.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!isEditingProfile) {
                        Text(
                            text = "$userName (${userAge}세)",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(SandGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (loggedInProvider == "NONE") "게스트 회원" else "$loggedInProvider 로그인 중",
                                    color = SandGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SageCardElevated,
                                    contentColor = TextWhite
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Upload", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("사진 업로드", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showPresetPicker = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SageCardElevated,
                                    contentColor = SandGold
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                            ) {
                                Icon(Icons.Default.Face, contentDescription = "Preset", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("캐릭터 선택", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { isEditingProfile = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SandGold,
                                    contentColor = DeepForest
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("정보 수정", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Profile Editing Form
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextField(
                                value = tempName,
                                onValueChange = { tempName = it },
                                label = { Text("이름", color = TextMutedGreen) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = SageCardElevated,
                                    unfocusedContainerColor = SageCardElevated,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedIndicatorColor = SandGold,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            var ageInputText by remember { mutableStateOf(tempAge.toString()) }
                            TextField(
                                value = ageInputText,
                                onValueChange = {
                                    ageInputText = it
                                    val parsed = it.toIntOrNull()
                                    if (parsed != null) {
                                        tempAge = parsed
                                    }
                                },
                                label = { Text("나이 (세)", color = TextMutedGreen) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = SageCardElevated,
                                    unfocusedContainerColor = SageCardElevated,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedIndicatorColor = SandGold,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { isEditingProfile = false },
                                    border = BorderStroke(1.dp, TextMutedGreen),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("취소", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.updateProfileInfo(tempName, tempAge)
                                        isEditingProfile = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SandGold, contentColor = DeepForest),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("저장", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. STATS & ANALYTICS REPORT
        item {
            Column {
                Text(
                    text = "📊 나의 실천 및 기록 통계",
                    color = SandGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                     colors = CardDefaults.cardColors(containerColor = SageCard),
                     shape = RoundedCornerShape(16.dp),
                     border = BorderStroke(1.dp, SageCardElevated),
                     modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("체크인 총 기록", color = TextMutedGreen, fontSize = 11.sp)
                                Text("$totalCheckins 회", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(modifier = Modifier.width(1.dp).height(36.dp).background(SageCardElevated))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("루틴 실천 완료", color = TextMutedGreen, fontSize = 11.sp)
                                Text("$totalCompletions 회", color = SandGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = SageCardElevated, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Brief smart summary
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(SandGold.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = SandGold, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                val greetingMsg = if (totalCompletions >= 15) {
                                    "대단해요! 왕성한 체력 루틴을 충실히 완성하며 흔들림 없는 컨디션을 유지하고 계십니다."
                                } else if (totalCompletions >= 5) {
                                    "멋진 시작입니다! 건강 분석 이력을 계속 쌓아 몸과 정신의 활기를 매일 업그레이드하세요."
                                } else {
                                    "맨업 루틴을 시작한 당신, 우아하고 확실하게 중년의 활기를 회복할 완벽한 기초가 서고 있습니다."
                                }
                                Text(
                                    text = greetingMsg,
                                    color = TextWhite,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. PRICING & BILLING PLAN CONFIG
        item {
            Column {
                Text(
                    text = "💳 맨업 멤버십 요금제 변경",
                    color = SandGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "클릭하여 원하는 멤버십 등급으로 바로 자유롭게 가입/변경할 수 있습니다.",
                    color = TextMutedGreen,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BillingPlanView(
                        title = "🎁 무료 체험판 멤버십 (FREE TRIAL)",
                        desc = "맨업의 6대 코어 루틴 체크 기능 무제한 실습 체험 권한을 부여하며, 일상 기록 일지는 총 10회까지 저장 분석이 가능합니다.",
                        price = "우선 체험권 / ₩0 무료",
                        isCurrent = subscriptionTier == "FREE_TRIAL",
                        onSelect = { viewModel.updateSubscription("FREE_TRIAL") }
                    )

                    BillingPlanView(
                        title = "⭐ 베이직 멤버십 (BASIC)",
                        desc = "월 3회 기록 한도 내에서 병원 리포트를 출력하여 실제 전립선 및 혈류 상담 의무와 실시간 연동이 구성되는 가성비 기반 분석 등급입니다.",
                        price = "정기 가입 / 월 ₩10,000",
                        isCurrent = subscriptionTier == "BASIC",
                        onSelect = { viewModel.updateSubscription("BASIC") }
                    )

                    BillingPlanView(
                        title = "👑 최고 등급 프리미엄 멤버십 (PREMIUM)",
                        desc = "일일 최대 10회 기록 한도까지 확장해 드리며, 맨업 AI가 직접 생성 주도권을 지닌 초개인화 전립선 피지크·수면 명상 AI 수칙 무제한 제안 처방이 연동됩니다.",
                        price = "최고 등급 / 월 ₩30,000",
                        isCurrent = subscriptionTier == "PREMIUM",
                        onSelect = { viewModel.updateSubscription("PREMIUM") }
                    )
                }
            }
        }

        // 4. SHUTDOWN OR LOG OUT BUTTON LINE
        item {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { viewModel.logout() },
                border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .defaultMinSize(minHeight = 44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Log out", modifier = Modifier.size(16.dp))
                    Text("안전하게 로그아웃", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Avatar Selection Dialog
    if (showPresetPicker) {
        Dialog(onDismissRequest = { showPresetPicker = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DeepForest,
                border = BorderStroke(1.5.dp, SandGold),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "캐릭터 아바타 프로필 선택",
                        color = SandGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "나의 일상 활력 콘셉트에 맞는 아바타를 선택해 보세요.",
                        color = TextMutedGreen,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        presetAvatars.chunked(2).forEach { rowPresets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowPresets.forEach { (emoji, label) ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SageCard),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, SageCardElevated),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                viewModel.updateProfileImage("preset:$emoji")
                                                showPresetPicker = false
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(emoji, fontSize = 28.sp)
                                            Text(
                                                label,
                                                color = TextWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showPresetPicker = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SageCardElevated, contentColor = TextWhite),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("닫기", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
