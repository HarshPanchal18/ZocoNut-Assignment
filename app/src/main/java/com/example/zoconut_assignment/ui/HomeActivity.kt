package com.example.zoconut_assignment.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.zoconut_assignment.R
import com.example.zoconut_assignment.data.UserModel
import com.example.zoconut_assignment.databinding.ActivityHomeBinding
import com.example.zoconut_assignment.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.ByteArrayOutputStream
import kotlin.system.exitProcess

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var dbReference: DatabaseReference
    private var auth = FirebaseAuth.getInstance()
    private val user = auth.currentUser
    private var storageRef: StorageReference = FirebaseStorage.getInstance().getReference("images/")
    private var imageURI: Uri? = Uri.EMPTY
    private var imageURL: String? = null
    private var qrURL: String? = null
    private var value: UserModel? = UserModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbReference =
            FirebaseDatabase.getInstance().getReference("users").child(user?.uid.toString())

        binding.apply {

            profileImage.loadImage(user?.photoUrl.toString())
            nameBox.setText(user?.displayName.toString())
            mailBox.setText(user?.email.toString())

            saveAsQrBtn.setOnClickListener {
                createAndUploadQR()
                pushOnDatabase()
            }

            editBtn.setOnClickListener {
                editImage.visible()
                tilPhone.isEnabled = true
                tilName.isEnabled = true
                tilGithub.isEnabled = true
                tilCountry.isEnabled = true
                tilSkills.isEnabled = true
                saveAsQrBtn.isEnabled = true
            }
        }

        dbReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Assign database values to the views
                value = snapshot.getValue(UserModel::class.java)
                if (value?.userPicture?.isNotEmpty() == true)
                    binding.profileImage.loadImage(value?.userPicture)
                binding.nameBox.setText(value?.name)
                binding.githubBox.setText(value?.githubHandle)
                binding.skillBox.setText(value?.skills)
                binding.phoneBox.setText(value?.contact)
                binding.countryBox.setText(value?.country)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DatabaseError", error.message)
            }
        })

        binding.scannerBtn.setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        binding.editImage.setOnClickListener {
            val galleryIntent = Intent().apply {
                action = Intent.ACTION_GET_CONTENT
                type = "image/*"
            }
            startActivityForResult(galleryIntent, 2)
        }

        binding.logoutBtn.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Logout")
            builder.setMessage("Are you sure you want to Logout?")
            builder.setIcon(R.drawable.ic_logout)
            builder.setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                Intent(this, LoginActivity::class.java).also {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(it)
                }
                finish()
            }
            builder.setNegativeButton("No") { _, _ -> }

            val alertDialog = builder.create() // Create the AlertDialog
            alertDialog.setCancelable(false)
            alertDialog.show()
        }

        binding.profileImage.setOnClickListener {
            // Pops up QR code
            val dialogView: View = LayoutInflater.from(this).inflate(R.layout.qr_dialog_image, null)
            Glide.with(this).load(value?.qrPicture)
                .into(dialogView.findViewById(R.id.previewedImage))
            AlertDialog.Builder(this).setView(dialogView).show()
        }

        binding.contactListBtn.setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
        }
    }

    private fun createAndUploadQR() {
        val qrRef = storageRef.child("${user?.uid}_qr")
        qrRef.putBytes(getQRBitmap()).addOnSuccessListener {
            qrRef.downloadUrl.addOnSuccessListener { qrUri ->
                qrURL = qrUri.toString()
                Log.e("QR", qrURL.toString())
                binding.imageUploading.gone()
                binding.saveAsQrBtn.isEnabled = true
            }.addOnFailureListener {
                Log.e("QRLinkException", it.message.toString())
            }
        }.addOnFailureListener {
            Log.e("QRRefException", it.message.toString())
        }.addOnProgressListener {
            binding.imageUploading.visible()
            binding.saveAsQrBtn.isEnabled = false
        }
    }

    private fun getQRBitmap(): ByteArray {
        val barcodeEncoder = BarcodeEncoder()
        // Generate QRCode in Bitmap format
        // Attach current user's userID, Specify Format, Give width-height
        val qrcode = barcodeEncoder.encodeBitmap(user?.uid, BarcodeFormat.QR_CODE, 500, 500)
        val byteArrayOpStream = ByteArrayOutputStream()
        qrcode.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOpStream)
        return byteArrayOpStream.toByteArray()
    }

    private fun uploadProfileImageToFirebaseStorage(imageUri: Uri) {
        val profileRef = storageRef.child("${user?.uid}")
        profileRef.putFile(imageUri).addOnSuccessListener {
            profileRef.downloadUrl.addOnSuccessListener { imageUri ->
                imageURL = imageUri.toString()
                Log.d("ImageURL", "Image URL: $imageURL")
                binding.imageUploading.gone()
                binding.saveAsQrBtn.isEnabled = true
            }.addOnFailureListener {
                Log.e("ImageURLException", "Image Exception: ${it.message.toString()}")
            }
        }.addOnFailureListener { exception ->
            // Handle the image upload failure
            Log.e("ImageUploadException", "Image upload failed: ${exception.message}")
        }.addOnProgressListener {
            binding.imageUploading.visible()
            binding.saveAsQrBtn.isEnabled = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2 && data?.data != null) {
            imageURI = data.data
            binding.profileImage.loadImage(imageURI.toString()) // set Image after selecting
            imageURI?.let { uploadProfileImageToFirebaseStorage(it) }
        }
    }

    private fun pushOnDatabase() {
        val name = binding.nameBox.text.toString()
        val github = binding.githubBox.text.toString()
        val skills = binding.skillBox.text.toString()
        val phone = binding.phoneBox.text.toString()
        val country = binding.countryBox.text.toString()

        if (value?.userPicture?.isNotEmpty() == true && imageURL == null)
            imageURL = value?.userPicture
        if (value?.qrPicture?.isNotEmpty() == true && qrURL == null)
            qrURL = value?.qrPicture

        val userData =
            UserModel(
                imageURL,
                qrURL,
                user?.uid.toString(),
                name,
                user?.email,
                github,
                skills,
                phone,
                country
            )
        dbReference.setValue(userData)
            .addOnSuccessListener {
                Log.i("MainActivity", imageURL.toString())
                Log.i("MainActivity", qrURL.toString())
                this.showToast("Data inserted successfully")
                binding.apply {
                    editImage.gone()
                    imageUploading.gone()
                    tilPhone.isEnabled = false
                    tilName.isEnabled = false
                    tilGithub.isEnabled = false
                    tilCountry.isEnabled = false
                    tilSkills.isEnabled = false
                    saveAsQrBtn.isEnabled = false
                }
            }.addOnFailureListener {
                this.showToast(it.message.toString())
            }
    }

    private fun ImageView.loadImage(url: String?) {
        Glide.with(this)
            .load(url)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.error)
            .error(R.drawable.error)
            .into(this)
    }

    // Ask again for exit
    private var backPressedTime: Long = 0
    override fun onBackPressed() {

        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
            exitProcess(0)
        }
        this.showToast("Press again to exit")
        backPressedTime = System.currentTimeMillis()
    }

    private fun Context.showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun View.visible() {
        visibility = View.VISIBLE
    }

    private fun View.gone() {
        visibility = View.GONE
    }
}
