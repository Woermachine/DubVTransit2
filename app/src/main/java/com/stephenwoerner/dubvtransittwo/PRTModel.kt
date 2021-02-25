package com.stephenwoerner.dubvtransittwo

import android.content.Context
import com.google.gson.Gson
import com.google.maps.model.LatLng
import com.soywiz.klock.DateTime
import com.soywiz.klock.DayOfWeek
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.pow

class PRTModel private constructor() {
    val PRT_STATUS_CLOSED = "7"

    var status : String? = PRT_STATUS_CLOSED
    var message = ""
    private lateinit var duration : Array<String>
    private var busesDispatched = ""

    private lateinit var stations: Array<String>
    private val prtStations = arrayOf("WalnutPRT", "BeechurstPRT", "EngineeringPRT", "TowersPRT", "MedicalPRT")
    private val travelMins = doubleArrayOf(2.5, 5.0, 1.5, 3.0)
    lateinit var prtHashMap: HashMap<String, LatLng>
    lateinit var buildingHashMap: HashMap<String, LatLng>
    lateinit var dormHashMap: HashMap<String, LatLng>
    lateinit var allHashMap: HashMap<String, LatLng>
    private var lastPRTRequestTime = 0L

    companion object {
        private lateinit var model: PRTModel
        @JvmStatic
        fun get(): PRTModel {
            if (!::model.isInitialized) {
                model = PRTModel()
                model.initializeHashMaps()
            }
            return model
        }
    }

    fun estimateTime(prtStationA: String, prtStationB: String, timeInMillis: Long): Double {

        val dateTime = DateTime.fromUnix(timeInMillis)
        //Wait Time
        if (prtStationA == prtStationB) return -1.0
        var time = 3.0
        when (dateTime.dayOfWeek) {
            DayOfWeek.Monday, DayOfWeek.Wednesday, DayOfWeek.Friday -> if (dateTime.minutes > 45 || dateTime.minutes < 5) time += 7.0
            DayOfWeek.Tuesday, DayOfWeek.Thursday -> if (dateTime.minutes > 40 && dateTime.hours < 55) time += 7.0
            DayOfWeek.Saturday -> time += 2.0
            DayOfWeek.Sunday -> time+= 24*60*60*1000
        }
        //Average TravelTime
        //Implement traveltime between stations
        var stationAIndex = -1
        var stationBIndex = -1
        for (i in prtStations.indices) {
            if (prtStations[i] == prtStationA) stationAIndex = i
            if (prtStations[i] == prtStationB) stationBIndex = i
        }
        var x = stationAIndex
        if (stationAIndex < stationBIndex) {
            while (x < stationBIndex) {
                time += travelMins[x]
                x++
            }
        } else {
            while (x > 0 && x != stationBIndex) {
                time += travelMins[x - 1]
                x--
            }
        }
        return time
    }

//    fun openAtCalendarTime(calendar: Calendar): Boolean {
//        Timber.d("Open at calendar time ${calendar[Calendar.HOUR_OF_DAY]}:${calendar[Calendar.MINUTE]}")
//        when (calendar[Calendar.DAY_OF_WEEK]) {
//            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY -> if (calendar[Calendar.HOUR_OF_DAY] < 22 && (calendar[Calendar.HOUR_OF_DAY] > 6 || calendar[Calendar.HOUR_OF_DAY] > 5 && calendar[Calendar.MINUTE] > 30)) return true
//            Calendar.SATURDAY -> if (calendar[Calendar.HOUR_OF_DAY] < 17 && (calendar[Calendar.HOUR_OF_DAY] > 9 || calendar[Calendar.HOUR_OF_DAY] > 8 && calendar[Calendar.MINUTE] > 30)) return true
//        }
//        return false
//    }

