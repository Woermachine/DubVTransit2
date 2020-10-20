package com.stephenwoerner.dubvtransittwo

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * The main activity. Allows user to specify an origin, destination, and departure
 * Created by Stephen on 3/23/2017.
 */
class MainActivity : Activity() {
    private lateinit var leavingTime: Calendar
    private lateinit var model: PRTModel


    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
    private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    private var useCurrentTime = true

    override fun onCreate(savedInstanceState: Bundle?) {
        //initialize
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        model = PRTModel.get()
        CoroutineScope(IO).launch {
            model.requestPRTStatus()
        }
        destBtn.setOnClickListener { showLocationList(it) }
        startBtn.setOnClickListener { showLocationList(it) }
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
        prtStatusBtn.setOnClickListener {
            CoroutineScope(IO).launch {
                if (model.requestPRTStatus())
                    Toast.makeText(
                        applicationContext,
                        "You can only update the status once every 30 seconds\nLong press to see full prt status",
                        Toast.LENGTH_LONG
                    ).show()
                prtButtonColor()
            }
        }
        prtStatusBtn.setOnLongClickListener {
            showPRTDialog()
            true
        }
        continueBtn.setOnClickListener {
            launchDirectionActivity()
        }
        courseBtn.setOnClickListener {
            launchCourseList()
        }
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
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
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
    fun showLocationList(v: View) {
        val requestCode = if(v.id == R.id.destBtn) 0 else 1
        val intent = Intent(this, PickLocationExpandable::class.java)
        startActivityForResult(intent, requestCode)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)  {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val selected = data!!.getStringExtra("selected")
            when (requestCode) {
                0 ->  destBtn.text = selected
                1 ->  startBtn.text = selected
            }
        }
    }

    /**
     * Reveals a time picker dialog
     */
    private fun showTimePickerDialog() {
        Timber.d("building time picker dialog")
        val listener = OnTimeSetListener { _, hourOfDay, minute ->
            leavingTime[Calendar.HOUR_OF_DAY] = hourOfDay
            leavingTime[Calendar.MINUTE] = minute
            timeBtn.text = timeFormat.format(leavingTime.time)
        }
        val timePickerDialog = TimePickerDialog(
            this,
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
        val listener = OnDateSetListener { _, year, month, dayOfMonth ->
            leavingTime[Calendar.YEAR] = year
            leavingTime[Calendar.MONTH] = month
            leavingTime[Calendar.DAY_OF_MONTH] = dayOfMonth
            dateBtn!!.text = dateFormat.format(leavingTime.time)
        }
        val datePickerDialog = DatePickerDialog(
            this,
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
            Toast.makeText(this, "Must specify a destination", Toast.LENGTH_LONG).show()
            return
        }
        if (destBtn.text.toString().compareTo("Current Location", true) == 0) {
            Timber.d("Failed to launch, current location is not a valid destination")
            Toast.makeText(this, "Current Location is not a valid destination", Toast.LENGTH_LONG)
                .show()
            return
        }
        val intent = Intent(this, DirectionActivity::class.java)
        intent.putExtra("origin", startBtn.text)
        intent.putExtra("destination", destBtn.text)
        intent.putExtra("leavingTime", leavingTime.timeInMillis)
        intent.putExtra("useCurrentTime", useCurrentTime)
        Timber.d("Launching direction activity")
        startActivity(intent)
    }

    /**
     * Creates a new instance of CourseList
     */
    private fun launchCourseList() {
        val intent = Intent(this, CourseList::class.java)
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
        val alertDialog = AlertDialog.Builder(this).create()
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
            prt_badge.background = ContextCompat.getDrawable(this, R.drawable.rounded_bar_green)
        else
            prt_badge.background = ContextCompat.getDrawable(this, R.drawable.rounded_bar_red)
    }
}