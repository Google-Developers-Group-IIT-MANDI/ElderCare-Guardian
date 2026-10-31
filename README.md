# ElderCare Guardian
> Elderly safety and health monitoring wearable — vitals, fall detection, SOS, and caregiver alerts.

**Team:** Pranay · Ariyan · Reman Dey  
**Institution:** IIT Mandi  
**Version:** 1.0.0-beta — Pre-pilot (June 2026)
**Date:** June 2026

---

## What is this?

ElderCare Guardian is a compact wrist/clip wearable for elderly people living alone or needing light daily monitoring. It streams vitals and motion data to a phone, detects possible falls or distress, and can alert caregivers.

The revised prototype intentionally avoids smart fabric and suit embedding. That makes the build simpler, faster, and more impactful for a showcase: one wearable device, one Android app, one caregiver-focused safety story.

The existing BLE, Samsung Health, simulator, GATT parsing, and Android dashboard work still applies. The product focus changes from fitness intelligence to elderly safety, health trends, and caregiver peace of mind.

---

## Features

**Biometrics monitored** *(showcase — real)*
- SpO2 + heart rate (MAX30102) ✅
- IMU 6-axis motion (MPU-6050) ✅
- Battery voltage (ESP32-C3 ADC) ✅

**Biometrics monitored** *(simulator-backed — real hardware deferred)*
- ECG waveform (AD8232) — simulator generates 256-sample QRS window ✅
- Skin temperature (TMP117) — simulator generates skinTempC ✅
- Sweat / humidity (SHT40) — simulator generates humidityPercent + sweatRate ✅

**Elder safety**
- Fall detection — IMU-based impact + posture change ✅
- SOS trigger — app and device-side emergency event ✅
- Inactivity monitoring — no movement for unusual duration ✅
- Caregiver alert state — Normal / Check / Warning / Emergency ✅

**Health alerts**
- ECG anomaly detection: Normal / AFib / Tachycardia / Bradycardia ✅
- Dehydration risk: Low / Medium / High ✅
- Fall risk: Low / Medium / High ✅
- Abnormal vitals: Safe / Monitor / Alert ✅

**Platform**
- Samsung Health integration — HR, SpO2, Temp written to Health history (BP removed)
- "Works With Samsung Health" BLE Accessory SDK compliance
- Caregiver dashboard — live status, alert level, BLE readiness, and health trend ✅
- DPDPA consent flow — mandatory first-launch consent under India's Digital Personal Data Protection Act 2023 ✅
- Foreground Service for 24/7 background BLE monitoring ✅
- BLE auto-reconnect with exponential backoff ✅
- Room database with SQLCipher encryption ✅

---

## Prototype

- ESP32-C3 BLE wearable (nRF5340 deferred to production).
- MAX30102 for heart rate and SpO2.
- MPU-6050 IMU for fall, inactivity, and posture state.
- Optional TMP117 for temperature.
- Android app with simulator mode, BLE mode, caregiver alert flow, and Samsung Health path.
- Rechargeable battery or USB power for showcase reliability.

---

## Hardware

### MCU + BLE

| Stage | Chip | Why | Status |
|-------|------|-----|--------|
| Prototype | ESP32-C3 | Cheap, widely available in India, BLE 5.0 | ✅ Firmware complete |
| Final wearable | nRF5340 | BLE 5.3, sleeps at ~1 µA, better for always-on monitoring | ⏳ Deferred |

### Sensor suite

| Parameter | Chip | Interface | Placement |
|-----------|------|-----------|-----------|
| SpO2 + HR | MAX30102 | I²C (0x57) | Wrist/clip skin contact |
| IMU | MPU-6050 / ICM-42688 | I²C | Wrist/clip enclosure |
| Skin temp | TMP117 | I²C (0x48) | Optional skin contact |
| SOS input | Button / capacitive touch | GPIO | Wearable body |
| ECG | AD8232 | Analog ADC | Later clinical extension |

---

## System Architecture

```
MAX30102 + IMU + SOS button + optional TMP117
        │ I²C / GPIO
        ▼
ESP32-C3 / nRF5340 wearable
        │ BLE 5.x GATT
        ▼
Android phone app
 ├── live vitals
 ├── fall/SOS/inactivity alerts
 ├── caregiver status
 └── Samsung Health history path
```

Full architecture → see [`architecture.md`](./architecture.md)  
Android development setup → see [`apps/android/README.md`](./apps/android/README.md)<br>
ML roadmap → see [`docs/ml-roadmap.md`](./docs/ml-roadmap.md)<br>
Deployment plan → see [`DEPLOYMENT_PLAN.md`](./DEPLOYMENT_PLAN.md)<br>
Android app scaffold → see [`apps/android`](./apps/android)  
Remaining work → see [`NEXT_STEPS.md`](./NEXT_STEPS.md)

---

## Software

### Android App — module structure

```
app/src/main/java/com/eldercareguardian/
├── ble/              # BLE scan, GATT client, frame parser
├── consent/          # DPDPA consent screen and DataStore
├── samsung/          # Samsung Health Data SDK wrapper
├── ml/               # Rule-based engines (ECG, IMU, vitals) + TFLite scaffold
├── database/         # Room + SQLCipher encrypted persistence
├── notifications/    # Caregiver alert dispatcher + FCM
├── service/          # Foreground service for background monitoring
├── data/             # Domain models, exporter, deleter
├── settings/         # DataStore preferences (patient, alert, consent)
└── ui/               # Jetpack Compose screens + components
```

### ML Models *(rule-based + TFLite models deployed)*

