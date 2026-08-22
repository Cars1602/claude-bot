package com.example.cloudbot.net.dto

// ===================== IR =====================
data class LearnStartRequest(
    val correlationId: String,
    val timeoutMs: Int = 10000,
    val minRawLen: Int = 60
)

data class LearnPollResponse(
    val ok: Boolean,
    val learning: Boolean? = null,
    val correlationId: String? = null,
    val resultReady: Boolean? = null,
    val result: LearnResult? = null
)

data class LearnResult(
    val ok: Boolean,
    val correlationId: String,
    val khz: Int,
    val rawLen: Int,
    val raw: List<Long>
)

data class IrSendRequest(
    val correlationId: String,
    val khz: Int = 38,
    val repeat: Int = 1,
    val raw: List<Long>
)

data class IrSendResponse(
    val ok: Boolean,
    val correlationId: String? = null
)

// ===================== RF433 =====================
data class RfLearnStartRequest(
    val correlationId: String,
    val timeoutMs: Int = 10000,
    val minPulses: Int = 150
)

data class RfLearnPollResponse(
    val ok: Boolean,
    val learning: Boolean? = null,
    val correlationId: String? = null,
    val resultReady: Boolean? = null,
    val result: RfLearnResult? = null
)

data class RfLearnResult(
    val ok: Boolean,
    val correlationId: String,
    val rawLen: Int,
    val raw: List<Long>
)

data class RfSendRequest(
    val correlationId: String,
    val repeat: Int = 6,
    val raw: List<Long>
)

data class RfSendResponse(
    val ok: Boolean,
    val correlationId: String? = null
)