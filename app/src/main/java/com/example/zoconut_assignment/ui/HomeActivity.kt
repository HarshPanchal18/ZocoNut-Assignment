package com.example.zoconut_assignment.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.zoconut_assignment.R
import com.example.zoconut_assignment.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
