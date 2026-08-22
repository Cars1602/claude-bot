package com.example.cloudbot.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "remotes",
    indices = [Index("hubId")]
)
data class RemoteEntity(
    @PrimaryKey val remoteId: String,
    val hubId: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)
