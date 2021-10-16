package com.andyh.wat2eat

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.andyh.wat2eat.databinding.ActivityCreditsPageBinding
import com.andyh.wat2eat.databinding.ActivityMainBinding
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import de.psdev.licensesdialog.LicensesDialog
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20
import de.psdev.licensesdialog.licenses.License
import de.psdev.licensesdialog.model.Notice

class CreditsPage : AppCompatActivity() {

    private lateinit var binding: ActivityCreditsPageBinding
    private lateinit var closeBTN: ImageButton
    private lateinit var glideLicenseBTN:Button
    private lateinit var googlePlacesAPILicenseBTN:Button
    private lateinit var licenseDialogLicenseBTN:Button

    private lateinit var gitHubLink:ImageButton
    private lateinit var instagramLink:ImageButton
    private lateinit var linkedInLink:ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreditsPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initElement()
    }

    private fun initElement(){
        closeBTN = binding.CreditsPageCloseBTN
        glideLicenseBTN= binding.CreditsPageGlideLisenceBTN
        googlePlacesAPILicenseBTN = binding.CreditsPageGooglePlacesAPILisenceBTN
        licenseDialogLicenseBTN = binding.CreditsPageLicenseDialogLicenseBTN

        gitHubLink = binding.CreditsPageGithubLink
        instagramLink = binding.CreditsPageInstagramLink
        linkedInLink = binding.CreditsPageLinkedInLink

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
            finish()
        }

        glideLicenseBTN.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("Notice")
            builder.setMessage(resources.getString(R.string.GlideLisence))
            builder.setPositiveButton("CLOSE",null).show()

        }


        googlePlacesAPILicenseBTN.setOnClickListener {
            val name = "LicensesDialog";
            val url = "http://psdev.de";
            val copyright = "Copyright 2013 Philip Schiffer <admin@psdev.de>";
            val license:License = ApacheSoftwareLicense20();
            val notice: Notice  = Notice(name, url, copyright, license);
            val builder = LicensesDialog.Builder(this)
            builder.setNotices(notice)
                .build()
                .show();
        }


        licenseDialogLicenseBTN.setOnClickListener {
            val name = "LicensesDialog";
            val url = "http://psdev.de";
            val copyright = "Copyright 2013 Philip Schiffer <admin@psdev.de>";
            val license:License = ApacheSoftwareLicense20();
            val notice: Notice  = Notice(name, url, copyright, license);
            val builder = LicensesDialog.Builder(this)
            builder.setNotices(notice)
                .build()
                .show();
        }}



    private fun goToUrl(url:String) {

        val intent:Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent)
    }
    }
