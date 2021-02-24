package com.stephenwoerner.dubvtransittwo

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Allows user to specify an origin, destination, and departure
 */
class MapFragment : Fragment() , View.OnClickListener, OnMapReadyCallback, LocationListener {
    private lateinit var leavingTime: Calendar
    private lateinit var model: PRTModel

    private lateinit var locationManager : LocationManager

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
    private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    private var useCurrentTime = true

    private lateinit var mMap: GoogleMap

    lateinit var navController: NavController

    private val location: com.google.maps.model.LatLng
        get() {
            var currentLocation = com.google.maps.model.LatLng(0.0, 0.0)
            locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
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
                currentLocation = com.google.maps.model.LatLng(lat, lon)

            }
            return currentLocation
        }

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View? {

        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        model = PRTModel.get()
        CoroutineScope(Dispatchers.IO).launch {
            model.requestPRTStatus()
        }
        val destBtn = view.findViewById<Button>(R.id.destBtn)
        destBtn.setOnClickListener { showLocationList(it) }

        val startBtn = view.findViewById<Button>(R.id.startBtn)
        startBtn.setOnClickListener { showLocationList(it) }

        leavingTime = Calendar.getInstance()
        val timeBtn = view.findViewById<Button>(R.id.timeBtn)
        timeBtn.text = timeFormat.format(leavingTime.time)

        val dateBtn = view.findViewById<Button>(R.id.dateBtn)
        dateBtn.text = dateFormat.format(leavingTime.time)

        val useCurrentTimeCB = view.findViewById<CheckBox>(R.id.useCurrentTimeCB)
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

        val prtStatusBtn = view.findViewById<FloatingActionButton>(R.id.prtStatusBtn)
        prtStatusBtn.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val prtOn = model.requestPRTStatus()
                activity?.runOnUiThread {
                    if(prtOn)
                        Toast.makeText(
                            requireContext().applicationContext,
                            "You can only update the status once every 30 seconds\nLong press to see full prt status",
                            Toast.LENGTH_LONG
                        ).show()

                    prtButtonColor()
                }
            }
        }
        prtStatusBtn.setOnLongClickListener {
            showPRTDialog()
            true
        }

        val continueBtn = view.findViewById<Button>(R.id.continueBtn)
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
        startBtn.setOnClickListener {
            showLocationList(it)
        }
        timeBtn.setOnClickListener {
            showTimePickerDialog()
        }
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
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
        val requestCode = if(v.id == R.id.destBtn) 0 else 1
        val intent = Intent(requireContext(), PickLocationExpandable::class.java)
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)  {
        super.onActivityResult(requestCode, resultCode, data)
        val destBtn = view?.findViewById<Button>(R.id.destBtn)
        val startBtn = view?.findViewById<Button>(R.id.startBtn)

        if (resultCode == AppCompatActivity.RESULT_OK) {
            val selected = data!!.getStringExtra("selected")!!
            when (requestCode) {
                0 -> {
                    destBtn?.text = selected
                }
                1 -> {
                    startBtn?.text = selected
                }
            }

            mMap.clear()

            val destStr = destBtn?.text.toString()
            val destLoc = model.findLatLng(destStr, location, requireContext().applicationContext)
            destLoc?.let {
                val destLatLng = LatLng(it.lat, it.lng)
                mMap.addMarker(
                    MarkerOptions()
                        .position(destLatLng)
                        .title(destStr)
                )
            }


            val startStr = startBtn?.text.toString()
            val startLoc = model.findLatLng(startStr, location, requireContext().applicationContext)
            startLoc?.let {
                val startLatLng = LatLng(it.lat, it.lng)
                mMap.addMarker(
                    MarkerOptions()
                        .position(startLatLng)
                        .title(startStr)
                )
            }

        }
    }

    /**
     * Reveals a time picker dialog
     */
    private fun showTimePickerDialog() {
        Timber.d("building time picker dialog")
        val timeBtn = view?.findViewById<Button>(R.id.timeBtn)
        val listener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            leavingTime[Calendar.HOUR_OF_DAY] = hourOfDay
            leavingTime[Calendar.MINUTE] = minute
            timeBtn?.text = timeFormat.format(leavingTime.time)
        }
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            listener,
            leavingTime[Calendar.HOUR_OF_DAY],
            leavingTime[Calendar.MINUTE],
            false
        )
        timePickerDialog.setTitle("Time Dialog")
        //timePickerDialog.amPM
        Timber.d("showing time picker dialog")
        timePickerDialog.show()
    }

    /**
     * Reveals a date picker dialog
     *
     */
    private fun showDatePickerDialog() {
        Timber.d("building date picker dialog")
        val dateBtn = view?.findViewById<Button>(R.id.dateBtn)
        val listener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            leavingTime[Calendar.YEAR] = year
            leavingTime[Calendar.MONTH] = month
            leavingTime[Calendar.DAY_OF_MONTH] = dayOfMonth
            dateBtn?.text = dateFormat.format(leavingTime.time)
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
        val destBtn = view?.findViewById<Button>(R.id.destBtn)
        if (destBtn?.text.toString().compareTo("Destination", true) == 0) {
            Timber.d("Failed to launch, no destination selected")
            Toast.makeText(requireContext(), "Must specify a destination", Toast.LENGTH_LONG).show()
            return
        }
        if (destBtn?.text.toString().compareTo("Current Location", true) == 0) {
            Timber.d("Failed to launch, current location is not a valid destination")
            Toast.makeText(requireContext(), "Current Location is not a valid destination", Toast.LENGTH_LONG)
                .show()
            return
        }
        val startBtn = view?.findViewById<Button>(R.id.startBtn)
        val intent = Intent(requireContext(), DirectionActivity::class.java)
        intent.putExtra("origin", startBtn?.text)
        intent.putExtra("destination", destBtn?.text)
        intent.putExtra("leavingTime", leavingTime.timeInMillis)
        intent.putExtra("useCurrentTime", useCurrentTime)
        Timber.d("Launching direction activity")
        startActivity(intent)
    }

    /**
     * Creates a new instance of CourseList
     */
    private fun launchCourseList() {
        val intent = Intent(requireContext(), CourseList::class.java)
        //intent.putExtra("origin",startView.getText());
        //intent.putExtra("destination",destView.getText());
        //intent.putExtra("leavingTime",leavingTime.getTimeInMillis());
        //intent.putExtra("useCurrentTime",useCurrentTime);
        Timber.d("Launching course list")
        startActivity(intent)
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
        val prt_badge = view?.findViewById<TextView>(R.id.prt_badge)
        if (model.status == "1")
            prt_badge?.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_bar_green)
        else
            prt_badge?.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_bar_red)
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just move the camera to Sydney and add a marker in Sydney.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isMapToolbarEnabled = false
//        val allPointsHM = model.allHashMap
//        model.allHashMap.keys.forEach { placeName ->
//            val loc = allPointsHM[placeName]!!
//            val location = LatLng(loc.lat, loc.lng)
//            mMap.addMarker(MarkerOptions()
//                    .position(location)
//                    .title(placeName))
//        }

        // Add a marker in Sydney and move the camera
        val morgantown = LatLng(39.634224, -79.954850)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(morgantown.latitude, morgantown.longitude), 13.0f))
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
        when(v!!.id) {
            courseBtn.id -> navController.navigate(R.id.action_mapFragment_to_courseList)
        }
    }
}