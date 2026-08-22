package com.example.cloudbot.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.cloudbot.data.db.dao.ButtonDao
import com.example.cloudbot.data.db.dao.DeviceDao
import com.example.cloudbot.data.db.dao.RemoteDao
import com.example.cloudbot.data.db.entities.ButtonEntity
import com.example.cloudbot.data.db.entities.DeviceEntity
import com.example.cloudbot.data.db.entities.RemoteEntity

@Database(
    entities = [DeviceEntity::class, RemoteEntity::class, ButtonEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun remoteDao(): RemoteDao
    abstract fun buttonDao(): ButtonDao
}
