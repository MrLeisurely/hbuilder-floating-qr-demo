package uts.qrfloatlogin.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build

object QrFloatNative {
    const val REQUEST_MEDIA_PROJECTION = 8421

    private var projectionResultCode: Int? = null
    private var projectionData: Intent? = null
    private var projectionConsumed = false

    private var latestResultText: String? = null
    private var latestResultFormat: String = "QR_CODE"
    private var latestStatusMessage: String = "未启动"
    private var latestCapturePath: String = ""

    private var onDetectedCallback: ((String, String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    fun registerEventBridge(
        onDetected: ((String, String) -> Unit)?,
        onError: ((String) -> Unit)?
    ) {
        onDetectedCallback = onDetected
        onErrorCallback = onError
    }

    fun hasProjectionPermission(): Boolean {
        return projectionResultCode != null && projectionData != null && !projectionConsumed
    }

    fun createProjectionIntent(activity: Activity): Intent {
        return ProjectionController.createProjectionIntent(activity)
    }

    fun handleProjectionResult(activity: Activity, resultCode: Int, data: Intent?): Boolean {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return false
        }
        projectionResultCode = resultCode
        projectionData = data
        projectionConsumed = false
        val started = ProjectionController.startSession(activity.applicationContext, resultCode, data)
        if (!started) {
            projectionResultCode = null
            projectionData = null
            projectionConsumed = true
            notifyError("启动截屏会话失败")
            return false
        }
        notifyStatus("截屏会话已就绪")
        return true
    }

    fun startFloatService(context: Context): Boolean {
        if (!hasProjectionPermission()) {
            notifyError("未获取截屏授权")
            return false
        }
        notifyStatus("正在启动悬浮窗服务")
        val intent = Intent(context, FloatWindowService::class.java).apply {
            action = FloatWindowService.ACTION_START
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            notifyStatus("悬浮窗服务启动请求已发送")
        } catch (t: Throwable) {
            notifyError("启动悬浮窗服务失败: ${t.message ?: "未知错误"}")
            return false
        }
        return true
    }

    fun stopFloatService(context: Context) {
        context.stopService(Intent(context, FloatWindowService::class.java))
        ProjectionController.release()
        projectionResultCode = null
        projectionData = null
        projectionConsumed = true
        notifyStatus("悬浮窗已关闭")
    }

    fun captureAndDecodeOnce(
        context: Context,
        onSuccess: (String?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (projectionResultCode == null || projectionData == null) {
            onError("尚未获取截屏授权")
            return
        }
        ProjectionController.captureAndDecode(
            context,
            onSuccess,
            {
                notifyError(it)
                onError(it)
            }
        )
    }

    fun notifyDetected(text: String, format: String = "QR_CODE") {
        latestResultText = text
        latestResultFormat = format
        onDetectedCallback?.invoke(text, format)
    }

    fun notifyStatus(message: String) {
        latestStatusMessage = message
        onErrorCallback?.invoke(message)
    }

    fun notifyError(message: String) {
        latestStatusMessage = message
        onErrorCallback?.invoke(message)
    }

    fun getLatestResultText(): String? = latestResultText

    fun getLatestResultFormat(): String = latestResultFormat

    fun getLatestStatusMessage(): String = latestStatusMessage

    fun clearLatestResult() {
        latestResultText = null
        latestResultFormat = "QR_CODE"
    }

    fun setLatestCapturePath(path: String) {
        latestCapturePath = path
    }

    fun getLatestCapturePath(): String = latestCapturePath

    fun invalidateProjectionPermission() {
        projectionConsumed = true
        projectionResultCode = null
        projectionData = null
        ProjectionController.release()
    }
}