    fun isOpen(departingTimeInMillis: Long): Boolean {

        val departingTime = DateTime.fromUnix(departingTimeInMillis)

        println( "${DirectionFragment::class.simpleName} Open at calendar time ${departingTime.hours}:${departingTime.minutes}")
        return when (departingTime.dayOfWeek) {
            DayOfWeek.Sunday -> false
            DayOfWeek.Saturday -> {
                val hourOfDay = departingTime.hours
                val min = departingTime.minutes

                hourOfDay in 9..17 || (hourOfDay == 8 && min > 30)
            }
            else -> {
                val hourOfDay = departingTime.hours
                val min = departingTime.minutes

                hourOfDay in 6..22 || (hourOfDay == 5 && min > 30)
            }
        }
    }

    fun isOpenNow() : Boolean {
        return status == "1"
    }


    suspend fun requestPRTStatus(): Boolean {
        val unixTime = DateTime.nowUnixLong()
        if (unixTime - lastPRTRequestTime < 30000)  //1 min
            return false

        val url = "https://prtstatus.wvu.edu/api/$unixTime/?format=json"
        val client = HttpClient {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }
        val response = client.get<String>(url)
        val prtResponse = Gson().fromJson(response, PRTResponse::class.java)
        client.close()
        status = prtResponse.status
        duration = prtResponse.duration
        message = prtResponse.message
        stations = prtResponse.stations
        busesDispatched = prtResponse.bussesDispatched
        lastPRTRequestTime = unixTime
        return true
    }

    data class PRTResponse(var status : String, var message : String, var timestamp : String,
                           var stations : Array<String>, var bussesDispatched : String,
                           var duration : Array<String>)


    /**
     * Returns where the PRT is open between stations
     *
     * @param stationA starting location
     * @param stationB destination location
     * @return true if open between stations A and B
     */
    fun openBetweenStations(stationA: String, stationB: String): Boolean {
        val stations = stations
        for (station in stations)
            if (station == stationA || station == stationB)
                return false
        return true
    }

    fun findClosestPRT(point: LatLng?): String {
        var closest = LatLng(0.0, 0.0)
        var closestVal = Int.MAX_VALUE.toDouble()
//        val prtModel = PRTModel.getInstance(context)
        val prtNames = ArrayList(prtHashMap.keys)
        val prtValues = ArrayList(prtHashMap.values)
        for (originPoint in prtValues) {
            val dist = (point!!.lat - originPoint.lat).pow(2.0) + (point.lng - originPoint.lng).pow(2.0)
            if (dist < closestVal) {
                closestVal = dist
                closest = originPoint
            }
        }
        return prtNames[prtValues.indexOf(closest)]
    }

