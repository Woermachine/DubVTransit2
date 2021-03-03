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
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.maps.model.LatLng
import kotlinx.android.synthetic.main.fragment_directions.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*

class DirectionFragment : Fragment(), LocationListener {

    enum class Route { CAR, BUS, WALK, PRT }

    private lateinit var destinationStr: String
    private lateinit var closestPRTA: String
    private lateinit var closestPRTB: String
    private var useCurrentTime: Boolean = false
    private var selected : Int = R.id.carButton

    private lateinit var origin: LatLng
    private lateinit var destination: LatLng

    private var leavingTime = 0L

    private lateinit var locationManager: LocationManager

    private lateinit var carDirections: DirectionAdapter
    private lateinit var busDirections: DirectionAdapter
    private lateinit var walkingDirections: DirectionAdapter
    private lateinit var prtDirections: DirectionAdapter

    private var leavingTimeMillis: Long = 0L
    private val model = PRTModel.get()

    private val mapsDataClient = MapsDataClient()

    lateinit var navController: NavController
    private val viewModel: MyViewModel by activityViewModels()

    private val location: LatLng
        get() {
            var currentLocation = LatLng(0.0, 0.0)
            locationManager =
                requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ContextCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
                val criteria = Criteria()
                criteria.accuracy = Criteria.ACCURACY_FINE
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

                if (getDistanceFromLatLonInKm(
                        morgantown.lat,
                        morgantown.lng,
                        lat,
                        lon
                    ) > hundredMilesInKM
                ) {
                    navController.navigateUp()
                }

            }
            return currentLocation
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_directions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        walkButton.setOnClickListener {
            changeSelected(it)
        }

        busButton.setOnClickListener {
            changeSelected(it)
        }

        prtButton.setOnClickListener {
            changeSelected(it)
        }

        carButton.setOnClickListener {
            changeSelected(it)
        }

        var originStr = ""
        try {
            originStr = requireArguments().getString("origin")!!
            destinationStr = requireArguments().getString("destination")!!
            useCurrentTime = requireArguments().getBoolean("useCurrentTime", true)
            leavingTimeMillis = requireArguments().getLong(
                "leavingTime",
                Calendar.getInstance().timeInMillis
            )
        } catch (e: NullPointerException) {
            Timber.e(e)
            navController.navigateUp()
        }

        val displayOrigin = "From: $originStr"
        start_location.text = displayOrigin

        val displayDestination = "To: $destinationStr"
        destination_location.text = displayDestination

        origin = when (originStr) {
            getString(R.string.current_location) -> location
            else -> {
                val lookupString = if (model.allHashMap.containsKey(originStr)) {
                    originStr
                } else {
                    val courseDb = CourseDb.get(requireContext().applicationContext)
                    val course = courseDb.coursesQueries.selectCourse(originStr).executeAsOne()
                    course.location
                }
                model.allHashMap[lookupString]!!
            }
        }

        if (!model.allHashMap.containsKey(destinationStr)) { //If its not in the HashMap then its a user course
            val courseDb = CourseDb.get(requireContext().applicationContext)
            val course = courseDb.coursesQueries.selectCourse(destinationStr).executeAsOne()
            destinationStr = course.location
        }
        destination = model.allHashMap[destinationStr]!!

        navigationButton.setOnClickListener {
            val dest = if(selected == R.id.prtButton) R.string.nearest_prt else R.string.nearest_dest
            AlertDialog.Builder(requireActivity())
                .setIcon(R.drawable.navigation_black)
                .setTitle(R.string.alert_title)
                .setMessage(dest)
                .setPositiveButton("Open") { _,_ -> openMaps(dest) }
                .setCancelable(true)
                .show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            model.requestPRTStatus()
            val results = mapsDataClient.execute(
                leavingTimeMillis,
                origin,
                destination
            )

            requireActivity().runOnUiThread {
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

        fun getButtonText(stepsAndDur: MapsDataClient.StepsAndDuration): String {
            if (stepsAndDur.isAvailable) {
                return "${stepsAndDur.duration / 60} min"
            }
            return ""
        }

        busButton.text = getButtonText(mapsTaskResults.busStepsAndDuration)
        walkButton.text = getButtonText(mapsTaskResults.walkStepsAndDuration)
        prtButton.text = getButtonText(mapsTaskResults.prtStepsAndDuration)
        carButton.text = getButtonText(mapsTaskResults.carStepsAndDuration)

        list2.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
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
    private fun changeSelected(v: View) {
        val unselected = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.ButtonUnselected))
        carButton.background = unselected
        prtButton.background = unselected
        walkButton.background = unselected
        busButton.background = unselected

        selected = v.id
        val selectedColor = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.ButtonSelected))
        list2.adapter = when (selected) {
            R.id.busButton -> {
                busButton.background = selectedColor
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

    override fun onPause() {
        super.onPause()
        progress.visibility = View.GONE
    }

    /**
     * Open maps with a determined intent
     * @param strDest the string int representing prt or destination
     */
    private fun openMaps(strDest: Int) {
        val uriString = when(strDest) {
            R.string.nearest_dest -> "http://maps.google.com/maps?q=loc:" + destination.lat + "," + destination.lng + " (" + destinationStr + ")"
            else -> {
                val prt = model.allHashMap[closestPRTA]
                if (prt != null) {
                    "http://maps.google.com/maps?q=loc:" + prt.lat + "," + prt.lng + " ( Closest PRT Station )"
                } else {
                    ""
                }
            }
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
        requireActivity().startActivity(intent)
    }

    private fun prtButtonColor(): Drawable {
        return if (!useCurrentTime && model.isOpen(leavingTime))
            getMuhDrawable(R.drawable.rounded_textview_yellow)
        else if (useCurrentTime && model.openBetweenStations(
                closestPRTA,
                closestPRTB
            ) && model.status != "7"
        )
            getMuhDrawable(R.drawable.rounded_textview_yellow)
        else if (model.status == "1")
            getMuhDrawable(R.drawable.rounded_textview_green)
        else
            getMuhDrawable(R.drawable.rounded_textview_red)
    }

    private fun getMuhDrawable(id: Int): Drawable {
        return ContextCompat.getDrawable(requireContext().applicationContext, id)!!
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

    private fun getDistanceFromLatLonInKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r = 6371 // Radius of the earth in km
        val dLat = deg2rad(lat2 - lat1)  // deg2rad below
        val dLon = deg2rad(lon2 - lon1)
        val a =
            sin(dLat / 2) * sin(dLat / 2) + cos(deg2rad(lat1)) * cos(deg2rad(lat2)) * sin(dLon / 2) * sin(
                dLon / 2
            )

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c // Distance in km
    }

    private fun deg2rad(deg: Double): Double {
        return deg * (PI / 180.0)
    }

    class DirectionAdapter(private val dataList: ArrayList<MapsDataClient.SimpleDirections>) :
        RecyclerView.Adapter<DirectionsViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectionsViewHolder {
            val dirLayout = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item, parent, false) as RelativeLayout
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

    class DirectionsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.list_item)
        val distance: TextView = view.findViewById(R.id.distance)
        val duration: TextView = view.findViewById(R.id.duration)
    }
}