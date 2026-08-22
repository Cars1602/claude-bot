package com.example.cloudbot.data.repo

import com.example.cloudbot.data.db.dao.ButtonDao
import com.example.cloudbot.data.db.dao.RemoteDao
import com.example.cloudbot.data.db.entities.ButtonEntity
import com.example.cloudbot.data.db.entities.RemoteEntity

class RemoteRepository(
    private val remoteDao: RemoteDao,
    private val buttonDao: ButtonDao
) {
    fun observeButtons(remoteId: String) = buttonDao.observeByRemote(remoteId)

    suspend fun ensureRemote(remoteId: String, hubId: String, title: String) {
        val existing = remoteDao.getById(remoteId)
        if (existing == null) {
            remoteDao.upsert(
                RemoteEntity(
                    remoteId = remoteId,
                    hubId = hubId,
                    title = title
                )
            )
        }
    }

    suspend fun upsertButton(btn: ButtonEntity) = buttonDao.upsert(btn)
    suspend fun getButtonById(id: String) = buttonDao.getById(id)
    suspend fun deleteButton(id: String) = buttonDao.deleteById(id)
    suspend fun renameButton(id: String, label: String) = buttonDao.updateLabel(id, label)
}
