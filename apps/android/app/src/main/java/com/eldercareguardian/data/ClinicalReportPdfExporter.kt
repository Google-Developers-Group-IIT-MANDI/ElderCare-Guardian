package com.eldercareguardian.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.eldercareguardian.database.AlertEventEntity
import com.eldercareguardian.database.HealthDataEntity
import com.eldercareguardian.database.PatientEntity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ClinicalReportPdfExporter renders a professional, multi-section A4 medical report
 * using native Android [PdfDocument]. It provides physicians and geriatricians with a clean
 * clinical summary of the patient's resting vitals, rhythm observations, and safety incidents.
 */
object ClinicalReportPdfExporter {

    private const val TAG = "ClinicalReportPdf"
    private const val EXPORT_DIR = "exports"

    // Standard A4 dimensions in PostScript points (72 points per inch)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
    private val shortDateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.US)

    fun exportPdf(
        context: Context,
        patient: PatientEntity?,
        healthRecords: List<HealthDataEntity>,
        alerts: List<AlertEventEntity>,
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        drawReport(canvas, patient, healthRecords, alerts)

        document.finishPage(page)

        val dir = File(context.filesDir, EXPORT_DIR)
        if (!dir.mkdirs() && !dir.exists()) {
            throw IOException("Failed to create exports directory: ${dir.absolutePath}")
        }

        val timestamp = fileDateFormat.format(Date())
        val pdfFile = File(dir, "clinical_report_$timestamp.pdf")

        try {
            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write PDF: ${e.message}", e)
            throw IOException("Failed to write PDF report: ${e.message}", e)
        } finally {
            document.close()
        }

        pdfFile.setReadable(false, false)
        pdfFile.setWritable(false, false)
        pdfFile.setReadable(true, true)
        pdfFile.setWritable(true, true)

        return pdfFile
    }

    fun createShareIntent(context: Context, file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "ElderCare Guardian — Clinical Summary")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun drawReport(
        canvas: Canvas,
        patient: PatientEntity?,
        healthRecords: List<HealthDataEntity>,
        alerts: List<AlertEventEntity>,
    ) {
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG)
        val paintBg = Paint(Paint.ANTI_ALIAS_FLAG)
        val paintLine = Paint(Paint.ANTI_ALIAS_FLAG)

        val margin = 36f
        val contentWidth = PAGE_WIDTH - (margin * 2)
        var y = margin

        // 1. Header Banner (Teal)
        paintBg.color = Color.rgb(15, 118, 110) // #0F766E
        canvas.drawRoundRect(margin, y, margin + contentWidth, y + 64f, 10f, 10f, paintBg)

        paintText.color = Color.WHITE
        paintText.textSize = 18f
        paintText.isFakeBoldText = true
        canvas.drawText("ElderCare Guardian — Clinical Health Summary", margin + 16f, y + 28f, paintText)

        paintText.textSize = 10f
        paintText.isFakeBoldText = false
        val reportTime = displayDateFormat.format(Date())
        canvas.drawText("Generated on: $reportTime  •  Confidential Medical Summary", margin + 16f, y + 48f, paintText)

        y += 80f

        // 2. Patient Demographics & Monitoring Period
        paintBg.color = Color.rgb(248, 250, 252) // #F8FAFC
        canvas.drawRoundRect(margin, y, margin + contentWidth, y + 62f, 6f, 6f, paintBg)

        paintText.color = Color.rgb(30, 41, 59)
        paintText.textSize = 12f
        paintText.isFakeBoldText = true
        val pName = patient?.name?.ifBlank { "Unassigned Patient" } ?: "Unassigned Patient"
        val pAge = patient?.ageYears ?: 70
        canvas.drawText("Patient: $pName (Age: $pAge)", margin + 14f, y + 22f, paintText)

        paintText.isFakeBoldText = false
        paintText.textSize = 10f
        val cName = patient?.caregiverName?.ifBlank { "Not configured" } ?: "Not configured"
        val cPhone = patient?.caregiverPhone?.ifBlank { "—" } ?: "—"
        canvas.drawText("Caregiver: $cName  |  Emergency Contact: $cPhone", margin + 14f, y + 40f, paintText)

        val totalRecords = healthRecords.size
        val dateSpan = if (healthRecords.isNotEmpty()) {
            val oldest = shortDateFormat.format(Date(healthRecords.last().timestampMillis))
            val newest = shortDateFormat.format(Date(healthRecords.first().timestampMillis))
            "$oldest to $newest ($totalRecords samples)"
        } else {
            "No recorded telemetry"
        }
        canvas.drawText("Observation window: $dateSpan", margin + 14f, y + 54f, paintText)

        y += 78f

        // 3. Section Title: Physiological Vitals Summary
        drawSectionHeader(canvas, margin, y, "1. Physiological Vitals & Baselines", contentWidth)
        y += 24f

        if (healthRecords.isNotEmpty()) {
            val hrs = healthRecords.map { it.heartRateBpm }.filter { it > 0 }
            val spo2s = healthRecords.map { it.spo2Percent }.filter { it > 0f }
            val temps = healthRecords.map { it.skinTempC }.filter { it > 0f }
            val resps = healthRecords.map { it.respiratoryRate }.filter { it > 0 }

            val avgHr = if (hrs.isNotEmpty()) "%.0f bpm".format(hrs.average()) else "—"
            val minMaxHr = if (hrs.isNotEmpty()) "${hrs.minOrNull()} - ${hrs.maxOrNull()} bpm" else "—"
            val avgSpo2 = if (spo2s.isNotEmpty()) "%.1f %%".format(spo2s.average()) else "—"
            val minSpo2 = if (spo2s.isNotEmpty()) "%.1f %%".format(spo2s.minOrNull()) else "—"
            val avgTemp = if (temps.isNotEmpty()) "%.1f °C".format(temps.average()) else "—"
            val avgResp = if (resps.isNotEmpty()) "%.0f br/min".format(resps.average()) else "—"

            y = drawTableRow(canvas, margin, y, contentWidth, "Resting Heart Rate (Mean):", avgHr, "HR Range (Min - Max):", minMaxHr)
            y = drawTableRow(canvas, margin, y, contentWidth, "Mean Blood Oxygen (SpO2):", avgSpo2, "Lowest SpO2 Observed:", minSpo2)
            y = drawTableRow(canvas, margin, y, contentWidth, "Mean Skin Temperature:", avgTemp, "Mean Respiratory Rate:", avgResp)
        } else {
            paintText.textSize = 10f
            paintText.color = Color.rgb(100, 116, 139)
            canvas.drawText("No vital records stored in database yet.", margin + 10f, y + 14f, paintText)
            y += 20f
        }

        y += 14f

        // 4. Section Title: Cardiac Rhythm & Anomaly Events
        drawSectionHeader(canvas, margin, y, "2. Cardiac Rhythm & Anomaly Analysis", contentWidth)
        y += 24f

        val bradyCount = alerts.count { it.reasonName == "Bradycardia" }
        val tachyCount = alerts.count { it.reasonName == "Tachycardia" }
        val afibCount = alerts.count { it.reasonName == "AfibSuspected" || it.reasonName == "EcgAnomaly" }
        val hypoxCount = alerts.count { it.reasonName == "CriticalHypoxemia" }

        y = drawTableRow(canvas, margin, y, contentWidth, "Bradycardia Events (<50 bpm):", "$bradyCount episodes", "Tachycardia (>100 bpm):", "$tachyCount episodes")
        y = drawTableRow(canvas, margin, y, contentWidth, "Suspected Arrhythmia / AFib:", "$afibCount episodes", "Critical Hypoxemia (<90%):", "$hypoxCount episodes")

        y += 14f

        // 5. Section Title: Motion, Posture & Fall Safety Incidents
        drawSectionHeader(canvas, margin, y, "3. Safety & Fall Incident History (Past 10 Events)", contentWidth)
        y += 24f

        val recentAlerts = alerts.take(10)
        if (recentAlerts.isNotEmpty()) {
            paintBg.color = Color.rgb(241, 245, 249) // #F1F5F9
            canvas.drawRect(margin, y, margin + contentWidth, y + 18f, paintBg)

            paintText.isFakeBoldText = true
            paintText.textSize = 9f
            paintText.color = Color.rgb(51, 65, 85)
            canvas.drawText("Timestamp", margin + 8f, y + 12f, paintText)
            canvas.drawText("Severity Level", margin + 110f, y + 12f, paintText)
            canvas.drawText("Identified Reason", margin + 220f, y + 12f, paintText)
            y += 22f

            paintText.isFakeBoldText = false
            for (alert in recentAlerts) {
                val timeStr = displayDateFormat.format(Date(alert.timestampMillis))
                val level = alert.levelName
                val reason = alert.reasonName

                paintText.color = when (level) {
                    "Critical" -> Color.rgb(185, 28, 28) // red
                    "Warning" -> Color.rgb(217, 119, 6) // amber
                    else -> Color.rgb(30, 41, 59)
                }

                canvas.drawText(timeStr, margin + 8f, y + 12f, paintText)
                canvas.drawText(level, margin + 110f, y + 12f, paintText)
                canvas.drawText(reason, margin + 220f, y + 12f, paintText)

                paintLine.color = Color.rgb(226, 232, 240)
                canvas.drawLine(margin, y + 16f, margin + contentWidth, y + 16f, paintLine)
                y += 20f
            }
        } else {
            paintText.textSize = 10f
            paintText.color = Color.rgb(100, 116, 139)
            canvas.drawText("Zero emergency alerts or falls recorded. Patient in stable condition.", margin + 10f, y + 14f, paintText)
            y += 24f
        }

        // 6. Clinical Disclaimer at the Bottom
        val footerY = PAGE_HEIGHT - margin - 32f
        paintLine.color = Color.rgb(203, 213, 225)
        canvas.drawLine(margin, footerY, margin + contentWidth, footerY, paintLine)

        paintText.color = Color.rgb(100, 116, 139)
        paintText.textSize = 8f
        paintText.isFakeBoldText = false
        canvas.drawText(
            "CLINICAL DISCLAIMER: This document is generated for informational and wellness monitoring purposes only.",
            margin,
            footerY + 14f,
            paintText,
        )
        canvas.drawText(
            "It does not replace direct medical advice, professional diagnosis, or emergency dispatch services.",
            margin,
            footerY + 24f,
            paintText,
        )
    }

    private fun drawSectionHeader(canvas: Canvas, x: Float, y: Float, title: String, width: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 118, 110)
            textSize = 12f
            isFakeBoldText = true
        }
        canvas.drawText(title, x, y + 12f, paint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(204, 251, 241) // #CCFBF1
            strokeWidth = 1.5f
        }
        canvas.drawLine(x, y + 18f, x + width, y + 18f, linePaint)
    }

    private fun drawTableRow(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        label1: String,
        val1: String,
        label2: String,
        val2: String,
    ): Float {
        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
        }
        val paintVal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
            isFakeBoldText = true
        }

        val col1 = x + 8f
        val col2 = x + (width / 2f)

        canvas.drawText(label1, col1, y + 12f, paintLabel)
        canvas.drawText(val1, col1 + 140f, y + 12f, paintVal)

        canvas.drawText(label2, col2, y + 12f, paintLabel)
        canvas.drawText(val2, col2 + 140f, y + 12f, paintVal)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249)
            strokeWidth = 1f
        }
        canvas.drawLine(x, y + 18f, x + width, y + 18f, linePaint)

        return y + 20f
    }
}
