package com.example.cloudbot.ui.provision

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cloudbot.data.db.entities.DeviceEntity
import com.example.cloudbot.data.repo.DeviceRepository
import com.example.cloudbot.net.ApiFactory
import com.example.cloudbot.net.dto.ProvisionWifiRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class AddDeviceViewModel(private val repo: DeviceRepository) : ViewModel() {

    companion object {
        private const val AP_BASE_URL = "http://192.168.4.1/"
        private const val PROVISION_TIMEOUT_MS = 20000L
    }

    val routerSsid = MutableLiveData("")
    val routerPass = MutableLiveData("")
    val staIp = MutableLiveData<String?>(null)
    val hubId = MutableLiveData<String?>(null)

    val statusText = MutableLiveData("")
    val working = MutableLiveData(false)

    private var lastCorrelationId: String = ""

    fun startProvision() {
        viewModelScope.launch {
            working.value = true
            try {
                lastCorrelationId = UUID.randomUUID().toString()

                statusText.value = "Enviando WiFi al ESP32..."
                val apApi = ApiFactory.create(AP_BASE_URL)

                apApi.provisionWifi(
                    ProvisionWifiRequest(
                        correlationId = lastCorrelationId,
                        ssid = routerSsid.value.orEmpty(),
                        pass = routerPass.value.orEmpty()
                    )
                )

                statusText.value = "Conectando dispositivo..."
                val t0 = System.currentTimeMillis()

                while (System.currentTimeMillis() - t0 < PROVISION_TIMEOUT_MS) {
                    val poll = apApi.provisionPoll()

                    if (poll.state.equals("ONLINE", true) && !poll.staIp.isNullOrBlank()) {
                        staIp.value = poll.staIp
                        statusText.value =
                            "Listo OK.\nIP del router: ${poll.staIp}\nAhora pon un nombre y guarda."
                        working.value = false
                        return@launch
                    }
                    delay(1000)
                }

                statusText.value = "Timeout. Reintenta."
                working.value = false
            } catch (e: Exception) {
                statusText.value = "Error: ${e.message}"
                working.value = false
            }
        }
    }

    fun verifyAndSaveDevice(deviceName: String) {
        val ip = staIp.value ?: run {
            statusText.value = "IP vacia. Termina el provisioning primero."
            return
        }

        viewModelScope.launch {
            working.value = true
            try {
                val apApi = ApiFactory.create(AP_BASE_URL)

                val stillOnAp = try {
                    apApi.provisionStatus().ok
                } catch (_: Exception) {
                    false
                }

                val api = if (stillOnAp) apApi else ApiFactory.create("http://$ip/")

                statusText.value = if (stillOnAp) {
                    "Sigues en CLOUDBOT.\nGuardando usando 192.168.4.1..."
                } else {
                    "Verificando /status en $ip ..."
                }

                val st = api.status()
                val hid = st.hubId ?: throw IllegalStateException("hubId vacio")

                hubId.value = hid

                val finalName = deviceName.ifBlank { "CloudBot Hub" }
                val existing = repo.getByHubAndIp(hid, ip)
                repo.upsert(
                    existing?.copy(
                        name = finalName,
                        ip = ip
                    ) ?: DeviceEntity(
                        deviceId = UUID.randomUUID().toString(),
                        hubId = hid,
                        name = finalName,
                        ip = ip
                    )
                )

                statusText.value = if (stillOnAp) {
                    "Guardado OK.\nAhora vuelve a tu WiFi del router para controlar el dispositivo."
                } else {
                    "Guardado OK."
                }

                working.value = false
            } catch (e: Exception) {
                statusText.value =
                    "No pude verificar/guardar.\nTip: vuelve a tu WiFi del router y presiona Guardar otra vez."
                working.value = false
            }
        }
    }

    class Factory(private val repo: DeviceRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AddDeviceViewModel(repo) as T
        }
    }
}
