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
import androidx.core.view.marginEnd
import androidx.core.view.setPadding
import com.andyh.wat2eat.databinding.ActivityCreditsPageBinding
import com.andyh.wat2eat.databinding.ActivityMainBinding
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import de.psdev.licensesdialog.LicensesDialog
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20
import de.psdev.licensesdialog.licenses.License
import de.psdev.licensesdialog.model.Notice
import org.w3c.dom.Text

class CreditsPage : AppCompatActivity() {

    private lateinit var binding: ActivityCreditsPageBinding
    private lateinit var closeBTN: ImageButton

    private lateinit var volleyLicenseBTN:TextView
    private lateinit var glideLicenseBTN:TextView
    private lateinit var googlePlacesAPILicenseBTN:TextView
    private lateinit var licenseDialogLicenseBTN:TextView

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

        volleyLicenseBTN = binding.CreditsPageVolleyLicenseBTN
        glideLicenseBTN= binding.CreditsPageGlideLicenseBTN
        googlePlacesAPILicenseBTN = binding.CreditsPageGooglePlacesAPILicenseBTN

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

        volleyLicenseBTN.setOnClickListener {
            val name = "Volley";
            val url = "https://github.com/google/volley/blob/master/LICENSE";
            val copyright = "Copyright 2013 Philip Schiffer <admin@psdev.de>";
            val license:License = ApacheSoftwareLicense20();
            val notice: Notice  = Notice(name, url, copyright, license);
            val builder = LicensesDialog.Builder(this)
            builder.setNotices(notice)
                .build()
                .show();
        }
        glideLicenseBTN.setOnClickListener {
            val tv = TextView(this)
            tv.movementMethod=LinkMovementMethod.getInstance()
            tv.setText(R.string.GlideLicense)
            tv.setPadding(20,20,20,0)
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.notices_title)
            builder.setView(tv)
            builder.setPositiveButton(R.string.notices_close,null).show()

        }


        googlePlacesAPILicenseBTN.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.notices_title)
            builder.setMessage(resources.getString(R.string.GlideLicense))
            builder.setPositiveButton(R.string.notices_close,null).show()
        }
    }



    private fun goToUrl(url:String) {

        val intent:Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent)
    }
    }
