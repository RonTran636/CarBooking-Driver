package com.carbooking.driver.activity.login

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.ChangeBounds
import android.transition.Transition
import android.view.Window
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.carbooking.driver.R
import com.carbooking.driver.activity.login.socialLogin.SocialLoginActivity
import com.carbooking.driver.databinding.ActivityLoginSplashBinding
import android.util.Pair as UtilPair


class LoginSplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginSplashBinding
    companion object {
        private const val RESULT_OK = 111
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        with(window) {
            requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
            sharedElementEnterTransition = enterTransition()
            sharedElementReturnTransition = returnTransition()
        }
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_login_splash)
        binding.tvOtherLoginMethod.setOnClickListener { callSocialScreen() }

        binding.phoneLayout.setOnClickListener { callPhoneLoginScreen() }
        binding.etNumberSendOtp.setOnClickListener { callPhoneLoginScreen() }
        binding.splashLayout.setOnClickListener { callPhoneLoginScreen() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode== RESULT_OK){
            binding.countryCode.text = data!!.getStringExtra("countryCode")
            val image : Bitmap? = data.getParcelableExtra("countryFlag")
            binding.countryFlag.setImageBitmap(image)
        }
    }

    private fun callPhoneLoginScreen() {
            val option = ActivityOptions.makeSceneTransitionAnimation(
                this,
                UtilPair.create(binding.titleSplash, "titleTransition"),
                UtilPair.create(binding.phoneLayout, "phoneTransition"),
                UtilPair.create(binding.tvOtherLoginMethod, "socialLogin")
            )
            startActivityForResult(Intent(this, LoginActivity::class.java), RESULT_OK, option.toBundle())
    }

    private fun callSocialScreen(){
            startActivity( Intent(this, SocialLoginActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private var doubleBackToExitPressedOnce = false

    override fun onBackPressed() {

        if (doubleBackToExitPressedOnce) {
            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(startMain)
        }
        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()

        Handler(Looper.myLooper()!!).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
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