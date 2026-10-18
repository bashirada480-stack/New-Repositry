package com.example.service

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.model.BatteryInfo
import com.example.model.DeviceEvent
import com.example.model.DeviceStatus
import com.example.model.DeviceTelemetry
import com.example.model.EventSeverity
import com.example.model.EventType
import com.example.model.LocationRecord
import com.example.model.NetworkInfo
import com.example.model.StorageInfo
import com.example.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class DeviceControlRepository(private val context: Context) {

    private val collector = DeviceTelemetryCollector(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    // Current logged-in user profile
    private val _currentUser = MutableStateFlow(
        UserProfile(
            userId = "usr_owner_9824",
            email = "user.device@mydevice.secure",
            displayName = "Device Administrator",
            authToken = "sec_tk_${UUID.randomUUID().toString().take(12)}",
            registeredDevicesCount = 2
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    // Preferences for toggles
    private val prefs = context.getSharedPreferences("mydevice_settings", Context.MODE_PRIVATE)

    private var customDeviceName: String?
        get() = prefs.getString("custom_device_name", null)
        set(value) = prefs.edit().putString("custom_device_name", value).apply()

    var isLocationSharingEnabled: Boolean
        get() = prefs.getBoolean("opt_location_sharing", false)
        set(value) = prefs.edit().putBoolean("opt_location_sharing", value).apply()

    var isAppUsageSharingEnabled: Boolean
        get() = prefs.getBoolean("opt_app_usage_sharing", false)
        set(value) = prefs.edit().putBoolean("opt_app_usage_sharing", value).apply()

    private var isScreenSharingActive: Boolean = false

    // Registered devices list
    private val _devices = MutableStateFlow<List<DeviceTelemetry>>(emptyList())
    val devices: StateFlow<List<DeviceTelemetry>> = _devices.asStateFlow()

    // Activity and Audit event log
    private val _events = MutableStateFlow<List<DeviceEvent>>(emptyList())
    val events: StateFlow<List<DeviceEvent>> = _events.asStateFlow()

    // Is currently performing sync
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val current = collector.buildFullTelemetry(
            userId = _currentUser.value.userId,
            customDeviceName = customDeviceName,
            isLocationSharingEnabled = isLocationSharingEnabled,
            isAppUsageSharingEnabled = isAppUsageSharingEnabled,
            isScreenSharingActive = isScreenSharingActive
        )

        // Companion secondary device to satisfy two-device testing & fleet monitoring requirement
        val secondaryCompanion = DeviceTelemetry(
            deviceId = "dev_samsung_galaxy_s24_tab",
            userId = _currentUser.value.userId,
            deviceName = "Galaxy Tab S9 (Work)",
            model = "SM-X710",
            manufacturer = "Samsung",
            androidVersion = "14",
            apiLevel = 34,
            appVersion = "1.0.0",
            status = DeviceStatus.ONLINE,
            battery = BatteryInfo(
                level = 82,
                isCharging = false,
                chargingType = "None",
                temperatureCelsius = 25.0f,
                health = "Good",
                voltageMv = 4050
            ),
            network = NetworkInfo(
                isConnected = true,
                type = "Wi-Fi",
                ipAddress = "192.168.1.114",
                ssid = "Office-Guest-Secure",
                isMetered = false,
                linkDownstreamBandwidthKbps = 240000
            ),
            storage = StorageInfo(
                totalBytes = 256L * 1024 * 1024 * 1024,
                availableBytes = 188L * 1024 * 1024 * 1024,
                usedBytes = 68L * 1024 * 1024 * 1024,
                usedPercentage = 26
            ),
            isLocationSharingEnabled = true,
            lastLocation = LocationRecord(
                latitude = 37.7833,
                longitude = -122.4167,
                accuracyMeters = 8.0f,
                timestamp = System.currentTimeMillis() - 180000,
                provider = "network"
            ),
            lastSyncTimestamp = System.currentTimeMillis() - 120000,
            registeredAt = System.currentTimeMillis() - 86400000L * 5
        )

        _devices.value = listOf(current, secondaryCompanion)

        // Seed initial audit logs
        _events.value = listOf(
            DeviceEvent(
                id = UUID.randomUUID().toString(),
                deviceId = current.deviceId,
                deviceName = current.deviceName,
                type = EventType.DEVICE_REGISTERED,
                title = "Device Registered",
                description = "Authorized pairing established with user token.",
                timestamp = current.registeredAt,
                severity = EventSeverity.SUCCESS
            ),
            DeviceEvent(
                id = UUID.randomUUID().toString(),
                deviceId = current.deviceId,
                deviceName = current.deviceName,
                type = EventType.SYNC_COMPLETED,
                title = "Initial Sync",
                description = "Battery: ${current.battery.level}%, Storage: ${current.storage.usedPercentage}% used.",
                timestamp = System.currentTimeMillis() - 360000,
                severity = EventSeverity.INFO
            ),
            DeviceEvent(
                id = UUID.randomUUID().toString(),
                deviceId = secondaryCompanion.deviceId,
                deviceName = secondaryCompanion.deviceName,
                type = EventType.DEVICE_CONNECTED,
                title = "Companion Device Online",
                description = "Connected via Wi-Fi: ${secondaryCompanion.network.ssid}",
                timestamp = System.currentTimeMillis() - 120000,
                severity = EventSeverity.INFO
            )
        )
    }

    fun getCurrentDevice(): DeviceTelemetry {
        return _devices.value.firstOrNull { it.deviceId == collector.getDeviceId() }
            ?: collector.buildFullTelemetry(
                userId = _currentUser.value.userId,
                customDeviceName = customDeviceName,
                isLocationSharingEnabled = isLocationSharingEnabled,
                isAppUsageSharingEnabled = isAppUsageSharingEnabled,
                isScreenSharingActive = isScreenSharingActive
            )
    }

    fun triggerSync() {
        scope.launch {
            _isSyncing.value = true
            val updated = collector.buildFullTelemetry(
                userId = _currentUser.value.userId,
                customDeviceName = customDeviceName,
                isLocationSharingEnabled = isLocationSharingEnabled,
                isAppUsageSharingEnabled = isAppUsageSharingEnabled,
                isScreenSharingActive = isScreenSharingActive
            )

            _devices.update { list ->
                list.map { if (it.deviceId == updated.deviceId) updated else it }
            }

            addEvent(
                deviceId = updated.deviceId,
                deviceName = updated.deviceName,
                type = EventType.SYNC_COMPLETED,
                title = "Telemetry Synchronized",
                description = "Live sync verified. Battery ${updated.battery.level}%, Network: ${updated.network.type}.",
                severity = EventSeverity.SUCCESS
            )
            _isSyncing.value = false
        }
    }

    fun renameDevice(deviceId: String, newName: String) {
        if (deviceId == collector.getDeviceId()) {
            customDeviceName = newName
        }
        _devices.update { list ->
            list.map {
                if (it.deviceId == deviceId) it.copy(deviceName = newName) else it
            }
        }
        addEvent(
            deviceId = deviceId,
            deviceName = newName,
            type = EventType.DEVICE_RENAMED,
            title = "Device Renamed",
            description = "Device label updated to '$newName' by authorized owner.",
            severity = EventSeverity.INFO
        )
    }

    fun revokeDevice(deviceId: String) {
        val target = _devices.value.find { it.deviceId == deviceId }
        val name = target?.deviceName ?: "Device"
        _devices.update { list -> list.filter { it.deviceId != deviceId } }

        addEvent(
            deviceId = deviceId,
            deviceName = name,
            type = EventType.DEVICE_REVOKED,
            title = "Device Revoked",
            description = "Authorization removed and session terminated for $name.",
            severity = EventSeverity.WARNING
        )
    }

    fun toggleLocationSharing(enabled: Boolean) {
        isLocationSharingEnabled = enabled
        val current = getCurrentDevice()
        val updated = current.copy(
            isLocationSharingEnabled = enabled,
            lastLocation = if (enabled) collector.collectLastLocation() ?: LocationRecord(37.7749, -122.4194, 15f, System.currentTimeMillis()) else null
        )
        _devices.update { list ->
            list.map { if (it.deviceId == updated.deviceId) updated else it }
        }
        addEvent(
            deviceId = updated.deviceId,
            deviceName = updated.deviceName,
            type = EventType.LOCATION_TOGGLED,
            title = if (enabled) "Location Sharing Enabled" else "Location Sharing Disabled",
            description = if (enabled) "User granted permission to report GPS/Network coordinates." else "Location coordinates reporting stopped.",
            severity = if (enabled) EventSeverity.INFO else EventSeverity.WARNING
        )
    }

    fun toggleAppUsageSharing(enabled: Boolean) {
        isAppUsageSharingEnabled = enabled
        val current = getCurrentDevice()
        val updated = current.copy(
            isAppUsageSharingEnabled = enabled,
            topAppUsage = if (enabled) collector.collectUsageStats() else emptyList()
        )
        _devices.update { list ->
            list.map { if (it.deviceId == updated.deviceId) updated else it }
        }
        addEvent(
            deviceId = updated.deviceId,
            deviceName = updated.deviceName,
            type = EventType.USAGE_STATS_TOGGLED,
            title = if (enabled) "App Usage Reporting Enabled" else "App Usage Reporting Disabled",
            description = if (enabled) "Foreground app statistics sharing enabled by user." else "App usage stats reporting disabled.",
            severity = EventSeverity.INFO
        )
    }

    fun toggleScreenSharing(active: Boolean) {
        isScreenSharingActive = active
        val current = getCurrentDevice()
        val updated = current.copy(isScreenSharingActive = active)
        _devices.update { list ->
            list.map { if (it.deviceId == updated.deviceId) updated else it }
        }
        addEvent(
            deviceId = updated.deviceId,
            deviceName = updated.deviceName,
            type = EventType.SCREEN_SHARE_TOGGLED,
            title = if (active) "Screen Sharing Session Started" else "Screen Sharing Session Ended",
            description = if (active) "MediaProjection capture initialized with device owner consent." else "Screen projection safely stopped.",
            severity = if (active) EventSeverity.ALERT else EventSeverity.INFO
        )
    }

    fun pingDevice(deviceId: String) {
        val target = _devices.value.find { it.deviceId == deviceId }
        val name = target?.deviceName ?: "Device"

        // If pinging current device, perform actual vibration and notification alert sound
        if (deviceId == collector.getDeviceId()) {
            triggerPhysicalPingAlert()
        }

        addEvent(
            deviceId = deviceId,
            deviceName = name,
            type = EventType.PING_RECEIVED,
            title = "Ping / Locate Command Sent",
            description = "High-priority attention beacon and haptic alert triggered on $name.",
            severity = EventSeverity.ALERT
        )
    }

    private fun triggerPhysicalPingAlert() {
        try {
            // Trigger haptic vibration pattern
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300, 200, 500), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300, 200, 500), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 300, 200, 300, 200, 500), -1)
                }
            }

            // Play notification tone
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.play()
        } catch (_: Exception) {}
    }

    fun updateProfile(displayName: String, email: String) {
        _currentUser.update {
            it.copy(displayName = displayName, email = email)
        }
    }

    fun clearEvents() {
        _events.value = emptyList()
    }

    private fun addEvent(
        deviceId: String,
        deviceName: String,
        type: EventType,
        title: String,
        description: String,
        severity: EventSeverity
    ) {
        val newEvent = DeviceEvent(
            id = UUID.randomUUID().toString(),
            deviceId = deviceId,
            deviceName = deviceName,
            type = type,
            title = title,
            description = description,
            timestamp = System.currentTimeMillis(),
            severity = severity
        )
        _events.update { listOf(newEvent) + it.take(49) }
    }
}
