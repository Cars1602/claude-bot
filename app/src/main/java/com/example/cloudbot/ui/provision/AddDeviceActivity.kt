package com.example.cloudbot.ui.provision

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.example.cloudbot.R
import com.example.cloudbot.databinding.ActivityAddDeviceBinding
import com.example.cloudbot.di.ServiceLocator
import com.example.cloudbot.ui.provision.steps.WifiCredentialsFragment

class AddDeviceActivity : AppCompatActivity() {

    private lateinit var b: ActivityAddDeviceBinding

    private val vm: AddDeviceViewModel by viewModels {
        AddDeviceViewModel.Factory(ServiceLocator.deviceRepo(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAddDeviceBinding.inflate(layoutInflater)
        setContentView(b.root)

        title = "Anadir dispositivo"

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.provisionContainer, WifiCredentialsFragment())
            }
        }
    }
}
