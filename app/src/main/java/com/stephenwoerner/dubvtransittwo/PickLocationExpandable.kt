package com.stephenwoerner.dubvtransittwo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.SimpleExpandableListAdapter
import com.stephenwoerner.dubvtransittwo.PRTModel.Companion.get
import kotlinx.android.synthetic.main.expandable_list_view.*
import java.util.*

/**
 * Control interface to pick locations
 * Created by srwoerner on 5/13/17.
 */
class PickLocationExpandable : Activity() {

    companion object {
        private const val NAME = "NAME"
    }

    // string arrays for group and child items
    private var groupItems = arrayOf("PRT Stations", "Campus Buildings", "Dorms", "My Classes", "Other")
    private var childItems = arrayOf(arrayOf(), arrayOf(), arrayOf(), arrayOf(), arrayOf("Current Location"))
    private val groupData: MutableList<Map<String, String?>> = ArrayList()
    private val childData: MutableList<List<Map<String, String?>>> = ArrayList()

    //No Courses
    private val groupItemsB = arrayOf("PRT Stations", "Campus Buildings", "Dorms", "Other")
    private val childItemsB = arrayOf(arrayOf(), arrayOf(), arrayOf(), arrayOf("Current Location"))

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.expandable_list_view)
        val useCourses = intent.getBooleanExtra("useCourses", true)
        if (!useCourses) {
            groupItems = groupItemsB
            childItems = childItemsB
        }
//        simpleExpandableListView = findViewById(R.id.expandable_list)
        val prtModel = get(applicationContext)
        val prtStrings = ArrayList(prtModel.prtHashMap.keys)
        prtStrings.sort()
        //childItems[0] = prtStrings.toArray(new String[prtStrings.size()]);
        childItems[0] = prtStrings.toTypedArray()
        val buildingStrings = ArrayList(prtModel.buildingHashMap.keys)
        buildingStrings.sort()
        //childItems[1] = buildingStrings.toArray(new String[buildingStrings.size()]);
        childItems[1] = buildingStrings.toTypedArray()
        val dormStrings = ArrayList(prtModel.dormHashMap.keys)
        dormStrings.sort()
        //childItems[2] = dormStrings.toArray(new String[dormStrings.size()]);
        childItems[2] = dormStrings.toTypedArray()
        if (useCourses) {
            val courseStrings = ArrayList<String>()
            val courseDbAdapter = CourseDbAdapter().open(applicationContext)
            val cursor = courseDbAdapter.fetchAllCourses()
            while (cursor.moveToNext()) {
                courseStrings.add(cursor.getString(cursor.getColumnIndex(CourseDbAdapter.KEY_COURSE)))
            }
            courseStrings.sort()
            //childItems[3] = courseStrings.toArray(new String[courseStrings.size()]);
            childItems[3] = courseStrings.toTypedArray()
        }
        for (i in groupItems.indices) {
            val curGroupMap: MutableMap<String, String?> = HashMap()
            groupData.add(curGroupMap)
            curGroupMap[NAME] = groupItems[i]
            val children: MutableList<Map<String, String?>> = ArrayList()
            for (element in childItems[i]) {
                val curChildMap: MutableMap<String, String?> = HashMap()
                children.add(curChildMap)
                curChildMap[NAME] = element
            }
            childData.add(children)
        }


        // define arrays for displaying data in Expandable list view
        val groupFrom = arrayOf(NAME)
        val groupTo = intArrayOf(R.id.heading)
        val childFrom = arrayOf(NAME)
        val childTo = intArrayOf(R.id.childItem)


        // Set up the adapter
        val mAdapter = SimpleExpandableListAdapter(this, groupData,
                R.layout.group_items,
                groupFrom, groupTo,
                childData, R.layout.child_items,
                childFrom, childTo)
        expandable_list.setAdapter(mAdapter)

        // perform set on group click listener event
        expandable_list.setOnGroupClickListener { _, _, _, _ -> false }
        // perform set on child click listener event
        expandable_list.setOnChildClickListener{ _, _, groupPosition, childPosition, _ ->
            val resultIntent = Intent()
            resultIntent.putExtra("selected", childItems[groupPosition][childPosition])
            setResult(RESULT_OK, resultIntent)
            finish()
            false
        }
    }
}