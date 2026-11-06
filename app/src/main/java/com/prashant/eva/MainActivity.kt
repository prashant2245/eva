package com.prashant.eva

import android.Manifest
import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    // EVA ka wahi page jo GitHub Pages par live hai. Isi se saara UI/brain aata hai.
    private val EVA_URL = "https://prashant2245.github.io/eva/"

    private lateinit var webView: WebView

    private val contactPicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    val cursor = contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val idx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val number = if (idx >= 0) it.getString(idx) else null
                            if (number != null) dialNumber(number)
                            else Toast.makeText(this, "Is contact mein number nahi mila", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    private val micPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.addJavascriptInterface(Bridge(), "Android")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread { request.grant(request.resources) }
            }
        }
        webView.webViewClient = WebViewClient()
        webView.loadUrl(EVA_URL)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    private fun dialNumber(number: String) {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    /** index.html se window.Android.xxx() ke zariye call hota hai. */
    inner class Bridge {

        @JavascriptInterface
        fun call(number: String) {
            if (number.isNotBlank()) runOnUiThread { dialNumber(number) }
        }

        @JavascriptInterface
        fun pickContact() {
            runOnUiThread {
                val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                contactPicker.launch(intent)
            }
        }

        @JavascriptInterface
        fun youtubeSearch(query: String) {
            runOnUiThread {
                try {
                    val intent = Intent(Intent.ACTION_SEARCH)
                    intent.setPackage("com.google.android.youtube")
                    intent.putExtra("query", query)
                    startActivity(intent)
                } catch (e: Exception) {
                    val fallback = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))
                    )
                    startActivity(fallback)
                }
            }
        }

        @JavascriptInterface
        fun googleSearch(query: String) {
            runOnUiThread {
                try {
                    val intent = Intent(Intent.ACTION_WEB_SEARCH)
                    intent.putExtra(SearchManager.QUERY, query)
                    startActivity(intent)
                } catch (e: Exception) {
                    val fallback = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
                    )
                    startActivity(fallback)
                }
            }
        }
    }
}
