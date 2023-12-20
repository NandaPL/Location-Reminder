package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.utils.Constants

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {
    private val TAG = AuthenticationActivity::class.java.simpleName
    private lateinit var dataBinding: ActivityAuthenticationBinding
    private lateinit var mViewModel: AuthenticationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataBinding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(dataBinding.root)

        mViewModel = ViewModelProvider(this)[AuthenticationViewModel::class.java]

        dataBinding.btnAuthenticationLogin.setOnClickListener {
            loginFLow()
        }

        onAuthenticationState()
    }

    /**
     * Give users the option to login or register with their email or Google account. If users
     * choose to register with their email, they will need to create a password as well.
     *
     * Create and launch sign-in intent. We listen to the response of this activity with the
     * SIGN_IN_RESULT_CODE code.
     */
    private fun loginFLow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        startActivityForResult(
            AuthUI
                .getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(), Constants.SIGN_IN_RESULT_CODE
        )
    }

    private fun onAuthenticationState(){
        mViewModel.authenticationState.observe(this) {
            when (it) {
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> {
                    startActivity(Intent(this, RemindersActivity::class.java))
                    Log.i(TAG, "User successfully login in the app")
                }
                else -> {
                    Log.e(TAG, "Login unsuccessful")
                }
            }
        }
    }
}