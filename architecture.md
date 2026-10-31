# ElderCare Guardian — System Architecture
**Team:** Pranay · Ariyan · Reman Dey  
**Version:** 1.0 | **Date:** June 2026

> Current direction: elderly safety and health monitoring wearable. Older smart-suit, fabric, and energy-harvesting sections are retained as background only; the prototype scope below is the current implementation direction.
>
> **Showcase scope (June 2026):** Layers 1–2 describe the full research product (TEG, solar, 3× IMU suit). The prototype builds Layers 3–5 only: ESP32-C3 + MAX30102 + MPU-6050 → BLE GATT → Android app → caregiver dashboard. Energy harvesting, multi-IMU suit integration, and AD8232 ECG are deferred.

---

## Architecture at a Glance

```
┌──────────────────────────────────────────────────────────────────────┐
│  LAYER 5 — SAMSUNG HEALTH PLATFORM                                   │
│  Samsung Health app · Health history · Third-party integrations      │
└───────────────────────────────┬──────────────────────────────────────┘
                                │ Samsung Health Data SDK (write consent)
┌───────────────────────────────▼──────────────────────────────────────┐
│  LAYER 4 — ANDROID APPLICATION                                       │
│  BLE GATT client · Samsung SDK wrapper · ML inference · Dashboard UI │
└───────────────────────────────┬──────────────────────────────────────┘
                                │ BLE 5.x GATT notifications (~1.6 s cycle)
┌───────────────────────────────▼──────────────────────────────────────┐
│  LAYER 3 — FIRMWARE (MCU + BLE radio)                                │
│  nRF5340 / ESP32-C3 · GATT server · I²C/SPI bus · Burst-sleep loop  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │ I²C / SPI / Analog
┌───────────────────────────────▼──────────────────────────────────────┐
│  LAYER 2 — SENSOR SUITE                                              │
│  ECG · SpO2 · 3× IMU · Temp · Humidity                              │
└───────────────────────────────┬──────────────────────────────────────┘
                                │ DC regulated
┌───────────────────────────────▼──────────────────────────────────────┐
│  LAYER 1 — POWER SUBSYSTEM                                           │
│  TEG (body heat) · Solar panels · Piezo shoe · Boost converter       │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Layer 1 — Power Subsystem

### Energy sources

| Source | Component | Output | Placement | Role |
|--------|-----------|--------|-----------|------|
| Body heat (TEG) | TEC1-12706 | ~30–100 mV raw | Upper back (skin contact) | Primary |
| Sunlight | DFRobot FIT0333 | 1.5 V / 250 mA | Left + right shoulder | Supplementary |
| Footsteps | Piezo membrane | Pulse AC | Shoe sole (heel-arch) | Supplementary |

### Power management circuit

```
TEG (30–100 mV)  ─┐
Solar (1.5 V)    ─┼──► Boost converter (TPS61220 / MAX17222) ──► Supercapacitor (470 µF)
Piezo (AC pulse) ─┘         ↑                                        │
                        Step up to 3.3–4 V                           │
                                                              Hysteretic switch
                                                                      │
                                                            ▼ fire when Vcap > 3.3 V
                                                        MCU + Sensors
                                                            ▼ hibernate when Vcap < 3.0 V
```

**Burst-and-sleep strategy (from Nature 2023 paper):**  
The system does not run continuously. The hysteretic comparator wakes the MCU only when the supercapacitor charges above 3.3 V, allows one sensor read + BLE notify cycle (~1.6 s), then hibernates. This is what enables battery-free operation.

### Backup
100 mAh flat LiPo cell connected to power rail — prevents cold-start failure if TEG hasn't charged the cap yet. Charges from solar when available.

---

## Layer 2 — Sensor Suite

### Sensor map on the suit

```
                 [Solar L] ──── [nRF5340 MCU + BLE] ──── [Solar R]
                                        │
                  ┌─────────────────────┼─────────────────────┐
                  │                     │                      │
          [IMU L elbow]          [IMU Lumbar]           [IMU R elbow]
          MPU-6050/ICM          (Core / posture)         MPU-6050/ICM

         Upper back (centre): [ECG pads] + [TEG patch]
         Chest lining:        [ECG dry-contact electrodes — AD8232]
         Wrist cuff:          [MAX30102 — SpO2 + pulse]
         Shirt fabric:        [SHT40 — humidity / sweat]
         Waistband:           [TMP117 — skin temperature]
         Shoe sole:           [Piezo membrane]
