package com.example.zoconut_assignment.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.zoconut_assignment.R
import com.example.zoconut_assignment.databinding.ActivityContactBinding

class ContactActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
