package com.example.zoconut_assignment.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraX
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysisConfig
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.zoconut_assignment.databinding.ActivityScannerBinding
import com.example.zoconut_assignment.helpers.QRCodeAnalyzer

class ScannerActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
    }

    private lateinit var binding: ActivityScannerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            baseContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (isCameraPermissionGranted()) {
                binding.textureView.post { startCamera() }
            } else {
                Toast.makeText(
                    this,
                    "Permission for camera is required to scan the QR",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val previewConfig = PreviewConfig.Builder()
            .setLensFacing(CameraX.LensFacing.BACK)
            .build()

        val preview = Preview(previewConfig)

        preview.setOnPreviewOutputUpdateListener { previewOutput ->
            val parent = binding.textureView.parent as ViewGroup
            parent.removeView(binding.textureView)
            binding.textureView.setSurfaceTexture(previewOutput.surfaceTexture)
            parent.addView(binding.textureView,0)
        }

        val imageAnalysisConfig = ImageAnalysisConfig.Builder().build()
        val imageAnalysis = ImageAnalysis(imageAnalysisConfig)

        val qrCodeAnalyzer = QRCodeAnalyzer { qrCodes ->
            qrCodes.forEach{ barcode ->
                Log.i("HomeActivity","Detected QR: ${barcode.rawValue}")
            }
        }
        imageAnalysis.analyzer = qrCodeAnalyzer

        CameraX.bindToLifecycle(this as LifecycleOwner, preview, imageAnalysis)
    }
}
