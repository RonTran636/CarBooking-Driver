package com.carbooking.driver.activity.login.verifyOtp

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PersistableBundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.carbooking.driver.R
import com.carbooking.driver.activity.home.DriverHomeActivity
import com.carbooking.driver.activity.login.relogin.ReLoginActivity
import com.carbooking.driver.activity.login.signup.SignUpActivity
import com.carbooking.driver.databinding.ActivityVerifyOtpBinding
import com.carbooking.driver.utils.Common
import com.google.android.gms.tasks.TaskExecutors
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_verify_otp.*
import java.util.concurrent.TimeUnit

class VerifyOtpActivity : AppCompatActivity() {

    private lateinit var userInfoRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var codeBySystem: String
    private lateinit var binding: ActivityVerifyOtpBinding
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var storedVerificationId: String? = null
    private var phoneNumber = ""
    private var msgAdded = false

    companion object {
        private const val TAG = "VerityOTP Activity"
        private const val KEY_VERIFICATION_ID = "Verification ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_verify_otp)
        AndroidBug5497Workaround.assistActivity(this)
        binding.pinView.requestFocus()

        //Get information from Intent
        phoneNumber = intent.getStringExtra("phoneNumber").toString()
        //Initialize Firebase
        auth = Firebase.auth
        userInfoRef = Firebase.database.reference.child(Common.DRIVER_INFO_REFERENCE)

        //Send Verification code for the first time
        sendVerificationCode(phoneNumber)
        binding.tvVerify.text = getString(R.string.title_verify, phoneNumber)

        if (storedVerificationId == null && savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState)
        }

        /** Add extra information for Verify title
         *
        var msgVerify = getString(R.string.title_verify, phoneNumber)
        message_verify.text = msgVerify

        val msgWrongNumber = SpannableString(getString(R.string.message_wrong_number))
        .setSpan(
        ForegroundColorSpan(R.color.accent_red),
        0,
        getString(R.string.message_wrong_number).length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )*/
        val timer = object : CountDownTimer(15000, 1000) {
            override fun onTick(p0: Long) {
                binding.tvOTPResend.text = getString(R.string.fui_resend_code_in, p0 / 1000)
            }

            override fun onFinish() {
                tvOTPResend.text = getString(R.string.fui_resend_code)
                tvOTPResend.isClickable = true
                //Handle Skip Button
                userInfoRef.child(auth.uid!!)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.value != null) {
                                //User are registered, allow to enter password to proceed, enable skip button
                                tv_skip.visibility = View.VISIBLE
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })

