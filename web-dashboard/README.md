# MyDevice Control Center - Architecture & Deployment Guide

This repository contains the complete solution for **MyDevice Control Center**, consisting of:
1. **Android Companion App (Kotlin & Jetpack Compose)**: Running on Android devices to collect battery, network, storage, permissions, location, and telemetry with full owner transparency.
2. **Web Dashboard (React, TypeScript & Tailwind CSS)**: Running in the browser to view registered devices, battery status, network state, and audit logs.
3. **Database Architecture & Zero-Trust Rules**: Cloud Firestore schemas and isolated security rules (`firestore.rules`).

---

## 1. System Architecture

```
+-----------------------------------------------------------+
|                     WEB DASHBOARD                         |
|   (React 18 + TypeScript + Tailwind CSS + Lucide Icons)   |
+-----------------------------+-----------------------------+
                              |
                              | Real-time Firestore Listeners
                              v
+-----------------------------------------------------------+
|                  GOOGLE CLOUD FIRESTORE                   |
|                  Zero-Trust Security Rules                |
|  - users/{userId}                                         |
|  - users/{userId}/devices/{deviceId}                      |
|  - users/{userId}/devices/{deviceId}/events               |
+-----------------------------+-----------------------------+
                              ^
                              | Background & Live Sync
                              | (HTTPS / Firestore SDK)
+-----------------------------+-----------------------------+
|                 ANDROID COMPANION APP                     |
|  - BatteryManager (Level, Temp, Health, Charging Type)    |
|  - ConnectivityManager (Wi-Fi, Cellular, IP, Bandwidth)   |
|  - StatFs (Internal Storage Usage)                        |
|  - Permissions Audit (Location, Notifications, Usage)     |
|  - MediaProjection (User-Initiated Screen Sharing)        |
|  - Vibrator & Tone API (Attention Beacon / Ping)          |
+-----------------------------------------------------------+
```

---

## 2. Running the Android Companion App

- The Android application is compiled and runs directly in Android Studio or the AI Studio emulator.
- Switch between **Dashboard Mode** (fleet view) and **Agent Mode** (on-device controls) using the bottom navigation bar.
- To test the Attention Ping feature, tap **Ping** on any device card to trigger physical device vibration and alert tone.

---

## 3. Running the React Web Dashboard

```bash
cd web-dashboard
npm install
npm run dev
```

### Deploying the Web Dashboard to Firebase Hosting:

```bash
npm run build
firebase deploy --only hosting,firestore:rules
```

---

## 4. Zero-Trust Security Guarantees

- **No Covert Access**: Monitoring is strictly opt-in and transparent to the device owner.
- **User Isolation**: Firestore rules enforce `request.auth.uid == userId` across all device and event collections.
- **No Private Data Harvesting**: The app never inspects passwords, keystrokes, personal messages, or credentials.
