package jp.yuppe.camera2single

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.simpleName
    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private lateinit var textureView: TextureView
    private var isRecording: Boolean = false
    private lateinit var singleCameraManager: SingleCameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // パーミッションをリクエストする
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        textureView = findViewById(R.id.texture_view)
        singleCameraManager = SingleCameraManager(this, textureView)

        findViewById<Button>(R.id.takePhotoButton).apply {
            setOnClickListener {
                singleCameraManager.takePhoto(windowManager.defaultDisplay.rotation)
            }
        }

        findViewById<Button>(R.id.recordButton).apply {
            setOnClickListener {
                if (isRecording) {
                    singleCameraManager.stopRecording()
                    this.text = "Record Video"
                } else {
                    singleCameraManager.startRecording()
                    this.text = "Stop"
                }
                isRecording = !isRecording
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        singleCameraManager.onResume()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                singleCameraManager.onResume()
            } else {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
                //finish()

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.fromParts("package", this.packageName, null)
                    startActivity(intent)
                }
            }
        }
    }
}