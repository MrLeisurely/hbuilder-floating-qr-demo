package uts.qrfloatlogin.android

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.TimeUnit

object QrDecoder {
    private val mainHandler = Handler(Looper.getMainLooper())

    private val scanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    fun decode(
        bitmap: Bitmap,
        onSuccess: (String?) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val barcodes = Tasks.await(scanner.process(image), 3, TimeUnit.SECONDS)
                val first = barcodes.firstOrNull()
                mainHandler.post {
                    onSuccess(first?.rawValue)
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    onError(t.message ?: "二维码识别失败")
                }
            }
        }.start()
    }
}
