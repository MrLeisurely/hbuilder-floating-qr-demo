package uts.qrfloatlogin.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

class FloatWindowService : Service() {

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        private const val CHANNEL_ID = "qr_float_channel"
        private const val NOTIFICATION_ID = 10021
        private const val TAG = "QrFloatWindowService"
    }

    private var windowManager: WindowManager? = null
    private var floatView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                QrFloatNative.notifyStatus("收到关闭悬浮窗请求")
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                QrFloatNative.notifyStatus("悬浮窗服务已进入 onStartCommand")
                startForeground(NOTIFICATION_ID, buildNotification())
                showFloatWindow()
                return START_STICKY
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatWindow()
    }

    private fun buildNotification(): Notification {
        createChannel()
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("悬浮扫码已开启")
            .setContentText("切到目标应用二维码页后，点击悬浮球识别")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
        if (launchIntent != null) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                1,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pendingIntent)
        }
        return builder.build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "悬浮扫码服务",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun showFloatWindow() {
        if (!Settings.canDrawOverlays(this)) {
            QrFloatNative.notifyError("悬浮窗权限未授予")
            stopSelf()
            return
        }
        if (floatView != null) {
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams().apply {
            width = dp(72)
            height = dp(72)
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(180)
        }

        val floatButton = TextView(this).apply {
            text = "扫"
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xE62F7D57.toInt())
                setStroke(dp(2), 0xFFF6E7C2.toInt())
            }
            elevation = dp(8).toFloat()
        }

        floatButton.setOnClickListener {
            QrFloatNative.captureAndDecodeOnce(
                context = this,
                onSuccess = { text ->
                    if (text.isNullOrBlank()) {
                        QrFloatNative.notifyError("未识别到二维码")
                    } else {
                        QrFloatNative.notifyDetected(text)
                    }
                },
                onError = { message ->
                    QrFloatNative.notifyError(message)
                }
            )
        }

        floatButton.setOnTouchListener(createDragListener(params))
        floatView = floatButton
        try {
            windowManager?.addView(floatButton, params)
            Log.d(TAG, "float window added")
            QrFloatNative.notifyStatus("悬浮窗已显示")
        } catch (t: Throwable) {
            Log.e(TAG, "addView failed", t)
            QrFloatNative.notifyError("悬浮窗显示失败: ${t.message ?: "未知错误"}")
            stopSelf()
        }
    }

    private fun createDragListener(params: WindowManager.LayoutParams): View.OnTouchListener {
        return object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchX = 0f
            private var touchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x
                        startY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        params.x = startX + (event.rawX - touchX).toInt()
                        params.y = startY + (event.rawY - touchY).toInt()
                        windowManager?.updateViewLayout(floatView, params)
                        return true
                    }
                }
                return false
            }
        }
    }

    private fun removeFloatWindow() {
        floatView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Throwable) {
            }
        }
        floatView = null
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
