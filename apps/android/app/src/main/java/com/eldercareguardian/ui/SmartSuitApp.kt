package com.eldercareguardian.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eldercareguardian.ble.BleConnectionState
import com.eldercareguardian.ble.DiscoveredBleDevice
import com.eldercareguardian.ble.SmartSuitBleTelemetry
import com.eldercareguardian.ui.theme.AppTypography
import com.eldercareguardian.data.AlertEvent
import com.eldercareguardian.data.CaregiverAlertStatus
import com.eldercareguardian.data.FatigueStatus
import com.eldercareguardian.data.Patient
import com.eldercareguardian.data.PostureStatus
import com.eldercareguardian.data.RiskStatus
import com.eldercareguardian.data.SensorFrame
import com.eldercareguardian.permissions.SmartSuitPermissions
import com.eldercareguardian.samsung.SamsungHealthState
import com.eldercareguardian.ui.components.DataSourceChip
import com.eldercareguardian.ui.components.PillStatus
import com.eldercareguardian.ui.components.StatusPill
import com.eldercareguardian.ui.components.UrgentAlertBanner
import com.eldercareguardian.ui.screens.BodyScreen
import com.eldercareguardian.ui.screens.CaregiverScreen
import com.eldercareguardian.ui.screens.ReadinessScreen
import com.eldercareguardian.ui.screens.SafetyScreen
import com.eldercareguardian.ui.screens.SettingsScreen
import com.eldercareguardian.ui.screens.VitalsScreen
import com.eldercareguardian.notifications.CaregiverContact

@Composable
fun SmartSuitApp(
    smartSuitViewModel: SmartSuitViewModel = viewModel(),
) {
    val frame by smartSuitViewModel.frames.collectAsState()
    val bleConnectionState by smartSuitViewModel.bleConnectionState.collectAsState()
    val discoveredDevices by smartSuitViewModel.discoveredDevices.collectAsState()
    val bleTelemetry by smartSuitViewModel.bleTelemetry.collectAsState()
    val isLiveBleData by smartSuitViewModel.isLiveBleData.collectAsState()
    val samsungState by smartSuitViewModel.samsungState.collectAsState()
    val sosOverride by smartSuitViewModel.sosOverride.collectAsState()
    val acknowledgedUrgent by smartSuitViewModel.acknowledgedUrgent.collectAsState()
    val alertHistory by smartSuitViewModel.alertHistory.collectAsState()
    val hrTrend by smartSuitViewModel.hrTrend.collectAsState()
    val spo2Trend by smartSuitViewModel.spo2Trend.collectAsState()
    val caregiverPhoneNumber by smartSuitViewModel.caregiverPhoneNumber.collectAsState()
    val caregiverDisplayName by smartSuitViewModel.caregiverDisplayName.collectAsState()
    val smsEnabled by smartSuitViewModel.smsEnabled.collectAsState()
    val backendUrl by smartSuitViewModel.backendUrl.collectAsState()
    val patients by smartSuitViewModel.patients.collectAsState()
    val selectedPatientId by smartSuitViewModel.selectedPatientId.collectAsState()
    val selectedPatient by smartSuitViewModel.selectedPatient.collectAsState()
    val retentionDays by smartSuitViewModel.retentionDays.collectAsState()
    val permissionController = rememberPermissionController()
    var selectedTab by remember { mutableStateOf(AppTab.Vitals) }

    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { smartSuitViewModel.loadJsonFile(it) }
    }
    val onLoadJsonFile: () -> Unit = { jsonFilePickerLauncher.launch("*/*") }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF0F766E),
            secondary = Color(0xFF2563EB),
            surface = Color(0xFFFFFFFF),
            background = Color(0xFFF8FAFC),
        ),
        typography = AppTypography,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            frame?.let {
                AppShell(
                    frame = it,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    missingPermissions = permissionController.missingPermissions,
                    onRequestPermissions = permissionController.requestPermissions,
                    bleConnectionState = bleConnectionState,
                    discoveredDevices = discoveredDevices,
                    bleTelemetry = bleTelemetry,
                    samsungState = samsungState,
                    sosOverride = sosOverride,
                    acknowledgedUrgent = acknowledgedUrgent,
                    alertHistory = alertHistory,
                    hrTrend = hrTrend,
                    spo2Trend = spo2Trend,
                    caregiverDisplayName = caregiverDisplayName,
                    caregiverPhoneNumber = caregiverPhoneNumber,
                    onCallCaregiver = { ctx ->
                        if (com.eldercareguardian.settings.isValidPhone(caregiverPhoneNumber)) {
                            CaregiverContact.launchDialer(ctx, caregiverPhoneNumber)
                        }
                    },
                    onStartBleScan = smartSuitViewModel::startBleScan,
                    onStopBle = smartSuitViewModel::stopBle,
                    onConnectFirstDevice = smartSuitViewModel::connectToFirstDiscoveredDevice,
                    onStartSamsung = smartSuitViewModel::startSamsungBridge,
                    onTriggerSosDemo = smartSuitViewModel::triggerSosDemo,
                    onClearSosDemo = smartSuitViewModel::clearSosDemo,
                    onAcknowledgeUrgent = smartSuitViewModel::acknowledgeUrgent,
                    smsEnabled = smsEnabled,
                    onSmsEnabledChanged = smartSuitViewModel::setSmsEnabled,
                    backendUrl = backendUrl,
                    onBackendUrlChanged = smartSuitViewModel::setBackendUrl,
                    onUpdateCaregiverContact = smartSuitViewModel::updateCaregiverContact,
                    retentionDays = retentionDays,
                    onRetentionDaysChanged = smartSuitViewModel::setRetentionDays,
                    patients = patients,
                    selectedPatientId = selectedPatientId,
                    selectedPatient = selectedPatient,
                    onSelectPatient = smartSuitViewModel::selectPatient,
                    onAddPatient = smartSuitViewModel::addPatient,
                    onUpdatePatient = smartSuitViewModel::updatePatient,
                    onDeletePatient = smartSuitViewModel::deletePatient,
                    onExportData = smartSuitViewModel::exportData,
                    onExportClinicalPdf = smartSuitViewModel::exportClinicalPdf,
                    onDeleteAllData = smartSuitViewModel::deleteAllData,
                    onLoadJsonFile = onLoadJsonFile,
                )
            } ?: NoDataScreen()
        }
    }
}

