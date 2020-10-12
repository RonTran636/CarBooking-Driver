package com.carbooking.driver.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.text.TextUtils
import java.io.IOException
import java.lang.StringBuilder
import java.util.*

object LocationUtil {
    fun getAddressFromLocation(context: Context?, location: Location):String {
        val result = StringBuilder()
        val geocoder = Geocoder(context, Locale.getDefault())
        val addressList: List<Address>?
        return try {
            addressList = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                if (addressList[0].locality == null && !TextUtils.isEmpty(addressList[0].locality)){
                    //If address have city field
                    result.append(addressList[0].locality)
                }
                else if (addressList[0].subAdminArea == null && !TextUtils.isEmpty(addressList[0].subAdminArea)){
                    //If address don't have city field, search for subAdminArea
                    result.append(addressList[0].subAdminArea)
                }
                else if (addressList[0].adminArea == null && !TextUtils.isEmpty(addressList[0].adminArea)){
                    //If address don't have subAdminArea, search for adminArea
                    result.append(addressList[0].adminArea)
                }
                else {
                    //If address don't have adminArea, search for country name
                    result.append(addressList[0].countryName)
                }
                //Final result, add country code
                result.append(addressList[0].countryCode)
            }
            result.toString()
        } catch (e: IOException){
                result.toString()
        }
    }
}