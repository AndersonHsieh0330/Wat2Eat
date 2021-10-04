package com.andyh.wat2eat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.andyh.wat2eat.databinding.FragmentRestaurantBinding
import com.bumptech.glide.Glide
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
        val photoReference = arguments?.getString(PHOTOREFERENCE_TAG).toString()

        Glide.with(this)
            .load("https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photo_reference=$photoReference&key=AIzaSyDk0zxRUPq73N7hQ8nw7VhEgGcMdKRCpws")
            .into(binding.RestaurantFragmentImage)

        return binding.root
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