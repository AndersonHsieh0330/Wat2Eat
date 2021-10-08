package com.andyh.wat2eat

import android.content .Context
import android.content.SharedPreferences
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.andyh.wat2eat.databinding.FragmentRestaurantBinding
import com.bumptech.glide.Glide
import java.lang.ClassCastException
import java.util.*

class RestaurantFragment : Fragment(){
    private lateinit var binding: FragmentRestaurantBinding;
    private lateinit var restaurantName: TextView;
    private lateinit var restaurantRating: RatingBar;
    private lateinit var restaurantAddress: TextView;
    private lateinit var restaurantURL: TextView;
    private lateinit var restaurantImage: ImageView;

    //TAGs for API calls
    val LANGUAGE_TAG = Locale.getDefault().toLanguageTag()
    val ADDRESS_TAG = "formatted_address"
    val NAME_TAG = "name"
    val URL_TAG = "url"
    val RATING_TAG = "rating"
    val PHOTO_TAG = "photo"
    val PHOTOREFERENCE_TAG = "photo_reference"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentRestaurantBinding.inflate(inflater,container,false)

        initElements()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("EEEEE", "onViewCreated: called")
        val preferences = this.context?.getSharedPreferences("Wat2Eat_Pref", Context.MODE_PRIVATE)

        val photoReference = arguments?.getString(PHOTOREFERENCE_TAG).toString()

        if(preferences?.contains("restaurantImageWidth")!=true){
            try{
                //get the size of the screen
                val vto = restaurantImage.viewTreeObserver
                vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        val width = restaurantImage.width
                        preferences?.edit()?.putInt("restaurantImageWidth", width)?.apply()
                        Log.d("EEEEE", "onViewCreated22: $width + ${preferences.toString()}")

                        if(preferences?.contains("restaurantImageWidth")== true){
                            vto.removeOnGlobalLayoutListener(this)
                        }
                    }


                })
                //center crop auto adjust the size of the image to fit the size of the imageview
                //but doesn't retain the aspect ratio of the photo
                //default photo width is
                Glide.with(this)
                    .load("https://maps.googleapis.com/maps/api/place/photo?maxwidth=970&photo_reference=$photoReference&key=AIzaSyDk0zxRUPq73N7hQ8nw7VhEgGcMdKRCpws")
                    .centerCrop()
                    .into(binding.RestaurantFragmentImage)
            }
            catch (exception: ClassCastException){
                Log.d("RestaurantFragment", "failed getting value from sharedpreference: ${exception.message}")
            }
        }else{

            val width = preferences?.getInt("restaurantImageWidth", 0)
            //center crop auto adjust the size of the image to fit the size of the imageview
            //but doesn't retain the aspect ratio of the photo
            Log.d("EEEEE", "onViewCreated: $width")
            Glide.with(this)
                .load("https://maps.googleapis.com/maps/api/place/photo?maxwidth=$width&photo_reference=$photoReference&key=AIzaSyDk0zxRUPq73N7hQ8nw7VhEgGcMdKRCpws")
                .centerCrop()
                .into(binding.RestaurantFragmentImage)
        }
    }

    private fun initElements(){

        restaurantName = binding.RestaurantFragmentRestaurantName
        restaurantRating = binding.RestaurantFragmentRating
        restaurantAddress = binding.RestaurantFragmentAddress
        restaurantURL = binding.RestaurantFragmentURL
        restaurantImage = binding.RestaurantFragmentImage

        restaurantName.text = arguments?.getString(NAME_TAG).toString()
        restaurantRating.rating = arguments?.getDouble(RATING_TAG)?.toFloat() ?: 0.0f
        restaurantAddress.text = arguments?.getString(ADDRESS_TAG).toString()
        restaurantURL.text = arguments?.getString(URL_TAG).toString()

    }

}