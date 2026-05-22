package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun generateCustomRoutine(
        userMetricsSummary: String,
        onSuccess: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            onError("Gemini API 키가 설정되지 않았습니다. AI 스튜디오 Secrets 패널에서 GEMINI_API_KEY를 추가해주세요.")
            return
        }

        val prompt = """
            당신은 40대 및 50대 남성들의 건강 컨디션과 회복력, 그리고 체력(남성 갱년기 전반 증상)을 완화하는 것을 돕는 전문 헬스케어 코치입니다.
            최근 작성자가 남긴 일일 건강 체크인 및 컨디션 데이터 요약은 다음과 같습니다:
            $userMetricsSummary

            이 사용자를 위해 오직 맞춤 건강 '오늘의 루틴' 제안을 정확히 3개 생성해주세요.
            의료적 '진보된 진단'이나 '처방', '치료' 혹은 '테스토스테론 호르몬 상승 예측/처방' 등 의학적 판단 표현은 의료 규제 리스크 방지를 위해 절대 배제하고, 순수하게 웰빙, 라이프스타일, 식이, 안전한 운동 등의 "생활 관리 제안"으로 구성해야 합니다.
            전문의 감수 하에 작성되는 참고 도구 형식이라는 점을 명시해주기 위해 수면 개선, 스트레스 해소, 근력 및 회복에 대한 아주 실질적이고 따라하기 쉬운 루틴을 알려주세요.

            응답은 반드시 아래 형식과 같이 한 줄에 하나씩 정확히 3개의 루틴 제안을 출력해야 합니다. JSON 구조 대신 일반 텍스트 포맷으로 각 루틴을 슬래시(|) 기호로 [제목|설명] 구분하여 전달하세요:
            루틴1_제목 | 루틴1_설명
            루틴2_제목 | 루틴2_설명
            루틴3_제목 | 루틴3_설명

            예시:
            가벼운 스쿼트 15회 | 하체 근력은 테스토스테론 균형과 회복력에 매우 유익합니다. 무리하지 않고 가볍게 진행합시다.
            햇볕 쬐며 산책 20분 | 비타민 D는 기분 조절과 호르몬 생합성에 기본 요소입니다. 낮 호르몬 분비를 유도하세요.
            따뜻한 차 마시기 | 수면 1시간 전 스마트폰 사용을 멀리하고 심신의 긴장을 풀어 숙면 환경을 유도하세요.

            이 규칙에 어긋나지 않게, 제목과 설명을 슬래시(|)로 구분하여 딱 3줄만 응답해주세요.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (responseText != null) {
                val lines = responseText.split("\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it.contains("|") }
                    .take(3)
                
                if (lines.isNotEmpty()) {
                    onSuccess(lines)
                } else {
                    // Fallback to defaults
                    onSuccess(listOf(
                        "스쿼트 15회 | 하체 자극은 하반신 컨디션 회복의 혈행 개선과 코어 힘 강화에 가장 좋습니다.",
                        "점심 야외 산책 20분 | 세로토닌 합성으로 밤 숙면에 크나큰 도움이 되어 멜라토닌 분비를 촉진합니다.",
                        "따뜻한 수분 섭취 | 카페인을 줄이고 따듯하게 차나 물을 섭취해 체온과 면역 순환계를 보호하세요."
                    ))
                }
            } else {
                onError("Generative model returned empty response")
            }
        } catch (e: Exception) {
            onError("네트워크 요청 실패: ${e.message}")
        }
    }
}