private enum class AppTab(
    val label: String,
    val icon: ImageVector,
) {
    Vitals("Vitals", Icons.Filled.Favorite),
    Body("Body", Icons.Filled.AccessibilityNew),
    Safety("Safety", Icons.Filled.HealthAndSafety),
    Caregiver("Care", Icons.Filled.ContactPhone),
    Readiness("Ready", Icons.Filled.Checklist),
    Settings("Settings", Icons.Filled.Settings),
}

private data class PermissionController(
    val missingPermissions: List<String>,
    val requestPermissions: () -> Unit,
)

@Composable
private fun rememberPermissionController(): PermissionController {
    val context = LocalContext.current
    // Only track core permissions for the main "Grant" button.
    // Enhanced permissions (SMS, background location) are requested
    // progressively when their features are enabled — see B31.
    var missingPermissions by remember {
        mutableStateOf(SmartSuitPermissions.missingPermissions(context, "core"))
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        missingPermissions = SmartSuitPermissions.missingPermissions(context, "core")
    }

    return PermissionController(
        missingPermissions = missingPermissions,
        requestPermissions = {
            launcher.launch(SmartSuitPermissions.corePermissions().toTypedArray())
        },
    )
}

@Composable
private fun NoDataScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No sensor data",
                color = Color(0xFF64748B),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Connect to BLE wearable (ElderCare_v1)",
                color = Color(0xFF94A3B8),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AppShell(
    frame: SensorFrame,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    missingPermissions: List<String>,
    onRequestPermissions: () -> Unit,
    bleConnectionState: BleConnectionState,
    discoveredDevices: List<DiscoveredBleDevice>,
    bleTelemetry: SmartSuitBleTelemetry,
    samsungState: SamsungHealthState,
    sosOverride: Boolean,
    acknowledgedUrgent: Boolean,
    alertHistory: List<AlertEvent>,
    hrTrend: List<Float>,
    spo2Trend: List<Float>,
    onStartBleScan: () -> Unit,
    onStopBle: () -> Unit,
    onConnectFirstDevice: () -> Unit,
    onStartSamsung: () -> Unit,
    onTriggerSosDemo: () -> Unit,
    onClearSosDemo: () -> Unit,
    onAcknowledgeUrgent: () -> Unit,
    smsEnabled: Boolean,
    onSmsEnabledChanged: (Boolean) -> Unit,
    retentionDays: Int = 7,
    onRetentionDaysChanged: (Int) -> Unit = {},
    backendUrl: String,
    onBackendUrlChanged: (String) -> Unit,
    caregiverDisplayName: String,
    caregiverPhoneNumber: String,
    onCallCaregiver: (android.content.Context) -> Unit,
    onUpdateCaregiverContact: suspend (displayName: String, phoneNumber: String) -> Boolean,
    patients: List<Patient>,
    selectedPatientId: Long,
    selectedPatient: Patient?,
    onSelectPatient: (Long) -> Unit,
    onAddPatient: suspend (name: String, caregiverName: String, caregiverPhone: String, ageYears: Int) -> Long,
    onUpdatePatient: suspend (Patient) -> Unit,
    onDeletePatient: suspend (Patient) -> Unit,
    onExportData: suspend (android.content.Context) -> android.content.Intent,
    onExportClinicalPdf: suspend (android.content.Context) -> android.content.Intent,
    onDeleteAllData: () -> Unit,
    onLoadJsonFile: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    ) { padding ->
        if (selectedTab == AppTab.Settings) {
            SettingsScreen(
                initialName = caregiverDisplayName,
                initialPhone = caregiverPhoneNumber,
                smsEnabled = smsEnabled,
                onSmsEnabledChanged = onSmsEnabledChanged,
                backendUrl = backendUrl,
                onBackendUrlChanged = onBackendUrlChanged,
                onSave = onUpdateCaregiverContact,
                onBack = { onTabSelected(AppTab.Vitals) },
                modifier = Modifier.padding(padding),
                patients = patients,
                selectedPatientId = selectedPatientId,
                selectedPatient = selectedPatient,
                onSelectPatient = onSelectPatient,
                onAddPatient = onAddPatient,
                onUpdatePatient = onUpdatePatient,
                onDeletePatient = onDeletePatient,
                onExportData = onExportData,
                onExportClinicalPdf = onExportClinicalPdf,
                onDeleteAllData = onDeleteAllData,
                retentionDays = retentionDays,
                onRetentionDaysChanged = onRetentionDaysChanged,
                onLoadJsonFile = onLoadJsonFile,
            )
        } else {
            val context = androidx.compose.ui.platform.LocalContext.current
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Header(
                        frame = frame,
                        bleConnectionState = bleConnectionState,
                        patients = patients,
                        selectedPatientId = selectedPatientId,
                        selectedPatient = selectedPatient,
                        onSelectPatient = onSelectPatient,
                    )
                }
                if (frame.caregiverAlert == CaregiverAlertStatus.Emergency && !acknowledgedUrgent) {
                    item {
                        UrgentAlertBanner(
                            latestEvent = alertHistory.firstOrNull(),
                            onAcknowledge = onAcknowledgeUrgent,
                        )
                    }
                }
                if (missingPermissions.isNotEmpty()) {
                    item {
                        PermissionNotice(
                            missingPermissions = missingPermissions,
                            onRequestPermissions = onRequestPermissions,
                        )
                    }
                }
                when (selectedTab) {
                    AppTab.Vitals -> {
                        item {
                            VitalsScreen(
                                frame = frame,
                                hrTrend = hrTrend,
                                spo2Trend = spo2Trend,
                            )
                        }
                    }
                    AppTab.Body -> {
                        item {
                            BodyScreen(frame = frame)
                        }
                    }
                    AppTab.Safety -> {
                        item {
                            SafetyScreen(
                                frame = frame,
                                sosOverride = sosOverride,
                                onTriggerSosDemo = onTriggerSosDemo,
                                onClearSosDemo = onClearSosDemo,
                            )
                        }
                    }
                    AppTab.Caregiver -> {
                        item {
                            CaregiverScreen(
                                frame = frame,
                                caregiverDisplayName = caregiverDisplayName,
                                caregiverPhoneNumber = caregiverPhoneNumber,
                                onCallCaregiver = { onCallCaregiver(context) },
                                alertHistory = alertHistory,
                                onExportClinicalPdf = onExportClinicalPdf,
                            )
                        }
                    }
                    AppTab.Readiness -> {
                        item {
                            ReadinessScreen(
                                missingPermissions = missingPermissions,
                                bleConnectionState = bleConnectionState,
                                discoveredDevices = discoveredDevices,
                                bleTelemetry = bleTelemetry,
                                samsungState = samsungState,
                                onRequestPermissions = onRequestPermissions,
                                onStartBleScan = onStartBleScan,
                                onStopBle = onStopBle,
                                onConnectFirstDevice = onConnectFirstDevice,
                                onStartSamsung = onStartSamsung,
                            )
                        }
                    }
                    AppTab.Settings -> {
                        // Rendered outside the LazyColumn (see Scaffold content block).
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionNotice(
    missingPermissions: List<String>,
    onRequestPermissions: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Permissions needed for BLE mode",
                    color = Color(0xFF92400E),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = missingPermissions.joinToString { SmartSuitPermissions.label(it) },
                    color = Color(0xFF92400E),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(
                onClick = onRequestPermissions,
                shape = RoundedCornerShape(6.dp),
            ) {
                Text("Grant")
            }
        }
    }
}

@Composable
private fun Header(
    frame: SensorFrame,
    bleConnectionState: BleConnectionState,
    patients: List<Patient>,
    selectedPatientId: Long,
    selectedPatient: Patient?,
    onSelectPatient: (Long) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "ElderCare Guardian",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (patients.isNotEmpty()) {
                PatientSwitcher(
                    patients = patients,
                    selectedPatientId = selectedPatientId,
                    selectedPatient = selectedPatient,
                    onSelectPatient = onSelectPatient,
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DataSourceChip(bleConnectionState)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(label = frame.fatigue.name, status = PillStatus.Fatigue(frame.fatigue))
                Spacer(Modifier.width(0.dp))
            }
        }
    }
}

