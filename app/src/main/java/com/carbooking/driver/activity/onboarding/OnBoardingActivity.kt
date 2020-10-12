package com.carbooking.driver.activity.onboarding

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.viewpager.widget.ViewPager
import com.carbooking.driver.R
import com.carbooking.driver.databinding.ActivityOnBoardingBinding
import com.example.common.adapter.SliderAdapter
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_on_boarding.*


class OnBoardingActivity : AppCompatActivity() {
    private lateinit var viewPagerAdapter: SliderAdapter
    private lateinit var binding : ActivityOnBoardingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_on_boarding)
        viewPagerAdapter = SliderAdapter(this)
        binding.onboardViewPager.adapter = viewPagerAdapter
        addDot(0)
        binding.onboardViewPager.addOnPageChangeListener(listener)

        binding.buttonProceed.setOnClickListener {
            Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(permissionListener)
                .check()
        }
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
                binding.buttonProceed.visibility = View.GONE
            }
            1 -> {
                slider_second_dot.setSelectedDot()
                slider_first_dot.setNonSelectedDot()
                binding.buttonProceed.visibility = View.VISIBLE
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

    private val permissionListener: MultiplePermissionsListener = DialogOnAnyDeniedMultiplePermissionsListener.Builder
        .withContext(this)
        .withTitle("Location Permission")
        .withMessage(getString(R.string.message_permission_denied))
        .withButtonText(android.R.string.ok)
        .withIcon(R.mipmap.ic_launcher)
        .build()

}
