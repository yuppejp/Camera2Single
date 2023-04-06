package jp.yuppe.camera2single

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ImageCapture(context: Context, previewSize: Size, handler: Handler) {
    var imageReader: ImageReader
    private var context: Context
    private var orientations: SparseIntArray = SparseIntArray(4).apply {
        append(Surface.ROTATION_0, 0)
        append(Surface.ROTATION_90, 90)
        append(Surface.ROTATION_180, 180)
        append(Surface.ROTATION_270, 270)
    }

    init {
        this.context = context

        imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener(object : ImageReader.OnImageAvailableListener {
            override fun onImageAvailable(reader: ImageReader) {
                Toast.makeText(context, "Photo Taken!", Toast.LENGTH_SHORT).show()
                val image: Image = reader.acquireLatestImage()
                saveImageToDisk(image)
                image.close()
            }
        }, handler)

    }

    fun takePhoto(cameraDevice: CameraDevice, cameraCaptureSession: CameraCaptureSession, rotation: Int) {
        var captureRequestBuilder: CaptureRequest.Builder

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder.addTarget(imageReader.surface)
        val rotation = rotation
        captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientations.get(rotation))
        cameraCaptureSession.capture(
            captureRequestBuilder.build(),
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureStarted(session: CameraCaptureSession, request: CaptureRequest, timestamp: Long, frameNumber: Long) {}
                override fun onCaptureProgressed(session: CameraCaptureSession, request: CaptureRequest, partialResult: CaptureResult) {}
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {}
            }, null
        )
    }

    private fun saveImageToDisk(image: Image) {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.JAPAN)
        val file = File(context.filesDir, "${sdf.format(Date())}.jpg")
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val output = FileOutputStream(file)
        output.write(bytes)
        image.close()
        output.close()
    }

}