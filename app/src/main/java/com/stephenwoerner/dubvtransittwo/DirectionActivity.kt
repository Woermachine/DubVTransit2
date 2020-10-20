package com.stephenwoerner.dubvtransittwo

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.maps.model.*
import kotlinx.android.synthetic.main.display_layout.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*

class DirectionActivity : Activity(), LocationListener {

    enum class Route { CAR, BUS, WALK, PRT }

    private lateinit var destinationStr : String
    private lateinit var closestPRTA : String
    private lateinit var closestPRTB : String
    private var useCurrentTime : Boolean = false

    private lateinit var origin : LatLng
    private lateinit var destination : LatLng

    private var leavingTime = 0L

    private lateinit var locationManager : LocationManager

    private lateinit var carDirections : ArrayList<String>
    private lateinit var busDirections : ArrayList<String>
    private lateinit var walkingDirections : ArrayList<String>
    private lateinit var prtDirections : ArrayList<String>
    private var leavingTimeMillis : Long = 0L
    private var context = this@DirectionActivity
    private val model = PRTModel.get()

    private val mapsDataClient = MapsDataClient()

    private val location: LatLng
        get() {
            var currentLocation = LatLng(0.0, 0.0)
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
                val criteria = Criteria()
                criteria.accuracy = Criteria.ACCURACY_FINE
//                val providers = locationManager.allProviders
//                val bestProvider = providers[0]
                var location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                while (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (location == null) {
                        try {
                            Thread.sleep(1000)
                        } catch (e: InterruptedException) {
                            val mess = e.message
                            if (mess != null) Timber.d("Thread sleep failed: ${e.message}")
                        }
                    }
                }
                val lat: Double
                val lon: Double
                lat = location.latitude
                lon = location.longitude
                Timber.d("Location : %s, %s ", lat, lon)
                currentLocation = LatLng(lat, lon)
            }
            return currentLocation
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.display_layout)
        context = this@DirectionActivity

        var originStr = ""
        try {
            originStr = intent.getStringExtra("origin")!!
            destinationStr = intent.getStringExtra("destination")!!
            useCurrentTime = intent.getBooleanExtra("useCurrentTime", true)
            leavingTimeMillis =  intent.getLongExtra("leavingTime", Calendar.getInstance().timeInMillis)
        } catch (e: NullPointerException) {
            Timber.e( e)
            finish()
        }

        val displayOrigin = "From: $originStr"
        start_location.text = displayOrigin

        val displayDestination = "To: $destinationStr"
        destination_location.text = displayDestination

        origin = when (originStr) {
            getString(R.string.current_location) -> location
            else -> {
                val lookupString = if(model.allHashMap.containsKey(originStr)) {
                    originStr
                } else {
                    val courseDbAdapter = CourseDbAdapter().open(context)
                    val cursor = courseDbAdapter.fetchCourse(originStr)
                    val og = cursor.getString(cursor.getColumnIndex(CourseDbAdapter.KEY_LOCATION))
                    courseDbAdapter.close()
                    og
                }
                model.allHashMap[lookupString]!!
            }
        }

        if (!model.allHashMap.containsKey(destinationStr)) { //If its not in the HashMap then its a user course
            val courseDbAdapter = CourseDbAdapter().open(context)
            val cursor = courseDbAdapter.fetchCourse(destinationStr)
            destinationStr = cursor.getString(cursor.getColumnIndex(CourseDbAdapter.KEY_LOCATION))
            courseDbAdapter.close()
        }
        destination = model.allHashMap[destinationStr]!!
        navigationButton.setOnClickListener { AlertDialog.Builder(context).setView(R.layout.alert_contents).setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }.setCancelable(true).setTitle(R.string.alert_title).setIcon(R.drawable.ic_navigation_black_36dp).setMessage(R.string.alert_message).show() }

        progress = ProgressBar(context)
        progress.isIndeterminate = true
        progress.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            model.requestPRTStatus()
            val results = mapsDataClient.execute(
                leavingTimeMillis,
                origin,
                destination
            )

            runOnUiThread {
                onResults(results)
            }
        }
    }

    private fun onResults(mapsTaskResults: MapsDataClient.MapsTaskResults) {

        progress.visibility = View.GONE

        busDirections = mapsTaskResults.busStepsAndDuration.directions
        walkingDirections = mapsTaskResults.walkStepsAndDuration.directions
        prtDirections = mapsTaskResults.prtStepsAndDuration.directions
        carDirections = mapsTaskResults.carStepsAndDuration.directions
        closestPRTA = mapsTaskResults.closestPRTA
        closestPRTB = mapsTaskResults.closestPRTB
        leavingTime = mapsTaskResults.leavingTime

        val directionsStepArrayAdapter = when (mapsTaskResults.fastestRoute) {
            Route.BUS -> {
                ArrayAdapter(context, R.layout.list_item, busDirections)
            }
            Route.WALK -> {
                ArrayAdapter(context, R.layout.list_item, walkingDirections)
            }
            Route.PRT -> {
                ArrayAdapter(context, R.layout.list_item, prtDirections)
            }
            else -> {
                ArrayAdapter(context, R.layout.list_item, carDirections)
            }
        }

        val btnSelectColor = getMuhDrawable(R.color.ButtonSelected)

        val busButtonText = "${mapsTaskResults.busStepsAndDuration.duration / 60} min"
        val walkingButtonText = "${ mapsTaskResults.walkStepsAndDuration.duration / 60} min"
        val prtButtonText = "${ mapsTaskResults.prtStepsAndDuration.duration / 60} min"
        val carButtonText = "${ mapsTaskResults.carStepsAndDuration.duration / 60} min"

        busButton.text = busButtonText
        walkButton.text = walkingButtonText
        prtButton.text = prtButtonText
        carButton.text = carButtonText
        list2.adapter = directionsStepArrayAdapter
        prtBadge.background = prtButtonColor()

        when (mapsTaskResults.fastestRoute) {
            Route.CAR -> {
                carButton.background = btnSelectColor
            }
            Route.BUS -> {
                busButton.background = btnSelectColor
            }
            Route.WALK -> {
                walkButton.background = btnSelectColor
            }
            Route.PRT -> {
                prtButton.background = btnSelectColor
            }
        }
    }

    /**
     * Change directions
     * @param v the button which was pressed
     */
    fun changeSelected(v: View) {
        val unselected = ColorDrawable(ContextCompat.getColor(context, R.color.ButtonUnselected))
        carButton.background = unselected
        prtButton.background = unselected
        walkButton.background = unselected
        busButton.background = unselected

        val selectedColor = ColorDrawable(ContextCompat.getColor(context, R.color.ButtonSelected))
        list2.adapter = when (v.id) {
            R.id.busButton -> {
                prtButton.background = selectedColor
                ArrayAdapter(this, R.layout.list_item, busDirections)
            }
            R.id.walkButton -> {
                walkButton.background = selectedColor
                ArrayAdapter(this, R.layout.list_item, walkingDirections)
            }
            R.id.prtButton -> {
                prtButton.background = selectedColor
                ArrayAdapter(this, R.layout.list_item, prtDirections)
            }
            else -> { // Assume Car
                carButton.background = selectedColor
                ArrayAdapter(this, R.layout.list_item, carDirections)
            }
        }
    }

    /**
     * update location
     * @param location, user's current location
     */
    override fun onLocationChanged(location: Location) {
        Timber.v("Location Changed ${location.latitude} and ${location.longitude}")
        locationManager.removeUpdates(this)
    }

    public override fun onPause() {
        super.onPause()
        progress.visibility = View.GONE
    }

    /**
     * Open maps with a determined intent
     * @param v the TextView which was clicked
     */
    fun openMaps(v: View) {
        val prt = model.allHashMap[closestPRTA]
        val uri: String
        uri = if (v.id == R.id.option_dest) {
            "http://maps.google.com/maps?q=loc:" + destination.lat + "," + destination.lng + " (" + destinationStr + ")"
        } else {
            if (prt != null) {
                "http://maps.google.com/maps?q=loc:" + prt.lat + "," + prt.lng + " ( Closest PRT Station )"
            } else {
                ""
            }
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        context.startActivity(intent)
    }

    private fun prtButtonColor() : Drawable {
        return if (!useCurrentTime && model.isOpen(leavingTime))
            getMuhDrawable(R.drawable.rounded_textview_yellow)
        else if (useCurrentTime && model.openBetweenStations(closestPRTA, closestPRTB) && model.status != "7")
            getMuhDrawable(R.drawable.rounded_textview_yellow)
        else if (model.isOpen(leavingTime) || model.status == "1")
            getMuhDrawable(R.drawable.rounded_textview_green)
        else
            getMuhDrawable(R.drawable.rounded_textview_red)
    }

    private fun getMuhDrawable(id : Int) : Drawable {
        return ContextCompat.getDrawable(applicationContext, id)!!
    }

    // Required functions
    override fun onProviderDisabled(arg0: String) {
        // Unused
    }

    override fun onProviderEnabled(arg0: String) {
        // Unused
    }

    override fun onStatusChanged(arg0: String, arg1: Int, arg2: Bundle) {
        // Unused
    }

    companion object {
        private lateinit var progress: ProgressBar
    }


}