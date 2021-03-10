package com.stephenwoerner.dubvtransittwo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import android.widget.SimpleExpandableListAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.stephenwoerner.dubvtransittwo.shared.PRTModel
import timber.log.Timber

/**
 * Control interface to pick locations
 * Created by srwoerner on 5/13/17.
 */
class LocationListFragment : Fragment() {

    companion object {
        const val requestKeyArgKey = "requestKey"
        const val requestCodeArgKey = "requestCode"
        const val returnVal = "selected"
        const val allowUseCourses = "useCourses"
        const val allowCurrLocation = "allowCurrLocation"
    }

    private val NAME = "NAME"

    // string arrays for group and child items
    private var groupItems = arrayOf("PRT Stations", "Campus Buildings", "Dorms")
    private var childItems = arrayOf<Array<String>>(arrayOf(), arrayOf(), arrayOf())
    private val groupData: MutableList<Map<String, String?>> = ArrayList()
    private val childData: MutableList<List<Map<String, String?>>> = ArrayList()


    lateinit var navController: NavController
    lateinit var requestKey: String
    var requestCode: Int? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location_list, container, false)
    }

    lateinit var expandable_list: ExpandableListView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()

        expandable_list = view.findViewById(R.id.expandable_list)

        navController = Navigation.findNavController(view)
        args.getString(requestKeyArgKey)?.let {
            requestKey = it
        } ?: run {
            Timber.e("No Request Key")
            navController.navigateUp()
        }

        if (args.containsKey(requestCodeArgKey))
            requestCode = args.getInt(requestCodeArgKey)

        val useCourses = args.getBoolean(allowUseCourses, true)
        if (useCourses) {
            groupItems += "My Classes"
            childItems += arrayOf<String>()
        }

        val allowCurLoc = args.getBoolean(allowCurrLocation, true)
        if (allowCurLoc) {
            groupItems += "Other"
            childItems += arrayOf("Current Location")
        }

        val prtModel = PRTModel.get()
        val prtStrings = ArrayList(prtModel.prtHashMap.keys)
        prtStrings.sort()

        childItems[0] = prtStrings.toTypedArray()
        val buildingStrings = ArrayList(prtModel.buildingHashMap.keys)
        buildingStrings.sort()

        childItems[1] = buildingStrings.toTypedArray()
        val dormStrings = ArrayList(prtModel.dormHashMap.keys)
        dormStrings.sort()

        childItems[2] = dormStrings.toTypedArray()
        if (useCourses) {
            val courseStrings = ArrayList<String>()
            val courses =
                CourseDb.get(requireContext().applicationContext).coursesQueries.selectAll()
                    .executeAsList()
            for (course in courses) {
                courseStrings.add(course.course)
            }
            courseStrings.sort()
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
        val mAdapter = SimpleExpandableListAdapter(
            requireContext(), groupData,
            R.layout.group_item,
            groupFrom, groupTo,
            childData, R.layout.child_item,
            childFrom, childTo
        )
        expandable_list.setAdapter(mAdapter)

        // perform set on group click listener event
        expandable_list.setOnGroupClickListener { _, _, _, _ -> false }

        // perform set on child click listener event
        expandable_list.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val result = Bundle().apply {
                putString(returnVal, childItems[groupPosition][childPosition])
                requestCode?.let { putInt(requestCodeArgKey, it) }
            }
            parentFragmentManager.setFragmentResult(requestKey, result)
            navController.navigateUp()
            false
        }
    }
}