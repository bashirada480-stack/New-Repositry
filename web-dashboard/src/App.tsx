import React, { useState, useEffect } from 'react';
import { 
  Shield, 
  Smartphone, 
  Battery, 
  Wifi, 
  RefreshCw, 
  CheckCircle, 
  AlertTriangle, 
  Bell, 
  ExternalLink,
  Lock,
  Layers
} from 'lucide-react';
import { DeviceTelemetry, DeviceEvent } from './types';

export const App: React.FC = () => {
  const [devices, setDevices] = useState<DeviceTelemetry[]>([
    {
      deviceId: "dev_google_pixel_8_pro",
      userId: "usr_owner_9824",
      deviceName: "Pixel 8 Pro (Personal)",
      model: "Pixel 8 Pro",
      manufacturer: "Google",
      androidVersion: "14",
      apiLevel: 34,
      appVersion: "1.0.0",
      status: "ONLINE",
      battery: {
        level: 94,
        isCharging: true,
        chargingType: "USB-PD",
        temperatureCelsius: 28.2,
        health: "Good",
        voltageMv: 4210
      },
      network: {
        isConnected: true,
        type: "Wi-Fi",
        ipAddress: "192.168.1.105",
        ssid: "Home-WLAN-5G",
        isMetered: false,
        linkDownstreamBandwidthKbps: 350000
      },
      storage: {
        totalBytes: 128 * 1024 * 1024 * 1024,
        availableBytes: 68 * 1024 * 1024 * 1024,
        usedBytes: 60 * 1024 * 1024 * 1024,
        usedPercentage: 46
      },
      permissions: {
        locationFine: true,
        locationCoarse: true,
        notifications: true,
        usageStats: false
      },
      isLocationSharingEnabled: true,
      lastLocation: {
        latitude: 37.7749,
        longitude: -122.4194,
        accuracyMeters: 12.0,
        timestamp: Date.now() - 60000
      },
      isAppUsageSharingEnabled: false,
      topAppUsage: [],
      isScreenSharingActive: false,
      lastSyncTimestamp: Date.now() - 45000,
      registeredAt: Date.now() - 86400000 * 3
    },
    {
      deviceId: "dev_samsung_galaxy_tab_s9",
      userId: "usr_owner_9824",
      deviceName: "Galaxy Tab S9 (Work)",
      model: "SM-X710",
      manufacturer: "Samsung",
      androidVersion: "14",
      apiLevel: 34,
      appVersion: "1.0.0",
      status: "ONLINE",
      battery: {
        level: 82,
        isCharging: false,
        chargingType: "None",
        temperatureCelsius: 24.8,
        health: "Good",
        voltageMv: 4050
      },
      network: {
        isConnected: true,
        type: "Wi-Fi",
        ipAddress: "192.168.1.114",
        ssid: "Office-Secure",
        isMetered: false,
        linkDownstreamBandwidthKbps: 220000
      },
      storage: {
        totalBytes: 256 * 1024 * 1024 * 1024,
        availableBytes: 188 * 1024 * 1024 * 1024,
        usedBytes: 68 * 1024 * 1024 * 1024,
        usedPercentage: 26
      },
      permissions: {
        locationFine: false,
        locationCoarse: false,
        notifications: true,
        usageStats: false
      },
      isLocationSharingEnabled: false,
      isAppUsageSharingEnabled: false,
      topAppUsage: [],
      isScreenSharingActive: false,
      lastSyncTimestamp: Date.now() - 120000,
      registeredAt: Date.now() - 86400000 * 6
    }
  ]);

  const [selectedDevice, setSelectedDevice] = useState<DeviceTelemetry | null>(null);
  const [isSyncing, setIsSyncing] = useState(false);
  const [notification, setNotification] = useState<string | null>(null);

  const handleSyncFleet = () => {
    setIsSyncing(true);
    setTimeout(() => {
      setDevices(prev => prev.map(dev => ({
        ...dev,
        lastSyncTimestamp: Date.now()
      })));
      setIsSyncing(false);
      showNotification("Fleet telemetry refreshed successfully.");
    }, 800);
  };

  const handlePingDevice = (device: DeviceTelemetry) => {
    showNotification(`Attention ping dispatched to ${device.deviceName}. Haptic beacon triggered.`);
  };

  const showNotification = (msg: string) => {
    setNotification(msg);
    setTimeout(() => setNotification(null), 4000);
  };

  const onlineCount = devices.filter(d => d.status === 'ONLINE').length;
  const avgBattery = Math.round(devices.reduce((acc, d) => acc + d.battery.level, 0) / (devices.length || 1));

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 font-sans">
      {/* Top Navigation */}
      <header className="border-b border-slate-800 bg-slate-900/60 backdrop-blur sticky top-0 z-40 px-6 py-4 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-cyan-500/20 border border-cyan-500/30 flex items-center justify-center text-cyan-400">
            <Shield className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-tight">MyDevice Control Center</h1>
            <p className="text-xs text-slate-400">Authorized Fleet Monitoring & Security Telemetry</p>
          </div>
        </div>

        <div className="flex items-center space-x-4">
          <div className="hidden sm:flex items-center space-x-2 px-3 py-1.5 rounded-lg bg-slate-800 border border-slate-700 text-xs">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            <span className="text-slate-300">user.device@mydevice.secure</span>
          </div>
          <button 
            onClick={handleSyncFleet}
            disabled={isSyncing}
            className="flex items-center space-x-2 px-4 py-2 bg-cyan-600 hover:bg-cyan-500 disabled:opacity-50 text-white rounded-lg text-sm font-semibold transition"
          >
            <RefreshCw className={`w-4 h-4 ${isSyncing ? 'animate-spin' : ''}`} />
            <span>{isSyncing ? 'Syncing...' : 'Sync Fleet'}</span>
          </button>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-6 py-8">
        {/* Toast Notification */}
        {notification && (
          <div className="mb-6 p-4 rounded-xl bg-cyan-950/80 border border-cyan-500/40 text-cyan-200 text-sm flex items-center justify-between">
            <span>{notification}</span>
            <button onClick={() => setNotification(null)} className="text-cyan-400 hover:text-white">&times;</button>
          </div>
        )}

        {/* Fleet KPI Banner */}
        <section className="grid grid-cols-1 sm:grid-cols-4 gap-4 mb-8">
          <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
            <span className="text-xs text-slate-400 font-medium">Registered Devices</span>
            <div className="text-3xl font-extrabold mt-1 text-white">{devices.length}</div>
          </div>
          <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
            <span className="text-xs text-slate-400 font-medium">Online Status</span>
            <div className="text-3xl font-extrabold mt-1 text-emerald-400">{onlineCount} / {devices.length}</div>
          </div>
          <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
            <span className="text-xs text-slate-400 font-medium">Average Battery</span>
            <div className="text-3xl font-extrabold mt-1 text-cyan-400">{avgBattery}%</div>
          </div>
          <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
            <span className="text-xs text-slate-400 font-medium">Security State</span>
            <div className="text-3xl font-extrabold mt-1 text-indigo-400">Isolated</div>
          </div>
        </section>

        {/* Devices Section */}
        <section className="mb-12">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-bold">Authorized Android Devices</h2>
            <span className="text-xs text-emerald-400 flex items-center">
              <span className="w-2 h-2 rounded-full bg-emerald-400 mr-2"></span>
              Live Synchronization Connected
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {devices.map(device => (
              <div 
                key={device.deviceId} 
                className="p-6 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition"
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center space-x-3">
                    <div className="w-12 h-12 rounded-xl bg-slate-800 flex items-center justify-center text-cyan-400">
                      <Smartphone className="w-6 h-6" />
                    </div>
                    <div>
                      <h3 className="font-bold text-lg">{device.deviceName}</h3>
                      <p className="text-xs text-slate-400">{device.manufacturer} {device.model} • Android {device.androidVersion}</p>
                    </div>
                  </div>
                  <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                    {device.status}
                  </span>
                </div>

                {/* Telemetry Metrics */}
                <div className="grid grid-cols-3 gap-3 my-5 p-3 rounded-xl bg-slate-950/60 border border-slate-800/80">
                  <div>
                    <span className="text-xs text-slate-400 flex items-center">
                      <Battery className="w-3.5 h-3.5 mr-1 text-emerald-400" /> Battery
                    </span>
                    <p className="text-sm font-semibold mt-0.5">{device.battery.level}% {device.battery.isCharging ? '⚡' : ''}</p>
                  </div>
                  <div>
                    <span className="text-xs text-slate-400 flex items-center">
                      <Wifi className="w-3.5 h-3.5 mr-1 text-cyan-400" /> Network
                    </span>
                    <p className="text-sm font-semibold mt-0.5">{device.network.type}</p>
                  </div>
                  <div>
                    <span className="text-xs text-slate-400">Storage</span>
                    <p className="text-sm font-semibold mt-0.5">{device.storage.usedPercentage}% used</p>
                  </div>
                </div>

                <div className="flex items-center justify-between text-xs text-slate-400 mb-4">
                  <span>Last synced: {new Date(device.lastSyncTimestamp).toLocaleTimeString()}</span>
                  <span>ID: {device.deviceId.slice(0, 14)}...</span>
                </div>

                <div className="flex items-center space-x-3">
                  <button 
                    onClick={() => handlePingDevice(device)}
                    className="flex-1 py-2 px-3 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold flex items-center justify-center space-x-1.5 transition"
                  >
                    <Bell className="w-3.5 h-3.5" />
                    <span>Ping / Ring</span>
                  </button>
                  <button 
                    onClick={() => setSelectedDevice(device)}
                    className="flex-1 py-2 px-3 rounded-lg bg-cyan-600 hover:bg-cyan-500 text-white text-xs font-semibold flex items-center justify-center space-x-1.5 transition"
                  >
                    <span>View Telemetry</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Modal for Device Detail */}
        {selectedDevice && (
          <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <div className="w-full max-w-xl bg-slate-900 border border-slate-800 rounded-2xl p-6 overflow-y-auto max-h-[90vh]">
              <div className="flex items-center justify-between pb-4 border-b border-slate-800">
                <div>
                  <h3 className="text-lg font-bold">{selectedDevice.deviceName}</h3>
                  <p className="text-xs text-slate-400">Hardware ID: {selectedDevice.deviceId}</p>
                </div>
                <button 
                  onClick={() => setSelectedDevice(null)}
                  className="text-slate-400 hover:text-white text-xl"
                >
                  &times;
                </button>
              </div>

              <div className="space-y-4 my-5 text-sm">
                <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
                  <div className="flex justify-between text-xs">
                    <span className="text-slate-400">Battery Status:</span>
                    <span className="text-emerald-400 font-bold">{selectedDevice.battery.level}% ({selectedDevice.battery.health})</span>
                  </div>
                  <div className="flex justify-between text-xs">
                    <span className="text-slate-400">Temperature / Voltage:</span>
                    <span className="text-slate-200">{selectedDevice.battery.temperatureCelsius}°C • {selectedDevice.battery.voltageMv} mV</span>
                  </div>
                  <div className="flex justify-between text-xs">
                    <span className="text-slate-400">IP Address & SSID:</span>
                    <span className="text-slate-200">{selectedDevice.network.ipAddress} ({selectedDevice.network.ssid || selectedDevice.network.type})</span>
                  </div>
                </div>

                {selectedDevice.isLocationSharingEnabled && selectedDevice.lastLocation && (
                  <div className="p-4 rounded-xl bg-slate-950 border border-slate-800">
                    <span className="text-xs text-slate-400 font-semibold block mb-2">Authorized GPS Coordinates:</span>
                    <p className="font-mono text-cyan-400 text-xs">Lat: {selectedDevice.lastLocation.latitude}, Lon: {selectedDevice.lastLocation.longitude}</p>
                    <p className="text-xs text-slate-400 mt-1">Accuracy: ±{selectedDevice.lastLocation.accuracyMeters}m</p>
                  </div>
                )}
              </div>

              <div className="flex justify-end space-x-3 pt-4 border-t border-slate-800">
                <button 
                  onClick={() => setSelectedDevice(null)}
                  className="px-4 py-2 rounded-lg bg-slate-800 text-slate-300 text-xs font-semibold hover:bg-slate-700"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};