| Model | Status | Algorithm |
|-------|--------|-----------|
| Fall detection (CNN) | ✅ `fall_detection.tflite` (185 KB) | 1D-CNN Conv1D(32→64→128) → Dense |
| Fall detection (rule) | ✅ FallDetectionEngine + FallConfirmationBuffer | Temporal window, SisFall-calibrated |
| Health risk (unified MLP) | ✅ `health_risk.tflite` (14 KB) | Shared trunk + 3 output heads |
| Caregiver alert triage | ✅ CaregiverAlertPolicy | 4-level triage |
| Inactivity monitoring | ✅ InactivityMonitor | Motion gate |
| ECG anomaly | ✅ EcgAnomalyDetector + HeartRateExtractor | RMSSD + RR intervals |
| Dehydration risk | ✅ DehydrationRiskModel | Tabular rules |
| Vitals risk | ✅ VitalsRiskMonitor | Composite score (NEWS2) |
| Overexertion | ✅ OverexertionModel | HR reserve + SpO2 |
| BP estimation | ✅ Active — displayed with "est mmHg" label | Linear HR+temp model |

### Samsung Health Integration *(reflection bridge active, AAR optional)*

- **Data SDK:** `RealSamsungHealthBridge` uses reflection — always compiles. Transitions: `NeedsSdkAar` → `NeedsPartnerApproval` → `Ready`. No AAR required for build.
- **Accessory SDK:** GATT profile uses standard SIG services (HR 0x180D, PLX 0x1822) for auto-recognition.

Reference: https://developer.samsung.com/health

---

## BLE GATT Profile (wearable as peripheral)

```
Generic Access (0x1800)
  Device Name = "ElderCare_v1"

Battery Service (0x180F)
  Battery Level [0–100%] ← LiPo voltage derived via ADC

Heart Rate Service (0x180D)
  HR Measurement [0x2A37] ← MAX30102

PLX / SpO2 Service (0x1822)
  PLX Continuous Measurement [0x2A5F] ← MAX30102 (only when valid)

Health Thermometer (0x1809)
  Temperature Measurement [0x2A1C] ← TMP117 (optional)

Custom ElderCare Service (12345678-1234-5678-1234-567812345678)
  ECG_RAW       float32[256]    synthetic QRS-spike ECG window
  IMU_WRIST     float32[6]      ax,ay,az,gx,gy,gz (m/s², °/s)
  SOS_STATE     uint8           0=off 1=active
  FALL_RISK     float32         0.0–1.0 (firmware: SOS-based, Android: IMU-based)
  HUMIDITY      float32[2]      %RH, temp_C (synthetic)
  RESP_RATE     float32         breaths/min (synthetic)
  DEVICE_STATE  uint8           0=Normal 1=Check 2=Urgent
```

---

## Implementation Sessions

| Session | Scope | Status |
|---------|-------|--------|
| 1 | ViewModel race fix + SMS toggle | ✅ Done |
| 2 | SmartSuitApp.kt structural refactor (split into screens) | ✅ Done |
| 3 | FCM caregiver notification backend | ✅ Done |
| 4 | Firmware deep sleep + BLE scan mode fix | ✅ Done |
| 5 | Multi-patient profile system (Room + DataStore) | ✅ Done |
| 6 | Health history + DPDPA export/delete | ✅ Done |
| 7 | Samsung Health real AAR integration (reflection bridge) | ✅ Done |
| 8 | Release pipeline + signing (CI env vars) | ✅ Done |
| 9 | Privacy policy + Play Store listing | ✅ Done |
| 10 | SisFall validation harness + TFLite scaffold | ✅ Done |
| 11 | DPDPA consent screen + DataStore | ✅ Done |
| 12 | Multi-patient profile system (Room) | ✅ Done |
| 13 | Health history + data export/delete | ✅ Done |
| 14 | Samsung Health reflection bridge | ✅ Done |
| 15 | SpO2 quality + 3-reading debounce + 4-level alert | ✅ Done |
| 16 | Security hardening (SQLCipher, EncryptedSharedPrefs, permissions tier split) | ✅ Done |
| 17 | SisFall validation + CNN training + TFLite export | ✅ Done |
| 18 | Health risk MLP training + TFLite export | ✅ Done |
| 19 | MIT-BIH ECG validation + AFib threshold tuning | ✅ Done |
| 20 | Crash fixes: consent-guarded service, null-safe merger, AFib priority | ✅ Done |

---

## Prototype Cost

| Category | Cost (INR) |
|----------|------------|
| MCU + BLE | 600–1,800 |
| MAX30102 | 250–500 |
| IMU | 150–500 |
| Optional TMP117/temp | 300–600 |
| SOS button + enclosure | 300–800 |
| Battery / power | 300–700 |
| Misc | 500 |
| **Total** | **~2,400–5,400** |

Budget ceiling: ₹12,000 ✓

---

## Team

| Member | Responsibility |
|--------|----------------|
| **Pranay** | BLE firmware, Android app, caregiver dashboard UI |
| **Ariyan** | HR/SpO2 + IMU validation, PCB layout, wearable enclosure |
| **Reman Dey** | Prototype hardware assembly, power/battery benchmarking, enclosure reliability |

---

## References

1. Khan et al., "Flexible thermoelectric generator with radiative cooling for body-heat-driven self-powered BLE sensing system," Nano Energy / ScienceDirect, 2025. DOI: S2214993725004087
2. Yin et al., "Flexible thermoelectric generator and energy management electronics powered by body heat," Microsystems & Nanoengineering / Nature, August 2023. PMC10030726
3. Samsung Health Accessory SDK — https://developer.samsung.com/health/accessory
4. Samsung Health Data SDK — https://developer.samsung.com/health/data
5. PTB-XL ECG dataset — https://physionet.org/content/ptb-xl
6. MIT-BIH Arrhythmia dataset — https://physionet.org/content/mitdb

---

*ElderCare Guardian — IIT Mandi · June 2026*
