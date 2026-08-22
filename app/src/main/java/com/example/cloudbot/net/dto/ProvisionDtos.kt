package com.example.cloudbot.net.dto

data class ProvisionWifiRequest(
    val correlationId: String,
    val ssid: String,
    val pass: String
)

data class ProvisionPollResponse(
    val ok: Boolean,
    val correlationId: String? = null,
    val state: String? = null,
    val wifi: Boolean? = null,
    val staIp: String? = null
)

data class ProvisionStatusResponse(
    val ok: Boolean = false,
    val hubId: String? = null,
    val state: String? = null,
    val apSsid: String? = null,
    val apIp: String? = null
)

data class StatusResponse(
    val hubId: String? = null,
    val wifi: Boolean? = null,
    val ip: String? = null,
    val mdns: String? = null,
    val apSsid: String? = null,
    val irLearning: Boolean? = null,
    val rfLearning: Boolean? = null
)