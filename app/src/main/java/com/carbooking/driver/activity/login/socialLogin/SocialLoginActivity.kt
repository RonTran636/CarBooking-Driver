package com.carbooking.driver.activity.login.socialLogin

import android.content.Intent
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Transition
import android.util.Log
import android.view.Window
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.carbooking.driver.R
import com.carbooking.driver.activity.home.DriverHomeActivity
import com.carbooking.driver.activity.login.signup.SignUpActivity
import com.carbooking.driver.databinding.ActivitySocialLoginBinding
import com.carbooking.driver.utils.Common
import com.carbooking.driver.model.UserModel
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.BuildConfig
import com.google.firebase.ktx.Firebase


class SocialLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userInfoRef: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private lateinit var binding: ActivitySocialLoginBinding

    companion object {
        private const val TAG = "LoginSocialActivity"
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(window) {
            requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
            sharedElementExitTransition = returnTransition()
        }

        if (BuildConfig.DEBUG) {
            FacebookSdk.setIsDebugEnabled(true)
            FacebookSdk.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS)
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_social_login)
        binding.constraintLayout.animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)

        //[Set up Firebase environment]
        auth = Firebase.auth
        userInfoRef = FirebaseDatabase.getInstance().getReference(Common.DRIVER_INFO_REFERENCE)
        callbackManager = CallbackManager.Factory.create()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        //HANDLE LOGIN
        binding.loginFacebook.setOnClickListener { signInFacebook() }
        binding.loginGoogle.setOnClickListener { signInGoogle() }

        binding.backspace.setOnClickListener { onBackPressed() }
    }

    // [START auth_with_google]
    private fun signInGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    // [START auth_with_facebook]
    private fun signInFacebook(){
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                Log.d(TAG, "onSuccess:Facebook Id: ${result.accessToken.userId}")
                Log.d(TAG, "onSuccess: Access Token: ${result.accessToken.token}")
                val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                handleAccessToken(credential)
            }

            override fun onCancel() {
                Log.d(TAG, "onCancel: ")
            }

            override fun onError(error: FacebookException?) {
                Log.e(TAG, "onError: $error")
            }
        })
    }

    private fun handleAccessToken(credential: AuthCredential){
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    userInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.value != null) {
                                    //User are registered, to the main activity
                                    Common.currentUser = snapshot.getValue(UserModel::class.java)
                                    Log.d(TAG, "onDataChange: ${Common.currentUser!!.photoUrl}")
                                    Log.d(TAG, "onDataChange: ${snapshot.value}")
                                    startActivity(Intent(this@SocialLoginActivity, DriverHomeActivity::class.java))
                                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                                    finish()
                                } else {
                                    //New user, to the sign up activity
                                    startActivity(Intent(this@SocialLoginActivity, SignUpActivity::class.java))
                                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                            }
                        })
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
    // [START onActivityResult]
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
        //Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                val credential = GoogleAuthProvider.getCredential(account.idToken!!,null)
                Log.d(TAG, "firebaseAuthWithGoogle: ${account.idToken}")
                handleAccessToken(credential)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }
    // [END onActivityResult]

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }


    // [END auth_with_facebook]
    private fun returnTransition(): Transition? {
        val bounds = ChangeBounds()
        bounds.interpolator = DecelerateInterpolator()
        bounds.duration = 2000
        return bounds
    }
}