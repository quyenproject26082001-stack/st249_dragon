package com.dragon.oc.avatar.creator.ui.dragon_webview

import android.os.Bundle

import android.webkit.WebView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import com.dragon.oc.avatar.creator.R
import com.dragon.oc.avatar.creator.core.extensions.handleBackLeftToRight
import com.dragon.oc.avatar.creator.databinding.ActivityDragonWebViewBinding

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.dragon.oc.avatar.creator.core.extensions.gone
import com.dragon.oc.avatar.creator.core.extensions.hideNavigation
import com.dragon.oc.avatar.creator.core.extensions.invisible
import com.dragon.oc.avatar.creator.core.extensions.tap
import com.dragon.oc.avatar.creator.core.extensions.tapAndHold
import com.dragon.oc.avatar.creator.core.extensions.visible
import com.dragon.oc.avatar.creator.core.helper.MediaHelper
import com.dragon.oc.avatar.creator.core.utils.key.IntentKey
import com.dragon.oc.avatar.creator.core.utils.key.ValueKey
import com.dragon.oc.avatar.creator.data.model.custom.DragonCardEditModel
import com.dragon.oc.avatar.creator.dialog.DialogType
import com.dragon.oc.avatar.creator.dialog.WaitingDialog
import com.dragon.oc.avatar.creator.dialog.YesNoDialog
import com.dragon.oc.avatar.creator.ui.edit.EditActivity
import com.dragon.oc.avatar.creator.ui.success.SuccessActivity
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class DragonWebViewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_AUTO_RANDOM = "extra_auto_random"
        const val EXTRA_SELECTION_STATE = "extra_selection_state"
        const val EXTRA_EDIT_MODEL_ID = "extra_edit_model_id"
        private const val MIN_SCALE = 0.5f
        private const val MAX_SCALE = 1.5f
    }

    private var currentColorId: String = ""
    private var pickedColor: Int = android.graphics.Color.WHITE
    private var editSourcePath: String = ""
    private var editModelId: String = ""
    private var isWaitingForRender = false
    private val loadingDialog by lazy { WaitingDialog(this) }


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
        editSourcePath = intent.getStringExtra(IntentKey.EDIT_SOURCE_PATH).orEmpty()
        editModelId = intent.getStringExtra(EXTRA_EDIT_MODEL_ID).orEmpty()

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(
                "/assets/",
                WebViewAssetLoader.AssetsPathHandler(this)
            ).build()

        binding.btnRandomTraits.setOnClickListener {
            dispatchWithRenderLoading("RANDOMIZE_TRAITS") }
        binding.btnRandomColor.setOnClickListener {
            dispatchWithRenderLoading("RANDOMIZE_COLORS") }

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
        binding.webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)

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
                binding.webView.evaluateJavascript(
                    """
                    document.documentElement.classList.add('native-transparent');
                    document.body.classList.add('native-transparent');
                    document.querySelector('.bottom-tabs').style.display='none';
                    document.querySelector('.top-bar').style.display='none';
                    document.querySelector('.action-row').style.display='none';
                    document.querySelector('.panel-area').style.display='none';
                    """.trimIndent(),
                    null
                )
                binding.webView.setBackgroundColor(android.graphics.Color.WHITE)
                val selectionState = intent.getStringExtra(EXTRA_SELECTION_STATE)
                if (!selectionState.isNullOrBlank()) {
                    applySelectionStateWhenReady(selectionState)
                } else if (intent.getBooleanExtra(EXTRA_AUTO_RANDOM, false)) {
                    randomizeWhenReady()
                }
            }
        }

        showRenderLoading()
        binding.webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        binding.actionBar.apply {
            btnActionBarLeft.visibility = android.view.View.VISIBLE
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.setOnClickListener { confirmExit() }

            btnActionBarCenter.visibility = android.view.View.VISIBLE
            btnActionBarCenter.setImageResource(R.drawable.ic_reset)
            btnActionBarCenter.setOnClickListener { confirmResetDesign() }

            btnActionBarRightText.visible()
            tvRightText.visible()
            btnActionBarRightText.setOnClickListener { dispatch("CAPTURE_FOR_EDIT") }
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
        binding.rvPanel.itemAnimator = null
        binding.rvPanel.setHasFixedSize(true)
        selectTab(binding.btnTabHead)
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

    private fun confirmResetDesign() {
        val dialog = YesNoDialog(
            this,
            R.string.reset,
            R.string.change_your_whole_design_are_you_sure,
            dialogType = DialogType.RESET
        )
        dialog.show()

        fun closeDialog() {
            dialog.dismiss()
            hideNavigation()
        }

        dialog.onNoClick = { closeDialog() }
        dialog.onYesClick = {
            closeDialog()
            dispatchWithRenderLoading("RESET")
        }
    }

    private fun confirmExit() {
        val dialog = YesNoDialog(
            this,
            R.string.exit,
            R.string.do_you_want_to_exit,
            dialogType = DialogType.RESET
        )
        dialog.show()

        fun closeDialog() {
            dialog.dismiss()
            hideNavigation()
        }

        dialog.onNoClick = { closeDialog() }
        dialog.onYesClick = {
            closeDialog()
            finish()
            handleBackLeftToRight()
        }
    }

    private fun randomizeWhenReady(attempt: Int = 0) {
        binding.webView.evaluateJavascript(
            "(function(){try{return typeof window.dispatch === 'function' && typeof STATE !== 'undefined' && !!STATE.data;}catch(e){return false;}})()"
        ) { result ->
            if (result == "true") {
                dispatchWithRenderLoading("RANDOMIZE_ALL")
            } else if (attempt < 20) {
                binding.webView.postDelayed({ randomizeWhenReady(attempt + 1) }, 100)
            }
        }
    }

    private fun applySelectionStateWhenReady(stateJson: String, attempt: Int = 0) {
        binding.webView.evaluateJavascript(
            "(function(){try{return typeof window.dispatch === 'function' && typeof STATE !== 'undefined' && !!STATE.data;}catch(e){return false;}})()"
        ) { result ->
            if (result == "true") {
                showRenderLoading()
                applySelectionState(stateJson)
            } else if (attempt < 20) {
                binding.webView.postDelayed({ applySelectionStateWhenReady(stateJson, attempt + 1) }, 100)
            }
        }
    }

    private fun applySelectionState(stateJson: String) {
        try {
            val root = JSONObject(stateJson)
            val selectedStyles = mutableMapOf<String, String>()
            root.keys().forEach { key ->
                val item = root.optJSONObject(key) ?: return@forEach
                if (item.has("style")) {
                    selectedStyles[key] = item.optString("style")
                }
            }
            val action = JSONObject()
                .put("type", "APPLY_STATE")
                .put("state", root)
            binding.webView.evaluateJavascript(
                "window.dispatch(${JSONObject.quote(action.toString())})",
                null
            )
            adapter.setSelectedValues(selectedStyles)
        } catch (e: Exception) {
            android.util.Log.e("DragonWebView", "applySelectionState:${e.message}")
        }
    }

    private fun dispatchWithRenderLoading(type: String, vararg params: Pair<String, String>) {
        showRenderLoading()
        dispatch(type, *params)
    }

    private fun showRenderLoading() {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            isWaitingForRender = true
            if (!loadingDialog.isShowing) {
                loadingDialog.show()
            }
        }
    }

    fun onRenderComplete() {
        runOnUiThread {
            if (!isWaitingForRender) return@runOnUiThread
            isWaitingForRender = false
            if (loadingDialog.isShowing) {
                loadingDialog.dismiss()
                hideNavigation()
            }
        }
    }

    fun selectTab(selected: LinearLayout) {
        val tabs = listOf(
            TabItem(binding.btnTabHead, binding.icHead, binding.tvHeadTab),
            TabItem(binding.btnTabTorso, binding.icTorso, binding.tvTorsoTab),
            TabItem(binding.btnTabLegs, binding.icLegs, binding.tvLegsTab),
            TabItem(binding.btnTabWings, binding.icWings, binding.tvWingsTab),
            TabItem(binding.btnTabTail, binding.icTail, binding.tvTailTab)
        )
        tabs.forEach { item ->
            val tab = item.tab
            tab.isSelected = false
            item.icon.setImageResource(tabIcon(tab, false))
            item.label.setTextColor(getColor(R.color.white))
        }
        selected.isSelected = true
        tabs.firstOrNull { it.tab == selected }?.let { item ->
            item.icon.setImageResource(tabIcon(selected, true))
            item.label.setTextColor(getColor(R.color.app))
        }
    }

    private data class TabItem(
        val tab: LinearLayout,
        val icon: ImageView,
        val label: TextView
    )

    private fun tabIcon(tab: LinearLayout, selected: Boolean): Int {
        return when (tab) {
            binding.btnTabHead -> if (selected) R.drawable.head_hover else R.drawable.head
            binding.btnTabTorso -> if (selected) R.drawable.torso_hover else R.drawable.torso
            binding.btnTabLegs -> if (selected) R.drawable.legs_hover else R.drawable.legs
            binding.btnTabWings -> if (selected) R.drawable.wing_hover else R.drawable.wing
            binding.btnTabTail -> if (selected) R.drawable.tail_hover else R.drawable.tail
            else -> R.drawable.head
        }
    }

    fun saveAndShare(dataUrl: String) {
        try {
            val file = saveDragonPng(dataUrl)
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

    fun openEdit(dataUrl: String, selectionState: String = "") {
        try {
            val file = saveDragonPng(dataUrl, persistToAlbum = true)
            val model = registerSavedDragon(file, selectionState, keepExistingPreview = true)
            runOnUiThread {
                startActivity(
                    Intent(this, EditActivity::class.java).apply {
                        putExtra(IntentKey.EDIT_IMAGE_PATH, file.absolutePath)
                        putExtra(IntentKey.EDIT_SELECTION_STATE, selectionState)
                        putExtra(IntentKey.EDIT_MODEL_ID, model.id)
                        putExtra(IntentKey.EDIT_SOURCE_PATH, model.previewPath)
                    }
                )
                overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right)
            }
        } catch (e: Exception) {
            android.util.Log.e("DragonWebView", "openEdit:${e.message}")
        }
    }

    fun openSuccess(dataUrl: String, selectionState: String = "") {
        try {
            val file = saveDragonPng(dataUrl, persistToAlbum = true)
            registerSavedDragon(file, selectionState, keepExistingPreview = false)
            runOnUiThread {
                startActivity(
                    Intent(this, SuccessActivity::class.java).apply {
                        putExtra(IntentKey.INTENT_KEY, file.absolutePath)
                    }
                )
                overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right)
            }
        } catch (e: Exception) {
            android.util.Log.e("DragonWebView", "openSuccess:${e.message}")
        }
    }

    private fun registerSavedDragon(
        file: File,
        selectionState: String,
        keepExistingPreview: Boolean
    ): DragonCardEditModel {
        val list = MediaHelper
            .readListFromFile<DragonCardEditModel>(this, ValueKey.DRAGON_CARD_EDIT_FILE_INTERNAL)
            .toCollection(ArrayList())

        val index = when {
            editModelId.isNotEmpty() -> list.indexOfFirst { it.id == editModelId }
            editSourcePath.isNotEmpty() -> list.indexOfFirst { it.previewPath == editSourcePath }
            else -> -1
        }

        val existing = list.getOrNull(index)
        val model = DragonCardEditModel(
            id = existing?.id ?: editModelId.ifEmpty { "dragon_card_${System.currentTimeMillis()}" },
            previewPath = if (keepExistingPreview && existing != null) {
                existing.previewPath
            } else {
                file.absolutePath
            },
            dragonImagePath = file.absolutePath,
            nameTag = existing?.nameTag.orEmpty(),
            nameFont = existing?.nameFont ?: 0,
            nameColor = existing?.nameColor ?: 0,
            describe = existing?.describe.orEmpty(),
            describeFont = existing?.describeFont ?: 0,
            describeColor = existing?.describeColor ?: 0,
            starRating = existing?.starRating ?: 2,
            starStyle = existing?.starStyle ?: 1,
            atk = existing?.atk.orEmpty(),
            def = existing?.def.orEmpty(),
            bgImageColor = existing?.bgImageColor ?: 0,
            bgTagColor = existing?.bgTagColor ?: 0,
            bgImagePath = existing?.bgImagePath.orEmpty(),
            bgTagPath = existing?.bgTagPath.orEmpty(),
            selectionState = selectionState.ifEmpty { existing?.selectionState.orEmpty() }
        )

        if (index >= 0) {
            list[index] = model
        } else {
            list.add(0, model)
        }
        MediaHelper.writeListToFile(this, ValueKey.DRAGON_CARD_EDIT_FILE_INTERNAL, list)
        editModelId = model.id
        editSourcePath = model.previewPath
        return model
    }

    private fun saveDragonPng(dataUrl: String, persistToAlbum: Boolean = false): File {
        val bytes = Base64.decode(dataUrl.substringAfter("base64,"), Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
        val dir = if (persistToAlbum) {
            File(filesDir, ValueKey.DOWNLOAD_ALBUM)
        } else {
            File(cacheDir, "dragons")
        }.also { it.mkdirs() }
        val file = File(dir, "dragon_$ts.png")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return file
    }

    fun loadTab(tab: String) {
        val rows = PartsConfigParser.load(assets, tab)
        adapter.submitRows(rows)
    }

    private fun setupColorPicker() {
        binding.colorPickerView.hueSliderView = binding.hueSlider
        binding.colorPickerView.setOnColorChangedListener { color ->
            pickedColor = color
        }
        binding.btnBackColor.setOnClickListener { closeColorPicker() }
        binding.btnDoneColor.setOnClickListener {
            val hex = String.format("#%06X", 0xFFFFFF and pickedColor)
            dispatchWithRenderLoading("SET_COLOR", "colorId" to currentColorId, "hex" to hex)
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

    override fun onDestroy() {
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
        super.onDestroy()
    }
}
