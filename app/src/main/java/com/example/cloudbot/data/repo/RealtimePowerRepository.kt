package com.example.cloudbot.data.repo

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class RealtimePowerRepository(
    baseUrl: String = "https://agarre-de-senales-e91f9-default-rtdb.firebaseio.com/"
) {

    data class PowerState(
        val isOn: Boolean?,
        val resolvedPath: String?
    )

    private val database = FirebaseDatabase.getInstance(baseUrl)

    fun observePowerState(
        deviceName: String,
        hubId: String,
        onUpdate: (PowerState) -> Unit,
        onError: (String) -> Unit
    ): ValueEventListener {
        val candidates = buildCandidatePaths(deviceName, hubId)
        val rootRef = database.reference

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (path in candidates) {
                    val value = snapshot.child(path).getValue(Boolean::class.java)
                    if (value != null) {
                        onUpdate(PowerState(isOn = value, resolvedPath = path))
                        return
                    }
                }
                onUpdate(PowerState(isOn = null, resolvedPath = null))
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }

        rootRef.addValueEventListener(listener)
        return listener
    }

    fun removeObserver(listener: ValueEventListener) {
        database.reference.removeEventListener(listener)
    }

    private fun buildCandidatePaths(deviceName: String, hubId: String): List<String> {
        val normalizedLabId = normalizeLabId(deviceName)
        return listOf(
            "laboratorios/$normalizedLabId/encendido",
            "laboratorios/$normalizedLabId/isOn",
            "laboratorios/$normalizedLabId/estado",
            "dispositivos/$hubId/encendido",
            "dispositivos/$hubId/isOn",
            "dispositivos/$hubId/estado",
            "$hubId/encendido",
            "$hubId/isOn",
            "$normalizedLabId/encendido",
            "$normalizedLabId/isOn"
        )
    }

    private fun normalizeLabId(name: String): String {
        val normalized = name.trim().lowercase(Locale.ROOT)
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_\\-]"), "")
        return if (normalized.isBlank()) "laboratorio_general" else normalized
    }
}
