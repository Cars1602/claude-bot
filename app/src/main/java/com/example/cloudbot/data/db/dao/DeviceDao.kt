package com.example.cloudbot.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.cloudbot.data.db.entities.DeviceEntity

@Dao
interface DeviceDao {

    @Query("SELECT * FROM devices ORDER BY createdAt DESC")
    fun observeAll(): LiveData<List<DeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: DeviceEntity)

    @Query("SELECT * FROM devices WHERE hubId = :hubId AND ip = :ip LIMIT 1")
    suspend fun getByHubAndIp(hubId: String, ip: String): DeviceEntity?

    @Query("SELECT * FROM devices WHERE hubId = :hubId LIMIT 1")
    suspend fun getByHubId(hubId: String): DeviceEntity?

    @Query("UPDATE devices SET name = :name WHERE deviceId = :deviceId")
    suspend fun updateName(deviceId: String, name: String)

    @Query("UPDATE devices SET ip = :ip WHERE deviceId = :deviceId")
    suspend fun updateIp(deviceId: String, ip: String)

    @Query("DELETE FROM devices WHERE deviceId = :deviceId")
    suspend fun deleteById(deviceId: String)
}
