package com.example.zoconut_assignment.ui

import android.os.Bundle
import android.util.Log
import android.view.View
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
    private var userContacts = ArrayList<UserModel?>()
    val adapter = ContactAdapter(this,userContacts)
    private val profileSaves: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.contactRecycler.setHasFixedSize(true)
        binding.contactRecycler.layoutManager = LinearLayoutManager(this)
        binding.backBtn.setOnClickListener { finish() }

        dbReference =
            FirebaseDatabase.getInstance().getReference("users")

        fetchProfileSaves(user?.uid.toString())
        binding.contactRecycler.adapter = adapter
    }

    private fun fetchUsers(profileSaves: List<String>) {
        val usersList: ArrayList<UserModel> = arrayListOf()
        dbReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snap in dataSnapshot.children) {
                    val userModel = snap.getValue(UserModel::class.java)
                    userModel?.let {
                        val id = snap.key
                        // Store users into usersList for adapter
                        if (id != null && profileSaves.contains(id))
                            usersList.add(userModel)
                    }
                }
                if(usersList.size == 0)
                    binding.emptyContacts.visibility = View.VISIBLE
                adapter.setUsers(usersList) // Set Adapter after storing users
                Log.e("UserList", usersList.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    "DatabaseError",
                    "Error fetching user details: ${error.message}"
                )
            }
        })
    }

    private fun fetchProfileSaves(userId: String) {
        dbReference.child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userModel = snapshot.getValue(UserModel::class.java)
                    userModel?.let {
                        val profileSaves = userModel.profileSaves.distinct()
                        fetchUsers(profileSaves) // Store each userID to fetch every userID's data
                        Log.d("Profiles", userModel.profileSaves.toString())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(
                        "DatabaseError",
                        "Error fetching user details: ${error.message}"
                    )
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        profileSaves.clear()
        adapter.notifyDataSetChanged()
    }
}
