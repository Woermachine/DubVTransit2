package com.stephenwoerner.dubvtransittwo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.material.floatingactionbutton.FloatingActionButton
import timber.log.Timber

/**
 * EditCourse allows a user to edit the contents of a Course which is stored as an entry in the database
 *
 * Created by srwoerner on 9/22/17.
 */
class EditCourseFragment : Fragment(), FragmentResultListener {
    companion object {
        val requestKey = "key_${EditCourseFragment::class.java.simpleName}"
    }

    private lateinit var navController: NavController
    private lateinit var courseDb: CourseDb
    private var rowId: Long = 0
    private lateinit var title: String
    private var returned: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_course, container, false)
    }

    lateinit var done_button: FloatingActionButton
    lateinit var locationBtn: AppCompatButton
    lateinit var editable_course_title: EditText
    lateinit var course_note: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        done_button = view.findViewById(R.id.done_button)
        locationBtn = view.findViewById(R.id.locationBtn)
        editable_course_title = view.findViewById(R.id.editable_course_title)
        course_note = view.findViewById(R.id.course_note)

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

        courseDb = CourseDb.get(requireContext().applicationContext)

        done_button.setOnClickListener {
            if(locationBtn.text.toString() == getString(R.string.select_location)) {
                Toast.makeText(context, "Select a location, before saving", Toast.LENGTH_SHORT).show()
            } else {
                if (isNew)
                    courseDb.coursesQueries.insert(
                        course = editable_course_title.text.toString(),
                        location = locationBtn.text.toString(), note = course_note.text.toString()
                    )
                else
                    courseDb.coursesQueries.update(
                        course = editable_course_title.text.toString(),
                        location = locationBtn.text.toString(), note = course_note.text.toString(),
                        _id = rowId,
                    )
                navController.navigateUp()
            }
        }
        locationBtn.setOnClickListener {
            showLocationList()
        }
        if (!isNew) fillData()

        if (returned != null)
            locationBtn.text = returned

        parentFragmentManager.setFragmentResultListener(requestKey, requireActivity(), this)
    }

    private fun fillData() {
        val course = courseDb.coursesQueries.selectCourse(title).executeAsOneOrNull()
        if (course != null) {
            rowId = course._id
            editable_course_title.setText(course.course)
            locationBtn.text = course.location
            course_note.setText(course.note)
        }
    }

    private fun showLocationList() {
        val bundle = bundleOf(
            Pair(LocationListFragment.allowUseCourses, false),
            Pair(LocationListFragment.allowCurrLocation, false),
            Pair(LocationListFragment.requestKeyArgKey, requestKey)
        )
        navController.navigate(R.id.action_editCourse_to_pickLocationExpandable, bundle)
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        when (requestKey) {
            EditCourseFragment.requestKey -> {
                Timber.d(
                    String.format(
                        "Returned %s",
                        result.getString(LocationListFragment.returnVal)
                    )
                )
                returned = result.getString(LocationListFragment.returnVal)
            }
        }
    }
}