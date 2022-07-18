package com.htd.presensi.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.htd.presensi.R
import com.htd.presensi.rest.ApiClient.BASE_URL

class WebViewActivity : AppCompatActivity() {
    lateinit var webView:WebView
    lateinit var image:ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        supportActionBar?.title = "View"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val url = intent.getStringExtra("data")
        webView = findViewById(R.id.webView)
        image = findViewById(R.id.image)

        Log.d(packageName,url!!)

        if(url!!.contains(".jpg") and url!!.contains(".png") and url!!.contains(".jpeg") and url!!.contains(".gif")){
            webView.visibility = View.GONE
            image.visibility = View.VISIBLE
            Glide.with(this).load(url).into(image)
        }else{
            webView.webViewClient = WebViewClient()
            webView.settings.setSupportZoom(true)
            webView.settings.javaScriptEnabled = true
            webView.loadUrl("https://docs.google.com/gview?embedded=true&url=$url")
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}