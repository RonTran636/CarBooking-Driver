package com.carbooking.driver.activity.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.carbooking.driver.R
import com.carbooking.driver.activity.onboarding.OnBoardingActivity
import com.carbooking.driver.databinding.ActivitySplashBinding
import com.carbooking.driver.utils.Common
import com.carbooking.driver.activity.home.DriverHomeActivity
import com.example.common.model.UserModel
import com.example.common.utils.UserUtils
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener

    private lateinit var database :FirebaseDatabase
    private lateinit var userInfoRef: DatabaseReference
    private val handler by lazy { Handler(Looper.myLooper()!!) }

    companion object {
        private const val TAG = "SplashActivity"
        private const val RC_SIGN_IN = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) { }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)
//        revealBlack(binding.container, binding.logoImage)
//        checkPermissions()
        init()
    }


    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(listener)
        delaySplashScreen()
    }

    private fun checkUserFromFirebase() {
        userInfoRef
            .child(auth.currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val model = snapshot.getValue(UserModel::class.java)
                    Common.currentUser = model
                    startActivity(Intent(this@SplashActivity, DriverHomeActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "onCancelled: ${error.message}")
                }
            })
    }


    private fun delaySplashScreen() {
        handler.postDelayed({}, 500)
    }

    private fun init() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        userInfoRef = database.getReference(Common.DRIVER_INFO_REFERENCE)
        listener = FirebaseAuth.AuthStateListener {
            if (auth.currentUser != null) {
                //User Logged in - Move to HomeActivity
                checkUserFromFirebase()
                FirebaseInstanceId.getInstance()
                    .instanceId
                    .addOnFailureListener { e -> Toast.makeText(this, e.message, Toast.LENGTH_LONG).show() }
                    .addOnSuccessListener { instanceIdResult ->
                        UserUtils.updateToken(this@SplashActivity, instanceIdResult.token)
                    }
            } else {
                //New user detected - Move to LoginActivity
                startActivity(Intent(this@SplashActivity, OnBoardingActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
            }
        }
    }
}