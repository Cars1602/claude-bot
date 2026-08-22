package com.example.cloudbot.ui.devices

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cloudbot.databinding.ActivityDeviceDashboardBinding
import com.example.cloudbot.ui.remote.RemoteEditorActivity

class DeviceDashboardActivity : AppCompatActivity() {

    private lateinit var b: ActivityDeviceDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDeviceDashboardBinding.inflate(layoutInflater)
        setContentView(b.root)

        val hubId = intent.getStringExtra("hubId") ?: return
        val ip = intent.getStringExtra("deviceIp") ?: ""
        val name = intent.getStringExtra("deviceName") ?: "Dispositivo"

        b.txtTitle.text = name
        b.txtSubtitle.text = "IP: $ip"
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
    }
}
