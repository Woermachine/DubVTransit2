package com.stephenwoerner.dubvtransittwo

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.edit_course.*

/**
 * EditCourse allows a user to edit the contents of a Course which is stored as an entry in the database
 *
 * Created by srwoerner on 9/22/17.
 */
class EditCourse : Fragment() {
//    private lateinit var courseDbAdapter: CourseDbAdapter

    private lateinit var navController: NavController
    private lateinit var courseDb: CourseDb
    private var rowId: Long = 0
    private lateinit var title: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.edit_course, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)
        var isNew = true
        arguments?.let {
            isNew = it.getBoolean("isNew", true)
            if (!isNew) {
                title = it.getString("title")!!
            }
        } ?: run {
            Toast.makeText(activity, "Problem opening EditCourse", Toast.LENGTH_SHORT).show()
            childFragmentManager.popBackStack()
        }

        courseDb = CourseDb.get(requireContext())

        done_button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                run {
                    if (isNew)
                        courseDb.coursesQueries.insert(course = editable_course_title.text.toString(),
                            location = startBtn.text.toString(), note = course_note.text.toString())
                    else
                        courseDb.coursesQueries.update( course = editable_course_title.text.toString(),
                            location =  startBtn.text.toString(),note = course_note.text.toString(),
                            _id = rowId,)
                    childFragmentManager.popBackStack()
                }
            }
        })
        startBtn.setOnClickListener {
            showLocationList()
        }
        if (!isNew) fillData()
    }

    private fun fillData() {
        val course = courseDb.coursesQueries.selectCourse(title).executeAsOneOrNull()
        if (course != null) {
            rowId = course._id
            editable_course_title.setText( course.course)
            startBtn.text = course.location
            course_note.setText(course.note)
        }
    }

    private fun showLocationList() {
        val intent = Intent(context, PickLocationExpandable::class.java)
        intent.putExtra("useCourses", false)
        val requestCode = 2
        //TODO
//        startActivityForResult(intent, requestCode)
    }

    fun onResult(requestCode: Int, resultCode: Int, data: Intent) {
        //TODO
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                startBtn.text = data.getStringExtra("selected")
            }
        }
    }
}