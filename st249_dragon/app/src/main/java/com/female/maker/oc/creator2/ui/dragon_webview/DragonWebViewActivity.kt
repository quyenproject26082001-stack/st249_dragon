package com.female.maker.oc.creator2.ui.dragon_webview

import android.os.Bundle

import android.webkit.WebView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import com.female.maker.oc.creator2.R
import com.female.maker.oc.creator2.core.extensions.handleBackLeftToRight
import com.female.maker.oc.creator2.databinding.ActivityDragonWebViewBinding

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.widget.LinearLayout
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class DragonWebViewActivity : AppCompatActivity() {


    private lateinit var binding: ActivityDragonWebViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //setContentView(R.layout.activity_dragon_web_view)

        binding = ActivityDragonWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(
                "/assets/",
                WebViewAssetLoader.AssetsPathHandler(this)
            ).build()

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
        }

        binding.webView.addJavascriptInterface(DragonBridge(this), "AndroidBridge")

        binding.webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request!!.url)
            }
        }

        binding.webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        binding.actionBar.btnActionBarLeft.setOnClickListener { handleBackLeftToRight() }

        binding.btnTabTorso.setOnClickListener {
            selectTab(binding.btnTabTorso)
            dispatch("SET_TAB", "tab" to "torso")
        }
        binding.btnTabLegs.setOnClickListener {
            selectTab(binding.btnTabLegs)
            dispatch("SET_TAB", "tab" to "legs")
        }
        binding.btnTabWings.setOnClickListener {
            selectTab(binding.btnTabWings)
            dispatch("SET_TAB", "tab" to "wings")
        }
        binding.btnTabTail.setOnClickListener {
            selectTab(binding.btnTabTail)
            dispatch("SET_TAB", "tab" to "tail")
        }
        binding.btnTabHead.setOnClickListener {
            selectTab(binding.btnTabHead)
            dispatch("SET_TAB", "tab" to "head")
        }

    }

    fun dispatch(type: String, vararg params: Pair<String, String>) {
        val json = buildString {
            append("{\"type\":\"$type\"")
            params.forEach { (k, v) -> append(",\"$k\":\"$v\"") }
            append("}")
        }
        runOnUiThread {

            binding.webView.evaluateJavascript(
                "window.dispatch('${json}')",
                null
            )
        }
    }

    fun selectTab(selected: LinearLayout) {
        listOf(
            binding.btnTabHead,
            binding.btnTabTorso,
            binding.btnTabLegs,
            binding.btnTabWings,
            binding.btnTabTail
        ).forEach { it.isSelected = false }
        selected.isSelected = true
    }

    fun saveAndShare(dataUrl: String) {
        try {
            val bytes =
                Base64.decode(
                    dataUrl.substringAfter("base64,"),
                    Base64.DEFAULT
                )
            val bitmap = BitmapFactory.decodeByteArray(
                bytes,
                0, bytes.size
            )
            val ts = SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()
            ).format(Date())
            val dir = File(cacheDir, "dragons").also {
                it.mkdirs()
            }
            val file = File(dir, "dragon_$ts.png")
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.provider", file
            )
            runOnUiThread {
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)

                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, "Share Dragon"
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("DragonWebView", "saveAndShare:${e.message}")
        }
    }
}