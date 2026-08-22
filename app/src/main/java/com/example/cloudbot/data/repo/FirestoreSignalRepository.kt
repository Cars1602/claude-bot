package com.example.cloudbot.data.repo

import com.example.cloudbot.data.db.entities.ButtonEntity
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreSignalRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    data class CloudLab(
        val labId: String,
        val displayName: String
    )

    data class CloudSignal(
        val labId: String,
        val locationName: String,
        val hubId: String,
        val deviceName: String,
        val deviceIp: String,
        val applianceType: String,
        val button: ButtonEntity
    )

    suspend fun saveLearnedSignal(
        hubId: String,
        deviceName: String,
        deviceIp: String,
        button: ButtonEntity,
        locationName: String,
        applianceType: String,
        selectedSignalType: String
    ) {
        val labId = normalizeLabId(locationName)
        val deviceDoc = hashMapOf<String, Any?>(
            "hubId" to hubId,
            "nombre" to deviceName,
            "ip" to deviceIp,
            "marca" to "ESP32",
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val signalDoc = hashMapOf<String, Any?>(
            "labId" to labId,
            "hubId" to hubId,
            "deviceName" to deviceName,
            "deviceIp" to deviceIp,
            "locationName" to locationName,
            "applianceType" to applianceType,
            "remoteId" to button.remoteId,
            "buttonId" to button.buttonId,
            "label" to button.label,
            "signalType" to button.signalType,
            "signalTechnology" to if (button.signalType == "RF433") "Radiofrecuencia" else "Infrarroja",
            "selectedSignalType" to selectedSignalType,
            "selectedSignalTechnology" to if (selectedSignalType == "RF433") "Radiofrecuencia" else "Infrarroja",
            "khz" to button.khz,
            "repeat" to button.repeat,
            "rawJson" to button.rawJson,
            "createdAtLocal" to button.createdAt,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        awaitUnit(
            firestore.collection("laboratorios")
                .document(labId)
                .set(
                    mapOf(
                        "nombre" to locationName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
        )
        awaitUnit(
            firestore.collection("laboratorios")
                .document(labId)
                .collection("dispositivos")
                .document(hubId)
                .set(deviceDoc, SetOptions.merge())
        )
        awaitUnit(
            firestore.collection("laboratorios")
                .document(labId)
                .collection("dispositivos")
                .document(hubId)
                .collection("senales")
                .document(button.buttonId)
                .set(signalDoc)
        )
    }

    suspend fun fetchSignals(hubId: String, remoteId: String, deviceIp: String, labHint: String): List<ButtonEntity> {
        val errors = mutableListOf<Exception>()
        val merged = LinkedHashMap<String, ButtonEntity>()

        // 1) Rutas directas por laboratorio (rápidas, sin escanear toda la BD).
        for (labId in labIdCandidates(labHint)) {
            try {
                val snap = awaitQuery(
                    firestore.collection("laboratorios")
                        .document(labId)
                        .collection("dispositivos")
                        .document(hubId)
                        .collection("senales")
                        .get()
                )
                mergeButtons(
                    target = merged,
                    incoming = parseButtons(snap, remoteId),
                    requestedRemoteId = remoteId
                )
            } catch (e: Exception) {
                errors += e
            }
        }
        if (merged.isNotEmpty()) return merged.values.toList()

        // 2) Fallback por collectionGroup para recuperar si el laboratorio no coincide.
        try {
            val snap = awaitQuery(
                firestore.collectionGroup("senales")
                    .whereEqualTo("hubId", hubId)
                    .whereEqualTo("remoteId", remoteId)
                    .get()
            )
            mergeButtons(
                target = merged,
                incoming = parseButtons(snap, remoteId),
                requestedRemoteId = remoteId
            )
        } catch (e: Exception) {
            errors += e
        }
        if (merged.isNotEmpty()) return merged.values.toList()

        // 3) Fallback global por hubId (cambios de IP/remoteId).
        try {
            val snap = awaitQuery(
                firestore.collectionGroup("senales")
                    .whereEqualTo("hubId", hubId)
                    .get()
            )
            mergeButtons(
                target = merged,
                incoming = parseButtons(snap, remoteId),
                requestedRemoteId = remoteId
            )
        } catch (e: Exception) {
            errors += e
        }
        if (merged.isNotEmpty()) return merged.values.toList()

        // 4) Compatibilidad con estructura antigua.
        try {
            val oldScoped = awaitQuery(
                firestore.collection("hubs")
                    .document(hubId)
                    .collection("signals")
                    .whereEqualTo("remoteId", remoteId)
                    .get()
            )
            mergeButtons(
                target = merged,
                incoming = parseButtons(oldScoped, remoteId),
                requestedRemoteId = remoteId
            )
        } catch (e: Exception) {
            errors += e
        }
        if (merged.isNotEmpty()) return merged.values.toList()

        try {
            val legacyRemoteId = "remote_$hubId"
            val oldLegacy = awaitQuery(
                firestore.collection("hubs")
                    .document(hubId)
                    .collection("signals")
                    .whereEqualTo("remoteId", legacyRemoteId)
                    .get()
            )
            mergeButtons(
                target = merged,
                incoming = parseButtons(oldLegacy, remoteId),
                requestedRemoteId = remoteId
            )
        } catch (e: Exception) {
            errors += e
        }
        if (merged.isNotEmpty()) return merged.values.toList()

        // Si nada salió y hubo errores, no esconder el fallo real.
        if (errors.isNotEmpty()) {
            val first = errors.first()
            throw IllegalStateException("Fallo al consultar Firestore: ${first.message}", first)
        }
        return emptyList()
    }

    suspend fun deleteSignal(hubId: String, buttonId: String, labHint: String): Int {
        val errors = mutableListOf<Exception>()
        var deleted = 0

        for (labId in labIdCandidates(labHint)) {
            try {
                val ref = firestore.collection("laboratorios")
                    .document(labId)
                    .collection("dispositivos")
                    .document(hubId)
                    .collection("senales")
                    .document(buttonId)
                val doc = awaitDocument(ref.get())
                if (doc.exists()) {
                    awaitUnit(ref.delete())
                    deleted++
                }
            } catch (e: Exception) {
                errors += e
            }
        }

        try {
            val group = awaitQuery(
                firestore.collectionGroup("senales")
                    .whereEqualTo("hubId", hubId)
                    .whereEqualTo("buttonId", buttonId)
                    .get()
            )
            for (doc in group.documents) {
                awaitUnit(doc.reference.delete())
                deleted++
            }
        } catch (e: Exception) {
            errors += e
        }

        try {
            val oldRef = firestore.collection("hubs")
                .document(hubId)
                .collection("signals")
                .document(buttonId)
            val oldDoc = awaitDocument(oldRef.get())
            if (oldDoc.exists()) {
                awaitUnit(oldRef.delete())
                deleted++
            }
        } catch (e: Exception) {
            errors += e
        }

        if (deleted == 0 && errors.isNotEmpty()) {
            val first = errors.first()
            throw IllegalStateException("No se pudo borrar en Firestore: ${first.message}", first)
        }
        return deleted
    }

    suspend fun listLabs(): List<CloudLab> {
        val snap = awaitQuery(
            firestore.collection("laboratorios").get()
        )
        return snap.documents.map { doc ->
            CloudLab(
                labId = doc.id,
                displayName = doc.getString("nombre") ?: doc.id
            )
        }.sortedBy { labSortKey(it.labId, it.displayName) }
    }

    suspend fun fetchSignalsByLab(labId: String): List<CloudSignal> {
        val labDoc = awaitDocument(
            firestore.collection("laboratorios")
                .document(labId)
                .get()
        )
        val locationName = labDoc.getString("nombre") ?: labId
        val devices = awaitQuery(
            firestore.collection("laboratorios")
                .document(labId)
                .collection("dispositivos")
                .get()
        )

        val collected = mutableListOf<CloudSignal>()
        for (deviceDoc in devices.documents) {
            val deviceHubId = deviceDoc.getString("hubId") ?: deviceDoc.id
            val deviceName = deviceDoc.getString("nombre") ?: "Dispositivo"
            val signalSnap = awaitQuery(
                firestore.collection("laboratorios")
                    .document(labId)
                    .collection("dispositivos")
                    .document(deviceDoc.id)
                    .collection("senales")
                    .get()
            )
            for (signalDoc in signalSnap.documents) {
                val data = signalDoc.data ?: continue
                val fallbackRemoteId = (data["remoteId"] as? String)?.ifBlank { null }
                    ?: "remote_$deviceHubId"
                val button = parseButtonDocument(signalDoc, fallbackRemoteId) ?: continue
                collected += CloudSignal(
                    labId = labId,
                    locationName = data["locationName"] as? String ?: locationName,
                    hubId = data["hubId"] as? String ?: deviceHubId,
                    deviceName = data["deviceName"] as? String ?: deviceName,
                    deviceIp = data["deviceIp"] as? String ?: deviceDoc.getString("ip").orEmpty(),
                    applianceType = data["applianceType"] as? String ?: "Equipo",
                    button = button
                )
            }
        }

        return collected.sortedWith(
            compareBy<CloudSignal>({ it.locationName.lowercase(Locale.ROOT) })
                .thenBy { it.deviceName.lowercase(Locale.ROOT) }
                .thenBy { it.button.label.lowercase(Locale.ROOT) }
        )
    }

    private fun parseButtons(snapshot: QuerySnapshot, fallbackRemoteId: String): List<ButtonEntity> {
        return snapshot.documents.mapNotNull { doc ->
            parseButtonDocument(doc, fallbackRemoteId)
        }
    }

    private fun parseButtonDocument(doc: DocumentSnapshot, fallbackRemoteId: String): ButtonEntity? {
        val data = doc.data ?: return null
        val buttonId = (data["buttonId"] as? String)?.ifBlank { null }
            ?: doc.id.ifBlank { UUID.randomUUID().toString() }
        val docRemoteId = (data["remoteId"] as? String)?.ifBlank { null } ?: fallbackRemoteId
        val label = (data["label"] as? String)?.ifBlank { null } ?: "Boton"
        val signalType = if ((data["signalType"] as? String) == "RF433") "RF433" else "IR"
        val khz = (data["khz"] as? Number)?.toInt() ?: 38
        val repeat = (data["repeat"] as? Number)?.toInt() ?: 1
        val rawJson = data["rawJson"] as? String ?: "[]"
        val createdAt = (data["createdAtLocal"] as? Number)?.toLong() ?: System.currentTimeMillis()

        return ButtonEntity(
            buttonId = buttonId,
            remoteId = docRemoteId,
            label = label,
            signalType = signalType,
            khz = khz,
            repeat = repeat,
            rawJson = rawJson,
            createdAt = createdAt
        )
    }

    private fun mergeButtons(
        target: MutableMap<String, ButtonEntity>,
        incoming: List<ButtonEntity>,
        requestedRemoteId: String
    ) {
        for (btn in incoming) {
            val existing = target[btn.buttonId]
            if (existing == null) {
                target[btn.buttonId] = btn
                continue
            }
            val existingScore = scoreButton(existing, requestedRemoteId)
            val newScore = scoreButton(btn, requestedRemoteId)
            if (newScore > existingScore) {
                target[btn.buttonId] = btn
            }
        }
    }

    private fun scoreButton(btn: ButtonEntity, requestedRemoteId: String): Int {
        var score = 0
        if (btn.remoteId == requestedRemoteId) score += 10
        if (btn.rawJson.isNotBlank() && btn.rawJson.trim() != "[]") score += 5
        return score
    }

    private fun labIdCandidates(labHint: String): List<String> {
        val set = linkedSetOf<String>()
        val normalized = normalizeLabId(labHint)
        set += normalized
        set += normalized.replace(" ", "_")
        set += normalized.replace("_", "")

        val justDigits = Regex("(\\d+)").find(normalized)?.groupValues?.get(1)
        if (!justDigits.isNullOrBlank()) {
            set += "lab_$justDigits"
            set += "lab$justDigits"
            set += "laboratorio$justDigits"
            set += "laboratorio_$justDigits"
        }

        set += "laboratorio_general"
        return set.toList()
    }

    private suspend fun awaitUnit(task: Task<Void>) {
        suspendCancellableCoroutine<Unit> { cont ->
            task.addOnSuccessListener {
                if (cont.isActive) cont.resume(Unit)
            }.addOnFailureListener { e ->
                if (cont.isActive) cont.resumeWithException(e)
            }
        }
    }

    private suspend fun awaitQuery(task: Task<QuerySnapshot>): QuerySnapshot {
        return suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { result ->
                if (cont.isActive) cont.resume(result)
            }.addOnFailureListener { e ->
                if (cont.isActive) cont.resumeWithException(e)
            }
        }
    }

    private suspend fun awaitDocument(task: Task<DocumentSnapshot>): DocumentSnapshot {
        return suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { result ->
                if (cont.isActive) cont.resume(result)
            }.addOnFailureListener { e ->
                if (cont.isActive) cont.resumeWithException(e)
            }
        }
    }

    private fun normalizeLabId(locationName: String): String {
        val normalized = locationName.trim().lowercase(Locale.ROOT)
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_\\-]"), "")
        return if (normalized.isBlank()) "laboratorio_general" else normalized
    }

    private fun labSortKey(labId: String, displayName: String): String {
        val source = "$displayName $labId".lowercase(Locale.ROOT)
        val digits = Regex("(\\d+)").find(source)?.groupValues?.get(1)?.padStart(4, '0').orEmpty()
        return if (digits.isNotBlank()) "0_${digits}_$source" else "1_$source"
    }
}
