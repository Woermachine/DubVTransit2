package com.stephenwoerner.dubvtransittwo

import android.Manifest
import android.annotation.SuppressLint
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
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.stephenwoerner.dubvtransittwo.databinding.FragmentMapBinding
import timber.log.Timber
import java.util.*
import kotlin.math.*


/**
 * Allows user to specify an origin, destination, and departure
 */
class MapFragment : Fragment(), OnMapReadyCallback, LocationListener,
    FragmentResultListener {

    companion object {
        val TAG: String = MapFragment::class.java.simpleName
        val requestKey = "key_$TAG"
        private const val PERMISSION_REQUEST_CODE = 1000
    }

    private lateinit var locationManager: LocationManager

    private lateinit var mMap: GoogleMap

    private lateinit var navController: NavController
    private lateinit var binding : FragmentMapBinding
    private val viewModel: MyViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)
        parentFragmentManager.setFragmentResultListener(requestKey, requireActivity(), this)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        viewModel.requestPRTStatus()

        binding.apply {
            viewModel.destination.observe(viewLifecycleOwner) { item ->
                destBtn.text = item
            }
            destBtn.setOnClickListener { showLocationList(it) }

            viewModel.source.observe(viewLifecycleOwner) { item ->
                locationBtn.text = item
            }
            locationBtn.setOnClickListener { showLocationList(it) }

            viewModel.prtUpdated.observe(viewLifecycleOwner) { isOpen ->
                isOpen?.let { open ->
                    if (open)
                        Toast.makeText(
                            requireContext().applicationContext,
                            "You can only update the status once every 30 seconds\nLong press to see full prt status",
                            Toast.LENGTH_LONG
                        ).show()

                    prtButtonColor()
                }
            }

            viewModel.leavingTime.observe(viewLifecycleOwner) {
                timeBtn.text = AppUtils.timeFormat.format(it.time)
                dateBtn.text = AppUtils.dateFormat.format(it.time)
            }

            useCurrentTimeCB.isChecked = viewModel.useCurrentTime.value ?: true
            viewModel.useCurrentTime.observe(viewLifecycleOwner) {
                AppUtils.showView(timeBtn, it)
                AppUtils.showView(dateBtn, it)
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
                viewModel.requestPRTStatus()
            }
            prtBadge.setOnLongClickListener {
                showPRTDialog()
                true
            }

            continueBtn.setOnClickListener {
                launchDirectionActivity()
            }


            dateBtn.setOnClickListener { showDatePickerDialog() }
            destBtn.setOnClickListener { showLocationList(it) }
            locationBtn.setOnClickListener { showLocationList(it) }
            timeBtn.setOnClickListener { showTimePickerDialog() }


            val showTime = viewModel.useCurrentTime.value ?: false
            AppUtils.showView(timeBtn, showTime)
            AppUtils.showView(dateBtn, showTime)

            prtButtonColor()
            Timber.d("setup complete")
        }
        requestLocationPermission()
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        } else {
            setUpLocationListener()

        }
    }

    @SuppressLint("MissingPermission")
    private fun setUpLocationListener() {

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
        viewModel.location.value = com.google.maps.model.LatLng(lat, lon)

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
                Timber.d(TAG,
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


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if(grantResults.contains(PermissionChecker.PERMISSION_DENIED)) {
                    requestLocationPermission()
                } else {
                    setUpLocationListener()
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
            viewModel.leavingTime.value?.apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
            }
        }
        viewModel.leavingTime.value?.let {

            val timePickerDialog = TimePickerDialog(
                requireContext(),
                listener,
                it[Calendar.HOUR_OF_DAY],
                it[Calendar.MINUTE],
                false
            )
            timePickerDialog.setTitle(R.string.time_dialog)
            Timber.d("showing time picker dialog")
            timePickerDialog.show()
        }
    }

    /**
     * Reveals a date picker dialog
     *
     */
    private fun showDatePickerDialog() {
        Timber.d("building date picker dialog")
        val listener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            viewModel.leavingTime.value?.apply {
                this[Calendar.YEAR] = year
                this[Calendar.MONTH] = month
                this[Calendar.DAY_OF_MONTH] = dayOfMonth
            }
        }
        viewModel.leavingTime.value?.let {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                listener,
                it[Calendar.YEAR],
                it[Calendar.MONTH],
                it[Calendar.DAY_OF_MONTH]
            )
            datePickerDialog.setTitle(R.string.date_dialog)
            Timber.d("showing date picker")
            datePickerDialog.show()
        }
    }

    /**
     * Sends information to a new instance of DirectionActivity via an intent
     */
    private fun launchDirectionActivity() {
        binding.apply {
            if (destBtn.text.toString().compareTo("Destination", true) == 0) {
                Timber.d("Failed to launch, no destination selected")
                Toast.makeText(requireContext(), R.string.must_specify, Toast.LENGTH_LONG)
                    .show()
                return
            }
            if (destBtn.text.toString().compareTo("Current Location", true) == 0) {
                Timber.d("Failed to launch, current location is not a valid destination")
                Toast.makeText(
                    requireContext(),
                    R.string.current_location_invalid,
                    Toast.LENGTH_LONG
                )
                    .show()
                return
            }

            val bundle = Bundle().apply {
                putString("origin", locationBtn.text.toString())
                putString("destination", destBtn.text.toString())
                putLong("leavingTime", viewModel.leavingTime.value!!.timeInMillis)
                putBoolean("useCurrentTime", viewModel.useCurrentTime.value!!)
            }

            if (locationBtn.text.toString() == getString(R.string.current_location)) {
                // Check distance to Morgantown
                val morgantown = com.google.maps.model.LatLng(39.634224, -79.954850)
                val hundredMilesInKM = 160.934

                viewModel.location.value?.let {
                    if (getDistanceFromLatLonInKm(
                            morgantown.lat,
                            morgantown.lng,
                            it.lat,
                            it.lng
                        ) < hundredMilesInKM
                    ) {
                        Timber.d("Launching DirectionFragment")
                        navController.navigate(R.id.action_mapFragment_to_directionFragment, bundle)
                    } else {
                        Toast.makeText(context, R.string.too_far_from_morgantown, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            } else {
                Timber.d("Launching DirectionFragment")
                navController.navigate(R.id.action_mapFragment_to_directionFragment, bundle)
            }
        }
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

    /**
     * Displays a simple message which is the current status of the PRT
     */
    private fun showPRTDialog() {
        Timber.d("building prt dialog")
        val alertDialog = AlertDialog.Builder(requireContext()).create()
        alertDialog.setMessage(viewModel.prtModel.value?.message)
        Timber.d("showing PRT Dialog")
        alertDialog.show()
    }

    /**
     * Set PRT status color
     */
    private fun prtButtonColor() {
        Timber.d("updating a prt status color")
            binding.prtBadge.background =
                ContextCompat.getDrawable(requireContext(),
                    if (viewModel.prtModel.value?.status == "1") R.drawable.rounded_bar_green
                    else R.drawable.rounded_bar_red)

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
        viewModel.apply {
            destination.observe(viewLifecycleOwner) { item ->
                location.value?.let { loc ->
                    prtModel.value?.findLatLng(item, loc, requireContext().applicationContext)
                        ?.let {
                            mMap.addMarker(
                                MarkerOptions()
                                    .position(LatLng(it.lat, it.lng))
                                    .title(item)
                            )
                        }
                }
            }

            source.observe(viewLifecycleOwner) { item ->
                viewModel.location.value?.let { loc ->
                    prtModel.value?.findLatLng(item, loc, requireContext().applicationContext)?.let {
                        mMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(it.lat, it.lng))
                                .title(item)
                        )
                    }
                }
            }
        }

        // Add a marker in Sydney and move the camera
        val morgantown = LatLng(39.634224, -79.954850)
        mMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    morgantown.latitude,
                    morgantown.longitude
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
        viewModel.location.value = com.google.maps.model.LatLng(location.latitude, location.longitude)
    }

}