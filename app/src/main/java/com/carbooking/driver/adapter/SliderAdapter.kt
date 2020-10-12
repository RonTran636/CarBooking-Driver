package com.carbooking.driver.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import com.carbooking.driver.R


class SliderAdapter(val context: Context) :PagerAdapter() {

    private val listImages= listOf(R.drawable.onboard_cheers,R.drawable.onboard_drive_safety)
    private val listHeader = listOf(R.string.first_slide_title,R.string.second_slide_title)
    private val listDescription = listOf(R.string.first_slide_header,R.string.second_slide_header)

    override fun getCount() = listHeader.size

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return  view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(context).inflate(R.layout.content_sliders,container,false)
        val imageView= view.findViewById<ImageView>(R.id.slider_image)
        val headerView = view.findViewById<TextView>(R.id.slider_header)
        val descriptionView = view.findViewById<TextView>(R.id.slider_description)

        imageView.setImageResource(listImages[position])
        headerView.setText(listHeader[position])
        descriptionView.setText(listDescription[position])

        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}