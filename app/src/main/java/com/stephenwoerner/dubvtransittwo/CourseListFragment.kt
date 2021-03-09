package com.stephenwoerner.dubvtransittwo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Created by srwoerner on 8/26/17.
 */
class CourseListFragment : Fragment(), View.OnClickListener {

    //private String DEBUG = "CourseList";
//    private var context: Context? = null
//    private lateinit var mDbHelper: CourseDbAdapter
    private lateinit var mDb: CourseDb
    private lateinit var courseArrayList: ArrayList<COURSES>
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_course_lists, container, false)
    }

    lateinit var add_course_button: FloatingActionButton
    lateinit var course_list: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        add_course_button = view.findViewById(R.id.add_course_button)
        course_list = view.findViewById(R.id.course_list)

        navController = Navigation.findNavController(view)

        add_course_button.setOnClickListener(this)
        mDb = CourseDb.get(requireContext().applicationContext)
        course_list.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        fillData()
    }

    override fun onResume() {
        super.onResume()
        fillData()
    }

    private fun fillData() {
        val courseQuery = mDb.coursesQueries.selectAll().executeAsList()
        courseArrayList = ArrayList()
        for (c in courseQuery) {
            courseArrayList.add(c)
        }
//        course_list.notifyDataSe
//        val adapter = CustomArrayAdapter(applicationContext, R.layout.course_items, courseArrayList)
        val a = CourseListAdapter(courseArrayList)
        course_list.adapter = a
    }

    private inner class CourseListAdapter(val dataList: ArrayList<COURSES>) :
        RecyclerView.Adapter<CourseListViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseListViewHolder {
            val dirLayout = LayoutInflater.from(parent.context)
                .inflate(R.layout.course_item, parent, false) as RelativeLayout
            return CourseListViewHolder(dirLayout)
        }

        override fun onBindViewHolder(holder: CourseListViewHolder, position: Int) {
            holder.textView.text = dataList[position].course

            holder.deleteButton.setOnClickListener {
                mDb.coursesQueries.deleteByID(dataList[position]._id)
                dataList.removeAt(position)  // remove the item from list
                notifyItemRemoved(position)
                notifyDataSetChanged()
            }

            holder.cellBackground.setOnClickListener {
                val bundle = bundleOf(Pair("isNew", false), Pair("title", dataList[position].course))
                navController.navigate(R.id.action_courseList_to_editCourse, bundle)
            }

        }

        override fun getItemCount(): Int {
            return dataList.size
        }
    }

    class CourseListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.course_title)
        val deleteButton: ImageView = view.findViewById(R.id.delete_button)
        val cellBackground: View = view.findViewById(R.id.cell)
        val id: Int? = null
    }

    override fun onClick(v: View) {
        when (v.id) {
            add_course_button.id -> {
                val bundle = bundleOf(Pair("isNew", true))
                navController.navigate(R.id.action_courseList_to_editCourse, bundle)
            }
        }
    }
}
