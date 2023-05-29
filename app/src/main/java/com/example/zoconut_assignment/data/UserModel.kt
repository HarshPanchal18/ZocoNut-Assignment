package com.example.zoconut_assignment.data

data class UserModel(
    val userPicture: String? = null,
    val qrPicture: String? = null,
    val userId: String? = null,
    val name: String? = null,
    val mail: String? = null,
    val githubHandle: String? = null,
    val skills: String? = null,
    val contact: String? = null,
    val country: String? = null,
    val profileSaves: ArrayList<String> = arrayListOf()
)
