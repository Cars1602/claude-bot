package com.example.cloudbot.ui.provision.steps

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.cloudbot.R
import com.example.cloudbot.databinding.FragmentConnectHubBinding
import com.example.cloudbot.di.ServiceLocator
import com.example.cloudbot.net.ApiFactory
import com.example.cloudbot.ui.provision.AddDeviceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConnectHubFragment : Fragment(R.layout.fragment_connect_hub) {

    private var _b: FragmentConnectHubBinding? = null
    private val b get() = _b!!

    private val vm: AddDeviceViewModel by activityViewModels {
        AddDeviceViewModel.Factory(ServiceLocator.deviceRepo(requireContext()))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentConnectHubBinding.bind(view)

        b.txtHint.text =
            "Conectate al WiFi del ESP32 (ej. CLOUDBOT-XXXX) y luego pulsa el boton."

        b.btnOpenWifi.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }

        b.btnImConnected.setOnClickListener {
            b.txtHint.text = "Verificando conexion con el dispositivo en 192.168.4.1..."

            viewLifecycleOwner.lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val api = ApiFactory.create("http://192.168.4.1/")
                        api.provisionStatus().ok
                    } catch (_: Exception) {
                        false
                    }
                }

                if (ok) {
                    parentFragmentManager.commit {
                        replace(R.id.provisionContainer, ProvisioningFragment())
                        addToBackStack("hub")
                    }
                } else {
                    b.txtHint.text =
                        "No pude acceder a 192.168.4.1.\n" +
                            "1) Conectate al WiFi del ESP32 (ej. CLOUDBOT-XXXX)\n" +
                            "2) Contrasena: 12345678\n" +
                            "3) Si dice 'Sin internet', elige 'Mantener conexion'\n" +
                            "4) Desactiva el cambio automatico a datos moviles."
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
