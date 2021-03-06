package com.stephenwoerner.dubvtransittwo

import android.Manifest.permission.*
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.CheckBox
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.model.LatLng
import com.stephenwoerner.dubvtransittwo.shared.Greeting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*


/**
 * Allows user to specify an origin, destination, and departure
 */
class MapFragment : Fragment(), View.OnClickListener, OnMapReadyCallback, LocationListener,
    FragmentResultListener {

    companion object {
        val TAG: String = MapFragment::class.java.simpleName
        val requestKey = "key_$TAG"
    }

    private lateinit var leavingTime: Calendar
    private lateinit var model: PRTModel

    private lateinit var locationManager: LocationManager

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
    private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    private var useCurrentTime = true

    private lateinit var mMap: GoogleMap

    lateinit var navController: NavController
    private val viewModel: MyViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    lateinit var destBtn: Button
    lateinit var locationBtn: Button
    lateinit var timeBtn: Button
    lateinit var dateBtn: Button
    lateinit var refreshBtn: AppCompatImageView
    lateinit var continueBtn: FloatingActionButton
    lateinit var courseBtn: FloatingActionButton
    lateinit var useCurrentTimeCB: CheckBox
    lateinit var prtBadge: RelativeLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("Hello from shared module: %s", Greeting().greeting())

        navController = Navigation.findNavController(view)
        parentFragmentManager.setFragmentResultListener(requestKey, requireActivity(), this)

        destBtn = view.findViewById(R.id.destBtn)
        locationBtn = view.findViewById(R.id.locationBtn)
        timeBtn = view.findViewById(R.id.timeBtn)
        dateBtn = view.findViewById(R.id.dateBtn)
        prtBadge = view.findViewById(R.id.prt_badge)
        refreshBtn = view.findViewById(R.id.refreshBtn)
        continueBtn = view.findViewById(R.id.continueBtn)
        courseBtn = view.findViewById(R.id.courseBtn)
        useCurrentTimeCB = view.findViewById(R.id.useCurrentTimeCB)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        model = PRTModel.get()
        CoroutineScope(Dispatchers.IO).launch {
            model.requestPRTStatus()
        }

        viewModel.destination.observe(viewLifecycleOwner, { item ->
            destBtn.text = item
        })
        destBtn.setOnClickListener { showLocationList(it) }

        viewModel.source.observe(viewLifecycleOwner, { item ->
            locationBtn.text = item
        })
        locationBtn.setOnClickListener { showLocationList(it) }

        leavingTime = Calendar.getInstance()
        timeBtn.text = timeFormat.format(leavingTime.time)
        dateBtn.text = dateFormat.format(leavingTime.time)

        useCurrentTimeCB.setOnClickListener {
            if (useCurrentTimeCB.isChecked) {
                useCurrentTime = true
                timeBtn.visibility = View.GONE
                dateBtn.visibility = View.GONE
            } else {
                useCurrentTime = false
                timeBtn.visibility = View.VISIBLE
                dateBtn.visibility = View.VISIBLE
            }
        }

        prtBadge.setOnClickListener {
            val rotateAnimation = RotateAnimation(
                0F,
                359F,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            rotateAnimation.repeatCount = Animation.ABSOLUTE
            rotateAnimation.repeatMode = Animation.RESTART
            rotateAnimation.duration = 1000
            refreshBtn.startAnimation(rotateAnimation)
            CoroutineScope(Dispatchers.IO).launch {
                val prtOn = model.requestPRTStatus()
                activity?.runOnUiThread {
                    if (prtOn)
                        Toast.makeText(
                            requireContext().applicationContext,
                            "You can only update the status once every 30 seconds\nLong press to see full prt status",
                            Toast.LENGTH_LONG
                        ).show()

                    prtButtonColor()
                }
            }
        }
        prtBadge.setOnLongClickListener {
            showPRTDialog()
            true
        }

        continueBtn.setOnClickListener {
            launchDirectionActivity()
        }

        courseBtn.setOnClickListener(this)

        dateBtn.setOnClickListener {
            showDatePickerDialog()
        }
        destBtn.setOnClickListener {
            showLocationList(it)
        }
        locationBtn.setOnClickListener {
            showLocationList(it)
        }
        timeBtn.setOnClickListener {
            showTimePickerDialog()
        }
        if (!hasPermissions()) {
            requestPermissions()
        }
        if (useCurrentTimeCB.isChecked) {
            useCurrentTime = true
            timeBtn.visibility = View.GONE
            dateBtn.visibility = View.GONE
        } else {
            useCurrentTime = false
            timeBtn.visibility = View.VISIBLE
            dateBtn.visibility = View.VISIBLE
        }
        prtButtonColor()
        Timber.d("setup complete")

    }

    /**
     * Show's a location of list
     *
     * @param v button
     */
    private fun showLocationList(v: View) {
        val requestCode = v.id
        val bundle = bundleOf(
            Pair(LocationListFragment.requestKeyArgKey, requestKey),
            Pair(LocationListFragment.requestCodeArgKey, requestCode)
        )
        if (v.id == R.id.destBtn)
            bundle.putBoolean(LocationListFragment.allowCurrLocation, false)

        navController.navigate(R.id.action_mapFragment_to_pickLocationExpandable, bundle)
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        when (requestKey) {
            MapFragment.requestKey -> {
                val requestCode = result.getInt(LocationListFragment.requestCodeArgKey)
                val selected = result.getString(LocationListFragment.returnVal)!!
                Timber.d(
                    String.format(
                        "PickLocationExpandable returned: %s %s",
                        requestCode,
                        selected
                    )
                )
                when (requestCode) {
                    R.id.destBtn -> viewModel.setDestination(selected)
                    R.id.locationBtn -> viewModel.setSource(selected)
                }
            }
        }
    }

    /**
     * Reveals a time picker dialog
     */
    private fun showTimePickerDialog() {
        Timber.d("building time picker dialog")
        val listener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            leavingTime[Calendar.HOUR_OF_DAY] = hourOfDay
            leavingTime[Calendar.MINUTE] = minute
            timeBtn.text = timeFormat.format(leavingTime.time)
        }
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            listener,
            leavingTime[Calendar.HOUR_OF_DAY],
            leavingTime[Calendar.MINUTE],
            false
        )
        timePickerDialog.setTitle("Time Dialog")
        Timber.d("showing time picker dialog")
        timePickerDialog.show()
    }

    /**
     * Reveals a date picker dialog
     *
     */
    private fun showDatePickerDialog() {
        Timber.d("building date picker dialog")
        val listener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            leavingTime[Calendar.YEAR] = year
            leavingTime[Calendar.MONTH] = month
            leavingTime[Calendar.DAY_OF_MONTH] = dayOfMonth
            dateBtn.text = dateFormat.format(leavingTime.time)
        }
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            listener,
            leavingTime[Calendar.YEAR],
            leavingTime[Calendar.MONTH],
            leavingTime[Calendar.DAY_OF_MONTH]
        )
        datePickerDialog.setTitle("Date Dialog")
        Timber.d("showing date picker")
        datePickerDialog.show()
    }

    /**
     * Sends information to a new instance of DirectionActivity via an intent
     */
    private fun launchDirectionActivity() {
        if (destBtn.text.toString().compareTo("Destination", true) == 0) {
            Timber.d("Failed to launch, no destination selected")
            Toast.makeText(requireContext(), "Must specify a destination", Toast.LENGTH_LONG).show()
            return
        }
        if (destBtn.text.toString().compareTo("Current Location", true) == 0) {
            Timber.d("Failed to launch, current location is not a valid destination")
            Toast.makeText(
                requireContext(),
                "Current Location is not a valid destination",
                Toast.LENGTH_LONG
            )
                .show()
            return
        }

        val bundle = Bundle().apply {
            putString("origin", locationBtn.text.toString())
            putString("destination", destBtn.text.toString())
            putLong("leavingTime", leavingTime.timeInMillis)
            putBoolean("useCurrentTime", useCurrentTime)
        }

        if (locationBtn.text.toString() == getString(R.string.current_location)) {
            // Check distance to Morgantown
            val morgantown = LatLng(39.634224, -79.954850)
            val hundredMilesInKM = 160934
            val location = getLocation()

            if (getDistanceFromLatLonInMeters(
                    morgantown.lat,
                    morgantown.lng,
                    location.lat,
                    location.lng
                ) < hundredMilesInKM
            ) {
                Timber.d("Launching DirectionFragment")
                navController.navigate(R.id.action_mapFragment_to_directionFragment, bundle)
            } else {
                Toast.makeText(context, "Too far from Morgantown", Toast.LENGTH_LONG).show()
            }
        } else {
            Timber.d("Launching DirectionFragment")
            navController.navigate(R.id.action_mapFragment_to_directionFragment, bundle)
        }
    }


    private fun getDistanceFromLatLonInMeters(
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

    /**
     * Displays a simple message which is the current status of the PRT
     */
    private fun showPRTDialog() {
        Timber.d("building prt dialog")
        val alertDialog = AlertDialog.Builder(requireContext()).create()
        alertDialog.setMessage(model.message)
        Timber.d("showing PRT Dialog")
        alertDialog.show()
    }

    /**
     * Set PRT status color
     */
    private fun prtButtonColor() {
        Timber.d("updating a prt status color")
        if (model.status == "1")
            prtBadge.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.rounded_bar_green)
        else
            prtBadge.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.rounded_bar_red)
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just move the camera to Sydney and add a marker in Sydney.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        if (AppUtils.isDarkTheme(requireActivity())) {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireActivity(),
                    R.raw.style_json
                )
            )
            if (!success) {
                Timber.e("Style parsing failed.")
            }
        }

        mMap = googleMap

        mMap.uiSettings.isMapToolbarEnabled = false

        mMap.clear()
        viewModel.destination.observe(viewLifecycleOwner, { item ->
            if(hasPermissions()) {
                val location = getLocation()
                val destLoc = model.findLatLng(item, location, requireContext().applicationContext)
                destLoc?.let {
                    val destLatLng = com.google.android.gms.maps.model.LatLng(it.lat, it.lng)
                    mMap.addMarker(
                        MarkerOptions()
                            .position(destLatLng)
                            .title(item)
                    )
                }
            }
        })

        viewModel.source.observe(viewLifecycleOwner, { item ->
            if(hasPermissions()) {
                val location = getLocation()
                val startLoc = model.findLatLng(item, location, requireContext().applicationContext)
                startLoc?.let {
                    val startLatLng =
                        com.google.android.gms.maps.model.LatLng(startLoc.lat, startLoc.lng)
                    mMap.addMarker(
                        MarkerOptions()
                            .position(startLatLng)
                            .title(item)
                    )
                }
            }
        })

        // Add a marker in Sydney and move the camera
        val morgantown = LatLng(39.634224, -79.954850)
        mMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                com.google.android.gms.maps.model.LatLng(
                    morgantown.lat,
                    morgantown.lng
                ), 13.0f
            )
        )
    }


    /**
     * update location
     * @param location, user's current location
     */
    override fun onLocationChanged(location: Location) {
        Timber.v("Location Changed ${location.latitude} and ${location.longitude}")
        locationManager.removeUpdates(this)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            courseBtn.id -> navController.navigate(R.id.action_mapFragment_to_courseList)
        }
    }

    private fun hasPermissions(): Boolean {
        var hasPerm = ContextCompat.checkSelfPermission(
            requireContext(),
            ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
//            hasPerm = hasPerm && ContextCompat.checkSelfPermission(
//                requireContext(),
//                ACCESS_BACKGROUND_LOCATION
//            ) == PackageManager.PERMISSION_GRANTED
//        }
        return hasPerm
    }

    private fun requestPermissions() {
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
//            ActivityCompat.requestPermissions(
//                requireActivity(),
//                arrayOf(
//                    ACCESS_FINE_LOCATION
//                ), 1
//            )
//        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(ACCESS_FINE_LOCATION), 1
            )
//        }
    }

    fun getLocation(): LatLng {
        var currentLocation = LatLng(0.0, 0.0)


        val hasPerm = ContextCompat.checkSelfPermission(
            requireContext(),
            ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermissions()) {
            requestPermissions()
        } else {
            locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
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

        }
        return currentLocation
    }
}