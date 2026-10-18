package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.DeviceEvent
import com.example.model.DeviceTelemetry
import com.example.model.UserProfile
import com.example.service.DeviceControlRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NavigationTab {
    DASHBOARD,
    DEVICE_AGENT,
    AUDIT_LOGS,
    SECURITY_SPEC
}

data class MainUiState(
    val activeTab: NavigationTab = NavigationTab.DASHBOARD,
    val currentUser: UserProfile? = null,
    val devices: List<DeviceTelemetry> = emptyList(),
    val currentDevice: DeviceTelemetry? = null,
    val selectedDeviceForDetails: DeviceTelemetry? = null,
    val events: List<DeviceEvent> = emptyList(),
    val isSyncing: Boolean = false,
    val deviceToRename: DeviceTelemetry? = null,
    val showArchitectureModal: Boolean = false,
    val pingNotificationMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeviceControlRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Collect repository states and update UI state reactively
        viewModelScope.launch {
            repository.currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }

        viewModelScope.launch {
            repository.devices.collect { deviceList ->
                val currId = repository.getCurrentDevice().deviceId
                val currentDev = deviceList.firstOrNull { it.deviceId == currId } ?: repository.getCurrentDevice()
                _uiState.update { state ->
                    val updatedSelected = if (state.selectedDeviceForDetails != null) {
                        deviceList.firstOrNull { it.deviceId == state.selectedDeviceForDetails.deviceId }
                            ?: state.selectedDeviceForDetails
                    } else null
                    state.copy(
                        devices = deviceList,
                        currentDevice = currentDev,
                        selectedDeviceForDetails = updatedSelected
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.events.collect { eventList ->
                _uiState.update { it.copy(events = eventList) }
            }
        }

        viewModelScope.launch {
            repository.isSyncing.collect { syncing ->
                _uiState.update { it.copy(isSyncing = syncing) }
            }
        }
    }

    fun selectTab(tab: NavigationTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun openDeviceDetails(device: DeviceTelemetry) {
        _uiState.update { it.copy(selectedDeviceForDetails = device) }
    }

    fun closeDeviceDetails() {
        _uiState.update { it.copy(selectedDeviceForDetails = null) }
    }

    fun triggerSync() {
        repository.triggerSync()
    }

    fun openRenameDialog(device: DeviceTelemetry) {
        _uiState.update { it.copy(deviceToRename = device) }
    }

    fun closeRenameDialog() {
        _uiState.update { it.copy(deviceToRename = null) }
    }

    fun confirmRename(deviceId: String, newName: String) {
        if (newName.isNotBlank()) {
            repository.renameDevice(deviceId, newName.trim())
        }
        closeRenameDialog()
    }

    fun revokeDevice(deviceId: String) {
        repository.revokeDevice(deviceId)
        closeDeviceDetails()
    }

    fun pingDevice(deviceId: String) {
        val target = uiState.value.devices.find { it.deviceId == deviceId }
        val name = target?.deviceName ?: "Device"
        repository.pingDevice(deviceId)
        _uiState.update { it.copy(pingNotificationMessage = "Attention signal and ping dispatched to $name") }
    }

    fun dismissPingMessage() {
        _uiState.update { it.copy(pingNotificationMessage = null) }
    }

    fun toggleLocationSharing(enabled: Boolean) {
        repository.toggleLocationSharing(enabled)
    }

    fun toggleAppUsageSharing(enabled: Boolean) {
        repository.toggleAppUsageSharing(enabled)
    }

    fun toggleScreenSharing(active: Boolean) {
        repository.toggleScreenSharing(active)
    }

    fun setShowArchitectureModal(show: Boolean) {
        _uiState.update { it.copy(showArchitectureModal = show) }
    }

    fun clearEvents() {
        repository.clearEvents()
    }
}
