package com.example.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>
)

@JsonClass(generateAdapter = true)
data class PartResponse(
    val text: String?
)

@JsonClass(generateAdapter = true)
data class ContentResponse(
    val parts: List<PartResponse>
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: ContentResponse
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)
