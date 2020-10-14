package com.stephenwoerner.dubvtransit2

import android.app.ListActivity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.course_items.*
import kotlinx.android.synthetic.main.course_items.view.*
import kotlinx.android.synthetic.main.course_lists.*
import java.util.*

/**
 * Created by srwoerner on 8/26/17.
 */
class CourseList : ListActivity() {

    //private String DEBUG = "CourseList";
//    private var context: Context? = null
    private lateinit var mDbHelper: CourseDbAdapter
    private lateinit var courseArrayList: ArrayList<String>

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.course_lists)
//        context = applicationContext
        //addButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); //getResources().getDrawable(R.drawable.ic_done_black_24dp)
        add_course_button.setOnClickListener {
            val i = Intent(applicationContext, EditCourse::class.java)
            i.putExtra("isNew", true)
            startActivity(i)
        }
        mDbHelper = CourseDbAdapter()
        mDbHelper.open(applicationContext)
        fillData()
        //registerForContextMenu(getListView());
    }

    public override fun onResume() {
        super.onResume()
        fillData()
    }

    private fun fillData() {
        val courseCursor = mDbHelper.fetchAllCourses()
        courseArrayList = ArrayList()
        while (courseCursor.moveToNext()) {
            courseArrayList.add(courseCursor.getString(courseCursor.getColumnIndex(CourseDbAdapter.KEY_COURSE)))
        }
        val adapter = CustomArrayAdapter(applicationContext, R.layout.course_items, courseArrayList)
        listAdapter = adapter
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        super.onActivityResult(requestCode, resultCode, intent)
        fillData()
    }

    private inner class CustomArrayAdapter<T>(context: Context, textViewResourceId: Int, var categories: ArrayList<T>) : ArrayAdapter<T>(context, textViewResourceId, categories) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            if(convertView==null) {
                val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val v = inflater.inflate(R.layout.course_items, parent, false)
                val title = categories[position] as String
                v.course_title.text = title
                v.course_title.setOnClickListener {
                    val i = Intent(context, EditCourse::class.java)
                    i.putExtra("isNew", false)
                    i.putExtra("title", courseArrayList[position])
                    startActivity(i)
                }

                v.delete_button.setOnClickListener {
                    mDbHelper.deleteCourse(title)
                    fillData()
                }
                v.delete_button.setImageDrawable(ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete))
                return v
            }
            return convertView
        }

    }
}