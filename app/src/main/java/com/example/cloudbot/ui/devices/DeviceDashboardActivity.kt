package com.example.cloudbot.ui.devices

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cloudbot.R
import com.example.cloudbot.databinding.ActivityDeviceDashboardBinding
import com.example.cloudbot.di.ServiceLocator
import com.example.cloudbot.ui.remote.RemoteEditorActivity
import com.google.firebase.database.ValueEventListener

class DeviceDashboardActivity : AppCompatActivity() {

    private lateinit var b: ActivityDeviceDashboardBinding
    private var realtimeListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDeviceDashboardBinding.inflate(layoutInflater)
        setContentView(b.root)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val hubId = intent.getStringExtra("hubId") ?: return
        val ip = intent.getStringExtra("deviceIp") ?: ""
        val name = intent.getStringExtra("deviceName") ?: "Dispositivo"

        b.txtTitle.text = name
        b.txtSubtitle.text = "IP: $ip"
        b.txtRealtimeState.text = "Estado en tiempo real: cargando..."
        b.btnBack.setOnClickListener { finish() }
        b.btnEdit.setOnClickListener {
            Toast.makeText(this, "Edicion del dispositivo disponible pronto", Toast.LENGTH_SHORT).show()
        }

        b.cardLearnButtons.setOnClickListener {
            startActivity(Intent(this, RemoteEditorActivity::class.java).apply {
                putExtra("hubId", hubId)
                putExtra("deviceIp", ip)
                putExtra("deviceName", name)
            })
        }

        b.cardPresets.setOnClickListener {
            Toast.makeText(this, "Proximamente: controles predefinidos", Toast.LENGTH_SHORT).show()
        }

        realtimeListener = ServiceLocator.realtimePowerRepo().observePowerState(
            deviceName = name,
            hubId = hubId,
            onUpdate = { state ->
                runOnUiThread {
                    b.txtRealtimeState.text = when (state.isOn) {
                        true -> "Estado en tiempo real: Encendido"
                        false -> "Estado en tiempo real: Apagado"
                        null -> "Estado en tiempo real: sin datos"
                    }
                    b.txtRealtimeState.setBackgroundResource(
                        when (state.isOn) {
                            true -> R.drawable.bg_status_on
                            false -> R.drawable.bg_status_off
                            null -> R.drawable.bg_dark_chip
                        }
                    )
                }
            },
            onError = { message ->
                runOnUiThread {
                    b.txtRealtimeState.text = "Estado en tiempo real: error ($message)"
                    b.txtRealtimeState.setBackgroundResource(R.drawable.bg_status_off)
                }
            }
        )
    }

    override fun onDestroy() {
        realtimeListener?.let { ServiceLocator.realtimePowerRepo().removeObserver(it) }
        realtimeListener = null
        super.onDestroy()
    }
}
