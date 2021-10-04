package com.andyh.wat2eat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.andyh.wat2eat.databinding.FragmentRestaurantBinding
import com.bumptech.glide.Glide

class RestaurantFragment : Fragment() {
    private lateinit var binding: FragmentRestaurantBinding;
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentRestaurantBinding.inflate(inflater,container,false)


        Glide.with(this)
            .load("https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photo_reference=Aap_uECLbgVtgRIByBJb64dt4Pm2tsUM_wgvQ0je7NyHn2luu4Cl67hRwp03Z6zM46Vzs1FYbWozKPC8bL0BfnmSogJvh0MjAJKlPRmWQSce1PFfgC8LvkuscLGW_8tIkRlyDi-CO-1w2XsZmUwdBlteeWm1q82CNaAIVaRkUW9d7cpFb5V6&key=AIzaSyDk0zxRUPq73N7hQ8nw7VhEgGcMdKRCpws")
            .into(binding.RestaurantFragmentImage)

        return binding.root
    }

}