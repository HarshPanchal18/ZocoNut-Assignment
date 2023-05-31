package com.example.zoconut_assignment.ui.login

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.zoconut_assignment.R
import com.example.zoconut_assignment.databinding.ActivityResetPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import com.jakewharton.rxbinding2.widget.RxTextView

class ResetPasswordActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityResetPasswordBinding

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        auth = FirebaseAuth.getInstance()

        // set email if user has entered his/her mail on login screen before entering into reset activity
        val passedMail = intent.getStringExtra("Mail")
        binding.etMail.setText(passedMail)

        val emailStream =
            RxTextView.textChanges(binding.etMail) // Creates an Observable for email box
                .skipInitialValue() // trim boundaries
                .map { mail ->
                    !Patterns.EMAIL_ADDRESS.matcher(mail).matches()
                } // maps each character whether the email is valid or not
        emailStream.subscribe { showEmailValidAlert(it) } // handle upon each character

        binding.resetPasswdBtn.setOnClickListener {
            val mail = binding.etMail.text.toString().trim()
            if (isOnline()) {
                auth.sendPasswordResetEmail(mail)
                    .addOnCompleteListener(this) { reset ->
                        if (reset.isSuccessful) {
                            Intent(this, LoginActivity::class.java).also {
                                it.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                showSuccessDialog("Password reset link is sent to your mail")
                                startActivity(it)
                            }
                            finish()
                        } else {
                            showErrorDialog(reset.exception?.message.toString())
                        }
                    }
            } else {
                try {
                    showErrorDialog(resources.getString(R.string.network_error_text))
                } catch (e: Exception) {
                    Log.e("Reset Password", e.message.toString())
                }
            }
        }

        binding.backToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun showEmailValidAlert(isNotValid: Boolean) {
        if (isNotValid) {
            binding.etMail.error = "Email is not valid"
            binding.resetPasswdBtn.isEnabled = false
            binding.resetPasswdBtn.backgroundTintList =
                ContextCompat.getColorStateList(this, android.R.color.darker_gray)
        } else {
            binding.etMail.error = null
            binding.resetPasswdBtn.isEnabled = true
            binding.resetPasswdBtn.backgroundTintList = ContextCompat.getColorStateList(
                this,
                R.color.primary_color
            )
        }
    }

    private fun isOnline(): Boolean {
        val conMgr =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = conMgr.activeNetworkInfo
        if (netInfo == null || !netInfo.isConnected || !netInfo.isAvailable) {
            //Toast.makeText(this, "No Internet connection!", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun showErrorDialog(message: String) {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("ERROR")
        builder.setMessage(message)
        builder.setIcon(R.drawable.error)
        builder.setNeutralButton("Okay") { _, _ -> }

        val alertDialog = builder.create() // Create the AlertDialog
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun showSuccessDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Done")
        builder.setMessage(message)
        builder.setIcon(R.drawable.done)
        builder.setNeutralButton("Okay") { _, _ -> }

        val alertDialog = builder.create() // Create the AlertDialog
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
}
