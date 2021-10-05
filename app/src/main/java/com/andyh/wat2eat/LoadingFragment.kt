package com.andyh.wat2eat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.andyh.wat2eat.databinding.FragmentLoadingBinding
import com.andyh.wat2eat.databinding.FragmentTextBinding


class LoadingFragment : Fragment() {

    private lateinit var binding:FragmentLoadingBinding;

    private lateinit var messageText:TextView;
    private lateinit var taskProgressBar:ProgressBar;
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding = FragmentLoadingBinding.inflate(inflater,container,false)
        initElement()
        return binding.root
    }

    private fun initElement(){
        messageText = binding.LoadingFragmentMessageText
        taskProgressBar = binding.LoadingFragmentProgressBar

        messageText.setText("Fetching data")
    }

}