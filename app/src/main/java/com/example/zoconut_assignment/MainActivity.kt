package com.example.zoconut_assignment

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import com.example.zoconut_assignment.databinding.ActivityMainBinding
import com.example.zoconut_assignment.ui.HomeActivity
import com.example.zoconut_assignment.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        // finally change the color
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE

        Handler(Looper.getMainLooper()).postDelayed({
            auth = FirebaseAuth.getInstance()

            val user = auth.currentUser

            if (isOnline() && user != null)
                startActivity(Intent(this, HomeActivity::class.java))
            else
                startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 2000)
    }

    private fun isOnline(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network =
            connectivityManager.activeNetwork // returns a 'Network' object representing the currently active network.
        // retrieve the network capabilities using the getNetworkCapabilities() method which returns a NetworkCapabilities object that provides information about the network.
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        // NET_CAPABILITY_INTERNET capability, indicates that the device has an internet connection available or not
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
