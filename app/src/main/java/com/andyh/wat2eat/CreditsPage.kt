package com.andyh.wat2eat

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.andyh.wat2eat.databinding.ActivityCreditsPageBinding


class CreditsPage : AppCompatActivity() {

    private lateinit var binding: ActivityCreditsPageBinding
    private lateinit var closeBTN: ImageButton

    private lateinit var volleyLicenseBTN: TextView
    private lateinit var glideLicenseBTN: TextView
    private lateinit var googlePlacesAPILicenseBTN: TextView
    private lateinit var termsNConditions: TextView
    private lateinit var privacyPolicy: TextView

    private lateinit var gitHubLink: ImageButton
    private lateinit var instagramLink: ImageButton
    private lateinit var linkedInLink: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreditsPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initElement()
    }

    private fun initElement() {
        closeBTN = binding.CreditsPageCloseBTN

        volleyLicenseBTN = binding.CreditsPageVolleyLicenseBTN
        glideLicenseBTN = binding.CreditsPageGlideLicenseBTN
        googlePlacesAPILicenseBTN = binding.CreditsPageGooglePlacesAPILicenseBTN
        termsNConditions = binding.CreditsPageTermsNConditions
        privacyPolicy = binding.CreditsPagePrivacyPolicy

        gitHubLink = binding.CreditsPageGithubLink
        instagramLink = binding.CreditsPageInstagramLink
        linkedInLink = binding.CreditsPageLinkedInLink

        //to my social media profile
        gitHubLink.setOnClickListener {
            goToUrl(resources.getString(R.string.GitHubLink))
        }
        instagramLink.setOnClickListener {
            goToUrl(resources.getString(R.string.InstagramLink))
        }
        linkedInLink.setOnClickListener {
            goToUrl(resources.getString(R.string.LinkedInLink))
        }
        closeBTN.setOnClickListener {
            //use finish() instead of starting a new activity since our launch mode is "standard"
            finish()
        }

        volleyLicenseBTN.setOnClickListener {
            displayLargeTextScrollableDialog(R.string.noticeTitle,R.string.VolleyLicense)

        }
        glideLicenseBTN.setOnClickListener {
        displayLargeTextScrollableDialog(R.string.noticeTitle,R.string.GlideLicense)

        }


        googlePlacesAPILicenseBTN.setOnClickListener {
            displayLargeTextScrollableDialog(R.string.noticeTitle,R.string.GooglePlacesAPILicense)
        }
        termsNConditions.setOnClickListener {
            displayLargeTextScrollableDialog(R.string.termsNConditions,R.string.termsAndConditions_Context)
        }

        privacyPolicy.setOnClickListener {
            displayLargeTextScrollableDialog(R.string.privacyPolicy,R.string.privacyPolicy_Context)
        }
    }

    private fun displayLargeTextScrollableDialog(title:Int, message:Int){
        //display text in a textview => scrollview instead of using the setMessage() method of alert dialog
        //To let the hyper links be clickable inside of the message
        val tv = TextView(this)
        tv.movementMethod = LinkMovementMethod.getInstance()
        tv.setText(message)
        tv.setPadding(70, 20, 70, 0)
        val sv = ScrollView(this)
        sv.addView(tv)
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setView(sv)
        builder.setPositiveButton(R.string.close, null).show()
    }

    private fun goToUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
