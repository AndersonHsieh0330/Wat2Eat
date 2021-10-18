package com.andyh.wat2eat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.andyh.wat2eat.databinding.FragmentTextBinding

class TextFragment : Fragment() {
    private lateinit var binding: FragmentTextBinding
    private lateinit var statusText: TextView

    val STATUSMESSAGE_TAG = "statueMessage"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTextBinding.inflate(inflater,container,false)

        initElements()
        return binding.root
    }

    private fun initElements(){
        statusText = binding.TextFragmentStatusText
        statusText.text = arguments?.getString(STATUSMESSAGE_TAG).toString()
    }


}