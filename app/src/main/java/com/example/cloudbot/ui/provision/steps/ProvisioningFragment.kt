package com.example.cloudbot.ui.provision.steps

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import com.example.cloudbot.R
import com.example.cloudbot.databinding.FragmentProvisioningBinding
import com.example.cloudbot.di.ServiceLocator
import com.example.cloudbot.ui.provision.AddDeviceViewModel

class ProvisioningFragment : Fragment(R.layout.fragment_provisioning) {

    private var _b: FragmentProvisioningBinding? = null
    private val b get() = _b!!

    private val vm: AddDeviceViewModel by activityViewModels {
        AddDeviceViewModel.Factory(ServiceLocator.deviceRepo(requireContext()))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentProvisioningBinding.bind(view)

        b.txtStatus.text = "Listo para provisionar el dispositivo."

        b.btnStart.setOnClickListener {
            vm.startProvision()
        }

        vm.statusText.observe(viewLifecycleOwner) {
            b.txtStatus.text = it
        }

        vm.working.observe(viewLifecycleOwner) { w ->
            b.progress.visibility = if (w) View.VISIBLE else View.INVISIBLE
        }

        vm.staIp.observe(viewLifecycleOwner) { ip ->
            if (!ip.isNullOrBlank()) {
                parentFragmentManager.commit {
                    replace(R.id.provisionContainer, DoneFragment())
                    addToBackStack("done")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}