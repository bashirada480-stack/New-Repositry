export type DeviceStatus = 'ONLINE' | 'IDLE' | 'OFFLINE';

export type EventSeverity = 'INFO' | 'SUCCESS' | 'WARNING' | 'ALERT';

export interface BatteryInfo {
  level: number;
  isCharging: boolean;
  chargingType: string;
  temperatureCelsius: number;
  health: string;
  voltageMv: number;
}

export interface NetworkInfo {
  isConnected: boolean;
  type: string;
  ipAddress: string;
  ssid?: string;
  isMetered: boolean;
  linkDownstreamBandwidthKbps: number;
}

export interface StorageInfo {
  totalBytes: number;
  availableBytes: number;
  usedBytes: number;
  usedPercentage: number;
}

export interface PermissionsState {
  locationFine: boolean;
  locationCoarse: boolean;
  notifications: boolean;
  usageStats: boolean;
}

export interface LocationRecord {
  latitude: number;
  longitude: number;
  accuracyMeters: number;
  timestamp: number;
}

export interface AppUsageRecord {
  packageName: string;
  appName: string;
  totalTimeInForegroundMillis: number;
  lastTimeUsed: number;
}

export interface DeviceTelemetry {
  deviceId: string;
  userId: string;
  deviceName: string;
  model: string;
  manufacturer: string;
  androidVersion: string;
  apiLevel: number;
  appVersion: string;
  status: DeviceStatus;
  battery: BatteryInfo;
  network: NetworkInfo;
  storage: StorageInfo;
  permissions: PermissionsState;
  isLocationSharingEnabled: boolean;
  lastLocation?: LocationRecord;
  isAppUsageSharingEnabled: boolean;
  topAppUsage: AppUsageRecord[];
  isScreenSharingActive: boolean;
  lastSyncTimestamp: number;
  registeredAt: number;
}

export interface DeviceEvent {
  id: string;
  deviceId: string;
  deviceName: string;
  type: string;
  title: string;
  description: string;
  timestamp: number;
  severity: EventSeverity;
}
