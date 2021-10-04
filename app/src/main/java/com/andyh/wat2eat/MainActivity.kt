package com.andyh.wat2eat

import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.replace
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.andyh.wat2eat.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var searchBTN:Button
    private val FINELOCATIONRQ = 101;
    private lateinit var queue:RequestQueue;
    private val apiKey = "AIzaSyDk0zxRUPq73N7hQ8nw7VhEgGcMdKRCpws"

    //since google counts one nearby search and two follow up next page query as one whole query and charge accordingly
    //only allow each search to make two next_page queries following a nearby search request
    var pageLimit = 3

    //use to keep track of how many pages are initialized
    //reset to 0 when activity starts
    private var pageCount = 0

    //PlaceID of a currently opened restaurant
    private var placeID:String? = null

    //We retrieve restaurant rating during the "Nearby Search" request
    //since "Rating" is in the category of "Atmosphere Data", which will be counted as additional cost to Place Detail request
    //*Note: Name, Address, URL are in "Basic Data" category, no additional cost
    private var restaurantRating:Int? = null;

    //keep track of restaurants that were presented to user before
    //to avoid showing duplicate restaurants
    var placesFoundPreviously = mutableSetOf<String>()

    //create mutableList container for $pageLimit number of nullable JSONObjects
    var dataContainer = mutableListOf<JSONObject>()

    //indicating whether restaurant data is ready or not
    var isDataReady = false


    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        
        //set content to root of binding instead of XML layout
        setContentView(binding.root)
        queue = Volley.newRequestQueue(this)
        initElements()

        fetchNearByRestaurantData()


    }


    private fun checkForPermissions(permission:String, name:String, requestCode:Int){
        //check that user's OS systme has API level 23 or above
        //before API level 23, run time permission was not needed
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            when{
                //check each statement individually
                //when statement exit once a branch is executed
                ContextCompat.checkSelfPermission(applicationContext,permission) == PackageManager.PERMISSION_GRANTED -> {
                    //User already approved location permission
                    getLocation()
                    Toast.makeText(applicationContext,"$name permission granted", Toast.LENGTH_SHORT).show()

                }

                shouldShowRequestPermissionRationale(permission) -> {
                    showExplanationDialog();

                }
                else -> {
                    requestPermissions(
                        arrayOf(permission),
                        requestCode)

                }
            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            FINELOCATIONRQ -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                                grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // system permission request dialog is poped and granted the location permission
                    //execute getLocation() after user approve the location permission
                    getLocation()
                } else {
                    //system permission request dialog is poped and user selected to not allow the location permission
                }
                return
            }
            //make another branch for other permission here
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun showExplanationDialog(){
        val dialogBuilder:AlertDialog.Builder =  AlertDialog.Builder(this)
        dialogBuilder.setTitle("Permission Required")

        dialogBuilder.create().show();
    }

    private fun initElements(){
        //use view binding to bind view elements
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        searchBTN = binding.MainActivitySearchBTN
        //set listeners to buttons
//         searchBTN.setOnClickListener {
//            val restaurantFragment = RestaurantFragment()
//            supportFragmentManager.beginTransaction().replace(R.id.MainActivity_restaurantFragmentContainer, restaurantFragment).commit()
//        }
        searchBTN.setOnClickListener {
            if(isDataReady) {
                for(i in dataContainer.indices){
                    placeID = searchCurrentPage(dataContainer[i])
                    if(placeID!=null){
                        placesFoundPreviously.add(placeID.toString())
                        getPlaceDetail(placeID,restaurantRating)
                        break
                    }
                }
                //no restaurant available in all the available pages
            }
        }
    }

    private fun fetchNearByRestaurantData(){
        //Internet is not a dangerous permission, runtime permission request is not required
        checkForPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION,"Location",FINELOCATIONRQ)
    }

    private fun getLocation() {
        if(ContextCompat.checkSelfPermission(applicationContext,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                &&ContextCompat.checkSelfPermission(applicationContext,android.Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED){
            val locationTask:Task<Location> = fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            //use .getCurrentLocation() instead of getLastLocation() since we only need to request the location once
            locationTask.addOnCompleteListener { p0 ->
                if(p0.isSuccessful){
                    Log.d("MainActivity","long: ${p0.result.latitude} and lad: ${p0.result.longitude}")
                    makeRestaurantRequest(p0.result.latitude, p0.result.longitude)
                }else{
                    //request failed, GPS might be off
                    Toast.makeText(this,"Location Request Failed, turn on GPS", Toast.LENGTH_SHORT).show()
                    Log.d("MainActivity", "Get Location Failed ${p0.exception?.message}")
                }
            }
        }




    }

    private fun makeRestaurantRequest(latitude: Double, longitude: Double){
        //this function only executed when location request was successful and geocode is obtained
        val queryRadius = 500 //meters
        val queryType = "restaurant"
        val queryURL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=$latitude,$longitude&radius=$queryRadius&types=$queryType&key=$apiKey"

        val restaurantRequest = StringRequest(Request.Method.GET,
                queryURL,
            { response ->
                //Log.d("MainActivity","First response $response" )
                try {

                    dataContainer.add(JSONObject(response))
                    pageCount += 1;
                    //first page has been initialized

                    if(dataContainer[pageCount-1].has("next_page_token")) {
                        //user could be in the middle of no where and there's not enough restaurants for second page of data
                        val nextPageToken = dataContainer[pageCount - 1].getString("next_page_token")

                        Handler().postDelayed(Runnable {
                            //the next page token becomes available after a short time delay(refer to Google Places API docs)
                            //thus we delay the request to avoid error
                            makeNextPageRequest(nextPageToken)
                        }, 2000)

                    }else{
                        //done fetching data from Places API, start scanning for opening restaurants
                        isDataReady = true
                    }
                }catch (exception: JSONException){
                    Log.d("MainActivity", "Got page $pageCount response, but failed to make request for page2. Error Message: ${exception.message}")
                }
            },
            { error -> Log.d("MainActivity", "Failed to get page $pageCount response  Error Message: ${error.message}")})

        queue.add(restaurantRequest)
    }

    private fun makeNextPageRequest(nextPageToken: String){
        val queryURL:String = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?pagetoken=$nextPageToken&key=$apiKey"
        Log.d("PPPPP", "url: $queryURL")

        val nextPageRequest = StringRequest(Request.Method.GET,
            queryURL,
            { response ->
                //Log.d("MainActivity", "Next page response: $response")
                try {
                    dataContainer.add(JSONObject(response))
                    pageCount += 1

                    if(pageCount<=pageLimit && dataContainer[pageCount-1].has("next_page_token")) {
                        //user could be in the middle of no where and there's not enough restaurants for second page of data
                        val nextPageToken = dataContainer[pageCount - 1].getString("next_page_token")
                        Handler().postDelayed(Runnable {
                            //the next page token becomes available after a short time delay(refer to Google Places API docs)
                            // thus we delay the request to avoid error
                            makeNextPageRequest(nextPageToken)
                        }, 2000)
                    }else{
                        //done fetching data from Places API, start scanning for opening restaurants
                        isDataReady = true
                    }
                }catch (exception: JSONException){
                    Log.d("MainActivity", "Got page1 response, but failed to make request for page2. Error Message: ${exception.message}")

                }

            },
            { error -> Log.d("MainActivity", "Page $pageCount request failed ${error.toString()}")})
        queue.add(nextPageRequest);

    }

    private fun getPlaceDetail(placeID:String?, rating:Int?){
        val LANGUAGE_TAG = Locale.getDefault().toLanguageTag()
        val ADDRESS_TAG = "formatted_address"
        val NAME_TAG = "name"
        val URL_TAG = "url"
        val PHOTO_TAG = "photo"
        if(placeID== null){
            //should never get here, there should be a restaurant open if this function is called
        }else{
            val queryURL:String = "https://maps.googleapis.com/maps/api/place/details/json?language=zh-TW&fields=$NAME_TAG,$ADDRESS_TAG,$URL_TAG,$PHOTO_TAG&place_id=$placeID&key=$apiKey"
            val nextPageRequest = StringRequest(Request.Method.GET,
                    queryURL,
                { response ->
                    try {
                        val jsonObject = JSONObject(response)
                        val placeDetail = jsonObject.getJSONObject("result")
                        Log.d("PPPPPP", "getPlaceDetail: $placeDetail")
                        val restaurantAddress = placeDetail.getString("formatted_address")
                        val restaurantName = placeDetail.getString("name")
                        val restaurantRating = rating
                        val restaurantURL = placeDetail.getString("url")

                        //Log.d("MainActivity","Place detail response: \naddress: $restaurantAddress\nname: $restaurantName\nRating: $restaurantRating\nURL: $restaurantURL ")

                    }catch (exception:JSONException){
                        Log.d("MainActivity", "place detail response failed")
                    }

                },
                { error -> Log.d("MainActivity", "Next page request failed ${error.toString()}")})
            queue.add(nextPageRequest);
        }
    }

    private fun searchCurrentPage(jsonObject: JSONObject):String?{
        val currentPageResults:JSONArray = jsonObject.getJSONArray("results")

        //iterate through all the restaurants until an open place is founded
        for(i in (0 until currentPageResults.length())){
            try {
                val restaurant = currentPageResults.getJSONObject(i)
                val restaurantOpeningHours = restaurant.getJSONObject("opening_hours")
                val isOpen = restaurantOpeningHours.getBoolean("open_now")
                val currentRestaurantPlaceID = restaurant.getString("place_id")
                if(isOpen&&!placesFoundPreviously.contains(currentRestaurantPlaceID)){
                    //return restaurant placeID if found
                        // also initialize restaurant rating alone with placeID found
                            restaurantRating = restaurant.getInt("rating")
                    return currentRestaurantPlaceID
                }
            }catch (exception: JSONException){
                //some restaurants might not have register their opening time
                //which makes their results end up here
                Log.d("MainActivity", "scanCurrentPage: exception thrown ${exception.message} ${placeID}")
            }

        }
        //only gets here when no restaurant is open in current page
        return null
    }

    override fun onStop() {
        super.onStop()
        //cancel the request when this activity is not visible
        CancellationTokenSource().cancel()
    }
}