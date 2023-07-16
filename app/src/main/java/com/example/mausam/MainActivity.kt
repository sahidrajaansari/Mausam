package com.example.mausam

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.mausam.Models.WeatherResponse
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class MainActivity : AppCompatActivity() {
    private lateinit var mFusedLocationClient:FusedLocationProviderClient
    private lateinit var customProgressDialog:Dialog

    private var tv_main:TextView?=null
    private var tv_temp:TextView?=null
    private var tv_sunrise:TextView?=null
    private var tv_sunset:TextView?=null
    private var tv_cityName:TextView?=null
    private var tv_minTemp:TextView?=null
    private var tv_maxTemp:TextView?=null
    private var tv_WindSpeed:TextView?=null
    private var tv_humidity:TextView?=null
    private var tv_cloudiness:TextView?=null
    private var tv_feels_like:TextView?=null
    private var iv_main:ImageView?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mFusedLocationClient=LocationServices.getFusedLocationProviderClient(this)
        tv_main=findViewById(R.id.tv_main)
        tv_temp=findViewById(R.id.tv_temp)
        tv_sunrise=findViewById(R.id.tv_sunrise)
        tv_sunset=findViewById(R.id.tv_sunset)
        tv_cityName=findViewById(R.id.tv_cityName)
        tv_minTemp=findViewById(R.id.tv_minTemp)
        tv_maxTemp=findViewById(R.id.tv_maxTemp)
        tv_WindSpeed=findViewById(R.id.tv_WindSpeed)
        tv_humidity=findViewById(R.id.tv_humidity)
        tv_cloudiness=findViewById(R.id.tv_cloudiness)
        tv_feels_like=findViewById(R.id.tv_feels_like)
        iv_main=findViewById(R.id.iv_main)

        if(!isLocationEnabled()){
            Toast.makeText(this,"Please Turn On Your Location ",Toast.LENGTH_SHORT).show()

            val intent=Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        else{
            Dexter.withActivity(this).withPermissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION).withListener(object :MultiplePermissionsListener{
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if(report!!.areAllPermissionsGranted()){
                        requestLocationData()
                    }

                    if(report!!.isAnyPermissionPermanentlyDenied){
                        Toast.makeText(this@MainActivity,"Please Turn On Your Location ",Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationalDialogForPermissions()
                }
            }).onSameThread().check()

        }
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }
    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location? = locationResult.lastLocation
            val latitude = mLastLocation?.latitude
            Log.i("Current Latitude", "$latitude")

            val longitude = mLastLocation?.longitude
            Log.i("Current Longitude", "$longitude")
            longitude?.let { latitude?.let { it1 -> getLocationWeatherDetails(it1, it) } }
        }
    }

    private fun getLocationWeatherDetails(lattitude:Double,longitude:Double){
        if(Constants.isNetworkAvailable(this)){
            val retrofit:Retrofit=Retrofit.Builder().
            baseUrl(Constants.Base_URL).
            addConverterFactory(GsonConverterFactory.create()).
            build()

            val service:WeatherService=retrofit.
            create<WeatherService>(WeatherService::class.java)
            customProgressDialogFunction()
            val listCall:Call<WeatherResponse> = service.getWeather(lattitude,longitude,Constants.Metric_Unit,Constants.APP_ID)
            listCall.enqueue(object :Callback<WeatherResponse>{
                override fun onResponse(response: Response<WeatherResponse>?, retrofit: Retrofit?) {
                    if(response!!.isSuccess){

                        val weatherList:WeatherResponse=response.body()
                        Log.i("Response Result","$weatherList")
                        setupUI(weatherList)
                        cancelProgressDialog()
                    }
                    else{
                        val rc=response.code()
                        when(rc){
                            400->{
                                Log.e("Error 400","Bad Connection")
                            }
                            404->{
                                Log.e("Error 404","Not Found")
                            }
                            else->{
                                Log.e("Error","Generic Error")
                            }

                        }
                    }
                }

                override fun onFailure(t: Throwable?) {
                    Log.e("Errorrrr",t!!.message.toString())
                    cancelProgressDialog()
                }

            })
        }
        else{
            Toast.makeText(this,"Internet Not Available",Toast.LENGTH_SHORT).show()
        }
    }

    private fun customProgressDialogFunction() {
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog.show()
    }

    private fun cancelProgressDialog(){
        customProgressDialog.dismiss()
    }


    private fun isLocationEnabled():Boolean{
        val locationManager:LocationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun setupUI(weatherList:WeatherResponse){
        for(i in weatherList.weather.indices){
            Log.i("Weather Name",weatherList.weather.toString())
            tv_main?.text=weatherList.weather[i].description
            val degreeSymbol = '\u00B0'
            var temperature:String=weatherList.main.temp.toString()+degreeSymbol+"C"
            tv_temp?.text=temperature
            tv_sunset?.text=unixTime(weatherList.sys.sunset)
            tv_sunrise?.text=unixTime(weatherList.sys.sunrise)
            tv_cityName?.text=weatherList.name
            tv_minTemp?.text="Min: "+weatherList.main.temp_min.toString()+degreeSymbol+"C"
            tv_maxTemp?.text="Max: "+weatherList.main.temp_max.toString()+degreeSymbol+"C"
            tv_WindSpeed?.text=weatherList.wind.speed.toString()
            tv_humidity?.text=weatherList.main.humidity.toString()
            val pressure=weatherList.main.pressure*0.001
            tv_cloudiness?.text=pressure.toString()
//            tv_feels_like?.text="Feels Like: "+weatherList.main.feels_like.toString()+degreeSymbol+"C"

            when (weatherList.weather[i].icon) {
                "01d" -> iv_main?.setImageResource(R.drawable.sunny)
                "02d" -> iv_main?.setImageResource(R.drawable.cloud)
                "03d" -> iv_main?.setImageResource(R.drawable.cloud)
                "04d" -> iv_main?.setImageResource(R.drawable.cloud)
                "04n" -> iv_main?.setImageResource(R.drawable.cloud)
                "10d" -> iv_main?.setImageResource(R.drawable.rain)
                "11d" -> iv_main?.setImageResource(R.drawable.storm)
                "13d" -> iv_main?.setImageResource(R.drawable.snowflake)
                "01n" -> iv_main?.setImageResource(R.drawable.cloud)
                "02n" -> iv_main?.setImageResource(R.drawable.cloud)
                "03n" -> iv_main?.setImageResource(R.drawable.cloud)
                "10n" -> iv_main?.setImageResource(R.drawable.cloud)
                "11n" -> iv_main?.setImageResource(R.drawable.rain)
                "13n" -> iv_main?.setImageResource(R.drawable.snowflake)
            }
        }
    }

    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        @SuppressLint("SimpleDateFormat") val sdf =
            SimpleDateFormat("HH:mm:ss")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
}