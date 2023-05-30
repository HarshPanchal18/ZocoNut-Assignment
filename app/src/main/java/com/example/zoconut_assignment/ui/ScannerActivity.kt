package com.example.zoconut_assignment.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
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
import com.google.firebase.auth.FirebaseAuth
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
    private lateinit var dbReference: DatabaseReference
    val children: MutableList<String> = mutableListOf()
    private var value: UserModel? = UserModel()
    private val user = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbReference =
            FirebaseDatabase.getInstance().getReference("users")

        dbReference.database.reference.child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (snap in snapshot.children) {
                        // Collecting all user's userIds into an array for checking if scanned profile's userId is registered or not
                        children.add(snap.key.toString())
                    }
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
        }
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
                    //Log.d("MainActivity", "Scanned User Code: $children")
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
        val sbinding = ScannedProfileBinding.inflate(LayoutInflater.from(this), null, false)
        val builder = AlertDialog.Builder(this)
        builder.setView(sbinding.root)

        dbReference.child(qrCodeData.toString()).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                value = snapshot.getValue(UserModel::class.java)
                if (value?.userPicture?.isNotEmpty() == true)
                    sbinding.profileImage.loadImage(value?.userPicture)
                sbinding.profileName.text = value?.name
                sbinding.profileGithub.text = value?.githubHandle
                sbinding.profileSkills.text = value?.skills
                sbinding.profileContact.text = value?.contact
                sbinding.profileCountry.text = value?.country

                sbinding.saveProfileBtn.setOnClickListener {

                    dbReference =
                        FirebaseDatabase.getInstance()
                            .getReference("users/${user?.uid}/profileSaves")
                    val arrayTicketRef = FirebaseDatabase.getInstance().reference.child("users/${user?.uid}/profileSaves")
                    arrayTicketRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val length = dataSnapshot.childrenCount.toInt()
                            arrayTicketRef.child(length.toString()).setValue(value?.userId)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        applicationContext,
                                        "Contact saved to your profile",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                    //Log.i("Profile", profileBookmarks.toString())
                                }.addOnFailureListener {
                                    Toast.makeText(
                                        applicationContext,
                                        it.message.toString(),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            // Handle error
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Create the AlertDialog
        val alertDialog = builder.create()
        alertDialog.setCancelable(true)
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
