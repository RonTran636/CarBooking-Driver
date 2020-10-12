package com.carbooking.driver.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.carbooking.driver.R
import com.example.common.model.UserModel

object Common {
    fun buildWelcomeMessage(): String {
        return StringBuilder("Welcome")
            .append(" ")
            .append(currentUser!!.fullName)
            .toString()
    }

    fun showNotification(context: Context, id: Int, title: String?, body: String?, intent: Intent?) {
        var pendingIntent :PendingIntent? = null
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (intent!=null){
            pendingIntent = PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT)
            if (Build.VERSION.SDK_INT>Build.VERSION_CODES.O){
                val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID,"CarBookingDemo",NotificationManager.IMPORTANCE_HIGH)
                notificationChannel.apply {
                    description = "CarBookingDemo"
                    enableLights(true)
                    lightColor = Color.RED
                    vibrationPattern= longArrayOf(0,1000,500,1000)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        builder.setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(false)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.ic_logo_app)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources,R.drawable.ic_logo_app))
        if (pendingIntent !=null){
            builder.setContentIntent(pendingIntent)
            val notification =  builder.build()
            notificationManager.notify(id,notification)
        }
    }
    const val NOTIFICATION_CHANNEL_ID = "private_channel"
    const val NOTI_BODY:String = "Body"
    const val NOTI_TITLE: String = "Title"
    const val TOKEN_REFERENCE: String = "Token"
    var currentUser : UserModel? = null
    const val DRIVERS_LOCATION_REFERENCE :String = "DriversLocation"
    const val DRIVER_INFO_REFERENCE :String = "DriverInfo"
}