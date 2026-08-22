package com.example.cloudbot.ui.remote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.cloudbot.data.db.entities.ButtonEntity
import com.example.cloudbot.data.repo.FirestoreSignalRepository
import com.example.cloudbot.data.repo.RemoteRepository
import com.example.cloudbot.net.ApiFactory
import com.example.cloudbot.net.Esp32Api
import com.example.cloudbot.net.dto.IrSendRequest
import com.example.cloudbot.net.dto.LearnStartRequest
import com.example.cloudbot.net.dto.RfLearnStartRequest
import com.example.cloudbot.net.dto.RfSendRequest
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class RemoteViewModel(
    private val repo: RemoteRepository,
    private val firestoreSignalRepository: FirestoreSignalRepository
) : ViewModel() {

    val deviceIp = MutableLiveData("")
    val hubId = MutableLiveData("")
    val deviceName = MutableLiveData("")
    val status = MutableLiveData("")

    private val remoteId = MutableLiveData("")

    val buttons: LiveData<List<ButtonEntity>> = remoteId.switchMap { rid ->
        repo.observeButtons(rid)
    }

    fun init(hubId: String, ip: String, name: String) {
        this.hubId.value = hubId
        this.deviceIp.value = ip
        this.deviceName.value = name

        val rid = buildRemoteId(hubId, ip)
        remoteId.value = rid

        viewModelScope.launch {
            repo.ensureRemote(remoteId = rid, hubId = hubId, title = "Control - $name")
        }
    }

    fun syncSignalsFromCloud() {
        val hub = hubId.value.orEmpty()
        val rid = remoteId.value.orEmpty()
        if (hub.isBlank() || rid.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            syncSignalsFromCloudInternal(hubId = hub, rid = rid)
        }
    }

    fun addButton(label: String, signalType: String) {
        val rid = remoteId.value.orEmpty()
        if (rid.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val btn = ButtonEntity(
                buttonId = UUID.randomUUID().toString(),
                remoteId = rid,
                label = label,
                signalType = if (signalType == "RF433") "RF433" else "IR",
                khz = 38,
                repeat = 1,
                rawJson = "[]"
            )
            repo.upsertButton(btn)
        }
    }

    fun renameButton(btn: ButtonEntity, newLabel: String) = viewModelScope.launch {
        repo.renameButton(btn.buttonId, newLabel)
    }

    fun deleteButton(btn: ButtonEntity) = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteButton(btn.buttonId)
    }

    fun deleteButtonLocal(btn: ButtonEntity) = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteButton(btn.buttonId)
        status.postValue("Eliminado local: ${btn.label}")
    }

    fun deleteButtonCloud(btn: ButtonEntity) {
        val hub = hubId.value.orEmpty()
        val labHint = deviceName.value.orEmpty()
        if (hub.isBlank()) {
            status.value = "No se pudo borrar en Firestore: hubId vacio."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deleted = firestoreSignalRepository.deleteSignal(
                    hubId = hub,
                    buttonId = btn.buttonId,
                    labHint = labHint
                )
                status.postValue(if (deleted > 0) {
                    "Eliminado en Firestore: ${btn.label}"
                } else {
                    "No se encontro en Firestore: ${btn.label}"
                })
            } catch (e: Exception) {
                status.postValue("Error borrando en Firestore: ${e.message}")
            }
        }
    }

    fun deleteButtonEverywhere(btn: ButtonEntity) {
        val hub = hubId.value.orEmpty()
        val labHint = deviceName.value.orEmpty()
        if (hub.isBlank()) {
            status.value = "No se pudo borrar en Firestore: hubId vacio."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deleted = firestoreSignalRepository.deleteSignal(
                    hubId = hub,
                    buttonId = btn.buttonId,
                    labHint = labHint
                )
                repo.deleteButton(btn.buttonId)
                status.postValue(if (deleted > 0) {
                    "Eliminado local y Firestore: ${btn.label}"
                } else {
                    "Eliminado local. No estaba en Firestore: ${btn.label}"
                })
            } catch (e: Exception) {
                status.postValue("Error borrando en Firestore: ${e.message}")
            }
        }
    }

    fun learnButton(btn: ButtonEntity) {
        val ip = deviceIp.value.orEmpty()
        if (ip.isBlank()) {
            status.value = "IP vacia"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = ApiFactory.create("http://$ip/")
                if (btn.signalType == "RF433") {
                    status.postValue("Aprendiendo RF433: ${btn.label} ...")
                    val capture = captureRf(api) ?: run {
                        status.postValue("No se capturo RF433.")
                        return@launch
                    }
                    repo.upsertButton(
                        btn.copy(
                            rawJson = Gson().toJson(capture),
                            repeat = 6
                        )
                    )
                    status.postValue("Listo RF433: ${btn.label}")
                } else {
                    status.postValue("Aprendiendo IR: ${btn.label} ...")
                    val capture = captureIr(api) ?: run {
                        status.postValue("No se capturo IR.")
                        return@launch
                    }
                    repo.upsertButton(
                        btn.copy(
                            rawJson = Gson().toJson(capture.raw),
                            khz = capture.khz,
                            repeat = 1
                        )
                    )
                    status.postValue("Listo IR: ${btn.label}")
                }
            } catch (e: Exception) {
                status.postValue("Error learn: ${e.message}")
            } finally {
                if (btn.signalType == "IR") {
                    runCatching {
                        val api = ApiFactory.create("http://$ip/")
                        releaseIrSession(api)
                    }
                }
            }
        }
    }

    fun sendButton(btn: ButtonEntity) {
        val ip = deviceIp.value.orEmpty()
        if (ip.isBlank()) {
            status.value = "IP vacia"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val raw = parseRawSignal(btn.rawJson)
                if (raw.isEmpty()) {
                    status.postValue("El boton '${btn.label}' no tiene senal.")
                    return@launch
                }

                val api = ApiFactory.create("http://$ip/")
                if (btn.signalType == "RF433") {
                    val rfRaw = sanitizeRfForSend(raw)
                    if (rfRaw.isEmpty()) {
                        status.postValue("La senal RF433 de '${btn.label}' no es valida para enviar.")
                        return@launch
                    }
                    status.postValue("Enviando RF433: ${btn.label} ...")
                    api.rfSend(
                        RfSendRequest(
                            correlationId = UUID.randomUUID().toString(),
                            repeat = btn.repeat.coerceIn(2, 6),
                            raw = rfRaw
                        )
                    )
                    status.postValue("Enviado RF433: ${btn.label}")
                } else {
                    status.postValue("Enviando IR: ${btn.label} ...")
                    api.irSend(
                        IrSendRequest(
                            correlationId = UUID.randomUUID().toString(),
                            khz = btn.khz,
                            repeat = btn.repeat.coerceIn(1, 5),
                            raw = raw
                        )
                    )
                    status.postValue("Enviado IR: ${btn.label}")
                }
            } catch (e: Exception) {
                status.postValue("Error send: ${e.message}")
            }
        }
    }

    fun saveButtonToCloud(
        btn: ButtonEntity,
        locationName: String,
        applianceType: String,
        selectedSignalType: String
    ) {
        val hub = hubId.value.orEmpty()
        if (hub.isBlank()) {
            status.value = "No se pudo guardar: hubId vacio."
            return
        }
        if (locationName.isBlank()) {
            status.value = "Escribe un nombre (ejemplo: Lab 4)."
            return
        }
        if (applianceType.isBlank()) {
            status.value = "Selecciona el tipo de equipo."
            return
        }
        if (btn.rawJson.isBlank() || btn.rawJson.trim() == "[]") {
            status.value = "Primero aprende una senal para '${btn.label}'."
            return
        }
        val normalizedSignalType = if (selectedSignalType == "RF433") "RF433" else "IR"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestoreSignalRepository.saveLearnedSignal(
                    hubId = hub,
                    deviceName = deviceName.value.orEmpty(),
                    deviceIp = deviceIp.value.orEmpty(),
                    button = btn,
                    locationName = locationName,
                    applianceType = applianceType,
                    selectedSignalType = normalizedSignalType
                )
                status.postValue("Guardado: ${btn.label} / $locationName / $applianceType (${btn.signalType})")
            } catch (e: Exception) {
                status.postValue("Error guardando en Firestore: ${e.message}")
            }
        }
    }

    private suspend fun syncSignalsFromCloudInternal(hubId: String, rid: String) {
        try {
            status.postValue("Sincronizando senales desde Firestore...")
            val ip = deviceIp.value.orEmpty()
            val labHint = deviceName.value.orEmpty()
            val cloudButtons = firestoreSignalRepository.fetchSignals(
                hubId = hubId,
                remoteId = rid,
                deviceIp = ip,
                labHint = labHint
            )
            var synced = 0
            for (btn in cloudButtons) {
                val mapped = btn.copy(remoteId = rid)
                val local = repo.getButtonById(btn.buttonId)
                val cloudHasSignal = mapped.rawJson.isNotBlank() && mapped.rawJson.trim() != "[]"
                if (local == null) {
                    if (cloudHasSignal) {
                        repo.upsertButton(mapped)
                        synced++
                    }
                    continue
                }

                val localHasSignal = local.rawJson.isNotBlank() && local.rawJson.trim() != "[]"
                val changed =
                    local.label != mapped.label ||
                    local.signalType != mapped.signalType ||
                    local.khz != mapped.khz ||
                    local.repeat != mapped.repeat ||
                    local.rawJson != mapped.rawJson

                if (cloudHasSignal && (!localHasSignal || changed)) {
                    repo.upsertButton(mapped)
                    synced++
                }
            }
            status.postValue("Sincronizacion completada: $synced de ${cloudButtons.size} senales.")
        } catch (e: Exception) {
            status.postValue("No se pudo sincronizar: ${e.message}")
        }
    }

    private data class IrCapture(val raw: List<Long>, val khz: Int)

    private suspend fun captureRf(api: Esp32Api): List<Long>? {
        val cid = UUID.randomUUID().toString()
        api.rfLearnStart(
            RfLearnStartRequest(
                correlationId = cid,
                timeoutMs = 12000,
                minPulses = 80
            )
        )
        val t0 = System.currentTimeMillis()
        var sawLearningState = false
        while (System.currentTimeMillis() - t0 < 14000L) {
            val poll = api.rfLearnPoll()
            if (poll.learning == true) sawLearningState = true
            val sameSession =
                poll.correlationId == cid || poll.result?.correlationId == cid
            val enoughElapsed = (System.currentTimeMillis() - t0) > 500L
            if (poll.resultReady == true && poll.result != null && poll.result.ok) {
                val raw = preserveRfCapture(poll.result.raw)
                if (sameSession && enoughElapsed && isValidRfCapture(raw, sawLearningState)) {
                    return raw
                }
            }
            delay(250)
        }
        return null
    }

    private suspend fun captureIr(api: Esp32Api): IrCapture? {
        releaseIrSession(api)
        val cid = UUID.randomUUID().toString()
        api.learnStart(
            LearnStartRequest(
                correlationId = cid,
                timeoutMs = 12000,
                minRawLen = 32
            )
        )
        val t0 = System.currentTimeMillis()
        var sawLearningState = false
        while (System.currentTimeMillis() - t0 < 14000L) {
            val poll = api.learnPoll()
            if (poll.learning == true) sawLearningState = true
            val sameSession =
                poll.correlationId == cid || poll.result?.correlationId == cid
            val enoughElapsed = (System.currentTimeMillis() - t0) > 500L
            if (poll.resultReady == true && poll.result != null && poll.result.ok) {
                val raw = sanitizeIrCapture(poll.result.raw)
                if (sameSession && enoughElapsed && isValidIrCapture(raw, sawLearningState)) {
                    releaseIrSession(api)
                    return IrCapture(raw = raw, khz = poll.result.khz)
                }
            }
            delay(250)
        }
        return null
    }

    private fun isValidCapture(raw: List<Long>, signalType: String): Boolean {
        if (raw.isEmpty()) return false
        val minLen = if (signalType == "RF433") 160 else 55
        if (raw.size < minLen) return false
        val invalid = raw.any { it <= 0L || it > 200000L }
        if (invalid) return false
        val distinct = raw.distinct().size
        val minDistinct = if (signalType == "RF433") 12 else 8
        if (distinct < minDistinct) return false
        return true
    }

    private fun preserveRfCapture(raw: List<Long>): List<Long> {
        return raw
            .filter { it > 0L }
            .map { it.coerceAtMost(1_000_000L) }
    }

    private fun isValidRfCapture(raw: List<Long>, sawLearningState: Boolean): Boolean {
        if (raw.isEmpty()) return false
        if (isValidCapture(raw, "RF433")) return true

        if (raw.size < 48) return false
        if (raw.any { it <= 0L || it > 1_000_000L }) return false

        val distinct = raw.distinct().size
        if (distinct < 4) return false

        // Algunos firmwares RF entregan resultado util sin exponer claramente el estado learning.
        return sawLearningState || raw.size >= 64
    }

    private fun parseRawSignal(rawJson: String): List<Long> {
        return try {
            JsonParser.parseString(rawJson)
                .asJsonArray
                .mapNotNull { element ->
                    runCatching { element.asBigDecimal.toLong() }.getOrNull()
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun sanitizeRfForSend(raw: List<Long>): List<Long> {
        return raw
            .filter { it > 0L }
            .map { it.coerceAtMost(1_000_000L) }
    }

    private fun sanitizeIrCapture(raw: List<Long>): List<Long> {
        return raw
            .filter { it > 0L }
            .map { it.coerceAtMost(200_000L) }
    }

    private fun isValidIrCapture(raw: List<Long>, sawLearningState: Boolean): Boolean {
        if (raw.isEmpty()) return false
        if (isValidCapture(raw, "IR")) return true

        if (raw.size < 24) return false
        if (raw.any { it <= 0L || it > 200_000L }) return false

        val distinct = raw.distinct().size
        if (distinct < 4) return false

        val longPulses = raw.count { it >= 400L }
        if (longPulses < 8) return false

        return sawLearningState || raw.size >= 32
    }

    private suspend fun releaseIrSession(api: Esp32Api) {
        repeat(4) {
            val poll = runCatching { api.learnPoll() }.getOrNull() ?: return
            val busy = poll.learning == true
            val hasPendingResult = poll.resultReady == true || poll.result != null
            if (!busy && !hasPendingResult) return
            delay(80)
        }
    }


    private fun buildRemoteId(hubId: String, ip: String): String {
        return "remote_$hubId"
    }

    class Factory(
        private val repo: RemoteRepository,
        private val firestoreSignalRepository: FirestoreSignalRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return RemoteViewModel(repo, firestoreSignalRepository) as T
        }
    }
}
