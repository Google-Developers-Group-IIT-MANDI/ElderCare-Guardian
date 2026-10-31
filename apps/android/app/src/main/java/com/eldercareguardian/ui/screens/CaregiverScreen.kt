package com.eldercareguardian.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eldercareguardian.data.AlertEvent
import com.eldercareguardian.data.RiskStatus
import com.eldercareguardian.data.SensorFrame
import com.eldercareguardian.ui.components.AlertTimeline
import com.eldercareguardian.ui.components.BatteryRow
import com.eldercareguardian.ui.components.MetricCard
import com.eldercareguardian.ui.components.PillStatus
import com.eldercareguardian.ui.components.SectionTitle
import com.eldercareguardian.ui.components.SignalRow
import com.eldercareguardian.ui.components.StatusPill
import com.eldercareguardian.ui.theme.AppColors
import com.eldercareguardian.ui.riskProgress
import kotlinx.coroutines.launch

@Composable
fun CaregiverScreen(
    frame: SensorFrame,
    caregiverDisplayName: String,
    caregiverPhoneNumber: String,
    onCallCaregiver: () -> Unit,
    alertHistory: List<AlertEvent>,
    modifier: Modifier = Modifier,
    onExportClinicalPdf: (suspend (android.content.Context) -> android.content.Intent)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CaregiverPanel(frame, caregiverDisplayName, caregiverPhoneNumber, onCallCaregiver, alertHistory, onExportClinicalPdf)
        DailyStatusPanel(frame)
        AlertTimeline(events = alertHistory)
    }
}

@Composable
private fun CaregiverPanel(
    frame: SensorFrame,
    caregiverDisplayName: String,
    caregiverPhoneNumber: String,
    onCallCaregiver: () -> Unit,
    alertHistory: List<AlertEvent>,
    onExportClinicalPdf: (suspend (android.content.Context) -> android.content.Intent)? = null,
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Caregiver alert")
            val lastAlertText = remember(alertHistory, frame) {
                val first = alertHistory.firstOrNull()
                if (first == null) {
                    "None yet"
                } else {
                    val diffMs = System.currentTimeMillis() - first.timestampMillis
                    val diffMin = diffMs / 60000L
                    val diffHrs = diffMin / 60L
                    when {
                        diffMin < 1 -> "Just now"
                        diffMin < 60 -> "${diffMin}m ago"
                        else -> "${diffHrs}h ago"
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    label = "Status",
                    value = frame.caregiverAlert.name,
                    unit = "",
                    modifier = Modifier.weight(1f),
                    accentColor = AppColors.colorForAlert(frame.caregiverAlert)
                )
                MetricCard("Last alert", lastAlertText, "", Modifier.weight(1f))
            }
            StatusPill(label = frame.caregiverAlert.name, status = PillStatus.CaregiverAlert(frame.caregiverAlert))
            HorizontalDivider(color = AppColors.borderLight)
            Text(
                text = "Caregiver contact",
                color = AppColors.textSecondary,
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = caregiverDisplayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary,
                    )
                    Text(
                        text = caregiverPhoneNumber,
                        color = AppColors.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = onCallCaregiver,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary),
                ) {
                    Text("Call")
                }
            }
            if (onExportClinicalPdf != null) {
                HorizontalDivider(color = AppColors.borderLight)
                val context = androidx.compose.ui.platform.LocalContext.current
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                Button(
                    onClick = {
                        scope.launch {
                            val intent = onExportClinicalPdf(context)
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryDark),
                ) {
                    Text("Export Doctor's Summary (PDF)")
                }
            }
        }
    }
}

@Composable
private fun DailyStatusPanel(frame: SensorFrame) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Daily status")
            val hydrationColor = when (frame.dehydration) {
                RiskStatus.High -> AppColors.danger
                RiskStatus.Medium -> AppColors.warning
                RiskStatus.Low -> AppColors.primary
            }
            SignalRow("Hydration risk", frame.dehydration.name, riskProgress(frame.dehydration), barColor = hydrationColor)
            SignalRow("Breathing trend", "${frame.respiratoryRate} breaths/min", 0.55f)
            SignalRow("HR reserve", "${frame.hrReservePercent}%", (frame.hrReservePercent / 100f).coerceIn(0f, 1f))
            BatteryRow(frame.batteryPercent)
        }
    }
}
