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
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.female.maker.oc.creator2.core.extensions.gone
import com.female.maker.oc.creator2.core.extensions.hideNavigation
import com.female.maker.oc.creator2.core.extensions.invisible
import com.female.maker.oc.creator2.core.extensions.tap
import com.female.maker.oc.creator2.core.extensions.tapAndHold
import com.female.maker.oc.creator2.core.extensions.visible
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class DragonWebViewActivity : AppCompatActivity() {

    companion object {
        private const val MIN_SCALE = 0.5f
        private const val MAX_SCALE = 1.5f
    }

    private var currentColorId: String = ""
    private var pickedColor: Int = android.graphics.Color.WHITE


    private lateinit var binding: ActivityDragonWebViewBinding


    val adapter = PartRowAdapter(
        onOptionClick = { partId, value -> dispatch("SET_STYLE",
            "partId" to partId, "value" to value) },
        onColorClick = { colorId ->
            currentColorId = colorId
            binding.rvPanel.invisible()
            binding.btnRandomTraits.invisible()
            binding.btnRandomColor.invisible()
            binding.layoutChooseColor.visible()
        }
    )

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

        binding.btnRandomTraits.setOnClickListener {
            dispatch("RANDOMIZE_TRAITS") }
        binding.btnRandomColor.setOnClickListener {
            dispatch("RANDOMIZE_COLORS") }

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
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.webView.evaluateJavascript("""
   
  document.querySelector('.bottom-tabs').style.display='none';
   
  document.querySelector('.top-bar').style.display='none';
   
  document.querySelector('.action-row').style.display='none';
   
  document.querySelector('.panel-area').style.display='none';
          """.trimIndent(), null)
                binding.webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        }

        binding.webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        binding.actionBar.apply {
            btnActionBarLeft.visibility = android.view.View.VISIBLE
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.setOnClickListener { handleBackLeftToRight() }

            btnActionBarCenter.visibility = android.view.View.VISIBLE
            btnActionBarCenter.setImageResource(R.drawable.ic_reset)
            btnActionBarCenter.setOnClickListener { dispatch("RESET") }
        }

        binding.btnTabHead.setOnClickListener {
            selectTab(binding.btnTabHead)
            dispatch("SET_TAB", "tab" to "head")
            loadTab("head")
        }
        binding.btnTabTorso.setOnClickListener {
            selectTab(binding.btnTabTorso)
            dispatch("SET_TAB", "tab" to "torso")
            loadTab("torso")
        }
        binding.btnTabLegs.setOnClickListener {
            selectTab(binding.btnTabLegs)
            dispatch("SET_TAB", "tab" to "legs")
            loadTab("legs")
        }
        binding.btnTabWings.setOnClickListener {
            selectTab(binding.btnTabWings)
            dispatch("SET_TAB", "tab" to "wings")
            loadTab("wings")
        }
        binding.btnTabTail.setOnClickListener {
            selectTab(binding.btnTabTail)
            dispatch("SET_TAB", "tab" to "tail")
            loadTab("tail")
        }

        binding.rvPanel.layoutManager = LinearLayoutManager(this)
        binding.rvPanel.adapter = adapter
        loadTab("head")

        setupColorPicker()
        setupMoveControls()
        hideNavigation()


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

    fun loadTab(tab: String) {
        val rows = PartsConfigParser.load(assets, tab)
        adapter.submitList(rows)
    }

    private fun setupColorPicker() {
        binding.colorPickerView.hueSliderView = binding.hueSlider
        binding.colorPickerView.setOnColorChangedListener { color ->
            pickedColor = color
        }
        binding.btnBackColor.setOnClickListener { closeColorPicker() }
        binding.btnDoneColor.setOnClickListener {
            val hex = String.format("#%06X", 0xFFFFFF and pickedColor)
            dispatch("SET_COLOR", "colorId" to currentColorId, "hex" to hex)
            closeColorPicker()
        }
    }

    private fun closeColorPicker() {
        binding.layoutChooseColor.gone()
        binding.rvPanel.visible()
        binding.btnRandomTraits.visible()
        binding.btnRandomColor.visible()
    }

    private fun setupMoveControls() {
        val wv = binding.webView
        val STEP = 5f
        val ROTATE_STEP = 15f

        binding.btnMoveWebView.setOnClickListener {
            if (binding.layoutMove.isVisible) {
                binding.layoutMove.invisible()
                binding.rvPanel.visible()
                binding.btnRandomTraits.visible()
                binding.btnRandomColor.visible()
            } else {
                binding.layoutMove.visible()
                binding.rvPanel.invisible()
                binding.btnRandomTraits.invisible()
                binding.btnRandomColor.invisible()
            }
        }

        val initialProgress = scaleToProgress(1f)
        binding.SeekBar.max = 100
        binding.SeekBar.progress = initialProgress
        updateIcThumb(initialProgress)

        fun updateScaleFromTouch(event: MotionEvent): Boolean {
            if (binding.SeekBar.height <= 0) return true
            val loc = IntArray(2)
            binding.SeekBar.getLocationOnScreen(loc)
            val y = (event.rawY - loc[1]).coerceIn(0f, binding.SeekBar.height.toFloat())
            val progress = ((1f - y / binding.SeekBar.height) * binding.SeekBar.max)
                .toInt().coerceIn(0, binding.SeekBar.max)
            binding.SeekBar.progress = progress
            return true
        }

        binding.SeekBar.setOnTouchListener { _, event -> updateScaleFromTouch(event) }
        binding.IcThumb.setOnTouchListener { _, event -> updateScaleFromTouch(event) }

        binding.SeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val scale = progressToScale(progress)
                wv.scaleX = scale
                wv.scaleY = scale
                updateIcThumb(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnMoveLeft.tapAndHold {
            val maxX = wv.width / 2f
            wv.translationX = (wv.translationX - STEP).coerceIn(-maxX, maxX)
        }
        binding.btnMoveRight.tapAndHold {
            val maxX = wv.width / 2f
            wv.translationX = (wv.translationX + STEP).coerceIn(-maxX, maxX)
        }
        binding.btnMoveUp.tapAndHold {
            val maxY = wv.height / 2f
            wv.translationY = (wv.translationY - STEP).coerceIn(-maxY, maxY)
        }
        binding.btnMoveDown.tapAndHold {
            val maxY = wv.height / 2f
            wv.translationY = (wv.translationY + STEP).coerceIn(-maxY, maxY)
        }

        binding.rotateToLeft.tapAndHold { wv.rotation -= ROTATE_STEP }
        binding.rotateToRight.tapAndHold { wv.rotation += ROTATE_STEP }

        binding.btnResetMove.tap {
            wv.translationX = 0f
            wv.translationY = 0f
            wv.rotation = 0f
            wv.scaleX = 1f
            wv.scaleY = 1f
            val p = scaleToProgress(1f)
            binding.SeekBar.progress = p
            updateIcThumb(p)
        }
    }

    private fun progressToScale(progress: Int): Float =
        MIN_SCALE + (progress / 100f) * (MAX_SCALE - MIN_SCALE)

    private fun scaleToProgress(scale: Float): Int =
        (((scale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE)) * 100).toInt().coerceIn(0, 100)

    private fun updateIcThumb(progress: Int) {
        binding.SeekBar.post {
            val range = binding.SeekBar.height - binding.IcThumb.height
            binding.IcThumb.translationY = (0.5f - progress / 100f) * range
        }
    }
}