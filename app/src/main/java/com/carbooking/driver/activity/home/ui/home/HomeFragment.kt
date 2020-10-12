package com.carbooking.driver.activity.home.ui.home

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.carbooking.driver.R
import com.carbooking.driver.utils.Common
import com.carbooking.driver.utils.LocationUtil
import com.example.common.activity.ui.home.HomeViewModel
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeFragment : Fragment(),OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var mapFragment: SupportMapFragment
    private var cityName = ""

    //Location
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    //Online System
    private lateinit var onlineRef: DatabaseReference
    private var currentUserRef : DatabaseReference? = null
    private lateinit var driversLocationRef: DatabaseReference
    private lateinit var geoFire: GeoFire

    private val onlineValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists() && currentUserRef!= null) {
                currentUserRef!!.onDisconnect().removeValue()
            }
        }
        override fun onCancelled(error: DatabaseError) {
            Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        init()

        mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        statusCheck()
        return root
    }

    private fun init() {
        onlineRef = FirebaseDatabase.getInstance().reference.child(".info/connected")
        locationRequest = LocationRequest()
        locationRequest.apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            fastestInterval = 3000
            interval = 5000
            smallestDisplacement = 10f
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                val newPosition = LatLng(locationResult!!.lastLocation.latitude, locationResult.lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 18f))
                updateDriverLocation(locationResult.lastLocation)
            }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        onlineRef.removeEventListener(onlineValueEventListener)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        registerOnlineSystem()
    }

    private fun registerOnlineSystem() {
        onlineRef.addValueEventListener(onlineValueEventListener)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap!!
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.setOnMyLocationClickListener {
            fusedLocationProviderClient.lastLocation
                .addOnFailureListener {
                    Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                }
                .addOnCompleteListener { location ->
                    val userLatLng = LatLng(location.result.latitude, location.result.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f))
                }
        }

        val view = mapFragment.requireView()
            .findViewById<View>("1".toInt())!!
            .parent!! as View
        val locationButton = view.findViewById<View>("2".toInt())
        val params = locationButton.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.ALIGN_TOP, 0)
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
        params.bottomMargin = 50

        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.uber_maps_style
                )
            )
            if (!success) Log.e(TAG, "onMapReady: Style Parsing Error")
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "onMapReady: ${e.message}")
        }
    }

    private fun statusCheck() {
        val manager: LocationManager = context!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val builder= AlertDialog.Builder(this.context!!)
            builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("OK"
                ) { _, _ -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                .setNegativeButton("CANCEL"
                ) { dialog, _ -> dialog.cancel() }
            val alert: AlertDialog = builder.create()
            alert.show()
        }
    }

    private fun updateDriverLocation(location: Location){
        val cityName = LocationUtil.getAddressFromLocation(requireContext(),location)
        if (!TextUtils.isEmpty(cityName)) {
            driversLocationRef = FirebaseDatabase.getInstance().getReference(Common.DRIVERS).child(Common.DRIVER_INFO_REFERENCE)
                .child(cityName)
            currentUserRef = driversLocationRef.child(FirebaseAuth.getInstance().currentUser!!.uid)

            geoFire = GeoFire(driversLocationRef)
            //Update Location
            geoFire.setLocation(
                FirebaseAuth.getInstance().currentUser!!.uid,
                GeoLocation(location.latitude, location.longitude)
            ) { _: String, error: DatabaseError? ->
                if (error != null) {
                    Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()
                } else {
                    Snackbar.make(mapFragment.requireView(), "You are online!", Snackbar.LENGTH_LONG).show()
                }

                registerOnlineSystem()
            }
        }
    }
}