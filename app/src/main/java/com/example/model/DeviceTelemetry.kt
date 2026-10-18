package com.example.model

enum class DeviceStatus {
    ONLINE,
    IDLE,
    OFFLINE
}

enum class EventSeverity {
    INFO,
    SUCCESS,
    WARNING,
    ALERT
}

enum class EventType {
    DEVICE_REGISTERED,
    DEVICE_CONNECTED,
    DEVICE_DISCONNECTED,
    BATTERY_LOW,
    BATTERY_CHARGING,
    SYNC_COMPLETED,
    PERMISSION_CHANGED,
    LOCATION_TOGGLED,
    USAGE_STATS_TOGGLED,
    SCREEN_SHARE_TOGGLED,
    PING_RECEIVED,
    DEVICE_RENAMED,
    DEVICE_REVOKED
}

data class BatteryInfo(
    val level: Int = 100,
    val isCharging: Boolean = false,
    val chargingType: String = "AC", // AC, USB, Wireless, None
    val temperatureCelsius: Float = 27.5f,
    val health: String = "Good",
    val voltageMv: Int = 4120
)

data class NetworkInfo(
    val isConnected: Boolean = true,
    val type: String = "Wi-Fi", // Wi-Fi, Cellular, Ethernet, Disconnected
    val ipAddress: String = "192.168.1.105",
    val ssid: String? = "Home-5G",
    val isMetered: Boolean = false,
    val linkDownstreamBandwidthKbps: Int = 150000
)

data class StorageInfo(
    val totalBytes: Long = 128L * 1024 * 1024 * 1024, // 128 GB
    val availableBytes: Long = 64L * 1024 * 1024 * 1024, // 64 GB
    val usedBytes: Long = 64L * 1024 * 1024 * 1024,
    val usedPercentage: Int = 50
)

data class PermissionsState(
    val locationFine: Boolean = false,
    val locationCoarse: Boolean = false,
    val notifications: Boolean = true,
    val usageStats: Boolean = false
)

data class LocationRecord(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestamp: Long,
    val provider: String = "gps"
)

data class AppUsageRecord(
    val packageName: String,
    val appName: String,
    val totalTimeInForegroundMillis: Long,
    val lastTimeUsed: Long
)

data class DeviceTelemetry(
    val deviceId: String,
    val userId: String,
    val deviceName: String,
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val apiLevel: Int,
    val appVersion: String,
    val status: DeviceStatus = DeviceStatus.ONLINE,
    val battery: BatteryInfo = BatteryInfo(),
    val network: NetworkInfo = NetworkInfo(),
    val storage: StorageInfo = StorageInfo(),
    val permissions: PermissionsState = PermissionsState(),
    val isLocationSharingEnabled: Boolean = false,
    val lastLocation: LocationRecord? = null,
    val isAppUsageSharingEnabled: Boolean = false,
    val topAppUsage: List<AppUsageRecord> = emptyList(),
    val isScreenSharingActive: Boolean = false,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val registeredAt: Long = System.currentTimeMillis()
)

data class DeviceEvent(
    val id: String,
    val deviceId: String,
    val deviceName: String,
    val type: EventType,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val severity: EventSeverity = EventSeverity.INFO
)

data class UserProfile(
    val userId: String,
    val email: String,
    val displayName: String,
    val authToken: String,
    val registeredDevicesCount: Int = 1
)
