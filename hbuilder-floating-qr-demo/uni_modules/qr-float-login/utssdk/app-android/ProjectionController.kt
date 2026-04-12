package uts.qrfloatlogin.android

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.io.FileOutputStream

object ProjectionController {
    private var mediaProjection: MediaProjection? = null
    private var projectionManager: MediaProjectionManager? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var displayMetrics: DisplayMetrics? = null

    fun createProjectionIntent(activity: android.app.Activity): Intent {
        val manager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return manager.createScreenCaptureIntent()
    }

    fun startSession(
        context: Context,
        resultCode: Int,
        resultData: Intent
    ): Boolean {
        release()
        projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager?.getMediaProjection(resultCode, resultData)

        val projection = mediaProjection ?: return false
        val metrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        displayMetrics = metrics

        imageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2
        )

        virtualDisplay = projection.createVirtualDisplay(
            "qr-float-capture-session",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            Handler(Looper.getMainLooper())
        )
        return virtualDisplay != null
    }

    fun captureAndDecode(
        context: Context,
        onSuccess: (String?) -> Unit,
        onError: (String) -> Unit
    ) {
        val reader = imageReader
        val metrics = displayMetrics
        if (reader == null || metrics == null || virtualDisplay == null) {
            onError("截屏会话未启动，请重新申请截屏权限")
            return
        }

        try {
            val image = reader.acquireLatestImage() ?: run {
                onError("当前还没有可用画面，请等待 1 秒后再试")
                return
            }
            image.use {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * metrics.widthPixels

                val bitmap = Bitmap.createBitmap(
                    metrics.widthPixels + rowPadding / pixelStride,
                    metrics.heightPixels,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                val fullBitmap = Bitmap.createBitmap(bitmap, 0, 0, metrics.widthPixels, metrics.heightPixels)
                val focusRect = buildCenterRect(metrics.widthPixels, metrics.heightPixels)
                val centerBitmap = Bitmap.createBitmap(
                    fullBitmap,
                    focusRect.left,
                    focusRect.top,
                    focusRect.width(),
                    focusRect.height()
                )
                val savedPath = saveCaptureBitmap(context, fullBitmap)
                if (savedPath.isNotBlank()) {
                    QrFloatNative.setLatestCapturePath(savedPath)
                }

                val successCallback = onSuccess
                val errorCallback = onError
                QrDecoder.decode(centerBitmap, { result ->
                    if (result.isNullOrBlank()) {
                        QrDecoder.decode(fullBitmap, { fullResult ->
                            centerBitmap.recycle()
                            fullBitmap.recycle()
                            bitmap.recycle()
                            successCallback(fullResult)
                        }, { message ->
                            centerBitmap.recycle()
                            fullBitmap.recycle()
                            bitmap.recycle()
                            errorCallback(message)
                        })
                    } else {
                        centerBitmap.recycle()
                        fullBitmap.recycle()
                        bitmap.recycle()
                        successCallback(result)
                    }
                }, { message ->
                    centerBitmap.recycle()
                    fullBitmap.recycle()
                    bitmap.recycle()
                    errorCallback(message)
                })
            }
        } catch (t: Throwable) {
            onError(t.message ?: "截屏解析失败")
        }
    }

    fun release() {
        imageReader?.close()
        imageReader = null
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        mediaProjection = null
        projectionManager = null
        displayMetrics = null
    }

    private fun buildCenterRect(width: Int, height: Int): Rect {
        val rectWidth = (width * 0.7f).toInt()
        val rectHeight = (height * 0.55f).toInt()
        val left = ((width - rectWidth) / 2).coerceAtLeast(0)
        val top = ((height - rectHeight) / 2).coerceAtLeast(0)
        return Rect(left, top, left + rectWidth, top + rectHeight)
    }

    private fun saveCaptureBitmap(context: Context, bitmap: Bitmap): String {
        return try {
            val dir = File(context.cacheDir, "qr-captures")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, "capture-${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
            }
            file.absolutePath
        } catch (_: Throwable) {
            ""
        }
    }
}
