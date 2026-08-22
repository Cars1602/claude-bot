package com.example.cloudbot.di

import android.content.Context
import androidx.room.Room
import com.example.cloudbot.data.db.AppDatabase
import com.example.cloudbot.data.repo.DeviceRepository
import com.example.cloudbot.data.repo.FirestoreSignalRepository
import com.example.cloudbot.data.repo.RemoteRepository

object ServiceLocator {

    @Volatile private var db: AppDatabase? = null
    @Volatile private var fireRepo: FirestoreSignalRepository? = null

    fun database(context: Context): AppDatabase {
        return db ?: synchronized(this) {
            db ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "cloudbot.db"
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { db = it }
        }
    }

    fun deviceRepo(context: Context): DeviceRepository {
        val d = database(context)
        return DeviceRepository(d.deviceDao(), d.remoteDao(), d.buttonDao())
    }

    fun remoteRepo(context: Context): RemoteRepository {
        val d = database(context)
        return RemoteRepository(d.remoteDao(), d.buttonDao())
    }

    fun firestoreSignalRepo(): FirestoreSignalRepository {
        return fireRepo ?: synchronized(this) {
            fireRepo ?: FirestoreSignalRepository().also { fireRepo = it }
        }
    }
}
