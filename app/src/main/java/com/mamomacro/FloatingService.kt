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

        // Координаты (захардкожены)
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
            .setContentText("Кнопка Play активна")
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
            // Разрешаем мультитач внутри контейнера
            isSplitTouchEnabled = true
            // Обход блокировки ненадежных касаний для Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setAllowClickWhenObscured(true)
            }
        }

        // Чёрный треугольник Play на белом круге (как YouTube)
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

        // Настройка параметров окна с флагами мультитача
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 400
        }

        // Перетаскивание + клик
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 12 || Math.abs(dy) > 12) isClick = false
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        triggerMacro()
                    }
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

        // Пас сразу, Удар через 15 мс
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
        removeAll()
    }
}
