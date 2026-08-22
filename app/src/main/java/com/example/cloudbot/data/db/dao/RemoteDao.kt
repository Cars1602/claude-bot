package com.example.cloudbot.data.db.dao

import androidx.room.*
import com.example.cloudbot.data.db.entities.RemoteEntity

@Dao
interface RemoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(remote: RemoteEntity)

    @Query("SELECT * FROM remotes WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getById(remoteId: String): RemoteEntity?

    @Query("DELETE FROM remotes WHERE remoteId = :remoteId")
    suspend fun deleteById(remoteId: String)
}