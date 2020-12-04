package com.example.sensortest

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_motion_function.*
import org.jetbrains.anko.toast
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class MotionFunction : AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    var startLon = 0.0
    var startLat = 0.0
    var currentLon = 0.0
    var currentLat = 0.0
    lateinit var time : LocalTime;
    var distance = 0.0
    lateinit var loc:Location
    lateinit var loc2:Location

    var curTime = 0

    var flag  = 0; //记录初始位置
    var flag2 = 0; //记录第一次写文件


    var startSpeed : Float = 0.0F
    var acceleration = 0.0F
    // GPS record var

    var acce_Gps : Vector<Float> = Vector<Float>()

    var latitude_Gps : Vector<Double> = Vector<Double>()
    var longititue_Gps : Vector<Double> = Vector<Double>()
    var heading_Gps : Vector<Float> = Vector<Float>()

    //file record

    var record_Time = 0
    val file = File(Environment.getExternalStorageDirectory(), "Record_Gps.txt")

    //定义一个权限COde，用来识别Location权限
    private val LOCATION_PERMISSION = 1

    //使用匿名内部类创建了LocationListener的实例
    val locationListener = object : LocationListener {
        override fun onProviderDisabled(provider: String) {
            toast("关闭了GPS")

        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onProviderEnabled(provider: String) {
            toast("打开了GPS")

        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onLocationChanged(location: Location) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                time = LocalTime.now()
            }
            toast("GPS数据持续获取中")
            motion_GPS(locationManager)


            //记录数据用来进行后续分类
            record_Gps(locationManager)

            // Event Detect
            if(!heading_Gps.isEmpty() and ( heading_Gps.size > 2) )
            {
                Log.v("Heading_Detect", "Detecting")

                var heading_Detect = heading_Gps[heading_Gps.size - 1] - heading_Gps[heading_Gps.size - 2]

                if((Math.abs(heading_Detect) > 30))
                {
                    Detect1.text = "转弯速度过快！！！"
                }
                else if( Math.abs(heading_Detect) > 20  )
                {
                    Detect1.text = "正常转弯，转速正常"

                }
                else
                {
                    Detect1.text = "None"
                }
            }

            if(!acce_Gps.isEmpty() and (acce_Gps.size> 2))
            {


                var Braking_Detect = acce_Gps[acce_Gps.size - 1]- acce_Gps[acce_Gps.size - 2]

                if(Math.abs(Braking_Detect) > 3)
                {
                    Detect2.text = "Sudden Brake/ 瞬间制动"
                    Log.v("Brake_Detect", "Detecting too fast")
                }
                else if( Math.abs(Braking_Detect) < 3  )
                {
                    Detect2.text = "Braking /制动"
                    Log.v("Brake_Detect", "Detecting")

                }
                else if( Math.abs(Braking_Detect) > 0)
                {
                    Detect2.text = "None"
                }

            }




        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }



    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_motion_function)

        time = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalTime.now()
        } else {
            TODO("VERSION.SDK_INT < O")
        };


        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            if (hasLocationPermission != PackageManager.PERMISSION_GRANTED) {
                //requestPermissions是异步执行的
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION)
            }
            else {
                motion_GPS(locationManager)
                record_Gps(locationManager)

            }
        }
        else {
            motion_GPS(locationManager)
            record_Gps(locationManager)

        }



    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onPause() {
        super.onPause()
        val hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if ((locationManager != null) && ((hasLocationPermission == PackageManager.PERMISSION_GRANTED))) {
            locationManager.removeUpdates(locationListener)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            curTime =  time.toSecondOfDay() - record_Time
            val it = Intent()
            it.setClass(this,MainActivity::class.java)
            it.putExtra("Time",curTime)
            startActivity(it)
        }
        toast("RECORD PAUSE")

    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        //挂上LocationListener, 在状态变化时刷新位置显示，因为requestPermissionss是异步执行的，所以要先确认是否有权限
        super.onResume()
        val hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if ((locationManager != null) && ((hasLocationPermission == PackageManager.PERMISSION_GRANTED))) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0F, locationListener)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0F, locationListener)
            toast("刷新...GPS数据持续获取中")
            motion_GPS(locationManager)
        }
    }

    //申请下位置权限后，要刷新位置信息
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toast("获取了位置权限")
                motion_GPS(locationManager)

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun motion_GPS(Locationmanager: LocationManager)
    {
        showSpeed(motion3, locationManager)
        showLocation(motion4, locationManager)
        showBearing(motion1, locationManager)
        showDistance(motion2, locationManager)
        showAcceleratioin(motion0, locationManager)
    }
    @RequiresApi(Build.VERSION_CODES.M)
    fun record_Gps(locationManager: LocationManager) {
        acce_Gps.add(acceleration)
        latitude_Gps.add(getLocation(locationManager)?.latitude)
        longititue_Gps.add(getLocation(locationManager)?.altitude)
        heading_Gps.add(getLocation(locationManager)?.bearing)
    }

    fun save_Gps(locationManager: LocationManager, save_data : String)
    {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
           // val file = File(Environment.getExternalStorageDirectory(), "Record_Gps.txt")
            try {
                file.createNewFile()
                if(flag2 == 0) {  val fileWriter = FileWriter(file)
                flag2 = 1;
                    Log.v("store", "write data")
                    fileWriter.write("#GPS_RECORD#" + "\n")
                    fileWriter.write(save_data + "\n")
                    fileWriter.close()
                }
                else {
                    val fileWriter = FileWriter(file, true)
                    Log.v("store", "write data")

                    fileWriter.write(save_data + "\n")
                    fileWriter.close()
                }
                Log.v("store", "write data")


            } catch (error: Exception)
            {
                error.printStackTrace()
            }
        }
        else
        {
            toast("SdCard error")
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun showLocation(textview: TextView, locationManager: LocationManager) {
        textview.text = "LON:" + getLocation(locationManager)!!.longitude.toInt().toString() + "  " +  "LAT:" +getLocation(locationManager)!!.latitude.toInt().toString()
        save_Gps(locationManager,"LON:" + getLocation(locationManager)!!.longitude.toInt().toString() + "  " +  "LAT:" +getLocation(locationManager)!!.latitude.toInt().toString())
    }
    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.M)
    fun showSpeed(textview: TextView, locationManager: LocationManager) {
        textview.text = "SPEED: " + getLocation(locationManager)?.speed.toString()
        Log.v("speed", "Time" + time + "SPEED: " + getLocation(locationManager)?.speed.toString())
        save_Gps(locationManager,"#speed: "+ "Time" + time + "SPEED: " + getLocation(locationManager)?.speed.toString())
    }
    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.M)
    fun showBearing(textview: TextView, locationManager: LocationManager) {
        textview.text = "BEARING: " + getLocation(locationManager)?.bearing.toString()
    }
    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.M)
    fun showAltitude(textview: TextView, locationManager: LocationManager) {
        textview.text = "ALTITUDE: " + getLocation(locationManager)?.altitude.toString()
    }
    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.M)
    fun showLatitude(textview: TextView, locationManager: LocationManager) {
        textview.text = "LATITUDE: " + getLocation(locationManager)?.latitude.toString()
    }
    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.M)
    fun showLongitude(textview: TextView, locationManager: LocationManager) {
        textview.text = "SPEED: " + getLocation(locationManager)?.longitude.toString()
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.M)
    fun showDistance(textview: TextView, locationManager: LocationManager) {


        val results = FloatArray(1)
        if(flag == 0) {
            loc = this.getLocation(locationManager)!!
            startLat = this.getLocation(locationManager)!!.latitude
            startLon = this.getLocation(locationManager)!!.longitude
            flag = 1

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                record_Time = time.toSecondOfDay()
            }
        }
        else if(flag == 1)
        {
            loc2 = this.getLocation(locationManager)!!
            Location.distanceBetween(startLat, startLon, loc2.latitude, loc2.longitude, results)
            Log.v("Distance", "Time" + time + "Distance: " + loc.distanceTo(loc2).toString())

            textview.text = "DISTANCE:   " + loc.distanceTo(loc2).toString()
            distance = loc.distanceTo(loc2).toDouble()
        }


    }
    @RequiresApi(Build.VERSION_CODES.M)
    fun showAcceleratioin(textview: TextView, locationManager: LocationManager)
    {
        var curSpeed = this.getLocation(locationManager)!!.speed

        if(distance > 0)
        acceleration = (curSpeed*curSpeed / (2*distance)).toFloat()

        textview.text = "ACCELERATION:  " + acceleration.toString();
    }
    //获取位置信息
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
}





