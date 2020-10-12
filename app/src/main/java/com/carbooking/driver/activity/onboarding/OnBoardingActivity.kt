package com.carbooking.driver.activity.onboarding

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.viewpager.widget.ViewPager
import com.carbooking.driver.R
import com.carbooking.driver.activity.login.LoginSplashActivity
import com.carbooking.driver.adapter.SliderAdapter
import com.carbooking.driver.databinding.ActivityOnBoardingBinding
import com.tedpark.tedpermission.rx2.TedRx2Permission
import kotlinx.android.synthetic.main.activity_on_boarding.*


class OnBoardingActivity : AppCompatActivity(),View.OnClickListener {
    private lateinit var viewPagerAdapter: SliderAdapter
    private lateinit var binding: ActivityOnBoardingBinding
    private val handler by lazy { Handler(Looper.myLooper()!!) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_on_boarding)

        viewPagerAdapter = SliderAdapter(this)
        binding.onboardViewPager.adapter = viewPagerAdapter
        addDot(0)
        binding.onboardViewPager.addOnPageChangeListener(listener)

        binding.buttonProceed.setOnClickListener(this)
    }

    private val listener = object : ViewPager.OnPageChangeListener {
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

        }

        override fun onPageSelected(position: Int) {
            addDot(position)
        }

        override fun onPageScrollStateChanged(state: Int) {
        }
    }

    private fun addDot(position: Int) {
        when (position) {
            0 -> {
                slider_first_dot.setSelectedDot()
                slider_second_dot.setNonSelectedDot()
                binding.buttonProceed.visibility = View.INVISIBLE
            }
            1 -> {
                slider_second_dot.setSelectedDot()
                slider_first_dot.setNonSelectedDot()
                binding.buttonProceed.visibility = View.VISIBLE
                binding.buttonProceed.animation = AnimationUtils.loadAnimation(this,R.anim.slide_in_left)
            }
        }
    }

    private fun ImageView.setNonSelectedDot() {
        this.setImageDrawable(
            ContextCompat.getDrawable(
                this@OnBoardingActivity,
                R.drawable.non_selected_dot
            )
        )
    }

    private fun ImageView.setSelectedDot() {
        this.setImageDrawable(
            ContextCompat.getDrawable(
                this@OnBoardingActivity,
                R.drawable.selected_dot
            )
        )
    }

    private fun askPermission() {
        TedRx2Permission.with(this)
            .setRationaleMessage(R.string.rationale_message)
            .setDeniedCloseButtonText(android.R.string.cancel)
            .setPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            .setDeniedMessage(R.string.message_permission_denied)
            .request()
            .subscribe({ tedPermissionResult ->
                if (tedPermissionResult.isGranted) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    handler.postDelayed({
                        startActivity(Intent(this@OnBoardingActivity,LoginSplashActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    },200)
                } else {
                    Toast.makeText(this, "Permission Denied\n" + tedPermissionResult.deniedPermissions.toString(),Toast.LENGTH_SHORT).show()
                }
            }, { })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        askPermission()
    }

    override fun onClick(v: View?) {
        when (v){
            binding.buttonProceed -> askPermission()
        }
    }
}
