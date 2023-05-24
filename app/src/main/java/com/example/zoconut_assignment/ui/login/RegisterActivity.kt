package com.example.zoconut_assignment.ui.login

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.zoconut_assignment.R
import com.example.zoconut_assignment.databinding.ActivityRegisterBinding
import com.example.zoconut_assignment.databinding.ErrorDialogBinding
import com.example.zoconut_assignment.databinding.SuccessDialogBinding
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

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
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

        // Validation starts here
        val emailStream = RxTextView.textChanges(binding.etMail)
            .skipInitialValue()
            .map { mail -> !Patterns.EMAIL_ADDRESS.matcher(mail).matches() }
        emailStream.subscribe { showEmailValidAlert(it) }

        /*val userNameStream=RxTextView.textChanges(et_username)
            .skipInitialValue()
            .map { username -> username.length < 6 }
        userNameStream.subscribe { showTextMinimalAlert(it,"Username") }*/

        val passwordStream = RxTextView.textChanges(binding.etPassword)
            .skipInitialValue()
            .map { password -> password.length < 8 }
        passwordStream.subscribe { showTextMinimalAlert(it, "Password") }

        val passwordConfirmStream =
            Observable.merge(
                RxTextView.textChanges(binding.etPassword).skipInitialValue()
                    .map { password -> password.toString() != binding.etConfPassword.text.toString() },

                RxTextView.textChanges(binding.etConfPassword).skipInitialValue()
                    .map { confirmPassword -> confirmPassword.toString() != binding.etPassword.text.toString() })
        passwordConfirmStream.subscribe { showPasswordConfirmAlert(it) }

        // Button Enable/Disable
        val invalidFieldsStream: Observable<Boolean> = Observable.combineLatest(
            emailStream,
            passwordStream,
            passwordConfirmStream
        ) { emailInvalid: Boolean,
            passwordInvalid: Boolean,
            cpasswordInvalid: Boolean
            ->
            !emailInvalid && !passwordInvalid && !cpasswordInvalid
        }

        invalidFieldsStream.subscribe { isValid: Boolean ->
            if (isValid) {
                binding.registerbtn.isEnabled = true
                binding.registerbtn.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.primary_color)
            } else {
                binding.registerbtn.isEnabled = false
                binding.registerbtn.backgroundTintList =
                    ContextCompat.getColorStateList(this, android.R.color.darker_gray)
            }
        }

        binding.registerbtn.setOnClickListener {
            val mail = binding.etMail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (isOnline()) {
                registerUser(mail, pass)
            } else {
                try {
                    showErrorDialog(resources.getString(R.string.network_error_text))
                } catch (e: Exception) {
                    showErrorDialog(e.message.toString())
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
                    showErrorDialog(e.message.toString())
                }
            }
        }

        binding.tvHaveAccount.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun signInGoogle() {
        googleSignInClient.signOut()
        val signInIntent = googleSignInClient.signInIntent
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
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                showErrorDialog(it.exception?.message.toString())
            }
        }
    }

    private fun registerUser(mail: String, pass: String) {
        auth.createUserWithEmailAndPassword(mail, pass)
            .addOnCompleteListener(this) {
                if (it.isSuccessful) {
                    showSuccessDialog("Registered successfully")
                    sendMailVerification()
                } else {
                    showErrorDialog(it.exception?.message.toString())
                }
            }
    }

    private fun sendMailVerification() {
        val user = auth.currentUser
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener {
                //showSuccessDialog("Verification mail has been sent, verify and login")
                Toast.makeText(
                    applicationContext,
                    "Verification mail is sent, verify and login again",
                    Toast.LENGTH_SHORT
                ).show()
                auth.signOut()
                finish()
            }
        } else {
            showErrorDialog("Failed to sent a mail")
        }
    }

    private fun showTextMinimalAlert(isNotValid: Boolean, enteredtext: String) {
        if (enteredtext == "Password")
            binding.etPassword.error =
                if (isNotValid) "$enteredtext must be more than 8 letters!" else null
    }

    private fun showEmailValidAlert(isNotValid: Boolean) {
        binding.etMail.error = if (isNotValid) "Email is not valid!" else null
    }

    private fun showPasswordConfirmAlert(isNotValid: Boolean) {
        binding.etPassword.error = if (isNotValid) "Password are not the same" else null
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

    private fun showErrorDialog(message: String) {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        val ebinding: ErrorDialogBinding = ErrorDialogBinding.bind(
            LayoutInflater.from(this)
                .inflate(
                    R.layout.error_dialog,
                    findViewById<ConstraintLayout>(R.id.layoutDialogContainer)
                )
        )

        builder.setView(ebinding.root)
        ebinding.textTitle.text = resources.getString(R.string.network_error_title)
        ebinding.textMessage.text = message
        ebinding.buttonAction.text = resources.getString(R.string.okay)
        ebinding.imageIcon.setImageResource(R.drawable.error)

        val alertDialog = builder.create()
        ebinding.buttonAction.setOnClickListener { alertDialog.dismiss() }

        if (alertDialog.window != null) {
            alertDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }

        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun showSuccessDialog(message: String) {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        val sbinding: SuccessDialogBinding = SuccessDialogBinding.bind(
            LayoutInflater.from(this)
                .inflate(
                    R.layout.success_dialog,
                    this.findViewById<ConstraintLayout>(R.id.layoutDialogContainer)
                )
        )

        builder.setView(sbinding.root)
        sbinding.textTitle.text = resources.getString(R.string.success_title)
        sbinding.textMessage.text = message
        sbinding.buttonAction.text = resources.getString(R.string.okay)
        sbinding.imageIcon.setImageResource(R.drawable.done)

        val alertDialog = builder.create()
        sbinding.buttonAction.setOnClickListener {
            alertDialog.dismiss()
            finish()
        }

        if (alertDialog.window != null) {
            alertDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
}
