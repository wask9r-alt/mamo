package com.mamomacro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.btnOverlay).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Оверлей уже разрешён", Toast.LENGTH_SHORT).show()
            }
            updateStatus()
        }

        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Найди MamoMacro и включи", Toast.LENGTH_LONG).show()
        }

        // Калибровка больше не нужна — координаты захардкожены
        findViewById<Button>(R.id.btnCalibrate).visibility = View.GONE

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Сначала разреши оверлей", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startService(Intent(this, FloatingService::class.java).apply {
                action = FloatingService.ACTION_SHOW_PLAY
            })
            Toast.makeText(this, "Кнопка Play запущена", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopService(Intent(this, FloatingService::class.java))
            Toast.makeText(this, "Кнопка остановлена", Toast.LENGTH_SHORT).show()
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val overlay = if (Settings.canDrawOverlays(this)) "✅ Оверлей" else "❌ Оверлей"
        val access = if (isAccessibilityEnabled()) "✅ Accessibility" else "❌ Accessibility"
        statusText.text = "$overlay\n$access\n\nПас: 2058, 657\nУдар: 2054, 1050\nЗадержка: 12 мс"
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = "$packageName/${MacroAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
