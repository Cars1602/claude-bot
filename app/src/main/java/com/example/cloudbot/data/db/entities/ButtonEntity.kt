package com.example.cloudbot.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "buttons",
    foreignKeys = [
        ForeignKey(
            entity = RemoteEntity::class,
            parentColumns = ["remoteId"],
            childColumns = ["remoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("remoteId")]
)
data class ButtonEntity(
    @PrimaryKey val buttonId: String,
    val remoteId: String,
    val label: String,
    val signalType: String = "IR", // "IR" o "RF433"
    val khz: Int = 38,
    val repeat: Int = 1,
    val rawJson: String,
    val createdAt: Long = System.currentTimeMillis()
)