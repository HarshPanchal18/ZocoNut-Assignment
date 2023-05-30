package com.example.zoconut_assignment.ui

import android.os.Bundle
import android.util.Log
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
    val adapter = ContactAdapter(userContacts)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.contactRecycler.setHasFixedSize(true)
        binding.contactRecycler.layoutManager = LinearLayoutManager(this)

        dbReference =
            FirebaseDatabase.getInstance().getReference("users")

        fetchProfileSaves(user?.uid.toString())
    }

    private fun fetchUsers(profileSaves: List<String>) {
        val usersList: MutableList<UserModel> = mutableListOf()

        dbReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snap in dataSnapshot.children) {
                    val userModel = snap.getValue(UserModel::class.java)
                    userModel?.let {
                        val id = snap.key
                        if (id != null && profileSaves.contains(id))
                            usersList.add(userModel)
                    }
                }
                adapter.setUsers(usersList)
                Log.e("UserList", usersList.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    "ContactActivity",
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
                        val profileSaves = userModel.profileSaves
                        fetchUsers(profileSaves)
                        Log.e("Saves", userModel.profileSaves.toString())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(
                        "ContactActivity",
                        "Error fetching user details: ${error.message}"
                    )
                }
            })
    }
}
