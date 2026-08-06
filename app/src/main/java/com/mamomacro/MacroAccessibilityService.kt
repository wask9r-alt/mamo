package com.mamomacro

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class MacroAccessibilityService : AccessibilityService() {

    companion object {
        var instance: MacroAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    /**
     * Нажимает первую точку сразу, вторую через 12 мс
     */
    fun performMacro(x1: Float, y1: Float, x2: Float, y2: Float) {
        // Первое нажатие — сразу
        click(x1, y1)

        // Второе нажатие через 12 мс
        Handler(Looper.getMainLooper()).postDelayed({
            click(x2, y2)
        }, 12)
    }

    private fun click(x: Float, y: Float) {
        val path = Path().apply {
            moveTo(x, y)
        }

        val stroke = GestureDescription.StrokeDescription(path, 0, 50) // 50 мс удержание
        val gesture = GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        dispatchGesture(gesture, null, null)
    }
}
