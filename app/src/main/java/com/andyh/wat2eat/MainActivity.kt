package com.andyh.wat2eat

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
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
    private lateinit var searchBTN: Button
    private lateinit var creditsBTN: ImageButton
    private lateinit var infoBTN: ImageButton
    private val FINELOCATIONRQ = 101
    private lateinit var queue: RequestQueue
    private val apiKey = "AIzaSyDk0zxRUPq73N7hQ8nw7VhEgGcMdKRCpws"
    private lateinit var vibrator: Vibrator

    //parameter tags for API calls
    private val LANGUAGETAG = Locale.getDefault().toLanguageTag()
    private val ADDRESSTAG = "formatted_address"
    private val NAMETAG = "name"
    private val URLTAG = "url"
    private val RATINGTAG = "rating"
    private val PHOTOTAG = "photos"
    private val PHOTOREFERENCETAG = "photo_reference"
    private val RESULTTAG = "result"
    private val ATTRIBUTETAG = "html_attributions"

    //bundle tags
    val STATUSMESSAGETAG = "statueMessage"


    //since google counts one nearby search and two follow up next page query as one whole query and charge accordingly
    //only allow each search to make two next_page queries following a nearby search request
    var pageLimit = 3

    //use to keep track of how many pages are initialized
    //reset to 0 when activity starts
    private var pageCount = 0

    //PlaceID of a currently opened restaurant
    private var placeID: String? = null

    //We retrieve restaurant rating during the "Nearby Search" request
    //since "Rating" is in the category of "Atmosphere Data", which will be counted as additional cost to Place Detail request
    //*Note: Name, Address, URL are in "Basic Data" category, no additional cost
    private var restaurantRating: Double? = null;

    //keep track of restaurants that were presented to user before
    //to avoid showing duplicate restaurants
    var placesFoundPreviously = mutableSetOf<String>()

    //create mutableList container for $pageLimit number of nullable JSONObjects
    var dataContainer = mutableListOf<JSONObject>()

    //indicating whether restaurant data is ready or not
    var isDataReady = false

    //indicates that loading is finished
    //This value is used to unlock the infoBTN and About page when data fetching fails
    var isLoadingFinished = false


    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        //set content to root of binding instead of XML layout
        setContentView(binding.root)
        queue = Volley.newRequestQueue(this)
        initElements()

        val loadFrag = LoadingFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.MainActivity_restaurantFragmentContainer, loadFrag).commit()

        fetchNearByRestaurantData()

    }



    private fun initElements() {
        //note vibrator requires a permission in Manifest
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        //use view binding to bind view elements
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        searchBTN = binding.MainActivitySearchBTN
        infoBTN = binding.MainActivityInfoBTN
        creditsBTN = binding.MainActivityCreditsBTN

        creditsBTN.setOnClickListener {
            if (isLoadingFinished) {
                val intent = Intent(this, CreditsPage::class.java)
                startActivity(intent)
            }
        }

        infoBTN.setOnClickListener {
            if (isLoadingFinished) {
                displayTextDialog(R.string.infoDialogTitle, R.string.infoDialogMessage, true)

            }
        }

        //set listeners to buttons
        searchBTN.setOnClickListener {
            if (isDataReady) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    //only vibrate if user's devices api leve is >= 26
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            100,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                }
                for (i in dataContainer.indices) {
                    //for each page of the dataContainer
                    placeID = searchCurrentPage(dataContainer[i])
                    if (placeID != null) {
                        placesFoundPreviously.add(placeID.toString())
                        getPlaceDetail(placeID, restaurantRating)
                        break
                    }
                }
                //done scanning all the pages
                if (placeID == null) {
                    launchTextFragment(resources.getString(R.string.outOfRestaurants))
                }
            }
        }

        //disable the button until data is ready
        searchBTN.isEnabled = false
    }

    private fun fetchNearByRestaurantData() {
        //Internet is not a dangerous permission, runtime permission request is not required
        checkForPermissions(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            "Location",
            FINELOCATIONRQ
        )
    }

    private fun checkForPermissions(permission: String, name: String, requestCode: Int) {
        //check that user's OS systme has API level 23 or above
        //before API level 23, run time permission was not needed
        when {
            //check each statement individually
            //when statement exit once a branch is executed
            ContextCompat.checkSelfPermission(
                applicationContext,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                //User already approved location permission
                when {
                    !isGPSOn() -> {//GPS required dialog
                        displayTextDialog(
                            R.string.GPSRequestDialogTitle,
                            R.string.GPSRequestDialogMessage,
                            true
                        )
                        dataFetchingFailed()
                    }
                    !isWifiOn() -> {//Internet required dialog
                        displayTextDialog(
                            R.string.InternetConnectionRequestDialogTitle,
                            R.string.InternetConnectionRequestDialogMessage,
                            true
                        )
                        dataFetchingFailed()
                    }

                    isGPSOn() && isWifiOn() -> {
                        getLocation()
                    }
                }
            }

            shouldShowRequestPermissionRationale(permission) -> {
                displayTextDialog(
                    R.string.PermissionExplanationDialogTitle,
                    R.string.PermissionExplanationDialogMessage,
                    true
                )
                dataFetchingFailed()
            }
            else -> {
                requestPermissions(
                    arrayOf(permission),
                    requestCode
                )

            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            FINELOCATIONRQ -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    // system permission request dialog is popped and granted the location permission
                    //execute getLocation() after user approve the location permission
                    getLocation()
                } else {
                    //system permission request dialog is popped and user selected to not allow the location permission
                    dataFetchingFailed()
                }
                return
            }
            //make another branch for other permission here
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun getLocation() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.INTERNET
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationTask: Task<Location> = fusedLocationClient.getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            )
            //use .getCurrentLocation() instead of getLastLocation() since we only need to request the location once
            locationTask.addOnCompleteListener { p0 ->
                if (p0.isSuccessful) {
                    Log.d(
                        "MainActivity",
                        "long: ${p0.result.latitude} and lad: ${p0.result.longitude}"
                    )
                    makeRestaurantRequest(p0.result.latitude, p0.result.longitude)
                } else {
                    //request failed, GPS might be off
                    displayTextDialog(
                        R.string.restaurantRequestFailed_title,
                        R.string.restaurantRequestFailed_message,
                        true
                    )
                    dataFetchingFailed()
                    Log.d("MainActivity", "Get Location Failed ${p0.exception?.message}")
                }
            }
        }
    }

    private fun makeRestaurantRequest(latitude: Double, longitude: Double) {
        //this function only executed when location request was successful and geocode is obtained
        val queryRadius = 500 //meters
        val queryType = "restaurant"
        val queryURL =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=$latitude,$longitude&radius=$queryRadius&types=$queryType&key=$apiKey"

        Log.d("MainActivity", "makeRestaurantRequest: API request: $queryURL")
        val restaurantRequest = StringRequest(Request.Method.GET,
            queryURL,
            { response ->
                //Log.d("MainActivity","First response $response" )
                try {

                    dataContainer.add(JSONObject(response))
                    pageCount += 1
                    //first page has been initialized

                    if (dataContainer[pageCount - 1].has("next_page_token")) {
                        //user could be in the middle of no where and there's not enough restaurants for second page of data
                        val nextPageToken =
                            dataContainer[pageCount - 1].getString("next_page_token")

                        Handler().postDelayed({
                            //the next page token becomes available after a short time delay(refer to Google Places API docs)
                            //thus we delay the request to avoid error
                            makeNextPageRequest(nextPageToken)
                        }, 1500)

                    } else {
                        //done fetching data from Places API, start scanning for opening restaurants
                        isDataReady = true
                        isLoadingFinished = true
                        searchBTN.isEnabled = true
                        //let user know to that they can start searching restaurants
                        launchTextFragment(resources.getString(R.string.pressToSearch))

                    }
                } catch (exception: JSONException) {
                    Log.d(
                        "MainActivity",
                        "Got page $pageCount response, but error occur while requesting for next pages. Error Message: ${exception.message}"
                    )
                }
            },
            { error ->
                Log.d(
                    "MainActivity",
                    "Failed to get page $pageCount response  Error Message: ${error.message}"
                )
                //let user know to try again later
                displayTextDialog(
                    R.string.locationRequestFailed_title,
                    R.string.locationRequestFailed_Message,
                    true
                )
                dataFetchingFailed()
            })

        queue.add(restaurantRequest)
    }

    private fun makeNextPageRequest(nextPageToken: String) {
        val queryURL =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?pagetoken=$nextPageToken&key=$apiKey"
        Log.d("MainActivity", "NextPage Request Url: $queryURL")

        val nextPageRequest = StringRequest(Request.Method.GET,
            queryURL,
            { response ->
                //Log.d("MainActivity", "Next page response: $response")
                try {
                    dataContainer.add(JSONObject(response))
                    pageCount += 1

                    if (pageCount <= pageLimit && dataContainer[pageCount - 1].has("next_page_token")) {
                        //user could be in the middle of no where and there's not enough restaurants for second page of data
                        val nextPageToken =
                            dataContainer[pageCount - 1].getString("next_page_token")
                        Handler().postDelayed({
                            //the next page token becomes available after a short time delay(refer to Google Places API docs)
                            // thus we delay the request to avoid error
                            makeNextPageRequest(nextPageToken)
                        }, 2000)
                    } else {
                        //done fetching data from Places API, start scanning for opening restaurants
                        isDataReady = true
                        isLoadingFinished = true
                        searchBTN.isEnabled = true
                        //let user know to that they can start searching restaurants
                        launchTextFragment(resources.getString(R.string.pressToSearch))

                    }
                } catch (exception: JSONException) {
                    Log.d(
                        "MainActivity",
                        "Got page1 response, but error occur while requesting for next pages. Error Message: ${exception.message}"
                    )

                }

            },
            { error ->
                Log.d("MainActivity", "Page $pageCount request failed ${error.toString()}")
                //failed to get next page, but still allow user to see the pages that are already fetched
                isDataReady = true
                isLoadingFinished = true
                searchBTN.isEnabled = true
                //let user know to that they can start searching restaurants
                launchTextFragment(resources.getString(R.string.pressToSearch))
            })

        queue.add(nextPageRequest)

    }

    private fun getPlaceDetail(placeID: String?, rating: Double?) {

        if (placeID == null) {
            //should never get here, there should be a restaurant open if this function is called
        } else {
            val queryURL =
                "https://maps.googleapis.com/maps/api/place/details/json?language=$LANGUAGETAG&fields=$NAMETAG,$ADDRESSTAG,$URLTAG,$PHOTOTAG&place_id=$placeID&key=$apiKey"
            Log.d("MainActivity", "Place Detail Request Url: $queryURL ")
            val nextPageRequest = StringRequest(Request.Method.GET,
                queryURL,
                { response ->
                    try {
                        val jsonObject = JSONObject(response)
                        val placeDetail = jsonObject.getJSONObject(RESULTTAG)
                        val restaurantAddress = placeDetail.getString(ADDRESSTAG)
                        val restaurantName = placeDetail.getString(NAMETAG)
                        val restaurantURL = placeDetail.getString(URLTAG)
                        val restaurantPhotoArray = placeDetail.getJSONArray(PHOTOTAG)

                        //pick random photo out of all the photo returned
                        val selectedPhoto =
                            restaurantPhotoArray.getJSONObject(((0 until restaurantPhotoArray.length()).random()))
                        val restaurantPhotoReference = selectedPhoto.getString(PHOTOREFERENCETAG)

                        //attribution is the string "null" when there's nothing required

                        val restaurantPhotoAttribute =
                            selectedPhoto.getJSONArray(ATTRIBUTETAG)[0].toString()
                        sendDataToDisplay(
                            restaurantName,
                            restaurantRating,
                            restaurantAddress,
                            restaurantURL,
                            restaurantPhotoReference,
                            restaurantPhotoAttribute
                        )

                        Log.d(
                            "MainActivity",
                            "Place detail response: \naddress: $restaurantAddress\nname: $restaurantName\nRating: $rating\nURL: $restaurantURL\nPhoto_reference: $restaurantPhotoReference"
                        )

                    } catch (exception: JSONException) {
                        Log.d("MainActivity", "place detail response failed: ${exception.message}")
                    }

                },
                { error -> Log.d("MainActivity", "Next page request failed ${error.toString()}") })
            queue.add(nextPageRequest)
        }
    }

    private fun searchCurrentPage(jsonObject: JSONObject): String? {
        val currentPageResults: JSONArray = jsonObject.getJSONArray("results")

        //iterate through all the restaurants until an open place is founded
        for (i in (0 until currentPageResults.length())) {
            try {
                val restaurant = currentPageResults.getJSONObject(i)
                val restaurantOpeningHours = restaurant.getJSONObject("opening_hours")
                val isOpen = restaurantOpeningHours.getBoolean("open_now")
                val currentRestaurantPlaceID = restaurant.getString("place_id")
                if (isOpen && !placesFoundPreviously.contains(currentRestaurantPlaceID)) {
                    //return restaurant placeID if found
                    // also initialize restaurant rating alone with placeID found
                    restaurantRating = restaurant.getDouble("rating")
                    return currentRestaurantPlaceID
                }
            } catch (exception: JSONException) {
                //some restaurants might not have register their opening time
                //which makes their results end up here
                Log.d(
                    "MainActivity",
                    "scanCurrentPage: exception thrown ${exception.message} ${placeID}"
                )
            }

        }
        //only gets here when no restaurant is open in current page
        return null
    }

    private fun sendDataToDisplay(
        restaurantName: String,
        restaurantRating: Double?,
        restaurantAddress: String,
        restaurantURL: String,
        restaurantPhotoReference: String,
        restaurantPhotoAttribute: String
    ) {
        val bundle = Bundle()

        //pass default values over if there are no values received from api response
        bundle.putString(NAMETAG, restaurantName)
        bundle.putDouble(RATINGTAG, restaurantRating ?: 0.0)
        bundle.putString(ADDRESSTAG, restaurantAddress)
        bundle.putString(URLTAG, restaurantURL)
        bundle.putString(PHOTOREFERENCETAG, restaurantPhotoReference)
        bundle.putString(ATTRIBUTETAG, restaurantPhotoAttribute)

        val restaurantFragment = RestaurantFragment()
        restaurantFragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .replace(R.id.MainActivity_restaurantFragmentContainer, restaurantFragment).commit()

    }

    private fun launchTextFragment(message: String) {
        val textFrag = TextFragment()
        val bundle = Bundle()
        bundle.putString(STATUSMESSAGETAG, message)
        textFrag.arguments = bundle
        supportFragmentManager.beginTransaction()
            .add(R.id.MainActivity_restaurantFragmentContainer, textFrag).commit()

    }

    private fun isGPSOn(): Boolean {
        val manager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //GPS required dialog
            displayTextDialog(
                R.string.GPSRequestDialogTitle,
                R.string.GPSRequestDialogMessage,
                false
            )
            return false
        } else {
            return true
        }
    }

    private fun isWifiOn(): Boolean {
        val connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            // Wifi connection
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

            // Cellar connection
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

            // else return false
            else -> {
                false
            }
        }
    }

    private fun displayTextDialog(title: Int, message: Int, cancelable: Boolean) {
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(resources.getString(title))
        dialogBuilder.setMessage(resources.getString(message))
        dialogBuilder.setCancelable(cancelable)
        dialogBuilder.create().show()
    }

    private fun dataFetchingFailed(){
        //indicates that loading is finished and unlock About page and infoBTN
        isDataReady = false
        isLoadingFinished = true
        launchTextFragment(resources.getString(R.string.dataFetchingFailed))
    }

    override fun onStop() {
        super.onStop()
        //cancel the request when this activity is not visible
        CancellationTokenSource().cancel()
    }
}
