package com.example.zoconut_assignment.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.zoconut_assignment.R
import com.example.zoconut_assignment.data.UserModel
import com.example.zoconut_assignment.databinding.ActivityScannerBinding
import com.example.zoconut_assignment.databinding.ScannedProfileBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult

class ScannerActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
    }

    private lateinit var binding: ActivityScannerBinding
    private lateinit var alertBinding: ScannedProfileBinding
    private lateinit var dbReference: DatabaseReference
    val children: MutableList<String> = mutableListOf()
    private var value: UserModel? = UserModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbReference =
            FirebaseDatabase.getInstance().getReference("users")

        dbReference.database.reference.child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (snap in snapshot.children)
                        children.add(snap.key.toString())
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        askPermissionOrInitialize()
    }

    private fun askPermissionOrInitialize() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else
            initializeScanner()
    }

    private fun initializeScanner() {
        binding.barcodeScanner.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.let {
                    // Handle the scanned QR code result
                    val qrCodeData = result.text
                    if (children.contains(qrCodeData)) {
                        previewProfileOf(qrCodeData)
                    }
                    Log.d("MainActivity", "Scanned QR Code: $qrCodeData")
                    Log.d("MainActivity", "Scanned User Code: $children")
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
                // Optional callback for possible result points
            }
        })

        binding.barcodeScanner.setStatusText("Scan QR Code")

        binding.previewView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    binding.barcodeScanner.resume()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error starting camera preview: ${e.message}")
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                binding.barcodeScanner.pause()
            }
        })
    }

    private fun previewProfileOf(qrCodeData: String?) {
        /*val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        val sbinding = ScannedProfileBinding.bind(
            LayoutInflater.from(this).inflate(
                R.layout.scanned_profile,
                this.findViewById<RelativeLayout>(R.id.scannedProfile)
            )
        )
        builder.setView(sbinding.root)

        dbReference.child(qrCodeData.toString()).addValueEventListener(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    value = snapshot.getValue(UserModel::class.java)
                    if (value?.userPicture?.isNotEmpty() == true)
                        sbinding.profileImage.loadImage(value?.userPicture)
                    sbinding.profileName.text = value?.name
                    sbinding.profileGithub.text = value?.githubHandle
                    sbinding.profileSkills.text = value?.skills
                    sbinding.profileContact.text = value?.contact
                    sbinding.profileCountry.text = value?.country
                }

                override fun onCancelled(error: DatabaseError) {}
            }
        )
        val alertDialog = builder.create()
        builder.setPositiveButton("Save to contacts") { _, _ -> }
        builder.setNegativeButton("Dismiss") { _, _ -> }

        if (alertDialog.window != null) {
            alertDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }
        alertDialog.setCancelable(true)
        alertDialog.show()*/

        val builder = AlertDialog.Builder(this)
        builder.setTitle(qrCodeData)
        builder.setMessage(qrCodeData)
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        builder.setPositiveButton("Yes") { _, _ ->
            Toast.makeText(applicationContext, "clicked yes", Toast.LENGTH_LONG).show()
        }
        builder.setNegativeButton("No") { _, _ -> }

        // Create the AlertDialog
        val alertDialog: AlertDialog = builder.create()
        // Set other dialog properties
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeScanner()
            } else {
                // Camera permission not granted, handle accordingly
                Toast.makeText(
                    this,
                    "Please grant permission for Camera to Scan QR",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun ImageView.loadImage(url: String?) {
        Glide.with(this)
            .load(url)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.ic_account)
            .error(R.drawable.error)
            .into(this)
    }
}
