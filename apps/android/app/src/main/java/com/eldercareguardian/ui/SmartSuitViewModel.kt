package com.eldercareguardian.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.Manifest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eldercareguardian.MainActivity
import com.eldercareguardian.ble.BleConnectionState
import com.eldercareguardian.ble.DiscoveredBleDevice
import com.eldercareguardian.ble.JsonFileDataSource
import com.eldercareguardian.ble.SmartSuitBleDataSource
import com.eldercareguardian.ble.SmartSuitBleTelemetry
import com.eldercareguardian.data.AlertEvent
import com.eldercareguardian.data.CaregiverAlertStatus
import com.eldercareguardian.data.DataDeleter
import com.eldercareguardian.data.DataExporter
import com.eldercareguardian.data.HealthSnapshot
import com.eldercareguardian.data.Patient
import com.eldercareguardian.data.SensorFrame
import com.eldercareguardian.data.SensorFrameMerger
import com.eldercareguardian.database.ElderCareDatabase
import com.eldercareguardian.database.PatientEntity
import com.eldercareguardian.database.toAlertEvent
import com.eldercareguardian.database.toEntity
import com.eldercareguardian.database.toHealthSnapshot
import com.eldercareguardian.database.toPatient
import com.eldercareguardian.ml.AlertHistoryTracker
import com.eldercareguardian.ml.CaregiverAlertPolicy
import com.eldercareguardian.ml.FallDetectionEngine
import com.eldercareguardian.ml.FallDetectionTfliteModel
import com.eldercareguardian.notifications.CaregiverAlertDispatcher
import com.eldercareguardian.notifications.FcmAlertSender
import com.eldercareguardian.notifications.FcmTokenManager
import com.eldercareguardian.notifications.NotificationHelper
import com.eldercareguardian.samsung.SamsungHealthBridge
import com.eldercareguardian.samsung.SamsungHealthBridgeProvider
import com.eldercareguardian.samsung.SamsungHealthState
import com.eldercareguardian.service.ElderCareMonitorService
import com.eldercareguardian.settings.ActivePatientPreferences
import com.eldercareguardian.settings.CaregiverPreferences
import com.eldercareguardian.settings.DataRetentionPreferences
import com.eldercareguardian.settings.isValidPhone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class SmartSuitViewModel(application: Application) : AndroidViewModel(application) {
    private val bleDataSource = SmartSuitBleDataSource(application.applicationContext)

    private val tfliteModel = FallDetectionTfliteModel(application)

    fun loadJsonFile(uri: Uri) {
        viewModelScope.launch {
            val source = JsonFileDataSource(
                getApplication<Application>().contentResolver, uri
            )
            source.frames.collect { frame ->
                _bleFrameSource.value = frame
            }
        }
    }
    private val samsungBridge: SamsungHealthBridge = SamsungHealthBridgeProvider.create(application)
    private val db = ElderCareDatabase.getInstance(application.applicationContext)
    private val patientDao = db.patientDao()
    private val alertEventDao = db.alertEventDao()
    private val healthDataDao = db.healthDataDao()
    private val alertHistoryTracker = AlertHistoryTracker()
    private val prefs = CaregiverPreferences.getInstance(application)
    private val activePatientPrefs = ActivePatientPreferences.getInstance(application)
    private val consentPrefs = com.eldercareguardian.consent.ConsentPreferences.getInstance(application)
    private val retentionPrefs = DataRetentionPreferences.getInstance(application)

    private val _sosOverride = MutableStateFlow(false)
    val sosOverride: StateFlow<Boolean> = _sosOverride.asStateFlow()

    private val _acknowledgedEmergency = MutableStateFlow(false)
    val acknowledgedEmergency: StateFlow<Boolean> = _acknowledgedEmergency.asStateFlow()

    /** Back-compat alias for acknowledgedEmergency (renamed from acknowledgedUrgent). */
    val acknowledgedUrgent: StateFlow<Boolean> get() = _acknowledgedEmergency

    private val _smsEnabled = MutableStateFlow(false)
    val smsEnabled: StateFlow<Boolean> = _smsEnabled.asStateFlow()

    fun setSmsEnabled(enabled: Boolean) {
        _smsEnabled.value = enabled
    }

    private val _backendUrl = MutableStateFlow("")
    val backendUrl: StateFlow<String> = _backendUrl.asStateFlow()

    fun setBackendUrl(url: String) {
        _backendUrl.value = url.trim()
    }

    // ── Multi-patient profile management ──
    private val _patients = MutableStateFlow<List<Patient>>(emptyList())
    val patients: StateFlow<List<Patient>> = _patients.asStateFlow()

    private val _selectedPatientId = MutableStateFlow(ActivePatientPreferences.DEFAULT_PATIENT_ID)
    val selectedPatientId: StateFlow<Long> = _selectedPatientId.asStateFlow()

    val selectedPatient: StateFlow<Patient?> = combine(
        _patients,
        _selectedPatientId,
    ) { patients, selectedId ->
        patients.find { it.id == selectedId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = null,
    )

    private val _alertHistory = MutableStateFlow<List<AlertEvent>>(emptyList())
    val alertHistory: StateFlow<List<AlertEvent>> = _alertHistory.asStateFlow()

    private val _hrTrend = MutableStateFlow<List<Float>>(emptyList())
    val hrTrend: StateFlow<List<Float>> = _hrTrend.asStateFlow()

    private val _spo2Trend = MutableStateFlow<List<Float>>(emptyList())
    val spo2Trend: StateFlow<List<Float>> = _spo2Trend.asStateFlow()

    // Caregiver contact — collected from DataStore-backed flows. Initial value
    // is the DataStore default (empty phone, "Caregiver" name) so the UI has
    // something to render before the first read completes.
    val retentionDays: StateFlow<Int> = retentionPrefs.retentionDays.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = DataRetentionPreferences.DEFAULT_RETENTION_DAYS,
    )

    fun setRetentionDays(days: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            retentionPrefs.setRetentionDays(days)
        }
    }

    val caregiverPhoneNumber: StateFlow<String> = prefs.caregiverPhone.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = CaregiverPreferences.DEFAULT_PHONE,
    )
    val caregiverDisplayName: StateFlow<String> = prefs.caregiverName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = CaregiverPreferences.DEFAULT_NAME,
    )

    private val _bleFrameSource = MutableStateFlow<SensorFrame?>(null)

    val frames: StateFlow<SensorFrame?> = combine(
        _bleFrameSource,
        bleDataSource.telemetry,
        bleDataSource.connectionState,
        _sosOverride,
    ) { jsonFrame, bleTelemetry, connectionState, sosActive ->
        val frame = if (connectionState == BleConnectionState.Connected && bleTelemetry.heartRateBpm != null) {
            SensorFrameMerger.buildFromTelemetry(bleTelemetry, tfliteModel, selectedPatient.value?.ageYears ?: 70)
        } else {
            jsonFrame
        }
        if (frame != null && sosActive) {
            frame.copy(sosActive = true, caregiverAlert = CaregiverAlertStatus.Emergency)
        } else {
            frame
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = null,
    )

    private var _serviceStarted = false

    fun onUiVisible() {
        if (!_serviceStarted) {
            _serviceStarted = true
            val appCtx = getApplication<Application>()
            appCtx.startForegroundService(ElderCareMonitorService.startIntent(appCtx))
        }
    }

    init {
        // Observe patients from Room
        viewModelScope.launch {
            patientDao.getAll().collect { entities ->
                _patients.value = entities.map { it.toPatient() }
            }
        }

        // Load persisted selected patient ID
        viewModelScope.launch {
            activePatientPrefs.activePatientId.collect { id ->
                _selectedPatientId.value = id
            }
        }

        // Load alert history for the selected patient on startup and on patient switch.
        viewModelScope.launch {
            _selectedPatientId.collect { patientId ->
                val persisted = withContext(Dispatchers.IO) {
                    if (patientId == ActivePatientPreferences.DEFAULT_PATIENT_ID) {
                        alertEventDao.getRecent().first()
                    } else {
                        alertEventDao.getRecentForPatient(patientId).first()
                    }
                }.map { it.toAlertEvent() }
                _alertHistory.value = persisted
            }
        }

        // Watch live frames, update trends and append to alert history on transitions.
        viewModelScope.launch {
            frames.filterNotNull().collect { frame ->
                _hrTrend.value = (_hrTrend.value + frame.heartRateBpm.toFloat()).takeLast(60)
                _spo2Trend.value = (_spo2Trend.value + frame.spo2Percent).takeLast(60)

                val event = alertHistoryTracker.onFrame(frame)
                if (event != null) {
                    _alertHistory.value = alertHistoryTracker.prepend(_alertHistory.value, event)
                    // Keep foreground service notification in sync
                    ElderCareMonitorService.updateAlertStatus(getApplication(), event.level)
                    when (event.level) {
                        CaregiverAlertStatus.Emergency -> {
                            postEmergencyNotification(event)
                            dispatchCaregiverAlert(event, CaregiverAlertStatus.Emergency, _smsEnabled.value)
                            dispatchFcmAlert(event, CaregiverAlertStatus.Emergency)
                            _acknowledgedEmergency.value = false
                        }
                        CaregiverAlertStatus.Warning -> {
                            NotificationHelper.cancelEmergency(getApplication())
                            postWarningNotification(event)
                            dispatchCaregiverAlert(event, CaregiverAlertStatus.Warning, _smsEnabled.value)
                            dispatchFcmAlert(event, CaregiverAlertStatus.Warning)
                        }
                        else -> {
                            // Check or Normal — cancel any outstanding emergency/warning notifications
                            NotificationHelper.cancelEmergency(getApplication())
                            NotificationHelper.cancelWarning(getApplication())
                        }
                    }
                    val patientId = _selectedPatientId.value
                    val eventEntity = event.copy(patientId = patientId).toEntity()
                    viewModelScope.launch(Dispatchers.IO) {
                        val retentionMs = retentionPrefs.retentionDays.first() * 24L * 60 * 60 * 1000
                        alertEventDao.insert(eventEntity)
                        if (patientId == ActivePatientPreferences.DEFAULT_PATIENT_ID) {
                            alertEventDao.deleteOlderThan(System.currentTimeMillis() - retentionMs)
                        } else {
                            alertEventDao.deleteOlderThanForPatient(patientId, System.currentTimeMillis() - retentionMs)
                        }
                    }
                }
            }
        }

        // Health history recording — persist a snapshot every 60 s for the selected patient.
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                val frame = frames.value ?: continue
                val patientId = _selectedPatientId.value
                val snapshot = HealthSnapshot(
                    patientId = patientId,
                    timestampMillis = frame.timestampMillis,
                    heartRateBpm = frame.heartRateBpm,
                    spo2Percent = frame.spo2Percent,
                    respiratoryRate = frame.respiratoryRate,
                    skinTempC = frame.skinTempC,
                    posture = frame.posture.name,
                    fatigue = frame.fatigue.name,
                    fallRisk = frame.fallRisk.name,
                    caregiverAlert = frame.caregiverAlert.name,
                    batteryPercent = frame.batteryPercent,
                )
                withContext(Dispatchers.IO) {
                    val retentionMs = retentionPrefs.retentionDays.first() * 24L * 60 * 60 * 1000
                    healthDataDao.insert(snapshot.toEntity())
                    healthDataDao.deleteOlderThanForPatient(patientId, System.currentTimeMillis() - retentionMs)
                }
            }
        }

        // Samsung Health write cadence — fires every 5 s when bridge is connected.
        // NoOp until the real AAR is installed (isAvailable returns false); zero
        // performance cost when dormant.
        viewModelScope.launch {
            while (true) {
                delay(5_000L)
                val frame = frames.value ?: continue
                if (samsungBridge.isAvailable()) {
                    samsungBridge.writeVitals(frame)
                }
            }
        }
    }

    val bleConnectionState: StateFlow<BleConnectionState> = bleDataSource.connectionState
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = bleDataSource.discoveredDevices
    val bleTelemetry: StateFlow<SmartSuitBleTelemetry> = bleDataSource.telemetry
    val isLiveBleData: StateFlow<Boolean> = bleDataSource.isLiveData
    val samsungState: StateFlow<SamsungHealthState> = samsungBridge.state

    fun startBleScan() {
        bleDataSource.startScan()
    }

    fun stopBle() {
        bleDataSource.stop()
    }

    fun connectToFirstDiscoveredDevice() {
        val firstDevice = discoveredDevices.value.firstOrNull() ?: return
        bleDataSource.connect(firstDevice.address)
    }

    fun startSamsungBridge() {
        viewModelScope.launch(Dispatchers.IO) {
            samsungBridge.connect()
            samsungBridge.requestPermissions()
        }
    }

    fun triggerSosDemo() { _sosOverride.value = true }
    fun clearSosDemo() { _sosOverride.value = false }
    fun acknowledgeUrgent() { _acknowledgedEmergency.value = true }
    fun acknowledgeEmergency() { _acknowledgedEmergency.value = true }

    /**
     * Persist the caregiver contact. Both fields must be non-blank; the
     * phone must contain at least 7 digits. Returns true on success.
     */
    suspend fun updateCaregiverContact(name: String, phone: String): Boolean {
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        if (trimmedName.isBlank() || !isValidPhone(trimmedPhone)) return false
        withContext(Dispatchers.IO) {
            prefs.setBoth(trimmedName, trimmedPhone)
        }
        return true
    }

    // ── Patient profile CRUD ──

    fun selectPatient(patientId: Long) {
        CaregiverAlertPolicy.reset()
        FallDetectionEngine.reset()
        SensorFrameMerger.reset()
        viewModelScope.launch {
            activePatientPrefs.setActivePatientId(patientId)
        }
    }

    suspend fun addPatient(name: String, caregiverName: String, caregiverPhone: String, ageYears: Int): Long {
        val entity = PatientEntity(
            name = name.trim(),
            caregiverName = caregiverName.trim(),
            caregiverPhone = caregiverPhone.trim(),
            ageYears = ageYears,
        )
        return withContext(Dispatchers.IO) {
            patientDao.insert(entity)
        }
    }

    suspend fun updatePatient(patient: Patient) {
        withContext(Dispatchers.IO) {
            patientDao.update(patient.toEntity())
        }
    }

    suspend fun deletePatient(patient: Patient) {
        withContext(Dispatchers.IO) {
            patientDao.delete(patient.toEntity())
            if (_selectedPatientId.value == patient.id) {
                activePatientPrefs.setActivePatientId(ActivePatientPreferences.DEFAULT_PATIENT_ID)
            }
        }
    }

    // ── DPDPA Data export (suspend — call from UI coroutine scope) ──

    suspend fun exportData(context: Context): Intent {
        return try {
            val patients = withContext(Dispatchers.IO) { patientDao.getAllOnce() }
            val allHealth = withContext(Dispatchers.IO) { healthDataDao.getAll() }
            val allAlerts = withContext(Dispatchers.IO) { alertEventDao.getAll() }
            val file = withContext(Dispatchers.IO) {
                DataExporter.exportJson(context, patients, allHealth, allAlerts)
            }
            DataExporter.createShareIntent(context, file)
        } catch (e: IOException) {
            android.util.Log.e("SmartSuitViewModel", "Export failed: ${e.message}")
            // Return an empty intent — the UI must handle this gracefully
            Intent()
        }
    }

    suspend fun exportClinicalPdf(context: Context): Intent {
        return try {
            val patientId = _selectedPatientId.value
            val patient = withContext(Dispatchers.IO) {
                if (patientId != null) patientDao.getById(patientId) else null
            }
            val recentHealth = withContext(Dispatchers.IO) {
                if (patientId != null) healthDataDao.getRecentForPatient(patientId, 100)
                else healthDataDao.getAll().take(100)
            }
            val alerts = withContext(Dispatchers.IO) {
                alertEventDao.getAll().filter { patientId == null || it.patientId == patientId }
            }
            val file = withContext(Dispatchers.IO) {
                com.eldercareguardian.data.ClinicalReportPdfExporter.exportPdf(
                    context,
                    patient,
                    recentHealth,
                    alerts,
                )
            }
            com.eldercareguardian.data.ClinicalReportPdfExporter.createShareIntent(context, file)
        } catch (e: Exception) {
            android.util.Log.e("SmartSuitViewModel", "Clinical PDF export failed: ${e.message}", e)
            Intent()
        }
    }

    // ── DPDPA Right to erasure — wipe all local data ──

    fun deleteAllData() {
        val context = getApplication<Application>()
        context.stopService(ElderCareMonitorService.stopIntent(context))
        viewModelScope.launch(Dispatchers.IO) {
            DataDeleter.deleteAllData(context, alertEventDao, healthDataDao, patientDao, consentPrefs)
        }
    }

    fun buildCaregiverDialIntent(): Intent? {
        val phone = caregiverPhoneNumber.value
        if (!isValidPhone(phone)) return null
        return com.eldercareguardian.notifications.CaregiverContact.dialIntent(phone)
    }

    private fun postEmergencyNotification(event: AlertEvent) {
        val context = getApplication<Application>()
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        NotificationHelper.postEmergencyAlert(context, event, openApp)
    }

    private fun postWarningNotification(event: AlertEvent) {
        val context = getApplication<Application>()
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        NotificationHelper.postWarningAlert(context, event, openApp)
    }

    private fun dispatchCaregiverAlert(event: AlertEvent, level: CaregiverAlertStatus, smsEnabled: Boolean) {
        val context = getApplication<Application>()
        val phone = caregiverPhoneNumber.value
        val reason = event.reason.displayLabel
        val hasPermission = context.checkSelfPermission(Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED
        CaregiverAlertDispatcher.dispatch(
            context = context,
            level = level,
            reason = reason,
            caregiverPhone = phone,
            enableSms = smsEnabled && hasPermission,
        )
    }

    private fun dispatchFcmAlert(event: AlertEvent, level: CaregiverAlertStatus) {
        val context = getApplication<Application>()
        val url = _backendUrl.value
        if (url.isBlank()) return
        val patientName = selectedPatient.value?.name ?: caregiverDisplayName.value
        viewModelScope.launch(Dispatchers.IO) {
            val token = FcmTokenManager.getToken(context)
            if (token == null) {
                android.util.Log.w("SmartSuitViewModel", "No FCM token — skipping push")
                return@launch
            }
            val result = FcmAlertSender.sendAlert(
                backendUrl = url,
                level = level,
                reason = event.reason.displayLabel,
                patientName = patientName,
                caregiverFcmToken = token,
            )
            if (result.isFailure) {
                android.util.Log.e("SmartSuitViewModel", "FCM alert failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    override fun onCleared() {
        bleDataSource.stop()
        super.onCleared()
    }
}
