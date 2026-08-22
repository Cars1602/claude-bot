package com.example.cloudbot.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cloudbot.data.repo.DeviceRepository
import kotlinx.coroutines.launch

class DeviceListViewModel(private val repo: DeviceRepository) : ViewModel() {

    val devices = repo.observeAll()

    fun renameDevice(deviceId: String, newName: String) = viewModelScope.launch {
        repo.updateName(deviceId, newName)
    }

    fun updateDeviceIp(deviceId: String, newIp: String) = viewModelScope.launch {
        repo.updateIp(deviceId, newIp)
    }

    fun deleteDevice(deviceId: String) = viewModelScope.launch {
        repo.deleteDevice(deviceId)
    }

    class Factory(private val repo: DeviceRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return DeviceListViewModel(repo) as T
        }
    }
}
