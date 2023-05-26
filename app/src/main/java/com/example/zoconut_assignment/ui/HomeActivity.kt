package com.example.zoconut_assignment.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.zoconut_assignment.R
import com.example.zoconut_assignment.data.UserModel
import com.example.zoconut_assignment.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.system.exitProcess

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var dbReference: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private var auth = FirebaseAuth.getInstance()
    private val user = auth.currentUser
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbReference =
            FirebaseDatabase.getInstance().getReference(user?.uid.toString()).child("userProfile")


        getRealtimeData()
        binding.scannerBtn.setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        binding.apply {

            profileImage.loadImage(user?.photoUrl.toString())
            nameBox.setText(user?.displayName.toString())
            mailBox.setText(user?.email.toString())

            saveAsQrBtn.setOnClickListener {
                pushOnDatabase()
            }

            editBtn.setOnClickListener {
                editImage.visible()
                tilPhone.isEnabled = true
                tilMail.isEnabled = true
                tilName.isEnabled = true
                tilGithub.isEnabled = true
                tilCountry.isEnabled = true
                tilSkills.isEnabled = true
                saveAsQrBtn.isEnabled = true
                it.isEnabled = false
            }
        }
    }

    private fun getRealtimeData() {
        dbReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(UserModel::class.java)
                binding.githubBox.setText(value?.githubHandle)
                binding.skillBox.setText(value?.skills)
                binding.phoneBox.setText(value?.contact)
                binding.countryBox.setText(value?.country)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun pushOnDatabase() {
        val name = binding.nameBox.text.toString()
        val github = binding.githubBox.text.toString()
        val skills = binding.skillBox.text.toString()
        val phone = binding.phoneBox.text.toString()
        val country = binding.countryBox.text.toString()

        if (binding.phoneBox.text?.isEmpty() == true)
            binding.phoneBox.error = "Phone number is mandatory"
        if (name.isEmpty())
            binding.nameBox.error = "Username is mandatory"
        if (github.isEmpty())
            binding.githubBox.error = "Github Username is mandatory"

        val userData =
            UserModel(user?.uid.toString(), name, user?.email, github, skills, phone, country)
        dbReference.setValue(userData)
            .addOnSuccessListener {
                this.showToast("Data inserted successfully")
                binding.apply {
                    editImage.gone()
                    tilPhone.isEnabled = false
                    tilMail.isEnabled = false
                    tilName.isEnabled = false
                    tilGithub.isEnabled = false
                    tilCountry.isEnabled = false
                    tilSkills.isEnabled = false
                    saveAsQrBtn.isEnabled = false
                    editBtn.isEnabled = true
                }
            }.addOnFailureListener {
                this.showToast(it.message.toString())
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
