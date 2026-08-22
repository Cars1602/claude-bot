package com.example.cloudbot.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "devices",
    indices = [Index("hubId"), Index("ip")]
)
data class DeviceEntity(
    @PrimaryKey val deviceId: String,
    val hubId: String,
    val name: String,
    val ip: String,
    val createdAt: Long = System.currentTimeMillis()
)
