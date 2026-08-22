package com.example.cloudbot.ui.devices

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cloudbot.data.db.entities.ButtonEntity
import com.example.cloudbot.data.db.entities.DeviceEntity
import com.example.cloudbot.data.repo.FirestoreSignalRepository
import com.example.cloudbot.databinding.ActivityDeviceListBinding
import com.example.cloudbot.di.ServiceLocator
import com.example.cloudbot.ui.provision.AddDeviceActivity
import com.example.cloudbot.ui.remote.RemoteEditorActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class DeviceListActivity : AppCompatActivity() {

    private lateinit var b: ActivityDeviceListBinding
    private val fireRepo by lazy { ServiceLocator.firestoreSignalRepo() }
    private val remoteRepo by lazy { ServiceLocator.remoteRepo(this) }
    private val deviceRepo by lazy { ServiceLocator.deviceRepo(this) }
    private var currentDevices: List<DeviceEntity> = emptyList()

    private val vm: DeviceListViewModel by viewModels {
        DeviceListViewModel.Factory(ServiceLocator.deviceRepo(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDeviceListBinding.inflate(layoutInflater)
        setContentView(b.root)

        val adapter = DeviceAdapter(
            onOpen = { device ->
                startActivity(Intent(this, DeviceDashboardActivity::class.java).apply {
                    putExtra("hubId", device.hubId)
                    putExtra("deviceIp", device.ip)
                    putExtra("deviceName", device.name)
                })
            },
            onQuickDownload = { device ->
                openRemoteEditor(device, autoSyncFromCloud = true)
            },
            onQuickLearn = { device ->
                openRemoteEditor(device, autoSyncFromCloud = false)
            },
            onRename = { device ->
                val input = EditText(this).apply { setText(device.name) }
                MaterialAlertDialogBuilder(this)
                    .setTitle("Editar nombre")
                    .setView(input)
                    .setPositiveButton("Guardar") { _, _ ->
                        val newName = input.text.toString().trim()
                        if (newName.isNotBlank()) vm.renameDevice(device.deviceId, newName)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },
            onEditIp = { device ->
                val input = EditText(this).apply { setText(device.ip) }
                MaterialAlertDialogBuilder(this)
                    .setTitle("Editar IP")
                    .setView(input)
                    .setPositiveButton("Guardar") { _, _ ->
                        val newIp = input.text.toString().trim()
                        if (newIp.isNotBlank()) vm.updateDeviceIp(device.deviceId, newIp)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },
            onDelete = { device ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Eliminar dispositivo")
                    .setMessage("Seguro que deseas eliminar '${device.name}'?")
                    .setPositiveButton("Eliminar") { _, _ -> vm.deleteDevice(device.deviceId) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )

        b.recyclerDevices.layoutManager = LinearLayoutManager(this)
        b.recyclerDevices.adapter = adapter

        vm.devices.observe(this) { list ->
            currentDevices = list
            adapter.submit(list)

            val isEmpty = list.isEmpty()
            b.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            b.recyclerDevices.visibility = if (isEmpty) View.GONE else View.VISIBLE
            b.txtDeviceCount.text = if (isEmpty) "Sin activos" else "${list.size} activo${if (list.size > 1) "s" else ""}"
        }

        b.btnBrowseCloudSignals.setOnClickListener {
            browseCloudSignals()
        }

        b.fabAddDevice.setOnClickListener {
            startActivity(Intent(this, AddDeviceActivity::class.java))
        }
    }

    private fun browseCloudSignals() {
        lifecycleScope.launch {
            setCloudLoading(true)
            try {
                val labs = withContext(Dispatchers.IO) { fireRepo.listLabs() }
                if (labs.isEmpty()) {
                    Toast.makeText(this@DeviceListActivity, "No hay laboratorios en Firestore.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                showLabPicker(labs)
            } catch (e: Exception) {
                Toast.makeText(this@DeviceListActivity, "No se pudieron cargar laboratorios: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setCloudLoading(false)
            }
        }
    }

    private fun showLabPicker(labs: List<FirestoreSignalRepository.CloudLab>) {
        val labels = labs.map { "${it.displayName} (${it.labId})" }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("Elegir laboratorio de Firestore")
            .setItems(labels) { _, which ->
                val lab = labs[which]
                b.txtCloudSelection.text = "Seleccionado: ${lab.displayName} (${lab.labId})"
                loadSignalsFromLab(lab)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun loadSignalsFromLab(lab: FirestoreSignalRepository.CloudLab) {
        lifecycleScope.launch {
            setCloudLoading(true)
            try {
                val signals = withContext(Dispatchers.IO) { fireRepo.fetchSignalsByLab(lab.labId) }
                if (signals.isEmpty()) {
                    Toast.makeText(this@DeviceListActivity, "Ese laboratorio no tiene senales guardadas.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                showSignalPicker(lab, signals)
            } catch (e: Exception) {
                Toast.makeText(this@DeviceListActivity, "No se pudieron cargar las senales: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setCloudLoading(false)
            }
        }
    }

    private fun showSignalPicker(
        lab: FirestoreSignalRepository.CloudLab,
        signals: List<FirestoreSignalRepository.CloudSignal>
    ) {
        val items = buildList {
            add("Descargar todas las senales")
            addAll(signals.map { signal ->
                "${signal.button.label} - ${signal.button.signalType} - ${signal.applianceType}"
            })
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Senales en ${lab.displayName}")
            .setItems(items) { _, which ->
                val selected = if (which == 0) signals else listOf(signals[which - 1])
                showDevicePickerForImport(lab, selected)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDevicePickerForImport(
        lab: FirestoreSignalRepository.CloudLab,
        signals: List<FirestoreSignalRepository.CloudSignal>
    ) {
        val options = mutableListOf<String>()
        options += "Crear automaticamente ${lab.displayName}"
        options += currentDevices.map { "${it.name} - ${it.ip}" }

        MaterialAlertDialogBuilder(this)
            .setTitle("Descargar en que dispositivo")
            .setItems(options.toTypedArray()) { _, which ->
                if (which == 0) {
                    createDeviceAndImport(lab, signals)
                } else {
                    importSignalsToDevice(lab, currentDevices[which - 1], signals)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun createDeviceAndImport(
        lab: FirestoreSignalRepository.CloudLab,
        signals: List<FirestoreSignalRepository.CloudSignal>
    ) {
        lifecycleScope.launch {
            setCloudLoading(true)
            try {
                val seed = signals.firstOrNull()
                val newDevice = withContext(Dispatchers.IO) {
                    val suggestedHubId = seed?.hubId?.ifBlank { null } ?: "hub_${lab.labId}"
                    val suggestedIp = seed?.deviceIp?.ifBlank { null } ?: "0.0.0.0"
                    val existing = deviceRepo.getByHubAndIp(suggestedHubId, suggestedIp)
                        ?: deviceRepo.getByHubId(suggestedHubId)

                    val device = existing?.copy(
                        name = lab.displayName,
                        ip = suggestedIp
                    ) ?: DeviceEntity(
                        deviceId = UUID.randomUUID().toString(),
                        hubId = suggestedHubId,
                        name = lab.displayName,
                        ip = suggestedIp
                    )
                    deviceRepo.upsert(device)
                    device
                }
                importSignalsToDevice(lab, newDevice, signals)
            } catch (e: Exception) {
                setCloudLoading(false)
                Toast.makeText(this@DeviceListActivity, "No se pudo crear el espacio del laboratorio: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun importSignalsToDevice(
        lab: FirestoreSignalRepository.CloudLab,
        device: DeviceEntity,
        signals: List<FirestoreSignalRepository.CloudSignal>
    ) {
        lifecycleScope.launch {
            setCloudLoading(true)
            try {
                withContext(Dispatchers.IO) {
                    val remoteId = "remote_${device.hubId}"
                    remoteRepo.ensureRemote(
                        remoteId = remoteId,
                        hubId = device.hubId,
                        title = "Control - ${device.name}"
                    )

                    signals.forEach { signal ->
                        val imported = ButtonEntity(
                            buttonId = UUID.randomUUID().toString(),
                            remoteId = remoteId,
                            label = signal.button.label,
                            signalType = signal.button.signalType,
                            khz = signal.button.khz,
                            repeat = signal.button.repeat,
                            rawJson = signal.button.rawJson,
                            createdAt = System.currentTimeMillis()
                        )
                        remoteRepo.upsertButton(imported)
                    }
                }

                MaterialAlertDialogBuilder(this@DeviceListActivity)
                    .setTitle("Senales descargadas")
                    .setMessage("Se descargaron ${signals.size} senal(es) de ${lab.displayName} en ${device.name}.")
                    .setPositiveButton("Abrir control") { _, _ ->
                        openRemoteEditor(device, autoSyncFromCloud = false)
                    }
                    .setNegativeButton("Cerrar", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@DeviceListActivity, "No se pudieron importar las senales: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setCloudLoading(false)
            }
        }
    }

    private fun setCloudLoading(loading: Boolean) {
        b.btnBrowseCloudSignals.isEnabled = !loading
        b.btnBrowseCloudSignals.text = if (loading) {
            "Cargando laboratorios..."
        } else {
            "Elegir laboratorio de Firestore"
        }
    }

    private fun openRemoteEditor(
        device: DeviceEntity,
        autoSyncFromCloud: Boolean
    ) {
        startActivity(Intent(this, RemoteEditorActivity::class.java).apply {
            putExtra("hubId", device.hubId)
            putExtra("deviceIp", device.ip)
            putExtra("deviceName", device.name)
            putExtra("autoSyncFromCloud", autoSyncFromCloud)
        })
    }
}
