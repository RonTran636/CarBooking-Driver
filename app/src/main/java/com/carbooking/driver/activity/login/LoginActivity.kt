package com.carbooking.driver.activity.login

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.PersistableBundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.transition.ChangeBounds
import android.transition.Transition
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.DataBindingUtil
import com.carbooking.driver.R
import com.carbooking.driver.activity.login.socialLogin.SocialLoginActivity
import com.carbooking.driver.activity.login.verifyOtp.VerifyOtpActivity
import com.carbooking.driver.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase



class LoginActivity : AppCompatActivity(),View.OnClickListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private lateinit var rootRef : DatabaseReference
    private var storedVerificationId: String? = ""
    private var countryCode = ""
    private var phoneNumber = ""

    companion object {
        private const val TAG = "PhoneAuthActivity"
        private const val RC_SIGN_IN = 101
        private const val KEY_VERIFICATION_ID = "FireBase Auth ID"
        private const val RESULT_OK = 111
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        with(window){
            requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
            sharedElementExitTransition = enterTransition()
            sharedElementEnterTransition = enterTransition()
            sharedElementReturnTransition = returnTransition()
        }
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.etNumberSendOtp.requestFocus()

        // [START initialize_auth]
        // Initialize Firebase Auth
        auth = Firebase.auth
        rootRef = Firebase.database.reference.child("user")
        // [END initialize_auth]

        // [Start assign country code click listener]
        binding.ccAustralia.setOnClickListener(this)
        binding.ccKorean.setOnClickListener(this)
        binding.ccSingapore.setOnClickListener(this)
        binding.ccVietnam.setOnClickListener(this)
        binding.tvOtherLoginMethod.setOnClickListener(this)
        // [End assign country code click listener]


        countryCode = binding.countryCode.text.toString().trim()
        binding.ccp.setOnClickListener {
            binding.countryCodeLayout.visibility=View.VISIBLE
            binding.countryList.animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)
        }

        // Restore instance state
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState)
        }

        binding.btnSendOTP.setOnClickListener {
            phoneNumber = binding.etNumberSendOtp.text.toString().trim()
            Log.d(TAG, "onCreate: PhoneNumber is: $phoneNumber")
            if (validatePhoneNumber(phoneNumber)) {
                phoneNumber = fullNumberWithPlus()
                val intent = Intent(this@LoginActivity, VerifyOtpActivity::class.java)
                intent.putExtra("phoneNumber", phoneNumber)
                Log.d(TAG, "Sending phone number: $phoneNumber")
                startActivityForResult(intent, RC_SIGN_IN)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        binding.etNumberSendOtp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.etPhoneNumberLayout.error = null
            }
            override fun afterTextChanged(p0: Editable?) {
            }
        })

        binding.backspace.setOnClickListener { onBackPressed() }

    }

    override fun onResume() {
        super.onResume()
        AndroidBug5497Workaround.assistActivity(this)
    }

    private fun validatePhoneNumber(phoneNumber: String): Boolean {
        if (TextUtils.isEmpty(phoneNumber)||phoneNumber.length>10||phoneNumber.length<9) {
            binding.etPhoneNumberLayout.error = getString(R.string.message_phone_number_invalid)
            return false
        }
        return true
    }


    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putString(KEY_VERIFICATION_ID, storedVerificationId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        storedVerificationId = savedInstanceState.getString(KEY_VERIFICATION_ID)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent().apply {
            putExtra("countryCode", countryCode)
            val image = binding.countryFlag.drawable.toBitmap()
            putExtra("countryFlag", image)
            putExtra("countryCode", countryCode)
        }
        setResult(RESULT_OK, intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
    //[END sign_in_with_phone]
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

    override fun onClick(view: View) {
        when (view.id) {
            R.id.cc_australia -> {
                binding.countryCodeLayout.visibility = View.GONE
                binding.countryFlag.setImageResource(R.drawable.ic_australia_flag)
                countryCode = binding.ccAustraliaCode.text.toString().trim()
                binding.countryCode.text = countryCode
            }
            R.id.cc_korean -> {
                binding.countryCodeLayout.visibility = View.GONE
                binding.countryFlag.setImageResource(R.drawable.ic_south_korea_flag)
                countryCode = binding.ccSouthKoreaCode.text.toString().trim()
                binding.countryCode.text = countryCode
            }
            R.id.cc_vietnam -> {
                binding.countryCodeLayout.visibility = View.GONE
                binding.countryFlag.setImageResource(R.drawable.ic_vietnam_flag)
                countryCode = binding.ccVietnamCode.text.toString().trim()
                binding.countryCode.text = countryCode
            }
            R.id.singapore_flag -> {
                binding.countryCodeLayout.visibility = View.GONE
                binding.countryFlag.setImageResource(R.drawable.ic_singapore_flag)
                countryCode = binding.ccSingaporeCode.text.toString().trim()
                binding.countryCode.text = countryCode
            }

            R.id.tvOtherLoginMethod -> {
                val intent = Intent(this, SocialLoginActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left)
            }
        }
    }

    private fun fullNumberWithPlus():String{
        if (phoneNumber[0] =='0') phoneNumber= phoneNumber.substring(1)
        return countryCode + phoneNumber
    }

    private fun enterTransition(): Transition? {
        val bounds = ChangeBounds()
        bounds.duration = 500
        return bounds
    }

    private fun returnTransition(): Transition? {
        val bounds = ChangeBounds()
        bounds.interpolator = DecelerateInterpolator()
        bounds.duration = 500
        return bounds
    }

}

