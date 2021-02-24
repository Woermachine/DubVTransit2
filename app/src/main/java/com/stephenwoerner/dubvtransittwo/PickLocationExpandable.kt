package com.stephenwoerner.dubvtransittwo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleExpandableListAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.expandable_list_view.*

/**
 * Control interface to pick locations
 * Created by srwoerner on 5/13/17.
 */
class PickLocationExpandable : Fragment() {

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

    lateinit var navController: NavController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.expandable_list_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        val useCourses = requireArguments().getBoolean("useCourses", true)
        if (!useCourses) {
            groupItems = groupItemsB
            childItems = childItemsB
        }
//        simpleExpandableListView = findViewById(R.id.expandable_list)
        val prtModel = PRTModel.get()
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
            val courses = CourseDb.get(requireContext().applicationContext).coursesQueries.selectAll().executeAsList()
            for (course in courses) {
                courseStrings.add(course.course)
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
        val mAdapter = SimpleExpandableListAdapter(requireContext(), groupData,
                R.layout.group_items,
                groupFrom, groupTo,
                childData, R.layout.child_items,
                childFrom, childTo)
        expandable_list.setAdapter(mAdapter)

        // perform set on group click listener event
        expandable_list.setOnGroupClickListener { _, _, _, _ -> false }
        // perform set on child click listener event
        expandable_list.setOnChildClickListener{ _, _, groupPosition, childPosition, _ ->
//            val resultIntent = Intent()
//            resultIntent.putExtra("selected", childItems[groupPosition][childPosition])
//            setResult(RESULT_OK, resultIntent)
//            finish()
            val result = Bundle().apply {
                putString("selected", childItems[groupPosition][childPosition])
            }
            parentFragmentManager.setFragmentResult("request_key", result)
            navController.navigateUp()
            false
        }
    }
}