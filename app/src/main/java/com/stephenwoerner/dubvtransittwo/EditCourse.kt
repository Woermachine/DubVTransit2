package com.stephenwoerner.dubvtransittwo

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
//    private lateinit var courseDbAdapter: CourseDbAdapter

    private lateinit var courseDb: CourseDb
    private var rowId: Long = 0

    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.edit_course)
        val isNew = intent.getBooleanExtra("isNew", true)
        courseDb = CourseDb.get(applicationContext)
        val doneFab: FloatingActionButton = findViewById(R.id.done_button)

        doneFab.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                run {
                    if (isNew)
                        courseDb.coursesQueries.insert(course = editable_course_title.text.toString(),
                            location = startBtn.text.toString(), note = course_note.text.toString())
                    else
                        courseDb.coursesQueries.update( course = editable_course_title.text.toString(),
                            location =  startBtn.text.toString(),note = course_note.text.toString(),
                            _id = rowId,)
                    finish()
                }
            }
        })
        startBtn.setOnClickListener {
            showLocationList()
        }
        if (!isNew) fillData()
    }

    private fun fillData() {
        val title = intent.getStringExtra("title")!!
        val course = courseDb.coursesQueries.selectCourse(title).executeAsOneOrNull()
        if (course != null) {
            rowId = course._id
            editable_course_title.setText( course.course)
            startBtn.text = course.location
            course_note.setText(course.note)
        }
    }

    private fun showLocationList() {
        val intent = Intent(this, PickLocationExpandable::class.java)
        intent.putExtra("useCourses", false)
        val requestCode = 2
        startActivityForResult(intent, requestCode)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                startBtn.text = data.getStringExtra("selected")
            }
        }
    }
}