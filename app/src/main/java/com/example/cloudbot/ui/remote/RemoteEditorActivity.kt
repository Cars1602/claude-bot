package com.example.cloudbot.ui.remote

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cloudbot.data.db.entities.ButtonEntity
import com.example.cloudbot.databinding.ActivityRemoteEditorBinding
import com.example.cloudbot.di.ServiceLocator
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RemoteEditorActivity : AppCompatActivity() {

    private lateinit var b: ActivityRemoteEditorBinding

    private val vm: RemoteViewModel by viewModels {
        RemoteViewModel.Factory(
            ServiceLocator.remoteRepo(this),
            ServiceLocator.firestoreSignalRepo()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRemoteEditorBinding.inflate(layoutInflater)
        setContentView(b.root)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val hubId = intent.getStringExtra("hubId") ?: return
        val ip = intent.getStringExtra("deviceIp") ?: ""
        val name = intent.getStringExtra("deviceName") ?: "Dispositivo"
        val autoSyncFromCloud = intent.getBooleanExtra("autoSyncFromCloud", false)

        vm.init(hubId, ip, name)

        b.txtTitle.text = name
        b.txtSubtitle.text = if (autoSyncFromCloud) {
            "Sincronizando senales desde la base de datos"
        } else {
            "Modo edicion de botones"
        }

        val adapter = ButtonGridAdapter(
            onSend = { vm.sendButton(it) },
            onLearn = { vm.learnButton(it) },
            onSaveToDb = { btn -> showSaveMetadataDialog(btn) },
            onRename = { btn ->
                val input = EditText(this).apply { setText(btn.label) }
                MaterialAlertDialogBuilder(this)
                    .setTitle("Renombrar boton")
                    .setView(input)
                    .setPositiveButton("Guardar") { _, _ ->
                        val nl = input.text.toString().trim()
                        if (nl.isNotBlank()) vm.renameButton(btn, nl)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },
            onDelete = { btn ->
                showDeleteOptionsDialog(btn)
            }
        )

        b.recyclerButtons.layoutManager = GridLayoutManager(this, 2)
        b.recyclerButtons.adapter = adapter

        vm.buttons.observe(this) { adapter.submit(it) }
        vm.status.observe(this) { b.txtStatus.text = it }

        b.btnSyncFromDb.setOnClickListener {
            vm.syncSignalsFromCloud()
        }

        b.fabAddButton.setOnClickListener {
            showCreateButtonDialog()
        }

        if (autoSyncFromCloud) {
            vm.syncSignalsFromCloud()
        }
    }

    private fun showCreateButtonDialog() {
        val input = EditText(this).apply {
            hint = "Nombre del boton"
        }

        val radioGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }

        val rbIr = RadioButton(this).apply {
            text = "IR"
            id = View.generateViewId()
            isChecked = true
        }

        val rbRf = RadioButton(this).apply {
            text = "RF433"
            id = View.generateViewId()
        }

        radioGroup.addView(rbIr)
        radioGroup.addView(rbRf)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 0)
            addView(input)
            addView(radioGroup)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Nuevo boton")
            .setMessage("Escribe el nombre y elige el tipo de senal")
            .setView(container)
            .setPositiveButton("Crear") { _, _ ->
                val label = input.text.toString().trim()
                val signalType = if (rbRf.isChecked) "RF433" else "IR"
                if (label.isNotBlank()) {
                    vm.addButton(label, signalType)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showSaveMetadataDialog(btn: ButtonEntity) {
        val inputLocation = EditText(this).apply {
            hint = "Nombre (ejemplo: Lab 4)"
            setText("Lab 4")
        }

        val radioGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }

        val rbAire = RadioButton(this).apply {
            text = "Aire"
            id = View.generateViewId()
            isChecked = true
        }
        val rbTv = RadioButton(this).apply {
            text = "TV"
            id = View.generateViewId()
        }
        val rbCortina = RadioButton(this).apply {
            text = "Cortina"
            id = View.generateViewId()
        }

        val signalGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }
        val rbSignalIr = RadioButton(this).apply {
            text = "Infrarroja (IR)"
            id = View.generateViewId()
            isChecked = btn.signalType != "RF433"
        }
        val rbSignalRf = RadioButton(this).apply {
            text = "Radiofrecuencia (RF433)"
            id = View.generateViewId()
            isChecked = btn.signalType == "RF433"
        }

        radioGroup.addView(rbAire)
        radioGroup.addView(rbTv)
        radioGroup.addView(rbCortina)
        signalGroup.addView(rbSignalIr)
        signalGroup.addView(rbSignalRf)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 0)
            addView(inputLocation)
            addView(radioGroup)
            addView(signalGroup)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Guardar en Firestore")
            .setMessage("Completa los datos del equipo")
            .setView(container)
            .setPositiveButton("Guardar") { _, _ ->
                val locationName = inputLocation.text.toString().trim()
                val applianceType = when {
                    rbTv.isChecked -> "TV"
                    rbCortina.isChecked -> "Cortina"
                    else -> "Aire"
                }
                val signalType = if (rbSignalRf.isChecked) "RF433" else "IR"
                vm.saveButtonToCloud(
                    btn = btn,
                    locationName = locationName,
                    applianceType = applianceType,
                    selectedSignalType = signalType
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteOptionsDialog(btn: ButtonEntity) {
        val options = arrayOf(
            "Solo local",
            "Solo base de datos",
            "Local y base de datos"
        )
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar '${btn.label}'")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> vm.deleteButtonLocal(btn)
                    1 -> vm.deleteButtonCloud(btn)
                    2 -> vm.deleteButtonEverywhere(btn)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
