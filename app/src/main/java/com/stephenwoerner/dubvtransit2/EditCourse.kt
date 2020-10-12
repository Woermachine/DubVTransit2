package com.stephenwoerner.dubvtransit2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.edit_course.*

/**
 * EditCourse allows a user to edit the contents of a Course which is stored as an entry in the database
 *
 * Created by srwoerner on 9/22/17.
 */
class EditCourse : Activity() {
    private lateinit var courseDbAdapter: CourseDbAdapter
    private var rowId: Long = 0

    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.edit_course)
        val isNew = intent.getBooleanExtra("isNew", true)
        courseDbAdapter = CourseDbAdapter().open(applicationContext)
        val doneFab: FloatingActionButton = findViewById(R.id.done_button)
//        editableCourseTitle = findViewById(R.id.editable_course_title)
//        locationButton = findViewById(R.id.startView)
//        editNote = findViewById(R.id.course_note)
        doneFab.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                run {
                    if (isNew)
                        courseDbAdapter.createCourse(editable_course_title.text.toString(), startView.text.toString(), course_note.text.toString())
                    else
                        courseDbAdapter.updateCourse(rowId, editable_course_title.text.toString(), startView.text.toString(), course_note.text.toString())
                    finish()
                }
            }
        })
        if (!isNew) fillData()
    }

    private fun fillData() {
        val title = intent.getStringExtra("title")!!
        val cursor = courseDbAdapter.fetchCourse(title)
        cursor.moveToFirst()
        if (cursor.count == 1) {
            rowId = cursor.getLong(cursor.getColumnIndex(CourseDbAdapter.KEY_ROWID))
            editable_course_title.setText(cursor.getString(cursor.getColumnIndex(CourseDbAdapter.KEY_COURSE)))
            startView.text = cursor.getString(cursor.getColumnIndex(CourseDbAdapter.KEY_LOCATION))
            course_note.setText(cursor.getString(cursor.getColumnIndex(CourseDbAdapter.KEY_NOTE)))
        }
    }

    fun showLocationList(v: View?) {
        val intent = Intent(this, PickLocationExpandable::class.java)
        intent.putExtra("useCourses", false)
        val requestCode = 2
        startActivityForResult(intent, requestCode)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                startView.text = data.getStringExtra("selected")
            }
        }
    }
}