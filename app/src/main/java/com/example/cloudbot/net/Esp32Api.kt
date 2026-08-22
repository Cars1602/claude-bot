package com.example.cloudbot.net

import com.example.cloudbot.net.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface Esp32Api {
    @GET("/provision/status")
    suspend fun provisionStatus(): ProvisionStatusResponse

    @POST("/provision/wifi")
    suspend fun provisionWifi(@Body req: ProvisionWifiRequest): Map<String, Any>

    @GET("/provision/poll")
    suspend fun provisionPoll(): ProvisionPollResponse

    @GET("/status")
    suspend fun status(): StatusResponse

    // ===================== IR =====================
    @POST("/learn/start")
    suspend fun learnStart(@Body req: LearnStartRequest): Map<String, Any>

    @GET("/learn/poll")
    suspend fun learnPoll(): LearnPollResponse

    @POST("/ir/send")
    suspend fun irSend(@Body req: IrSendRequest): IrSendResponse

    // ===================== RF433 =====================
    @POST("/rf/learn/start")
    suspend fun rfLearnStart(@Body req: RfLearnStartRequest): Map<String, Any>

    @GET("/rf/learn/poll")
    suspend fun rfLearnPoll(): RfLearnPollResponse

    @POST("/rf/send")
    suspend fun rfSend(@Body req: RfSendRequest): RfSendResponse
}