```

### Sensor detail

| Sensor | Chip | Interface | Bus address | Sample rate | Data output |
|--------|------|-----------|-------------|-------------|-------------|
| ECG | AD8232 | Analog → MCU ADC | — | 256 Hz (configurable) | 12-bit raw, µV |
| SpO2 + HR | MAX30102 | I²C | 0x57 (fixed) | 100 Hz (PPG) | SpO2 %, HR bpm |
| IMU (elbow L) | MPU-6050 | I²C via TCA9548A ch0 | 0x68 | 200 Hz | Accel (m/s²), Gyro (°/s) |
| IMU (elbow R) | MPU-6050 | I²C via TCA9548A ch1 | 0x68 | 200 Hz | Accel, Gyro |
| IMU (lumbar) | MPU-6050 | I²C via TCA9548A ch2 | 0x68 | 200 Hz | Accel, Gyro |
| Skin temp | TMP117 | I²C | 0x48 | 1 Hz | °C, ±0.1°C |
| Humidity/sweat | SHT40 | I²C | 0x44 | 1 Hz | %RH, °C |

**I²C multiplexer note:** Three MPU-6050 modules share the same address (0x68). Use a TCA9548A multiplexer to give each its own I²C channel. Single I²C bus at 400 kHz (fast mode).

---

## Layer 3 — Firmware (MCU)

### MCU selection

| Option | BLE | Sleep current | RAM | Why |
|--------|-----|---------------|-----|-----|
| ESP32-C3 | BLE 5.0 | ~5 µA | 400 KB | Prototype — cheap, available in India |
| nRF5340 | BLE 5.3 | ~1 µA | 512 KB | Final — matches Nature 2023, better power |

Use ESP32-C3 (Arduino framework + ESP-IDF) for Phase 2 sensor testing, switch to nRF5340 (Zephyr RTOS) for the final suit.

### Firmware architecture

```
┌────────────────────────────────────────────────────┐
│  RTOS tasks (Zephyr / FreeRTOS)                    │
│                                                    │
│  power_mgr_task          sensor_task               │
│  - monitor Vcap          - read I²C bus            │
│  - fire wake interrupt   - read ADC (ECG)          │
│  - schedule hibernate    - buffer sensor frames    │
│                                                    │
│  ble_task                ml_preprocess_task        │
│  - GATT server           - window ECG (256 samples)│
│  - notify characteristics- compute PPG features   │
│  - handle connect/disc   - flag anomalies (simple) │
└────────────────────────────────────────────────────┘
```

### GATT server profile

Standard SIG services (Samsung Health recognises automatically via Accessory SDK):

```
0x1800  Generic Access
  0x2A00  Device Name = "ElderCare_v1"

0x180F  Battery Service
  0x2A19  Battery Level [0–100%]     ← derived from supercap voltage

0x180D  Heart Rate Service
  0x2A37  HR Measurement             ← from MAX30102

0x1822  PLX Service (SpO2)
  0x2A5F  PLX Continuous Measurement ← from MAX30102

0x1810  Blood Pressure Service
  0x2A35  BP Measurement             ← PPG-estimated (flagged ESTIMATED in flags byte)

0x1809  Health Thermometer
  0x2A1C  Temperature Measurement   ← from TMP117
```

Custom vendor service (UUID: `12345678-1234-5678-1234-567812345678`):

```
  ECG_RAW         notify  float32[256]   raw ECG window, 256 Hz
  IMU_WRIST       notify  float32[6]     [ax, ay, az, gx, gy, gz]
  SOS_STATE       notify  uint8          0=off 1=active
  FALL_RISK       notify  float32        0.0–1.0 firmware risk score
  HUMIDITY        notify  float32[2]     [%RH, temp_C]
  RESP_RATE       notify  float32        breaths/min (ECG-derived)
  DEVICE_STATE    notify  uint8          0=Normal 1=Check 2=Urgent
```

---

## Layer 4 — Android Application

### Module breakdown

```
app/src/main/java/com/eldercareguardian/
├── ble/              # BLE scan, GATT client, frame parser, simulator
├── consent/          # DpdpaConsentScreen, ConsentPreferences (DPDPA)   ← ADD
├── data/             # Domain models, SensorFrameMerger, DataExporter, DataDeleter
├── database/         # Room + SQLCipher, migrations 1→2→3
├── ml/               # Rule engines: ECG, IMU, vitals, alert triage + TFLite scaffold
├── notifications/    # FCM token, alert dispatcher, NotificationHelper
├── permissions/      # SmartSuitPermissions (core/enhanced two-tier)    ← ADD
├── samsung/          # Samsung Health reflection bridge
├── service/          # ElderCareMonitorService (foreground BLE)          ← ADD
├── settings/         # CaregiverPreferences, ActivePatientPreferences,
│                     # DataRetentionPreferences                           ← ADD
└── ui/               # Jetpack Compose screens + components + ViewModel
```

> **Prototype vs. full-suit scope:** The architecture above describes the full three-IMU suit.
> The current prototype implements a single wrist IMU (`IMU_WRIST`, `float32[6]`) over BLE.
> The TCA9548A multiplexer and elbow/lumbar IMUs are deferred to the production PCB.

### Samsung Health Data SDK integration

```kotlin
// 1. Samsung Health Data SDK is a local AAR from Samsung Developer Portal.
implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("samsung-health-data-api*.aar"))))

