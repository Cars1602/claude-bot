package com.example.cloudbot.ui.provision.steps

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.cloudbot.R
import com.example.cloudbot.databinding.FragmentDoneBinding
import com.example.cloudbot.di.ServiceLocator
import com.example.cloudbot.ui.provision.AddDeviceViewModel

class DoneFragment : Fragment(R.layout.fragment_done) {

    private var _b: FragmentDoneBinding? = null
    private val b get() = _b!!

    private val vm: AddDeviceViewModel by activityViewModels {
        AddDeviceViewModel.Factory(ServiceLocator.deviceRepo(requireContext()))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentDoneBinding.bind(view)

        b.txtIp.text = "IP del dispositivo: ${vm.staIp.value}"
        b.txtStatus.text =
            "Provisionamiento completado. Cambia a tu WiFi del router y guarda el dispositivo."

        if (b.edtName.text.isNullOrBlank()) {
            b.edtName.setText("CloudBot Hub")
        }

        b.btnOpenWifi.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }

        b.btnSave.setOnClickListener {
            val name = b.edtName.text.toString().trim().ifBlank { "CloudBot Hub" }
            vm.verifyAndSaveDevice(name)
        }

        vm.statusText.observe(viewLifecycleOwner) { b.txtStatus.text = it }
        vm.hubId.observe(viewLifecycleOwner) { hid ->
            if (!hid.isNullOrBlank()) requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}