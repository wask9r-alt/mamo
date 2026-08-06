package com.mamomacro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat

class FloatingService : Service() {

    companion object {
        const val ACTION_SHOW_PLAY = "SHOW_PLAY"

        // === КООРДИНАТЫ КЛИКА МАКРОСА ===
        const val PASS_X = 2058f
        const val PASS_Y = 657f
        const val SHOOT_X = 2054f
        const val SHOOT_Y = 1050f
    }

    private lateinit var windowManager: WindowManager
    private var playView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHOW_PLAY) {
            showPlayButton()
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = "mamomacro_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MamoMacro",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MamoMacro")
            .setContentText("Умная кнопка активна")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        startForeground(1, notification)
    }

    private fun showPlayButton() {
        removeAllViews()

        val size = 140
        val view = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
            }
            elevation = 12f
        }

        val playIcon = View(this).apply {
            background = object : android.graphics.drawable.Drawable() {
                override fun draw(canvas: android.graphics.Canvas) {
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.BLACK
                        style = android.graphics.Paint.Style.FILL
                    }
                    val path = android.graphics.Path()
                    val w = bounds.width().toFloat()
                    val h = bounds.height().toFloat()
                    path.moveTo(w * 0.32f, h * 0.22f)
                    path.lineTo(w * 0.32f, h * 0.78f)
                    path.lineTo(w * 0.78f, h * 0.50f)
                    path.close()
                    canvas.drawPath(path, paint)
                }
                override fun setAlpha(alpha: Int) {}
                override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
                override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
            }
        }

        view.addView(playIcon, FrameLayout.LayoutParams(size, size))

        // FLAG_WATCH_OUTSIDE_TOUCH — заставляет окно слушать клики по всему экрану
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 2145
            y = 1466
        }

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                // Если кликнули прямо по самой кнопке
                MotionEvent.ACTION_DOWN -> {
                    triggerMacro()
                    true
                }
                // Ловим касания ВНЕ кнопки (когда палец водит джойстик или отпускает его)
                MotionEvent.ACTION_OUTSIDE -> {
                    // Перемещаем кнопку ближе к месту движения (на основе rawX / rawY)
                    // Чтобы она "улетала" или подстраивалась, как в оригинале
                    params.x = (event.rawX).toInt() - (size / 2)
                    params.y = (event.rawY).toInt() - (size / 2)
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, params)
        playView = view
    }

    private fun triggerMacro() {
        val service = MacroAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, "Accessibility не включён!", Toast.LENGTH_SHORT).show()
            return
        }
        service.performMacro(PASS_X, PASS_Y, SHOOT_X, SHOOT_Y)
    }

    private fun removeAllViews() {
        playView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            playView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeAllViews()
    }
}
