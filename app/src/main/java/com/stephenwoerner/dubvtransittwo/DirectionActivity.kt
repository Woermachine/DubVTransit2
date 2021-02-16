package com.stephenwoerner.dubvtransittwo

import android.Manifest
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.maps.model.LatLng
import kotlinx.android.synthetic.main.display_layout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*

class DirectionActivity : AppCompatActivity(), LocationListener {

    enum class Route { CAR, BUS, WALK, PRT }

    private lateinit var destinationStr : String
    private lateinit var closestPRTA : String
    private lateinit var closestPRTB : String
    private var useCurrentTime : Boolean = false

    private lateinit var origin : LatLng
    private lateinit var destination : LatLng

    private var leavingTime = 0L

    private lateinit var locationManager : LocationManager

    private lateinit var carDirections : DirectionAdapter
    private lateinit var busDirections : DirectionAdapter
    private lateinit var walkingDirections : DirectionAdapter
    private lateinit var prtDirections : DirectionAdapter

    private var leavingTimeMillis : Long = 0L
    private var context = this@DirectionActivity
    private val model = PRTModel.get()

    private val mapsDataClient = MapsDataClient()

    private val location: LatLng
        get() {
            var currentLocation = LatLng(0.0, 0.0)
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
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
                            Thread.sleep(500)
                        } catch (e: InterruptedException) {
                            val mess = e.message
                            if (mess != null) Timber.d("Thread sleep failed: ${e.message}")
                        }
                    }
                }
                val lat = location.latitude
                val lon = location.longitude

                Timber.d("Location : %s, %s ", lat, lon)
                currentLocation = LatLng(lat, lon)

                val morgantown = LatLng(39.634224, -79.954850)
                val hundredMilesInKM = 160.934

                if( getDistanceFromLatLonInKm(morgantown.lat, morgantown.lng, lat, lon) > hundredMilesInKM ) {
                    finish()
                }

            }
            return currentLocation
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.requestFeature(Window.FEATURE_ACTION_BAR)
        supportActionBar?.hide()
        setContentView(R.layout.display_layout)
        context = this@DirectionActivity

        val viewModel = ViewModelProvider(this).get(DirectionViewModel::class.java)

        var originStr = ""
        try {
            originStr = intent.getStringExtra("origin")!!
            destinationStr = intent.getStringExtra("destination")!!
            useCurrentTime = intent.getBooleanExtra("useCurrentTime", true)
            leavingTimeMillis =  intent.getLongExtra(
                "leavingTime",
                Calendar.getInstance().timeInMillis
            )
        } catch (e: NullPointerException) {
            Timber.e(e)
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
                    val courseDb = CourseDb.get(applicationContext)
                    val course = courseDb.coursesQueries.selectCourse(originStr).executeAsOne()
                    course.location
                }
                model.allHashMap[lookupString]!!
            }
        }

        if (!model.allHashMap.containsKey(destinationStr)) { //If its not in the HashMap then its a user course
            val courseDb = CourseDb.get(applicationContext)
            val course = courseDb.coursesQueries.selectCourse(destinationStr).executeAsOne()
            destinationStr = course.location
        }
        destination = model.allHashMap[destinationStr]!!
        navigationButton.setOnClickListener { AlertDialog.Builder(context).setView(R.layout.alert_contents).setNegativeButton(
            "Cancel"
        ) { dialog, _ -> dialog.dismiss() }.setCancelable(true).setTitle(R.string.alert_title).setIcon(
            R.drawable.ic_navigation_black_36dp
        ).setMessage(R.string.alert_message).show() }

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

        busDirections = DirectionAdapter(mapsTaskResults.busStepsAndDuration.directions)
        walkingDirections = DirectionAdapter(mapsTaskResults.walkStepsAndDuration.directions)
        prtDirections = DirectionAdapter(mapsTaskResults.prtStepsAndDuration.directions)
        carDirections = DirectionAdapter(mapsTaskResults.carStepsAndDuration.directions)
        closestPRTA = mapsTaskResults.closestPRTA
        closestPRTB = mapsTaskResults.closestPRTB
        leavingTime = mapsTaskResults.leavingTime

        val directionsStepArrayAdapter = when (mapsTaskResults.fastestRoute) {
            Route.BUS -> busDirections
            Route.WALK -> walkingDirections
            Route.PRT -> prtDirections
            else -> carDirections
        }

        val btnSelectColor = getMuhDrawable(R.color.ButtonSelected)

        fun getButtonText(stepsAndDur : MapsDataClient.StepsAndDuration) : String {
            if(stepsAndDur.isAvailable) {
                return "${stepsAndDur.duration / 60} min"
            }
            return ""
        }

        busButton.text = getButtonText(mapsTaskResults.busStepsAndDuration)
        walkButton.text = getButtonText(mapsTaskResults.walkStepsAndDuration)
        prtButton.text = getButtonText(mapsTaskResults.prtStepsAndDuration)
        carButton.text = getButtonText( mapsTaskResults.carStepsAndDuration)

        list2.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL,false)
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
                busDirections
            }
            R.id.walkButton -> {
                walkButton.background = selectedColor
                walkingDirections
            }
            R.id.prtButton -> {
                prtButton.background = selectedColor
                prtDirections
            }
            else -> { // Assume Car
                carButton.background = selectedColor
                carDirections
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
        else if (model.status == "1")
            getMuhDrawable(R.drawable.rounded_textview_green)
        else
            getMuhDrawable(R.drawable.rounded_textview_red)
    }

    private fun getMuhDrawable(id: Int) : Drawable {
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

    private fun getDistanceFromLatLonInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double) : Double{
        val r = 6371 // Radius of the earth in km
        val dLat = deg2rad(lat2 - lat1)  // deg2rad below
        val dLon = deg2rad(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(deg2rad(lat1)) * cos(deg2rad(lat2)) * sin(dLon / 2) * sin(
            dLon / 2
        )

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c // Distance in km
    }

    private fun deg2rad(deg: Double) : Double {
        return deg * (PI / 180.0)
    }

    class DirectionAdapter(val dataList : ArrayList<MapsDataClient.SimpleDirections>) : RecyclerView.Adapter<DirectionsViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectionsViewHolder {
            val dirLayout = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false) as RelativeLayout
            return DirectionsViewHolder(dirLayout)
        }

        override fun onBindViewHolder(holder: DirectionsViewHolder, position: Int) {
            holder.textView.text = dataList[position].direction

            dataList[position].stepDistance?.let {
                holder.distance.visibility = View.VISIBLE
                holder.distance.text = it.toString()
            } ?: run {
                holder.distance.visibility = View.GONE
            }

            dataList[position].stepDuration?.let {
                holder.duration.visibility = View.VISIBLE
                holder.duration.text = it.toString()
            } ?: run {
                holder.duration.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int {
            return dataList.size
        }
    }

    class DirectionsViewHolder(val view : View) : RecyclerView.ViewHolder(view) {
        val textView : TextView = view.findViewById(R.id.list_item)
        val distance : TextView = view.findViewById(R.id.distance)
        val duration : TextView = view.findViewById(R.id.duration)
    }
}