@Composable
private fun PatientSwitcher(
    patients: List<Patient>,
    selectedPatientId: Long,
    selectedPatient: Patient?,
    onSelectPatient: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = selectedPatient?.name ?: if (patients.isEmpty()) "No patients" else "Select patient"
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(28.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            patients.forEach { patient ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Text(
                            patient.name,
                            fontWeight = if (patient.id == selectedPatientId) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSelectPatient(patient.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Vitals Dashboard")
@Composable
private fun AppShellVitalsPreview() {
    val mockFrame = SensorFrame(
        timestampMillis = System.currentTimeMillis(),
        heartRateBpm = 72,
        spo2Percent = 98.5f,
        systolicMmHg = 120,
        diastolicMmHg = 80,
        skinTempC = 36.6f,
        humidityPercent = 45f,
        respiratoryRate = 14,
        posture = PostureStatus.Good,
        fatigue = FatigueStatus.Safe,
        dehydration = RiskStatus.Low,
        fallRisk = RiskStatus.Low,
        caregiverAlert = CaregiverAlertStatus.Normal,
        sosActive = false,
        inactivityMinutes = 0,
        supercapPercent = 90,
        ecgSamples = List(50) { (kotlin.math.sin(it.toFloat() * 0.5f)) },
        batteryPercent = 85
    )

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF0F766E),
            secondary = Color(0xFF2563EB),
            surface = Color(0xFFFFFFFF),
            background = Color(0xFFF8FAFC),
        ),
        typography = AppTypography,
    ) {
        AppShell(
            frame = mockFrame,
            selectedTab = AppTab.Vitals,
            onTabSelected = {},
            missingPermissions = emptyList(),
            onRequestPermissions = {},
            bleConnectionState = BleConnectionState.Connected,
            discoveredDevices = emptyList(),
            bleTelemetry = SmartSuitBleTelemetry(),
            samsungState = SamsungHealthState.Ready,
            sosOverride = false,
            acknowledgedUrgent = false,
            alertHistory = emptyList(),
            hrTrend = List(20) { 70f + it % 5 },
            spo2Trend = List(20) { 98f + it % 2 },
            caregiverDisplayName = "Jane Doe",
            caregiverPhoneNumber = "+1234567890",
            onCallCaregiver = {},
            onStartBleScan = {},
            onStopBle = {},
            onConnectFirstDevice = {},
            onStartSamsung = {},
            onTriggerSosDemo = {},
            onClearSosDemo = {},
            onAcknowledgeUrgent = {},
            smsEnabled = false,
            onSmsEnabledChanged = {},
            backendUrl = "",
            onBackendUrlChanged = {},
            onUpdateCaregiverContact = { _, _ -> true },
            patients = emptyList(),
            selectedPatientId = 0L,
            selectedPatient = null,
            onSelectPatient = {},
            onAddPatient = { _, _, _, _ -> 0L },
            onUpdatePatient = {},
            onDeletePatient = {},
            onExportData = { android.content.Intent() },
            onExportClinicalPdf = { android.content.Intent() },
            onDeleteAllData = {},
            onLoadJsonFile = {},
        )
    }
}
