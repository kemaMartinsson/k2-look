# Product Requirement Document (PRD)  
**Project Name:** Karoo2 ↔ ActiveLook Gateway  
**Version:** 1.0  
**Date:** 2025-09-14  
**Owner:** TBD  

---

## 1. Overview
The purpose of this application is to act as a **real-time data gateway** between the **Hammerhead Karoo2 cycling computer** and **ActiveLook smart glasses**.  
The app will capture cycling metrics from the **Karoo2 SDK** and display them on the **ActiveLook heads-up display (HUD)** via the ActiveLook API.  

This enables cyclists to view critical ride data **without looking down** at the bike computer, improving **safety** and **performance monitoring**.  

---

## 2. Goals and Objectives
- Provide seamless **real-time streaming of Karoo2 ride metrics** to ActiveLook glasses.  
- Support **all metrics exposed by the Karoo2 SDK** (e.g., speed, cadence, heart rate, power, distance, navigation data, etc.).  
- Allow **customizable display layouts** on ActiveLook glasses (e.g., user chooses which metrics to see).  
- Ensure **low-latency (<500 ms)** data transfer to maintain situational awareness.  
- Provide a **reliable connection** over Bluetooth Low Energy (BLE).  
- Maintain **battery efficiency** on both Karoo2 and ActiveLook devices.  

---

## 3. Non-Goals
- The app will not replace or alter Karoo2’s main display interface.  
- No direct ride recording or logging will occur in the gateway app; recording remains on Karoo2.  
- No advanced data analytics beyond simple metric transformation and formatting.  

---

## 4. Stakeholders
- **Cyclists / End Users** – benefit from hands-free ride data visualization.  
- **Hammerhead (Karoo2)** – provides data source through Karoo Ext SDK.  
- **ActiveLook** – provides data rendering API for HUD.  
- **Developers / Maintainers** – responsible for building and updating gateway app.  

---

## 5. Functional Requirements

### 5.1 Data Integration
- The gateway app shall connect to **Karoo2 SDK** using the **Karoo Ext APIs**.  
- The app shall subscribe to **all available ride metrics**, including but not limited to:  
  - Speed (current, average, max)  
  - Distance (current trip, total)  
  - Time (ride time, elapsed time)  
  - Elevation (gain, loss, current altitude)  
  - Heart rate (current, average, max)  
  - Cadence (current, average, max)  
  - Power (instant, average, max, 3s, 10s, 30s)  
  - Navigation (turn-by-turn instructions, remaining distance, ETA)  
  - Battery levels (Karoo2 and paired sensors)  

### 5.2 Display Integration
- The app shall connect to **ActiveLook glasses** via BLE.  
- The app shall use the **ActiveLook API** to render metrics on the HUD.  
- The app shall support:  
  - **Customizable data fields** (user can choose up to 4 metrics to display simultaneously).  
  - **Multiple pages/screens** of data that can be cycled through (e.g., “Performance Page”, “Navigation Page”).  
  - **Basic navigation prompts** (arrows, distance to next turn).  
- Fonts, colors, and positions must follow ActiveLook API constraints.  

### 5.3 User Interaction
- Users configure which metrics to display via a simple mobile app UI (Android/iOS).  
- Presets should be available (e.g., “Training”, “Race”, “Navigation”).  
- Connection management (pair/unpair glasses, reconnect) must be intuitive.  

### 5.4 Performance
- Data latency must be **≤500 ms** from Karoo2 metric update to ActiveLook display.  
- App must maintain stable BLE connection during rides up to **10 hours**.  

---

## 6. Technical Requirements

### 6.1 Platforms
- **Karoo2:** Android-based, using [Karoo Ext SDK](https://github.com/hammerheadnav/karoo-ext).  
- **ActiveLook:** BLE API as per [ActiveLook API documentation](https://github.com/ActiveLook/Activelook-API-Documentation).  
- **Gateway App Host:** Mobile (Android/iOS) OR direct installable on Karoo2 (if SDK allows).  

### 6.2 Architecture
1. **Karoo2 SDK Client** – subscribes to metrics and navigation data.  
2. **Gateway Core** – processes and formats data.  
3. **ActiveLook Client** – sends formatted data to glasses via BLE.  
4. **UI Config Layer** – mobile app UI for user preferences.  

---

## 7. Dependencies
- Karoo Ext SDK (availability and API stability).  
- ActiveLook API BLE support.  
- Mobile device with BLE support (if gateway is not hosted directly on Karoo2).  

---

## 8. Risks & Mitigations
- **BLE connectivity drops** → Implement auto-reconnect logic.  
- **Battery drain on Karoo2** → Optimize data polling frequency.  
- **API changes (Karoo or ActiveLook)** → Version pinning and update testing.  

---

## 9. Success Metrics
- < 2% connection drop rate during 4+ hour rides.  
- < 500 ms latency from metric update to HUD.  
- User satisfaction > 80% in field tests.  

---

## 10. Future Enhancements
- Support for structured workout prompts on ActiveLook.  
- Voice feedback option (through paired headset).  
- Companion cloud sync for presets and layouts.  
