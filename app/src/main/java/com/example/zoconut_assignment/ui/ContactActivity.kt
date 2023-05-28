package com.example.zoconut_assignment.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.zoconut_assignment.adapters.ContactAdapter
import com.example.zoconut_assignment.data.UserModel
import com.example.zoconut_assignment.databinding.ActivityContactBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ContactActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactBinding
    private lateinit var dbReference: DatabaseReference
    private val user = FirebaseAuth.getInstance().currentUser
    private var users = ArrayList<UserModel?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.contactRecycler.setHasFixedSize(true)
        binding.contactRecycler.layoutManager = LinearLayoutManager(this)

        dbReference =
            FirebaseDatabase.getInstance().getReference("users")

        val allRef = dbReference.database.reference.child("users")
        allRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (snap in snapshot.children) {
                    val userModel = snap.getValue(UserModel::class.java)
                    if (userModel?.userId != user?.uid)
                        users.add(userModel)
                }
                binding.contactRecycler.adapter = ContactAdapter(users)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
