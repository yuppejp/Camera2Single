package jp.yuppe.camera2single

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView

class SingleCameraManager(activity: Activity, textureView: TextureView) {
    private val TAG = SingleCameraManager::class.simpleName
    private var activity: Activity
    private var textureView: TextureView
    private lateinit var cameraManager: CameraManager
    private lateinit var videoCapture: VideoCapture
    private lateinit var cameraId: String
    private lateinit var previewSize: Size
    private lateinit var videoSize: Size
    private var shouldProceedWithOnResume: Boolean = true
    private lateinit var backgroundHandlerThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    init {
        this.activity = activity
        this.textureView = textureView
        startBackgroundThread()
    }

    fun onResume() {
        startBackgroundThread()

        if (textureView.isAvailable && shouldProceedWithOnResume) {
            setupCamera()
        } else if (!textureView.isAvailable) {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                @SuppressLint("MissingPermission")
                override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                    Log.e(TAG, "*** SurfaceTextureListener@onSurfaceTextureAvailable")
                    setupCamera()
                    connectCamera()
                }

                override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
                    Log.e(TAG, "*** SurfaceTextureListener@onSurfaceTextureSizeChanged")
                }

                override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                    Log.e(TAG, "*** SurfaceTextureListener@onSurfaceTextureDestroyed")
                    return true
                }

                override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
                    //Log.e(TAG, "*** SurfaceTextureListener@onSurfaceTextureUpdated")
                }
            }
        }

        shouldProceedWithOnResume = !shouldProceedWithOnResume
    }

    fun setupCamera() {
        cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIds: Array<String> = cameraManager.cameraIdList

        for (id in cameraIds) {
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(id)

            // If we want to choose the rear facing camera instead of the front facing one
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                continue
            }

            val streamConfigurationMap: StreamConfigurationMap? = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            if (streamConfigurationMap != null) {
                previewSize = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                    .getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.height * it.width }!!
                videoSize = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                    .getOutputSizes(MediaRecorder::class.java).maxByOrNull { it.height * it.width }!!
            }
            cameraId = id
        }
    }

    @SuppressLint("MissingPermission")
    fun connectCamera() {
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                videoCapture = VideoCapture(activity, camera, textureView, previewSize, backgroundHandler)
                videoCapture.startPreview()
            }

            override fun onDisconnected(cameraDevice: CameraDevice) {
            }

            override fun onError(cameraDevice: CameraDevice, error: Int) {
                val errorMsg = when (error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                Log.e(TAG, "Error when trying to connect camera $errorMsg")
            }
        }, backgroundHandler)
    }

    /**
     * Background Thread
     */
    private fun startBackgroundThread() {
        backgroundHandlerThread = HandlerThread("CameraVideoThread")
        backgroundHandlerThread.start()
        backgroundHandler = Handler(backgroundHandlerThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundHandlerThread.quitSafely()
        backgroundHandlerThread.join()
    }

    fun startRecording() {
        videoCapture.startRecording(videoSize)
    }

    fun stopRecording() {
        videoCapture.stopRecording()
    }

    fun takePhoto(rotation: Int) {
        videoCapture.takePhoto(rotation)
    }

}