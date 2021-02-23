package com.stephenwoerner.dubvtransittwo

import android.app.ListActivity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.course_items.view.*
import kotlinx.android.synthetic.main.course_lists.*
import kotlin.collections.ArrayList

/**
 * Created by srwoerner on 8/26/17.
 */
class CourseList : Fragment(), View.OnClickListener, CellClickListener {

    //private String DEBUG = "CourseList";
//    private var context: Context? = null
//    private lateinit var mDbHelper: CourseDbAdapter
    private lateinit var mDb: CourseDb
    private lateinit var courseArrayList: ArrayList<String>
    private lateinit var navController: NavController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.course_lists, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        navController = Navigation.findNavController(view)
//        setContentView(R.layout.course_lists)
//        context = applicationContext
        //addButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); //getResources().getDrawable(R.drawable.ic_done_black_24dp)
        add_course_button.setOnClickListener(this)
        mDb = CourseDb.get(requireContext().applicationContext)
        course_list.layoutManager = LinearLayoutManager( requireContext(), RecyclerView.VERTICAL, false )
        fillData()
        //registerForContextMenu(getListView());
    }

    override fun onResume() {
        super.onResume()
        fillData()
    }

    private fun fillData() {
        val courseQuery = mDb.coursesQueries.selectAll().executeAsList()
        courseArrayList = ArrayList()
        for(c in courseQuery) {
            courseArrayList.add(c.course)
        }
//        course_list.notifyDataSe
//        val adapter = CustomArrayAdapter(applicationContext, R.layout.course_items, courseArrayList)
//        val a = CourseListAdapter(this, courseArrayList, this)
//        listAdapter = adapter
    }

    private inner class CustomArrayAdapter<T>(context: Context, textViewResourceId: Int, var categories: ArrayList<T>) : ArrayAdapter<T>(context, textViewResourceId, categories) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            if(convertView==null) {
                val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val v = inflater.inflate(R.layout.course_items, parent, false)
                val title = categories[position] as String
                v.course_title.text = title
                v.course_title.setOnClickListener {

                    val bundle = bundleOf(Pair("isNew", false), Pair("title", courseArrayList[position]))
                    navController.navigate(R.id.action_courseList_to_editCourse, bundle)

//                    val i = Intent(context, EditCourse::class.java)
//                    i.putExtra("isNew", false)
//                    i.putExtra("title", courseArrayList[position])
//                    startActivity(i)
                }

                v.delete_button.setOnClickListener {
                    mDb.coursesQueries.deleteByCourse(title)
                    fillData()
                }
                v.delete_button.setImageDrawable(ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete))
                return v
            }
            return convertView
        }
    }

    fun deleteCourse(str: String) {
        mDb.coursesQueries.deleteByCourse(str)

    }


    class CourseListAdapter(val dataList : ArrayList<String>) : RecyclerView.Adapter<CourseListViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseListViewHolder {
            val dirLayout = LayoutInflater.from(parent.context).inflate(R.layout.course_items, parent, false) as RelativeLayout
            return CourseListViewHolder(dirLayout)
        }

        override fun onBindViewHolder(holder: CourseListViewHolder, position: Int) {
            holder.textView.text = dataList[position]

            holder.deleteButton.setOnClickListener {
//                mDb.coursesQueries.deleteByCourse(dataList[position])
//                fillData()
            }
        }

        override fun getItemCount(): Int {
            return dataList.size
        }
    }

    class CourseListViewHolder(val view : View) : RecyclerView.ViewHolder(view) {
        val textView : TextView = view.findViewById(R.id.course_title)
        val deleteButton : ImageButton = view.findViewById(R.id.delete_button)
    }

    override fun onCellClickListener(position : Int) {
        TODO("Not yet implemented")
    }

    override fun onDeleteClickListener(position: Int) {
        TODO("Not yet implemented")
    }

    override fun onClick(v: View?) {
        when(v!!.id){
            add_course_button.id -> {
                val bundle = bundleOf(Pair("isNew", true))
                navController.navigate(R.id.action_courseList_to_editCourse, bundle)
            }
        }
    }
}

interface CellClickListener {
    fun onCellClickListener(position : Int)
    fun onDeleteClickListener(position : Int)
}