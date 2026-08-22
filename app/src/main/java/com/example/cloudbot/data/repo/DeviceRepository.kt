package com.example.cloudbot.data.repo

import com.example.cloudbot.data.db.dao.ButtonDao
import com.example.cloudbot.data.db.dao.DeviceDao
import com.example.cloudbot.data.db.dao.RemoteDao
import com.example.cloudbot.data.db.entities.DeviceEntity

class DeviceRepository(
    private val deviceDao: DeviceDao,
    private val remoteDao: RemoteDao,
    private val buttonDao: ButtonDao
) {
    fun observeAll() = deviceDao.observeAll()
    suspend fun upsert(device: DeviceEntity) = deviceDao.upsert(device)
    suspend fun getByHubAndIp(hubId: String, ip: String) = deviceDao.getByHubAndIp(hubId, ip)
    suspend fun getByHubId(hubId: String) = deviceDao.getByHubId(hubId)

    suspend fun updateName(deviceId: String, name: String) = deviceDao.updateName(deviceId, name)
    suspend fun updateIp(deviceId: String, ip: String) = deviceDao.updateIp(deviceId, ip)

    suspend fun deleteDevice(deviceId: String) {
        deviceDao.deleteById(deviceId)
    }
}
