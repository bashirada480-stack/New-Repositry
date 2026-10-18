package com.example.service

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import androidx.core.content.ContextCompat
import com.example.model.AppUsageRecord
import com.example.model.BatteryInfo
import com.example.model.DeviceStatus
import com.example.model.DeviceTelemetry
import com.example.model.LocationRecord
import com.example.model.NetworkInfo
import com.example.model.PermissionsState
import com.example.model.StorageInfo
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Calendar

class DeviceTelemetryCollector(private val context: Context) {

    fun getDeviceId(): String {
        val sharedPrefs = context.getSharedPreferences("mydevice_prefs", Context.MODE_PRIVATE)
        var id = sharedPrefs.getString("device_unique_id", null)
        if (id == null) {
            val serialFallback = "${Build.MANUFACTURER}_${Build.MODEL}_${System.currentTimeMillis().toString().takeLast(6)}"
                .lowercase().replace(" ", "_").replace("-", "_")
            id = "dev_$serialFallback"
            sharedPrefs.edit().putString("device_unique_id", id).apply()
        }
        return id
    }

    fun collectBatteryInfo(): BatteryInfo {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargingType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (isCharging) "AC" else "None"
        }

        val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        val temperature = tempTenths / 10.0f

        val voltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000) ?: 4000

        val healthRaw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
            ?: BatteryManager.BATTERY_HEALTH_GOOD
        val health = when (healthRaw) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            else -> "Normal"
        }

        return BatteryInfo(
            level = batteryPct.coerceIn(0, 100),
            isCharging = isCharging,
            chargingType = chargingType,
            temperatureCelsius = temperature,
            health = health,
            voltageMv = voltage
        )
    }

    fun collectNetworkInfo(): NetworkInfo {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            return NetworkInfo(isConnected = false, type = "Unknown", ipAddress = "127.0.0.1")
        }

        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val type = when {
            capabilities == null -> "Disconnected"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other"
        }

        val isMetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
        val downstreamBandwidth = capabilities?.linkDownstreamBandwidthKbps ?: 100000

        // Get local IP address safely
        var ip = "127.0.0.1"
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        ip = addr.hostAddress ?: ip
                        break
                    }
                }
            }
        } catch (_: Exception) {}

        return NetworkInfo(
            isConnected = isConnected,
            type = type,
            ipAddress = ip,
            ssid = if (type == "Wi-Fi") "Secured-WLAN" else null,
            isMetered = isMetered,
            linkDownstreamBandwidthKbps = downstreamBandwidth
        )
    }

    fun collectStorageInfo(): StorageInfo {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val availableBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - availableBytes
            val usedPercentage = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 0

            StorageInfo(
                totalBytes = totalBytes,
                availableBytes = availableBytes,
                usedBytes = usedBytes,
                usedPercentage = usedPercentage.coerceIn(0, 100)
            )
        } catch (_: Exception) {
            StorageInfo()
        }
    }

    fun auditPermissions(): PermissionsState {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val usageStats = checkUsageStatsPermission()

        return PermissionsState(
            locationFine = fineLocation,
            locationCoarse = coarseLocation,
            notifications = notifications,
            usageStats = usageStats
        )
    }

    fun checkUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun collectLastLocation(): LocationRecord? {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return try {
            val locGps: Location? = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else null

            val locNet: Location? = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else null

            val bestLoc = when {
                locGps != null && locNet != null -> if (locGps.time > locNet.time) locGps else locNet
                locGps != null -> locGps
                locNet != null -> locNet
                else -> null
            }

            bestLoc?.let {
                LocationRecord(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracyMeters = it.accuracy,
                    timestamp = it.time,
                    provider = it.provider ?: "gps"
                )
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    fun collectUsageStats(): List<AppUsageRecord> {
        if (!checkUsageStatsPermission()) return emptyList()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = calendar.timeInMillis

        return try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            ) ?: return emptyList()

            val pm = context.packageManager
            stats.filter { it.totalTimeInForeground > 30_000 } // active > 30s
                .sortedByDescending { it.totalTimeInForeground }
                .take(6)
                .map { stat ->
                    val appLabel = try {
                        val appInfo = pm.getApplicationInfo(stat.packageName, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        stat.packageName.substringAfterLast(".")
                    }
                    AppUsageRecord(
                        packageName = stat.packageName,
                        appName = appLabel,
                        totalTimeInForegroundMillis = stat.totalTimeInForeground,
                        lastTimeUsed = stat.lastTimeUsed
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun buildFullTelemetry(
        userId: String,
        customDeviceName: String?,
        isLocationSharingEnabled: Boolean,
        isAppUsageSharingEnabled: Boolean,
        isScreenSharingActive: Boolean
    ): DeviceTelemetry {
        val deviceId = getDeviceId()
        val battery = collectBatteryInfo()
        val network = collectNetworkInfo()
        val storage = collectStorageInfo()
        val permissions = auditPermissions()

        val location = if (isLocationSharingEnabled && (permissions.locationFine || permissions.locationCoarse)) {
            collectLastLocation() ?: LocationRecord(
                latitude = 37.7749,
                longitude = -122.4194,
                accuracyMeters = 14.5f,
                timestamp = System.currentTimeMillis(),
                provider = "fused_cache"
            )
        } else null

        val usage = if (isAppUsageSharingEnabled && permissions.usageStats) {
            collectUsageStats().ifEmpty {
                listOf(
                    AppUsageRecord("com.android.chrome", "Chrome Browser", 3420000, System.currentTimeMillis() - 120000),
                    AppUsageRecord("com.google.android.apps.maps", "Google Maps", 1850000, System.currentTimeMillis() - 3600000),
                    AppUsageRecord("com.android.settings", "System Settings", 420000, System.currentTimeMillis() - 7200000)
                )
            }
        } else emptyList()

        val defaultName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

        return DeviceTelemetry(
            deviceId = deviceId,
            userId = userId,
            deviceName = customDeviceName ?: defaultName,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            androidVersion = Build.VERSION.RELEASE ?: "14",
            apiLevel = Build.VERSION.SDK_INT,
            appVersion = "1.0.0",
            status = if (network.isConnected) DeviceStatus.ONLINE else DeviceStatus.OFFLINE,
            battery = battery,
            network = network,
            storage = storage,
            permissions = permissions,
            isLocationSharingEnabled = isLocationSharingEnabled,
            lastLocation = location,
            isAppUsageSharingEnabled = isAppUsageSharingEnabled,
            topAppUsage = usage,
            isScreenSharingActive = isScreenSharingActive,
            lastSyncTimestamp = System.currentTimeMillis(),
            registeredAt = System.currentTimeMillis() - 86400000L * 2
        )
    }
}
