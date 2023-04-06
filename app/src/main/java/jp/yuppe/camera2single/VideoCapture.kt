package jp.yuppe.camera2single

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VideoCapture(context: Context, cameraDevice: CameraDevice) {
    var cameraDevice: CameraDevice
    private val context: Context
    private val TAG = VideoCapture::class.simpleName
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var imageCapture: ImageCapture

    private lateinit var previewSurface: Surface
    private lateinit var previewSize: Size
    private lateinit var backgroundHandler: Handler

    init {
        this.context = context
        this.cameraDevice = cameraDevice
        this.mediaRecorder = MediaRecorder()
    }

    fun startPreview(previewSurface: Surface, previewSize: Size, backgroundHandler: Handler) {
        this.previewSurface = previewSurface
        this.previewSize = previewSize
        this.backgroundHandler = backgroundHandler

        val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(previewSurface)

        cameraDevice.createCaptureSession(
            listOf(previewSurface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                }

                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler)
                }
            },
            null
        )
    }

    fun takePhoto(rotation: Int) {
        val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(previewSurface)

        imageCapture = ImageCapture(context, previewSize, backgroundHandler)

        cameraDevice.createCaptureSession(
            listOf(previewSurface, imageCapture.imageReader.surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                }

                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler)

                    imageCapture.takePhoto(cameraDevice, cameraCaptureSession, rotation)
                }
            },
            null
        )
    }

    fun startRecording(surfaceTexture: SurfaceTexture?, previewSize: Size, videoSeize: Size, backgroundHandler: Handler) {
        setupMediaRecorder(context, videoSeize)

        surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(surfaceTexture)
        val recordingSurface = mediaRecorder.surface
        val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        captureRequestBuilder.addTarget(previewSurface)
        captureRequestBuilder.addTarget(recordingSurface)

        cameraDevice.createCaptureSession(
            listOf(previewSurface, recordingSurface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    Log.e(TAG, "Configuration failed")
                }

                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                    try {
                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler)
                        mediaRecorder.start()
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                        Log.e(TAG, "Failed to start camera preview because it couldn't access the camera")
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                    }
                }
            },
            backgroundHandler
        )
    }

    fun stopRecording() {
        mediaRecorder.stop()
        mediaRecorder.reset()
    }

    private fun setupMediaRecorder(context: Context, videoSize: Size) {
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder.setVideoSize(videoSize.width, videoSize.height)
        mediaRecorder.setVideoFrameRate(30)
        mediaRecorder.setOutputFile(createFile(context, "mp4").absolutePath)
        mediaRecorder.setVideoEncodingBitRate(10_000_000)
        mediaRecorder.prepare()
    }

    private fun createFile(context: Context, ext: String): File {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
        val file = File(context.filesDir, "${sdf.format(Date())}.$ext")

        // /data/user/0/{namespacing}/files/yyyy_MM_dd_HH_mm_ss_SSS.ext
        Log.d(TAG, "*** path: ${file.absolutePath}")
        return file
    }

}