// 2. Get the current Data SDK store.
val healthDataStore = HealthDataService.getStore(applicationContext)

// 3. Request permissions (user must grant in Samsung Health app)
val permissions = setOf(
    Permission.of(DataTypes.HEART_RATE, AccessType.WRITE),
    Permission.of(DataTypes.BLOOD_OXYGEN, AccessType.WRITE),
    Permission.of(DataTypes.BLOOD_PRESSURE, AccessType.WRITE),
    Permission.of(DataTypes.BODY_TEMPERATURE, AccessType.WRITE),
)

// 4. Write data sample
val dataPoint = HealthDataPoint.builder()
    .setStartTime(startTime)
    .setEndTime(endTime)
    .setDeviceId(registeredSmartSuitDeviceId)
    .addFieldData(DataType.HeartRateType.HEART_RATE, latestHR)
    .build()

val request = DataTypes.HEART_RATE.insertDataRequestBuilder
    .addData(dataPoint)
    .build()

healthDataStore.insertData(request)
```

### Samsung Health Accessory SDK — BLE device registration
The Accessory SDK defines GATT compatibility guidelines. Your suit firmware must implement the exact service/characteristic structure from the spec. Once a user pairs the suit in Samsung Health → Settings → Accessories, Samsung Health will:
- Show the suit in the connected device list
- Auto-read standard GATT services (HR, SpO2, Temp, BP)
- Apply the "Works With Samsung Health" mark to the device

Custom GATT services (IMU, humidity, ECG raw) must be consumed by your app separately — Samsung Health doesn't know about those.

---

## Layer 5 — ML Models

### Model 1 — ECG Anomaly Detection

```
Input:  256 ECG samples @ 256 Hz (1-second window) — float32[256]
Model:  1D-CNN
        Conv1D(32, 5) → ReLU → MaxPool(2)
        Conv1D(64, 5) → ReLU → MaxPool(2)
        Conv1D(128, 3) → ReLU → GlobalAvgPool
        Dense(64) → ReLU → Dense(4) → Softmax

Output: [P_Normal, P_AFib, P_Tachycardia, P_Bradycardia]
Alert if max class != Normal AND confidence > 0.85

Training data: PTB-XL + MIT-BIH Arrhythmia datasets
Target metric: Macro F1 > 0.85
TFLite size: ~150 KB
```

### Model 2 — Fall And Inactivity Detection

```
Input:  IMU window (2 seconds x 100-200 Hz x 6 axes)
        float32 window from wrist or clip wearable

Prototype rule engine:
        high acceleration spike + orientation change + low movement after impact

Optional ML model:
        CNN/LSTM -> Normal / PossibleFall / ConfirmedFall

Training data: public fall datasets + staged non-harmful motion tests
```

### Model 3 — Dehydration Risk

```
Input features (tabular, computed per minute):
  - sweat_rate     : delta %RH over 60s (SHT40)
  - skin_temp      : °C (TMP117)
  - heart_rate     : bpm (MAX30102)
  - activity_level : IMU magnitude mean

Model: Random Forest (50 trees) → export via sklearn → convert to TFLite FlatBuffer
Output: [P_Low, P_Medium, P_High]
Alert when P_High > 0.7
```

### Model 4 — Overexertion / Fatigue Detection

```
Input features (per 30s window):
  - hr_reserve_pct : (HR - HR_rest) / (HR_max - HR_rest) × 100
  - spo2           : % (drop from baseline)
  - resp_rate      : breaths/min (derived from ECG)
  - imu_intensity  : RMS of all 3 IMU magnitudes

Model: XGBoost (gradient boosted trees) → ONNX → TFLite
Output: [Safe, Caution, Stop]
If Stop for 3 consecutive windows → push urgent alert + vibration
```

### Model 5 — Blood Pressure Estimation (PPG-based)

```
Input:  MAX30102 PPG waveform features (per heartbeat):
  - PTT (pulse transit time, estimated from ECG-PPG delay)
  - PWV (pulse wave velocity proxy)
  - PIR (perfusion index ratio)
  - HR

