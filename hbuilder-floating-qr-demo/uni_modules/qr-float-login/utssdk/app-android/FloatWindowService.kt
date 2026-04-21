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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
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

    private enum class PanelState {
        CONFIRM_LOGOUT,
        READY_TO_LOGIN
    }

    private var windowManager: WindowManager? = null
    private var floatView: View? = null
    private var currentState: PanelState = PanelState.CONFIRM_LOGOUT
    private val mainHandler = Handler(Looper.getMainLooper())
    private var captureInProgress = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                QrFloatNative.notifyStatus("收到关闭悬浮窗请求")
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                currentState = PanelState.CONFIRM_LOGOUT
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
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(180)
        }

        val rootLayout = FrameLayout(this).apply {
            elevation = dp(8).toFloat()
        }

        fun renderState() {
            rootLayout.removeAllViews()
            when (currentState) {
                PanelState.CONFIRM_LOGOUT -> {
                    val bar = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(10), dp(10), dp(10), dp(10))
                        background = GradientDrawable().apply {
                            cornerRadius = dp(8).toFloat()
                            setColor(0xFF4A4A4A.toInt())
                        }
                    }

                    val exitButton = Button(this).apply {
                        text = "退出"
                        setTextColor(0xFFFFFFFF.toInt())
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        typeface = Typeface.DEFAULT_BOLD
                        background = GradientDrawable().apply {
                            cornerRadius = dp(4).toFloat()
                            setColor(0xFFAA6B1C.toInt())
                        }
                        setPadding(dp(14), dp(8), dp(14), dp(8))
                        setOnClickListener {
                            stopSelf()
                        }
                    }

                    val tips = TextView(this).apply {
                        text = "如果游戏是已经登录状态？请点击右上角退出登录"
                        setTextColor(0xFFEFEFEF.toInt())
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        setLineSpacing(dp(2).toFloat(), 1f)
                        setPadding(dp(10), 0, dp(10), 0)
                    }

                    val confirmButton = Button(this).apply {
                        text = "确认已退出个人账号"
                        setTextColor(0xFFFFFFFF.toInt())
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        typeface = Typeface.DEFAULT_BOLD
                        background = GradientDrawable().apply {
                            cornerRadius = dp(6).toFloat()
                            setColor(0xFFFFB24B.toInt())
                        }
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                        setOnClickListener {
                            currentState = PanelState.READY_TO_LOGIN
                            renderState()
                            QrFloatNative.notifyStatus("已切换到上号准备状态")
                        }
                    }

                    bar.addView(exitButton, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ))
                    bar.addView(tips, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    bar.addView(confirmButton, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ))
                    rootLayout.addView(bar, FrameLayout.LayoutParams(dp(340), ViewGroup.LayoutParams.WRAP_CONTENT))
                }

                PanelState.READY_TO_LOGIN -> {
                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(10), dp(10), dp(10), dp(10))
                        background = GradientDrawable().apply {
                            cornerRadius = dp(6).toFloat()
                            setColor(0xFFFFFFFF.toInt())
                        }
                    }

                    val closeRow = LinearLayout(this).apply {
                        gravity = Gravity.END
                    }

                    val closeButton = TextView(this).apply {
                        text = "×"
                        gravity = Gravity.CENTER
                        setTextColor(0xFFFFFFFF.toInt())
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(0xFFC9C9C9.toInt())
                        }
                        setPadding(dp(6), dp(2), dp(6), dp(2))
                        setOnClickListener {
                            stopSelf()
                        }
                    }
                    closeRow.addView(closeButton)

                    val loginButton = Button(this).apply {
                        text = "⚡ 点我上号"
                        setTextColor(0xFFFFFFFF.toInt())
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                        typeface = Typeface.DEFAULT_BOLD
                        isAllCaps = false
                        setSingleLine(true)
                        ellipsize = TextUtils.TruncateAt.END
                        minHeight = dp(42)
                        background = GradientDrawable().apply {
                            cornerRadius = dp(4).toFloat()
                            setColor(0xFFFFB24B.toInt())
                        }
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                        setOnClickListener {
                            triggerCaptureWithHiddenOverlay()
                        }
                    }

                    val hintRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER
                        setPadding(dp(8), dp(8), dp(8), dp(8))
                        background = GradientDrawable().apply {
                            cornerRadius = dp(4).toFloat()
                            setColor(0xFFB8B8B8.toInt())
                        }
                    }

                    val hintPrefix = TextView(this).apply {
                        text = "打开QQ扫码授权页 | "
                        setTextColor(0xFFF4F4F4.toInt())
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                        setSingleLine(true)
                        ellipsize = TextUtils.TruncateAt.END
                    }

                    val hintAction = TextView(this).apply {
                        text = "扫码"
                        setTextColor(0xFF2D8CFF.toInt())
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                        setSingleLine(true)
                        ellipsize = TextUtils.TruncateAt.END
                        typeface = Typeface.DEFAULT_BOLD
                    }

                    hintRow.addView(hintPrefix, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ))
                    hintRow.addView(hintAction, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ))

                    card.addView(closeRow)
                    card.addView(loginButton, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(6)
                    })
                    card.addView(hintRow, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(8)
                    })

                    rootLayout.addView(card, FrameLayout.LayoutParams(dp(176), ViewGroup.LayoutParams.WRAP_CONTENT))
                }
            }
        }

        renderState()

        rootLayout.setOnTouchListener(createDragListener(params))
        floatView = rootLayout
        try {
            windowManager?.addView(rootLayout, params)
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

    private fun triggerCaptureWithHiddenOverlay() {
        if (captureInProgress) {
            QrFloatNative.notifyStatus("正在截图识别，请稍候")
            return
        }
        captureInProgress = true
        setFloatWindowVisible(false)
        QrFloatNative.notifyStatus("悬浮窗已隐藏，准备截图")
        mainHandler.postDelayed({
            QrFloatNative.captureAndDecodeOnce(
                context = this@FloatWindowService,
                onSuccess = { text ->
                    restoreFloatWindowAfterCapture()
                    if (text.isNullOrBlank()) {
                        QrFloatNative.notifyError("未识别到二维码")
                    } else {
                        QrFloatNative.notifyDetected(text)
                    }
                },
                onError = { message ->
                    restoreFloatWindowAfterCapture()
                    QrFloatNative.notifyError(message)
                }
            )
        }, 220)
    }

    private fun restoreFloatWindowAfterCapture() {
        captureInProgress = false
        setFloatWindowVisible(true)
        QrFloatNative.notifyStatus("悬浮窗已恢复显示")
    }

    private fun setFloatWindowVisible(visible: Boolean) {
        val view = floatView ?: return
        view.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        try {
            windowManager?.updateViewLayout(view, view.layoutParams as WindowManager.LayoutParams)
        } catch (_: Throwable) {
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
