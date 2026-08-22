package com.example.cloudbot.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.cloudbot.data.db.entities.ButtonEntity

@Dao
interface ButtonDao {

    @Query("SELECT * FROM buttons WHERE remoteId = :remoteId ORDER BY createdAt ASC")
    fun observeByRemote(remoteId: String): LiveData<List<ButtonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(btn: ButtonEntity)

    @Query("SELECT * FROM buttons WHERE buttonId = :id LIMIT 1")
    suspend fun getById(id: String): ButtonEntity?

    @Query("DELETE FROM buttons WHERE buttonId = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE buttons SET label = :label WHERE buttonId = :id")
    suspend fun updateLabel(id: String, label: String)
}
