package com.example.Motion_test_tool

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import org.jetbrains.anko.toast
import java.time.LocalTime
import java.util.*


class MotionService : Service() {

    lateinit var time : LocalTime;
    var curTime =""
    var recordTime = 0
    private val binder = MotionBinder()
    private var locationManager: LocationManager? = null
    private val LOCATION_PERMISSION = 1
    private lateinit var c: Criteria

    private var motionListener : MotionListener? = null

    // GPS record var

    var acce_Gps : Vector<Float> = Vector<Float>()
    var latitudeGps : Vector<Double> = Vector<Double>()
    var longititueGps : Vector<Double> = Vector<Double>()
    var headingGps : Vector<Float> = Vector<Float>()
    var speedGps : Vector<Float> = Vector<Float>()
    //使用匿名内部类创建了LocationListener的实例
    private val locationListener = object : LocationListener {


        override fun onProviderDisabled(provider: String) {
            toast("关闭了GPS")

        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onProviderEnabled(provider: String) {
            toast("打开了GPS")

        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onLocationChanged(location: Location) {


            toast("GPS数据持续获取中")



            //记录数据用来进行后续分类
            locationManager?.let { recordGps(it) }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

                time = LocalTime.now()
                curTime = (time.toSecondOfDay() - recordTime).toString()
                motionListener?.updateTime(curTime)

            }
            motionListener?.updateData(locationManager!!)

        }


        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }
    }




    override fun onBind(intent: Intent): IBinder {


        return MotionBinder()

    }

    inner class MotionBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): MotionService = this@MotionService
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        toast("Start Service")
        super.onCreate()
        notificationFunction()

        //Time Record
        time = LocalTime.now()
        recordTime = time.toSecondOfDay()

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager


            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (!isGpsAble(locationManager!!)) {
                    openGPS2();
                }
                return
            }
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0F, locationListener)
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0F, locationListener)





    }

    fun CriteriacreateFineCriteria(): Criteria {
        c = Criteria()
        c.setAccuracy(Criteria.ACCURACY_FINE) //高精度
        c.setAltitudeRequired(true) //包含高度信息
        c.setBearingRequired(true) //包含方位信息
        c.setSpeedRequired(true) //包含速度信息
        c.setCostAllowed(true) //允许付费
        c.setPowerRequirement(Criteria.POWER_HIGH) //高耗电
        return c
    }

    override fun onDestroy() {
        toast("Record stop")
        locationManager?.removeUpdates(locationListener);

        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun notificationFunction()
    {
        //----------------------------------------------------------------
        val CHANNEL_ONE_ID = "com.primedu.cn"
        val CHANNEL_ONE_NAME = "Channel One"
        var notificationChannel: NotificationChannel? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.setShowBadge(true)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(notificationChannel)
        }
        //---------------------------------------------------------------
        val intent = Intent(this, MotionService::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        var notification: Notification = Notification.Builder(this)
                .build()
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        notification = Notification.Builder(this)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setChannelId(CHANNEL_ONE_ID)
                .setTicker("Nature")
                .setContentTitle("xxxx")
                .setContentIntent(pendingIntent)
                .getNotification()


        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR

        startForeground(1, notification)

        //目前程序开启的时候点击  通知栏可以直接跳转到  数据记录页面
        //但是若，程序前台被杀死之后点击通知栏就无法跳转到软件重。
    }



    @RequiresApi(Build.VERSION_CODES.M)
    private fun gpsRecord()
    {

    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun recordGps(locationManager: LocationManager) {
        toast("Service start recording gps")
        speedGps.add(getLocation(locationManager)?.speed)
        latitudeGps.add(getLocation(locationManager)?.latitude)
        longititueGps.add(getLocation(locationManager)?.altitude)
        headingGps.add(getLocation(locationManager)?.bearing)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getLocation(locationManager: LocationManager): Location? {
        var location: Location? = null
        if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            toast("没有位置权限")
        }
        else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            toast("没有打开GPS")
        }
        else {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location == null) {
                toast("位置信息为空")
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (location == null) {
                    toast("网络位置信息也为空")

                }
                else {
                    toast("当前使用网络位置,注意打开网络定位服务")
                }
            }
        }
        return location
    }

    private fun isGpsAble(lm: LocationManager): Boolean {
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }


    //打开设置页面让用户自己设置
    private fun openGPS2() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    // interface

    interface MotionListener{
        fun updateTime(curTime: String)

        @RequiresApi(value = 23)
        fun updateData(locationManager: LocationManager)
    }

    fun setCallback(motionListener: MotionListener)
    {
        this.motionListener = motionListener

    }


}