                //Append new text to title: Did you enter correct mobile number?
                Log.d(TAG, "onFinish: resend message is ${tv_verify.text}, $msgAdded")
                msgAdded = true
            }
        }
        timer.start()

        binding.btnVerifyOTP.setOnClickListener {
            codeBySystem = binding.pinView.text.toString().trim()
            if (validateOTP(codeBySystem)) {
                verifySignInCode(codeBySystem)
            }
        }
        binding.tvOTPResend.setOnClickListener {
            sendVerificationCode(phoneNumber)
            timer.start()
            binding.pinviewLayout.error = null
            tvOTPResend.isClickable = false
        }
        binding.tvSkip.setOnClickListener {
            val intent2 = Intent(this, ReLoginActivity::class.java)
            intent2.putExtra("phoneNumber", phoneNumber)
            startActivity(intent2)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        binding.changePhoneNumber.setOnClickListener { onBackPressed() }
        binding.backspace.setOnClickListener { onBackPressed() }
        binding.pinView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.pinviewLayout.error = null
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left)
    }

    // [START Verification]
    private fun sendVerificationCode(phoneNumber: String) {
        // [START start_phone_auth]
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber, // Phone number to verify
            30, // Timeout duration
            TimeUnit.SECONDS, // Unit of timeout
            TaskExecutors.MAIN_THREAD, // Activity (for callback binding)
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    val codeReceived = credential.smsCode
                    if (codeReceived != null) {
                        binding.pinView.setText(codeReceived.toString())
                        verifySignInCode(codeReceived)
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@VerifyOtpActivity, e.message, Toast.LENGTH_LONG).show()
                    Log.d(TAG, "onVerificationFailed: ${e.message}")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    storedVerificationId = verificationId
                    Log.d(TAG, "onCodeSent: Verification is $storedVerificationId")
                    resendToken = token
                }
            },
            resendToken
        ) // OnVerificationStateChangedCallbacks
        // [END start_phone_auth]
    }
    // [END Verification]

    private fun verifySignInCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)
        signInWithPhoneAuthCredential(credential)
    }

    // [START sign_in_with_phone]
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                when {
                    task.isSuccessful -> {
                        userInfoRef.child(auth.uid!!)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.value != null) {
                                        //User are registered, to the main activity
                                        startActivity(Intent(this@VerifyOtpActivity, DriverHomeActivity::class.java))
                                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                                        finish()
                                    } else {
                                        //New user, to the sign up activity
                                        startActivity(Intent(this@VerifyOtpActivity, SignUpActivity::class.java))
                                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                                        finish()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                }
                            })
                        Log.d(TAG, "signInWithPhoneAuthCredential: succeed")

                    }
                    task.exception is FirebaseAuthInvalidCredentialsException -> {
                        Log.d(TAG, "signInWithPhoneAuthCredential: Fail, ${task.exception}")
                        binding.pinviewLayout.error = getString(R.string.message_verify_invalid)
                        binding.pinView.setText("")
                    }
                    task.exception is Exception -> {
                        Toast.makeText(this, "Error ${task.exception}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }
    //[END sign_in_with_phone]

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putString(KEY_VERIFICATION_ID, storedVerificationId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        storedVerificationId = savedInstanceState.getString(KEY_VERIFICATION_ID).toString()
    }

    private fun validateOTP(code: String): Boolean {
        if (TextUtils.isEmpty(code) || code.length < 6) {
            binding.pinviewLayout.error = getString(R.string.message_verify_invalid)
            binding.pinView.setText("")
            return false
        }
        return true
    }

    class AndroidBug5497Workaround(activity: Activity) {
        private val mChildOfContent: View
        private var usableHeightPrevious = 0
        private val frameLayoutParams: ViewGroup.LayoutParams
        private fun possiblyResizeChildOfContent() {
            val usableHeightNow = computeUsableHeight()
            if (usableHeightNow != usableHeightPrevious) {
                val usableHeightSansKeyboard: Int = mChildOfContent.rootView.height
                val heightDifference = usableHeightSansKeyboard - usableHeightNow
                if (heightDifference > usableHeightSansKeyboard / 4) {
                    // keyboard probably just became visible
                    frameLayoutParams.height = usableHeightSansKeyboard - heightDifference
                } else {
                    // keyboard probably just became hidden
                    frameLayoutParams.height = usableHeightSansKeyboard
                }
                mChildOfContent.requestLayout()
                usableHeightPrevious = usableHeightNow
            }
        }

        private fun computeUsableHeight(): Int {
            val r = Rect()
            mChildOfContent.getWindowVisibleDisplayFrame(r)
            return r.bottom - r.top
        }

        companion object {
            // For more information, see https://issuetracker.google.com/issues/36911528
            // To use this class, simply invoke assistActivity() on an Activity that already has its content view set.
            fun assistActivity(activity: Activity) {
                AndroidBug5497Workaround(activity)
            }
        }

        init {
            val content = activity.findViewById<View>(android.R.id.content) as FrameLayout
            mChildOfContent = content.getChildAt(0)
            mChildOfContent.viewTreeObserver
                .addOnGlobalLayoutListener { possiblyResizeChildOfContent() }
            frameLayoutParams = mChildOfContent.layoutParams
        }
    }
}