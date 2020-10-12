package com.carbooking.driver.activity.home

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.carbooking.driver.R
import com.carbooking.driver.utils.Common
import com.carbooking.driver.activity.login.LoginSplashActivity
import com.example.common.utils.getProgressDrawable
import com.example.common.utils.loadImage
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.nav_header_main.*

class DriverHomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navView : NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController : NavController
    private lateinit var auth : FirebaseAuth
    private lateinit var userInfoRef : DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_home)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        init()

    }

    private fun init() {
        auth = Firebase.auth
        userInfoRef = Firebase.database.getReference(Common.DRIVER_INFO_REFERENCE).child(auth.currentUser!!.uid)
        navView.setNavigationItemSelectedListener {
            if (it.itemId == R.id.nav_sign_out) {
                val builder = AlertDialog.Builder(this@DriverHomeActivity)
                builder.setTitle(getString(R.string.menu_exit))
                    .setMessage(getString(R.string.confirm_exit))
                    .setNegativeButton(getString(R.string.fui_cancel)) { dialogInterface, _ -> dialogInterface.dismiss() }
                    .setPositiveButton(getString(R.string.menu_exit).toUpperCase()) { _, _ ->
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(this@DriverHomeActivity, LoginSplashActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        startActivity(intent)
                        finish()
                    }
                    .setCancelable(false)

                val dialog = builder.create()
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(resources.getColor(R.color.accent_red,applicationContext.theme))
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(resources.getColor(R.color.accent_black,applicationContext.theme))
                }
                dialog.show()
            }
            true

        }
        val headerView = navView.getHeaderView(0 )
        val tvName = headerView.findViewById<View>(R.id.tv_name) as TextView
        val tvPhoneNumber = headerView.findViewById<View>(R.id.tv_phone_number) as TextView
        val tvStar = headerView.findViewById<View>(R.id.tv_star) as TextView

        tvName.text = Common.buildWelcomeMessage()
        tvPhoneNumber.text = Common.currentUser!!.phoneNumber
        tvStar.text = StringBuilder().append(Common.currentUser!!.rating)
        img_avatar.loadImage(Common.currentUser!!.photoUrl, getProgressDrawable(img_avatar.context))

    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.driver_home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}