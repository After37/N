package org.after37.tickrecorder

import android.app.Activity
import android.os.Bundle
import android.os.Build
import android.os.SystemClock
import android.content.Intent
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class MainActivity : Activity() {
    private val rows = ArrayList<String>()
    private lateinit var status: TextView
    private lateinit var runButton: Button
    private lateinit var countSpinner: Spinner
    private lateinit var exportButton: Button
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 24)
        }
        root.addView(TextView(this).apply {
            text = "Запись измерений"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(TextView(this).apply {
            text = "Снимает реальные программные временные отметки на этом устройстве. Это НЕ запись каждого физического такта CPU."
            textSize = 15f
            setPadding(0, 12, 0, 18)
        })
        countSpinner = Spinner(this)
        countSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("10 000 измерений", "100 000 измерений", "500 000 измерений"))
        root.addView(countSpinner)
        runButton = Button(this).apply {
            text = "Начать измерение"
            setOnClickListener { startRun() }
        }
        root.addView(runButton)
        exportButton = Button(this).apply {
            text = "Экспорт CSV"
            isEnabled = false
            setOnClickListener { exportCsv() }
        }
        root.addView(exportButton)
        status = TextView(this).apply {
            textSize = 14f
            setPadding(0, 18, 0, 0)
            text = "Готово. Выбери размер серии и начни измерение."
        }
        root.addView(status)
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun startRun() {
        if (running) return
        running = true
        runButton.isEnabled = false
        exportButton.isEnabled = false
        rows.clear()
        rows.add("index,elapsed_realtime_ns,delta_ns,operation_result,device,android")
        val count = when (countSpinner.selectedItemPosition) { 1 -> 100_000; 2 -> 500_000; else -> 10_000 }
        val device = "${Build.MANUFACTURER} ${Build.MODEL}".replace(",", " ")
        val android = Build.VERSION.RELEASE.replace(",", " ")
        Thread {
            var previous = SystemClock.elapsedRealtimeNanos()
            var accumulator = 0L
            val start = previous
            for (i in 0 until count) {
                // Small deterministic software operation between two real monotonic clock reads.
                accumulator = (accumulator xor (i.toLong() * 0x9E3779B9L)) + 1L
                val now = SystemClock.elapsedRealtimeNanos()
                val delta = max(0L, now - previous)
                rows.add("$i,$now,$delta,$accumulator,$device,$android")
                previous = now
            }
            val elapsed = SystemClock.elapsedRealtimeNanos() - start
            runOnUiThread {
                running = false
                runButton.isEnabled = true
                exportButton.isEnabled = true
                status.text = "Готово: $count записей\nВремя серии: ${elapsed / 1_000_000.0} мс\nПоследняя дельта: ${rows.lastOrNull()?.split(",")?.getOrNull(2) ?: "—"} нс\nУстройство: $device\nAndroid: $android\n\nВажно: это программные замеры времени, не аппаратные такты."
                Toast.makeText(this, "Измерение завершено", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun exportCsv() {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "tick-measurements-$stamp.csv")
        }
        pendingCsv = rows.joinToString("\n")
        startActivityForResult(intent, EXPORT_REQUEST)
    }

    @Deprecated("Use Activity Result API; kept compatible with minimal SDK setup")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EXPORT_REQUEST && resultCode == RESULT_OK && data?.data != null) {
            try {
                contentResolver.openOutputStream(data.data!!)?.bufferedWriter()?.use { it.write(pendingCsv) }
                Toast.makeText(this, "CSV сохранён", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private var pendingCsv: String = ""
    companion object { private const val EXPORT_REQUEST = 71 }
}
