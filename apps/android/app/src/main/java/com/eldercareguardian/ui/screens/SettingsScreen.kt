package com.eldercareguardian.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.eldercareguardian.data.Patient
import com.eldercareguardian.settings.DataRetentionPreferences
import com.eldercareguardian.settings.isValidPhone
import com.eldercareguardian.settings.phoneDigitCount
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Settings screen — used for editing the caregiver's display name and
 * phone number. The phone field is validated: must contain at least 7
 * digits (per [com.eldercareguardian.settings.isValidPhone]).
 *
 * On save, a coroutine is launched to persist to DataStore. A "Saved."
 * indicator appears for 2 seconds before clearing.
 *
 * Caller is responsible for navigation. Pass [onBack] to receive the
 * close-event and route back to the dashboard.
 */
@Composable
fun SettingsScreen(
    initialName: String,
    initialPhone: String,
    smsEnabled: Boolean,
    onSmsEnabledChanged: (Boolean) -> Unit,
    backendUrl: String,
    onBackendUrlChanged: (String) -> Unit,
    onSave: suspend (name: String, phone: String) -> Boolean,
    retentionDays: Int = DataRetentionPreferences.DEFAULT_RETENTION_DAYS,
    onRetentionDaysChanged: (Int) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    patients: List<Patient> = emptyList(),
    selectedPatientId: Long = 0L,
    selectedPatient: Patient? = null,
    onSelectPatient: (Long) -> Unit = {},
    onAddPatient: suspend (name: String, caregiverName: String, caregiverPhone: String, ageYears: Int) -> Long = { _, _, _, _ -> 0L },
    onUpdatePatient: suspend (Patient) -> Unit = {},
    onDeletePatient: suspend (Patient) -> Unit = {},
    onExportData: suspend (Context) -> Intent = { Intent() },
    onExportClinicalPdf: suspend (Context) -> Intent = { Intent() },
    onDeleteAllData: () -> Unit = {},
    onLoadJsonFile: () -> Unit = {},
) {
    var name by remember { mutableStateOf(TextFieldValue(initialName)) }
    var phone by remember { mutableStateOf(TextFieldValue(initialPhone)) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val smsPermissionContext = LocalContext.current

    // B31: Request SEND_SMS permission on-demand when the SMS toggle is enabled.
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // Enable SMS only if the user actually granted the permission.
        onSmsEnabledChanged(granted)
    }

    LaunchedEffect(saved) {
        if (saved) {
            delay(2_000)
            saved = false
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F766E),
                    )
                }
                Spacer(Modifier.size(4.dp))
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = Color(0xFF0F766E),
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Caregiver contact",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "This is the person the app calls when there's an urgent alert. " +
                            "Use international format (+14155551234) so the dialer always recognises it.",
                        color = Color(0xFF475569),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorText = null
                        },
                        label = { Text("Display name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorText != null && name.text.isBlank(),
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            errorText = null
                        },
                        label = { Text("Phone number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = errorText != null && !isValidPhone(phone.text),
                        supportingText = {
                            val digits = phoneDigitCount(phone.text)
                            if (digits in 1..6) {
                                Text(
                                    text = "Needs at least 7 digits (currently $digits).",
                                    color = Color(0xFFB45309),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        },
                    )
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Send SMS on Warning/Emergency",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Requires SEND_SMS permission and a valid caregiver number",
                                color = Color(0xFF64748B),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = smsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    // B31: Only request SEND_SMS when user turns on SMS alerts.
                                    val hasSmsPerm = ContextCompat.checkSelfPermission(
                                        smsPermissionContext, Manifest.permission.SEND_SMS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasSmsPerm) {
                                        onSmsEnabledChanged(true)
                                    } else {
                                        smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                                    }
                                } else {
                                    onSmsEnabledChanged(false)
                                }
                            },
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Text(
                        text = "Backend URL (FCM alerts)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = backendUrl,
                        onValueChange = onBackendUrlChanged,
                        label = { Text("https://your-backend.railway.app") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )
                    Text(
                        text = "FCM alerts use this endpoint to notify the caregiver's device.",
                        color = Color(0xFF64748B),
                        style = MaterialTheme.typography.bodySmall,
                    )

                    if (errorText != null) {
                        Text(
                            text = errorText!!,
                            color = Color(0xFFB91C1C),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (saved) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF0F766E),
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    text = "Saved",
                                    color = Color(0xFF0F766E),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        } else {
                            Spacer(Modifier.size(1.dp))
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val ok = onSave(name.text, phone.text)
                                    if (ok) {
                                        errorText = null
                                        saved = true
                                    } else {
                                        errorText = "Enter a name and a phone number with at least 7 digits."
                                    }
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        ) {
                            Text("Save")
                        }
                    }
                }
            }

            PatientSection(
                patients = patients,
                selectedPatientId = selectedPatientId,
                selectedPatient = selectedPatient,
                onSelectPatient = onSelectPatient,
                onAddPatient = onAddPatient,
                onUpdatePatient = onUpdatePatient,
                onDeletePatient = onDeletePatient,
            )

            DataPrivacySection(
                onExportData = onExportData,
                onExportClinicalPdf = onExportClinicalPdf,
                onDeleteAllData = onDeleteAllData,
            )

            DataSourceSection(
                onLoadJsonFile = onLoadJsonFile,
            )

            DataRetentionSection(
                retentionDays = retentionDays,
                onRetentionDaysChanged = onRetentionDaysChanged,
            )

            Text(
                text = "Caregiver contact is stored locally in app-private DataStore. " +
                    "It is not transmitted off the device.",
                color = Color(0xFF94A3B8),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun PatientSection(
    patients: List<Patient>,
    selectedPatientId: Long,
    selectedPatient: Patient?,
    onSelectPatient: (Long) -> Unit,
    onAddPatient: suspend (name: String, caregiverName: String, caregiverPhone: String, ageYears: Int) -> Long,
    onUpdatePatient: suspend (Patient) -> Unit,
    onDeletePatient: suspend (Patient) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var patientToEdit by remember { mutableStateOf<Patient?>(null) }
    var patientToDelete by remember { mutableStateOf<Patient?>(null) }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Patient profiles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (patients.isEmpty()) {
                Text(
                    text = "No patients yet. Add a patient profile to track their vitals and alerts.",
                    color = Color(0xFF64748B),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                patients.forEach { patient ->
                    val isSelected = patient.id == selectedPatientId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = patient.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (patient.caregiverName.isNotBlank()) {
                                Text(
                                    text = "Caregiver: ${patient.caregiverName}",
                                    color = Color(0xFF64748B),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = { onSelectPatient(patient.id) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(28.dp),
                            ) {
                                Text(
                                    if (isSelected) "Active" else "Select",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            IconButton(onClick = { patientToEdit = patient }) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Edit",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFF64748B),
                                )
                            }
                            IconButton(onClick = { patientToDelete = patient }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFFB91C1C),
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                }
            }

            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add patient")
            }
        }
    }

    if (showAddDialog) {
        PatientFormDialog(
            title = "Add patient",
            initial = null,
            onConfirm = { name, cgName, cgPhone, age ->
                scope.launch {
                    val id = onAddPatient(name, cgName, cgPhone, age)
                    if (id > 0) onSelectPatient(id)
                }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    patientToEdit?.let { patient ->
        PatientFormDialog(
            title = "Edit patient",
            initial = patient,
            onConfirm = { name, cgName, cgPhone, age ->
                scope.launch {
                    onUpdatePatient(patient.copy(name = name, caregiverName = cgName, caregiverPhone = cgPhone, ageYears = age))
                }
                patientToEdit = null
            },
            onDismiss = { patientToEdit = null },
        )
    }

    patientToDelete?.let { patient ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { patientToDelete = null },
            title = { Text("Delete ${patient.name}?") },
            text = { Text("All vitals and alert history for this patient will be preserved in the database.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { onDeletePatient(patient) }
                        patientToDelete = null
                    },
                ) { Text("Delete", color = Color(0xFFB91C1C)) }
            },
            dismissButton = {
                TextButton(onClick = { patientToDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PatientFormDialog(
    title: String,
    initial: Patient?,
    onConfirm: (name: String, caregiverName: String, caregiverPhone: String, ageYears: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(TextFieldValue(initial?.name ?: "")) }
    var cgName by remember { mutableStateOf(TextFieldValue(initial?.caregiverName ?: "")) }
    var cgPhone by remember { mutableStateOf(TextFieldValue(initial?.caregiverPhone ?: "")) }
    var age by remember { mutableStateOf(TextFieldValue(initial?.ageYears?.toString() ?: "70")) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Patient name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = cgName,
                    onValueChange = { cgName = it },
                    label = { Text("Caregiver name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = cgPhone,
                    onValueChange = { cgPhone = it },
                    label = { Text("Caregiver phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age (years)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.text.isNotBlank()) {
                        val ageValue = age.text.toIntOrNull() ?: 70
                        onConfirm(name.text.trim(), cgName.text.trim(), cgPhone.text.trim(), ageValue.coerceIn(1, 120))
                    }
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DataPrivacySection(
    onExportData: suspend (Context) -> Intent,
    onExportClinicalPdf: suspend (Context) -> Intent,
    onDeleteAllData: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Data & Privacy (DPDPA)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Under the Digital Personal Data Protection Act, 2023, you have the right " +
                    "to access your data and the right to erasure.",
                color = Color(0xFF64748B),
                style = MaterialTheme.typography.bodySmall,
            )

            Button(
                onClick = {
                    scope.launch {
                        val intent = onExportClinicalPdf(context)
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                    }
                },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Export Doctor's Summary (PDF)")
            }

            Button(
                onClick = {
                    scope.launch {
                        val intent = onExportData(context)
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                    }
                },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Export raw telemetry (JSON)")
            }

            Button(
                onClick = { showDeleteConfirm = true },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Delete all data")
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = "Delete all data?",
            message = "This will permanently delete all patients, health history, and alert " +
                "records. Your DPDPA consent will be revoked and the app will restart to the " +
                "consent screen. This cannot be undone.",
            onConfirm = {
                onDeleteAllData()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun DataRetentionSection(
    retentionDays: Int,
    onRetentionDaysChanged: (Int) -> Unit,
) {
    val options = listOf(7, 14, 30, 60, 90)

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Data retention",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Old alert and health records are automatically deleted after the selected period.",
                color = Color(0xFF64748B),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { days ->
                    val selected = retentionDays == days
                    OutlinedButton(
                        onClick = { onRetentionDaysChanged(days) },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) Color(0xFF0F766E) else Color.Transparent,
                            contentColor = if (selected) Color.White else Color(0xFF334155),
                        ),
                        border = if (selected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f).height(36.dp),
                    ) {
                        Text(
                            text = "${days}d",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = Color(0xFFB91C1C)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DataSourceSection(
    onLoadJsonFile: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Data source",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "By default the app runs in demo mode with simulated sensor data. You can also load a JSON file containing recorded sensor frames.",
                color = Color(0xFF64748B),
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = onLoadJsonFile,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Load JSON file")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    var smsEnabled by remember { mutableStateOf(false) }
    var backendUrl by remember { mutableStateOf("") }
    MaterialTheme {
        SettingsScreen(
            initialName = "John Doe",
            initialPhone = "+1234567890",
            smsEnabled = smsEnabled,
            onSmsEnabledChanged = { smsEnabled = it },
            backendUrl = backendUrl,
            onBackendUrlChanged = { backendUrl = it },
            onSave = { _, _ -> true },
            onBack = {}
        )
    }
}
