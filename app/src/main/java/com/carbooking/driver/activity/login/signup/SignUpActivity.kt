package com.carbooking.driver.activity.login.signup

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.carbooking.driver.R
import com.carbooking.driver.activity.home.DriverHomeActivity
import com.carbooking.driver.databinding.ActivitySignUpBinding
import com.carbooking.driver.utils.Common
import com.carbooking.driver.activity.login.LoginActivity
import com.carbooking.driver.model.UserModel
import com.carbooking.driver.utils.HashUtils
import com.facebook.login.LoginManager
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class SignUpActivity : AppCompatActivity() {

    private lateinit var userInfoRef : DatabaseReference
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var user: FirebaseUser
    private var canProcess = true

    companion object{
        private const val TAG = "SignUpActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up)
        binding.lifecycleOwner = this
        binding.etNewUserName.requestFocus()
        userInfoRef = FirebaseDatabase.getInstance().getReference(Common.DRIVER_INFO_REFERENCE)
        auth = Firebase.auth
        user = auth.currentUser!!

        getUserProfile()


        binding.btnSignUp.setOnClickListener {
            signUp()
            refreshInputLayout()
        }
    }


    private fun getUserProfile() {
        // [START get_user_profile]
            // Name, email address, and profile photo Url
        binding.etNewUserName.setText(user.displayName)
        binding.etNewUserEmail.setText(user.email)
        binding.etNewUserPhoneNumber.setText(user.phoneNumber)
        // [END get_user_profile]
    }

    private fun signUp(){
        val model = UserModel()
        model.fullName = binding.etNewUserName.text.toString()
        model.email = binding.etNewUserEmail.text.toString()
        model.password = binding.etNewUserPassword.text.toString()
        model.phoneNumber = binding.etNewUserPhoneNumber.text.toString()
        model.password = binding.etNewUserPassword.text.toString()
        model.photoUrl = auth.currentUser!!.photoUrl.toString()

        //Validate Information:
        if (model.fullName.isEmpty()) {
            binding.newUserNameLayout.error = getString(R.string.error_field_empty, "First Name")
            canProcess=false
        }
        if (model.email.isEmpty()){
            binding.newUserEmailLayout.error = getString(R.string.error_field_empty, "Email")
            canProcess=false
        }
        if (!isValidPassword(model.password)){
            binding.newUserPasswordLayout.error = getString(R.string.error_invalid_password)
            canProcess=false
        }
        if (model.phoneNumber.isEmpty()|| model.phoneNumber.length>10|| model.phoneNumber.length<9) {
            binding.newUserPhoneNumberLayout.error = getString(R.string.message_phone_number_invalid)
            canProcess=false
        }
        //End of Validate Information

        if (canProcess){
            //Hash Password before store it
            model.password = HashUtils.sha256(model.password)
            Log.d(TAG, "signUp: ${model.password}")
            userInfoRef.child(user.uid)
                .setValue(model)
                .addOnFailureListener{ e->
                    Toast.makeText(this@SignUpActivity,"${e.message}",Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener{
                    Toast.makeText(this@SignUpActivity,getString(R.string.message_sign_up_successful),Toast.LENGTH_SHORT).show()
                    Common.currentUser = model
                    startActivity(Intent(this, DriverHomeActivity::class.java))
                    finish()
                }
        }
    }
    private fun removeError(editText: EditText,etLayout:TextInputLayout){
        editText.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                etLayout.error = null
                canProcess = true
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
    }

    private fun refreshInputLayout(){
        removeError(binding.etNewUserName,binding.newUserNameLayout)
        removeError(binding.etNewUserEmail,binding.newUserEmailLayout)
        removeError(binding.etNewUserPassword,binding.newUserPasswordLayout)
        removeError(binding.etNewUserPhoneNumber,binding.newUserPhoneNumberLayout)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        LoginManager.getInstance().logOut()
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        overridePendingTransition(R.anim.slide_out_right, R.anim.slide_in_left)
    }

    private fun isValidPassword(password: String?) : Boolean {
        password?.let {
            val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{6,}$"
            val passwordMatcher = Regex(passwordPattern)
            return passwordMatcher.find(password) != null
        } ?: return false
    }


}