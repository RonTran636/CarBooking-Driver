package com.carbooking.driver.activity.login.relogin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.carbooking.driver.R
import com.carbooking.driver.activity.home.DriverHomeActivity
import com.carbooking.driver.databinding.ActivityReLoginBinding
import com.carbooking.driver.utils.Common
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ReLoginActivity : AppCompatActivity() {

    private lateinit var userInfoRef : DatabaseReference
    private lateinit var binding: ActivityReLoginBinding
    companion object{
        private const val TAG = "ReLoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_re_login)
        binding.etConfirmPassword.requestFocus()
        userInfoRef = Firebase.database.reference.child(Common.DRIVERS).child(Common.DRIVER_INFO_REFERENCE)
        binding.btnReLogin.setOnClickListener {
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            Log.d(TAG, "onCreate: Confirm Password entered is $confirmPassword")
            userInfoRef.addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val passCode = snapshot.child("password").value.toString().trim()
                    Log.d(TAG, "onDataChange: $passCode")
                    if (passCode == confirmPassword) {
                        //Password confirmed, moving on to Main Activity
                        startActivity(Intent(this@ReLoginActivity, DriverHomeActivity::class.java))
                        finish()
                    } else {
                        //Wrong password, require enter again
                        binding.confirmPasswordLayout.error =  getString(R.string.fui_error_invalid_password)
                        binding.etConfirmPassword.setText("")
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
        }
        binding.backspace.setOnClickListener { onBackPressed() }
        binding.etConfirmPassword.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.confirmPasswordLayout.error = null
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })

    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_out_right,R.anim.slide_in_left)
    }
}