    private fun initializeHashMaps() {
        val prtHashMap = hashMapOf<String,LatLng>()
        prtHashMap["Beechurst PRT"] = LatLng(39.6348785, -79.95615320000002)
        prtHashMap["Walnut PRT"] = LatLng(39.629987, -79.9571886)
        prtHashMap["Engineering PRT"] = LatLng(39.647082, -79.973278)
        prtHashMap["Towers PRT"] = LatLng(39.6479945, -79.96771839999997)
        prtHashMap["Medical PRT"] = LatLng(39.6547986, -79.9602754)

        val buildingHashMap = hashMapOf<String,LatLng>()
        buildingHashMap["Aerodynamics Laboratory"] = LatLng(39.645725, -79.974273)
        buildingHashMap["Advanced Engineering Research"] = LatLng(39.646060, -79.971092)
        buildingHashMap["Agricultural Sciences Building"] = LatLng(39.645932, -79.969992)
        buildingHashMap["Allen Hall"] = LatLng(39.646378, -79.967270)
        buildingHashMap["Armstrong Hall"] = LatLng(39.635000, -79.955709)
        buildingHashMap["Arnold Apartments"] = LatLng(39.632370, -79.950616)
        buildingHashMap["Art Museum of WVU"] = LatLng(39.649298, -79.974428)
        buildingHashMap["Agricultural Sciences Annex"] = LatLng(39.646700, -79.968184)
        buildingHashMap["Brooks Hall"] = LatLng(39.635737, -79.956299)
        buildingHashMap["Biomedical Research Facility"] = LatLng(39.655432, -79.957032)
        buildingHashMap["Boreman Residential Faculty"] = LatLng(39.633622, -79.952193)
        buildingHashMap["Bennett Tower"] = LatLng(39.648216, -79.967015)
        buildingHashMap["Business & Economics Building"] = LatLng(39.636642, -79.954725)
        buildingHashMap["Braxton Tower"] = LatLng(39.648435, -79.966258)
        buildingHashMap["Creative Arts Center"] = LatLng(39.648113, -79.975619)
        buildingHashMap["Chitwood Hall"] = LatLng(39.636113, -79.954639)
        buildingHashMap["Clark Hall"] = LatLng(39.633742, -79.954312)
        buildingHashMap["Colson Hall"] = LatLng(39.633952, -79.955350)
        buildingHashMap["Coliseum"] = LatLng(39.649352, -79.981563)
        buildingHashMap["Chemistry Research Laboratory"] = LatLng(39.633382, -79.953577)
        buildingHashMap["Chestnut Ridge Research Bldg"] = LatLng(39.657052, -79.955259)
        buildingHashMap["Crime Scene Garage"] = LatLng(39.649059, -79.964949)
        buildingHashMap["Eiesland Hall"] = LatLng(39.633655, -79.956114)
        buildingHashMap["E. Moore (Elizabeth Moore) Hall"] = LatLng(39.634944, -79.955218)
        buildingHashMap["ERC RFL Annex Office Bldg"] = LatLng(39.648088, -79.965920)
        buildingHashMap["Engineering Sciences Building"] = LatLng(39.645920, -79.973736)
        buildingHashMap["Evansdale Crossing"] = LatLng(39.647254, -79.972851)
        buildingHashMap["Evansdale Library"] = LatLng(39.645205, -79.971274)
        buildingHashMap["Animal Science Farm"] = LatLng(39.662415, -79.928455)
        buildingHashMap["Greenhouse-1"] = LatLng(39.644210, -79.96956)
        buildingHashMap["Hodges Hall"] = LatLng(39.634186, -79.956047)
        buildingHashMap["Honors Hall"] = LatLng(39.638071, -79.956474)
        buildingHashMap["Health Sciences North"] = LatLng(39.655283, -79.958153)
        buildingHashMap["Health Sciences South"] = LatLng(39.654193, -79.957922)
        buildingHashMap["Knapp Hall"] = LatLng(39.632612, -79.957085)
        buildingHashMap["Library (Downtown) (Charles C Wise Jr)"] = LatLng(39.633257, -79.954529)
        buildingHashMap["Life Sciences Building"] = LatLng(39.637067, -79.955551)
        buildingHashMap["Law Center"] = LatLng(39.648419, -79.958593)
        buildingHashMap["Lyon Tower"] = LatLng(39.647873, -79.966425)
        buildingHashMap["Martin Hall"] = LatLng(39.635547, -79.954956)
        buildingHashMap["Mary Babb Randolph Cancer Cntr"] = LatLng(39.653824, -79.958678)
        buildingHashMap["Museum Education Center"] = LatLng(39.649253, -79.973881)
        buildingHashMap["Ming Hsieh Hall"] = LatLng(39.636534, -79.953545)
        buildingHashMap["Mineral Resources Building"] = LatLng(39.646742, -79.973795)
        buildingHashMap["Mountainlair"] = LatLng(39.634675, -79.953722)
        buildingHashMap["Natatorium-Shell"] = LatLng(39.650079, -79.984009)
        buildingHashMap["National Research Center"] = LatLng(39.645279, -79.972020)
        buildingHashMap["Nursery School"] = LatLng(39.649740, -79.978870)
        buildingHashMap["Oglebay Hall"] = LatLng(39.636039, -79.953759)
        buildingHashMap["One Waterfront Place"] = LatLng(39.624745, -79.963545)
        buildingHashMap["CPASS Building"] = LatLng(39.649270, -79.969493)
        buildingHashMap["Percival Hall"] = LatLng(39.645645, -79.967380)
        buildingHashMap["Milan Puskar Center"] = LatLng(39.650274, -79.955187)
        buildingHashMap["South Agricultural Sciences"] = LatLng(39.645048, -79.970027)
        buildingHashMap["Student Recreation Center"] = LatLng(39.648179, -79.970909)
        buildingHashMap["Student Services Center"] = LatLng(39.635563, -79.953582)
        buildingHashMap["Stansbury Hall"] = LatLng(39.635076, -79.956940)
        buildingHashMap["Stewart Hall"] = LatLng(39.634303, -79.954392)
        buildingHashMap["Student Health"] = LatLng(39.649270, -79.969493)
        buildingHashMap["Woodburn Hall"] = LatLng(39.635981, -79.955428)
        buildingHashMap["White Hall"] = LatLng(39.632833, -79.954655)

        val dormHashMap = hashMapOf<String,LatLng>()
        dormHashMap["Arnold Hall"] = LatLng(39.632370, -79.950616)
        dormHashMap["Arnold Apartments"] = LatLng(39.632370, -79.950616)
        dormHashMap["Boreman Hall North"] = LatLng(39.633622, -79.952193)
        dormHashMap["Boreman Hall South"] = LatLng(39.633126, -79.952595)
        dormHashMap["Brooke Tower"] = LatLng(39.648989, -79.965792)
        dormHashMap["Bennett Tower"] = LatLng(39.648220, -79.967015)
        dormHashMap["Braxton Tower"] = LatLng(39.648435, -79.966264)
        dormHashMap["College Park Apartments"] = LatLng(39.636844, -79.946523)
        dormHashMap["Dadisman Hall"] = LatLng(39.635497, -79.952252)
        dormHashMap["Pierpont Housing"] = LatLng(39.650459, -79.963468)
        dormHashMap["Fieldcrest Hall"] = LatLng(39.652424, -79.963159)
        dormHashMap["Honors Hall"] = LatLng(39.638071, -79.956474)
        dormHashMap["Lincoln Hall"] = LatLng(39.649451, -79.965674)
        dormHashMap["Lyon Tower"] = LatLng(39.647873, -79.966425)
        dormHashMap["Med Center Apartment J"] = LatLng(39.653908, -79.962975)
        dormHashMap["Med Center Apartment K"] = LatLng(39.654085, -79.961956)
        dormHashMap["Summit Hall"] = LatLng(39.638740, -79.956619)
        dormHashMap["International House"] = LatLng(39.631924, -79.952491)
        dormHashMap["Stalnaker Hall"] = LatLng(39.635311, -79.952745)
        dormHashMap["University Place South"] = LatLng(39.639682, -79.956168)
        dormHashMap["University Place North"] = LatLng(39.640343, -79.956318)
        dormHashMap["Vandalia Blue"] = LatLng(39.638059, -79.951753)
        dormHashMap["Vandalia Gold"] = LatLng(39.638059, -79.951753)
        dormHashMap["Oakland Hall"] = LatLng(39.649984, -79.962908)
        dormHashMap["University Park"] = LatLng(39.650476, -79.962310)

        val allHashMap = hashMapOf<String,LatLng>()
        allHashMap.putAll(prtHashMap)
        allHashMap.putAll(buildingHashMap)
        allHashMap.putAll(dormHashMap)


        this.prtHashMap = prtHashMap
        this.buildingHashMap = buildingHashMap
        this.dormHashMap = dormHashMap
        this.allHashMap = allHashMap
    }

    fun findLatLng( name : String , currentLocation: LatLng, context : Context ) : LatLng? {
        return when (name) {
            context.getString(R.string.current_location) -> currentLocation
            context.getString(R.string.destination) -> null
            else -> {
                val lookupString = if (allHashMap.containsKey(name)) {
                    name
                } else {
                    val courseDb = CourseDb.get(context)
                    val course = courseDb.coursesQueries.selectCourse(name).executeAsOne()
                    course.location
                }
                model.allHashMap[lookupString]!!
            }
        }
    }

}
