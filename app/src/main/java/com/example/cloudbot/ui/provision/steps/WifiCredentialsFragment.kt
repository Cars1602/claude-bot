package com.example.cloudbot.ui.provision.steps

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import com.example.cloudbot.R
import com.example.cloudbot.databinding.FragmentWifiCredentialsBinding
import com.example.cloudbot.di.ServiceLocator
import com.example.cloudbot.ui.provision.AddDeviceViewModel
import com.example.cloudbot.util.WifiUtil

class WifiCredentialsFragment : Fragment(R.layout.fragment_wifi_credentials) {

    private var _b: FragmentWifiCredentialsBinding? = null
    private val b get() = _b!!

    private val vm: AddDeviceViewModel by activityViewModels {
        AddDeviceViewModel.Factory(ServiceLocator.deviceRepo(requireContext()))
    }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { fillCurrentSsidIfPossible() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentWifiCredentialsBinding.bind(view)

        fillCurrentSsidIfPossible()

        b.btnUseCurrent.setOnClickListener {
            if (hasWifiSsidPermission()) fillCurrentSsidIfPossible()
            else requestWifiSsidPermission()
        }

        b.btnNext.setOnClickListener {
            val ssid = b.edtSsid.text.toString().trim()
            val pass = b.edtPass.text.toString()

            if (ssid.isBlank()) {
                b.edtSsid.error = "Ingresa el SSID (WiFi 2.4GHz)"
                return@setOnClickListener
            }

            vm.routerSsid.value = ssid
            vm.routerPass.value = pass

            parentFragmentManager.commit {
                replace(R.id.provisionContainer, ConnectHubFragment())
                addToBackStack("wifi")
            }
        }
    }

    private fun fillCurrentSsidIfPossible() {
        val ssid = WifiUtil.getCurrentSsid(requireContext())
        if (!ssid.isNullOrBlank()) b.edtSsid.setText(ssid)
    }

    private fun hasWifiSsidPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.NEARBY_WIFI_DEVICES) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestWifiSsidPermission() {
        val perms = if (Build.VERSION.SDK_INT >= 33)
            arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        else
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        permLauncher.launch(perms)
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}