Model: CNN feature extractor + regression head
Output: [SBP_mmHg, DBP_mmHg]
Note: Labelled ESTIMATED in Samsung Health write call (flags byte)
Accuracy target: MAE < 10 mmHg (comparable to wrist BP monitors)

Calibration: user enters one reference BP reading at app setup
             model shifts output to personalise predictions
```

### On-device inference pipeline

```
BLE notification arrives (~900 ms → 1.6 s cycle)
        │
        ▼
SensorFrame merged in SensorFrameMerger (BLE wins over simulator on overlap)
        │
        ├──► ECG samples (256 floats)     ──► HeartRateExtractor ──► EcgAnomalyDetector
        │                                     (RR intervals, RMSSD, anomaly class)
        │
        ├──► IMU_WRIST frame (6 floats)   ──► FallDetectionEngine ──► FallConfirmationBuffer
        │                                 ──► InactivityMonitor
        │
        ├──► Tabular per-frame            ──► DehydrationRiskModel
        │    (sweat rate, skinTemp, HR)   ──► OverexertionModel
        │                                 ──► VitalsRiskMonitor
        │
        ├──► HR + skinTemp                ──► BloodPressureEstimator
        │
        └──► All above outputs            ──► CaregiverAlertPolicy (triage → Normal/Check/Warning/Emergency)
                │
                ▼
        AlertHistoryTracker.onFrame() → AlertEvent on level transition → Room + notification
                │
                ▼
        StateFlow<SensorFrame> → Compose recompose (Vitals / Safety / Caregiver / Readiness tabs)
                │
                ▼
        SamsungHealthBridge.writeVitals() every 5 s (HR, SpO2, BP, Temp)
```

---

## Data Flow Summary

```
[Body heat + movement + sunlight]
        │ energy
        ▼
[TEG + Piezo + Solar] ──► [Boost converter] ──► [Supercap 470µF]
                                                        │ 3.3 V burst
                                                        ▼
[AD8232 ECG] ──────────────────────────────────► [nRF5340 / ESP32-C3]
[MAX30102 SpO2]  ──── I²C ─────────────────────► [     MCU      ]
[MPU-6050 × 3]  ──── I²C (via TCA9548A) ──────► [     GATT     ]
[TMP117 Temp]   ──── I²C ─────────────────────► [    server    ]
[SHT40 Humidity] ─── I²C ─────────────────────►        │
                                                  BLE 5.x
                                                        │
                                                        ▼
                                              [Android Phone]
                                           ┌──────────────────┐
                                           │ BLEManager       │
                                           │ MLEngine         │──► Alerts + UI
                                           │ Samsung Health   │──► Samsung Health app
                                           │ Dashboard UI     │
                                           └──────────────────┘
```

---

## Security & Privacy

Samsung Health Data SDK requires explicit user consent before your app can read or write any health data. The consent dialog is shown by Samsung Health itself — you cannot bypass it.

BLE pairing uses BLE Secure Simple Pairing (Numeric Comparison or Just Works depending on both device capabilities). For the prototype, Just Works is fine. Production would need Passkey Entry.

ECG and health data stored on-device only (SharedPreferences + Room DB). Nothing sent to a server for the prototype.

---

## Component Cost Summary

| Layer | Components | Approx cost (INR) |
|-------|------------|-------------------|
| Power | TEG × 2, solar × 2, boost IC, supercap, LiPo | 3,950 |
| MCU | nRF5340 dev board (or ESP32-C3 for proto) | 1,800 |
| Sensors | ECG, SpO2, IMU × 3, temp, humidity | 2,500 |
| Interconnect | Flex PCB, conductive thread, TCA9548A mux, JST connectors | 800 |
| Suit base | Compression shirt + shorts | 1,500 |
| Shoe insert | Piezo membrane | 400 |
| Misc | Solder, tape, enclosure pouch | 450 |
| **Total** | | **~11,400** |

Under the ₹12,000 prototype budget from the PDF.

---

## References

1. Khan et al., "Flexible TEG with radiative cooling for BLE sensing," ScienceDirect, 2025. DOI: S2214993725004087
2. Yin et al., "Flexible TEG and energy management powered by body heat," Nature/PMC, August 2023. PMC10030726
3. Samsung Health Accessory SDK — developer.samsung.com/health/accessory
4. Samsung Health Data SDK — developer.samsung.com/health/data
5. nRF5340 product page — nordicsemi.com/Products/nRF5340
6. PTB-XL ECG dataset — physionet.org/content/ptb-xl
7. MIT-BIH Arrhythmia dataset — physionet.org/content/mitdb
