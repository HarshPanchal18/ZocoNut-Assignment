package com.example.zoconut_assignment.ui.login

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.zoconut_assignment.R
import com.example.zoconut_assignment.databinding.ActivityLoginBinding
import com.example.zoconut_assignment.ui.HomeActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityLoginBinding

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Authentication
        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(Scopes.EMAIL))
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val userNameStream =
            RxTextView.textChanges(binding.etMail)// Creates an Observable for email box
                .skipInitialValue() // trim boundaries
                .map { username -> username.isEmpty() } // maps each character whether the email is valid or not
        userNameStream.subscribe {
            showTextMinimalAlert(
                it,
                "Email/Username"
            )
        } // handle upon each character

        val passwordStream =
            RxTextView.textChanges(binding.etPassword) // Creates an Observable for password box
                .skipInitialValue() // trim boundaries
                .map { password -> password.isEmpty() } // maps each character whether the password is empty or not
        passwordStream.subscribe {
            showTextMinimalAlert(
                it,
                "Password"
            )
        } // handle upon each character


        // Button Enable/Disable
        val invalidFieldsStream: Observable<Boolean> = Observable.combineLatest(
            userNameStream,
            passwordStream
        ) { usernameInvalid: Boolean, passwordInvalid: Boolean ->
            !usernameInvalid && !passwordInvalid
        }

        invalidFieldsStream.subscribe { isValid: Boolean ->
            if (isValid) {
                binding.loginbtn.isEnabled = true
                binding.loginbtn.backgroundTintList = ContextCompat.getColorStateList(
                    this,
                    R.color.primary_color
                )
            } else {
                binding.loginbtn.isEnabled = false
                binding.loginbtn.backgroundTintList =
                    ContextCompat.getColorStateList(this, android.R.color.darker_gray)
            }
        }

        binding.loginbtn.setOnClickListener {
            val mail = binding.etMail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()
            if (isOnline()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    loginUser(mail, pass)
                }
            } else {
                try {
                    showErrorDialog(resources.getString(R.string.network_error_text))
                } catch (e: Exception) {
                    Log.e("Login Exception", e.message.toString())
                }
            }
        }

        binding.googlesignbtn.setOnClickListener {
            if (isOnline()) {
                signInGoogle()
            } else {
                try {
                    showErrorDialog(resources.getString(R.string.network_error_text))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        binding.tvHaventAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ResetPasswordActivity::class.java)
            intent.putExtra("Mail", binding.etMail.text.toString().trim())
            startActivity(intent)
            finish()
        }
    }

    private fun signInGoogle() {
        googleSignInClient.signOut() // Sign out the previous logged user first
        val signInIntent = googleSignInClient.signInIntent // Starts the Google Sign In Flow
        launcher.launch(signInIntent)
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleResults(task)
            }
        }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            val account: GoogleSignInAccount? = task.result
            if (account != null) {
                updateUI(account)
            }
        } else {
            showErrorDialog(task.exception?.message.toString())
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        // Get user credentials by token and sign in if the creds are valid
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                showErrorDialog(it.exception?.message.toString())
            }
        }
    }

    private fun loginUser(mail: String, pass: String) {
        auth.signInWithEmailAndPassword(mail, pass)
            .addOnCompleteListener(this) { login ->
                if (login.isSuccessful) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Verify if user has been verified by pressing link given through mail
                        checkMailVerification()
                    }
                } else {
                    showErrorDialog(login.exception?.message.toString())
                }
            }
    }

    private fun checkMailVerification() {
        val user = auth.currentUser
        if (user?.isEmailVerified == true) {
            finish()
            Intent(this, HomeActivity::class.java).also {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(it)
            }
        } else {
            showErrorDialog("Seems like you have not verified your mail yet!")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        200,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            }
            auth.signOut()
        }
    }

    private fun showTextMinimalAlert(isNotValid: Boolean, text: String) {
        if (text == "Email/Username")
            binding.etMail.error = if (isNotValid) "$text cannot be empty!" else null
        else if (text == "Password")
            binding.etPassword.error = if (isNotValid) "$text cannot be empty!" else null
    }

    private fun isOnline(): Boolean { // Check Internet connections
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network =
            connectivityManager.activeNetwork // returns a 'Network' object representing the currently active network.
        // retrieve the network capabilities using the getNetworkCapabilities() method which returns a NetworkCapabilities object that provides information about the network.
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        // NET_CAPABILITY_INTERNET capability, indicates that the device has an internet connection available or not
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
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

}
