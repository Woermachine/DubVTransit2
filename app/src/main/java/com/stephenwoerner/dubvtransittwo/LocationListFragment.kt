package com.stephenwoerner.dubvtransittwo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleExpandableListAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.stephenwoerner.dubvtransittwo.databinding.FragmentLocationListBinding
import timber.log.Timber

/**
 * Control interface to pick locations
 * Created by Stephen Woerner on 5/13/17.
 */
class LocationListFragment : Fragment() {

    companion object {
        const val requestKeyArgKey = "requestKey"
        const val requestCodeArgKey = "requestCode"
        const val returnVal = "selected"
        const val allowUseCourses = "useCourses"
        const val allowCurrLocation = "allowCurrLocation"
        const val showCourseBtn = "showCourseBtn"
        private const val NAME = "NAME"
    }

    // string arrays for group and child items
    private var groupItems = arrayOf("PRT Stations", "Campus Buildings", "Dorms")
    private var childItems = arrayOf<Array<String>>(arrayOf(), arrayOf(), arrayOf())
    private val groupData: MutableList<Map<String, String?>> = ArrayList()
    private val childData: MutableList<List<Map<String, String?>>> = ArrayList()
    private var alreadyLoaded = false


    private lateinit var navController: NavController
    private lateinit var binding : FragmentLocationListBinding
    private lateinit var requestKey: String
    private var requestCode: Int? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLocationListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()

        navController = Navigation.findNavController(view)
        args.getString(requestKeyArgKey)?.let {
            requestKey = it
        } ?: run {
            Timber.e("No Request Key")
            navController.navigateUp()
        }

        if(!alreadyLoaded) {
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
        }
        alreadyLoaded = true

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
        binding.apply {

            expandableList.apply {
                setAdapter(mAdapter)

                // perform set on group click listener event
                setOnGroupClickListener { _, _, _, _ -> false }

                // perform set on child click listener event
                setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
                    val result = Bundle().apply {
                        putString(returnVal, childItems[groupPosition][childPosition])
                        requestCode?.let { putInt(requestCodeArgKey, it) }
                    }
                    parentFragmentManager.setFragmentResult(requestKey, result)
                    navController.navigateUp()
                    false
                }
            }

            courseBtn.setOnClickListener {
                navController.navigate(R.id.action_pickLocationExpandable_to_courseList)
            }
            AppUtils.showView(courseBtn, args.getBoolean(showCourseBtn, true))
        }
    }
}