package com.dragon.oc.avatar.creator.ui.trending

import android.animation.ObjectAnimator
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.viewModels
import androidx.core.graphics.createBitmap
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewAssetLoader
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.dragon.oc.avatar.creator.R
import com.dragon.oc.avatar.creator.core.base.BaseActivity
import com.dragon.oc.avatar.creator.core.extensions.handleBackLeftToRight
import com.dragon.oc.avatar.creator.core.extensions.hideNavigation
import com.dragon.oc.avatar.creator.core.extensions.setImageActionBar
import com.dragon.oc.avatar.creator.core.extensions.setTextActionBar
import com.dragon.oc.avatar.creator.core.extensions.showInterAll
import com.dragon.oc.avatar.creator.core.extensions.tap
import com.dragon.oc.avatar.creator.core.extensions.visible
import com.dragon.oc.avatar.creator.core.helper.InternetHelper
import com.dragon.oc.avatar.creator.core.helper.MediaHelper
import com.dragon.oc.avatar.creator.core.utils.key.IntentKey
import com.dragon.oc.avatar.creator.core.utils.key.ValueKey
import com.dragon.oc.avatar.creator.core.utils.state.HandleState
import com.dragon.oc.avatar.creator.core.utils.state.SaveState
import com.dragon.oc.avatar.creator.data.model.custom.SuggestionModel
import com.dragon.oc.avatar.creator.databinding.ActivityTrendingBinding
import com.dragon.oc.avatar.creator.dialog.YesNoDialog
import com.dragon.oc.avatar.creator.ui.customize.CustomizeCharacterViewModel
import com.dragon.oc.avatar.creator.ui.dragon_webview.DragonWebViewActivity
import com.dragon.oc.avatar.creator.ui.home.DataViewModel
import com.dragon.oc.avatar.creator.ui.random_character.RandomCharacterViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONTokener

class TrendingActivity : BaseActivity<ActivityTrendingBinding>() {

    private val viewModel: RandomCharacterViewModel by viewModels()
    private val dataViewModel: DataViewModel by viewModels()
    private val customizeCharacterViewModel: CustomizeCharacterViewModel by viewModels()

    private var currentSuggestion: SuggestionModel? = null
    private var isAnimating = false
    private var isWaitingForRandomRender = false

    override fun setViewBinding(): ActivityTrendingBinding {
        return ActivityTrendingBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        binding.tvGenerate.isSelected = true
        isWaitingForRandomRender = true
        lifecycleScope.launch { showLoading() }
        setupDragonWebView()
    }

    override fun dataObservable() {
        // Trending renders the dragon builder directly; customize data is not needed here.
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.tap { showInterAll { handleBackLeftToRight() } }
            actionBar.btnActionBarRight.tap { openDragonEditorWithCurrentState() }
            btnGenerate.tap(0) { handleGenerate() }
            btnDownload.tap { handleDownloadDragon() }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setImageActionBar(btnActionBarRight, R.drawable.ic_edit)
            btnActionBarRight.visible()
            setTextActionBar(tvCenter, getString(R.string.random))
            tvCenter.isSelected = true
            binding.actionBar.spTvCenter.visible()



        }
    }

    private fun setupDragonWebView() {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        binding.dragonWebView.settings.apply {
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
        binding.dragonWebView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        binding.dragonWebView.addJavascriptInterface(TrendingBridge(), "AndroidBridge")

        binding.dragonWebView.webViewClient = object : android.webkit.WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                return request?.url?.let { assetLoader.shouldInterceptRequest(it) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                hideDragonBuilderControls()
                binding.dragonWebView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        }

        binding.dragonWebView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    private fun hideDragonBuilderControls() {
        binding.dragonWebView.evaluateJavascript(
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
    }

    private fun randomizeDragonWhenReady(attempt: Int = 0, onDone: (() -> Unit)? = null) {
        binding.dragonWebView.evaluateJavascript(
            "(function(){try{return typeof window.dispatch === 'function' && typeof STATE !== 'undefined' && !!STATE.data;}catch(e){return false;}})()"
        ) { result ->
            if (result == "true") {
                hideDragonBuilderControls()
                binding.dragonWebView.evaluateJavascript("""window.dispatch('{"type":"RANDOMIZE_ALL"}')""", null)
            } else if (attempt < 20) {
                binding.dragonWebView.postDelayed({ randomizeDragonWhenReady(attempt + 1, onDone) }, 100)
            } else {
                onDone?.invoke()
            }
        }
    }

    private inner class TrendingBridge {
        @android.webkit.JavascriptInterface
        fun onEvent(eventJson: String) {
            try {
                val obj = org.json.JSONObject(eventJson)
                when (obj.optString("type")) {
                    "RENDER_COMPLETE" -> {
                        if (!isWaitingForRandomRender) return
                        runOnUiThread {
                            isWaitingForRandomRender = false
                            isAnimating = false
                            binding.btnGenerate.visibility = View.VISIBLE
                            binding.btnDownload.visibility = View.VISIBLE
                            lifecycleScope.launch { dismissLoading() }
                        }
                    }

                    "DOWNLOAD_READY" -> {
                        downloadDragonToGallery(obj.getString("data"))
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun openDragonEditorWithCurrentState(attempt: Int = 0) {
        binding.dragonWebView.evaluateJavascript(
            "(function(){try{return typeof window.dispatch === 'function' && typeof STATE !== 'undefined' && !!STATE.data;}catch(e){return false;}})()"
        ) { result ->
            if (result == "true") {
                binding.dragonWebView.evaluateJavascript(
                    """window.dispatch('{"type":"GET_EDITABLE_STATE"}')"""
                ) { rawState ->
                    val stateJson = try {
                        (JSONTokener(rawState).nextValue() as? String).orEmpty()
                    } catch (e: Exception) {
                        ""
                    }
                    launchDragonEditor(stateJson)
                }
            } else if (attempt < 20) {
                binding.dragonWebView.postDelayed({ openDragonEditorWithCurrentState(attempt + 1) }, 100)
            } else {
                launchDragonEditor()
            }
        }
    }

    private fun launchDragonEditor(selectionState: String = "") {
        val intent = Intent(this, DragonWebViewActivity::class.java).apply {
            if (selectionState.isNotBlank()) {
                putExtra(DragonWebViewActivity.EXTRA_SELECTION_STATE, selectionState)
            }
        }
        val option = ActivityOptions.makeCustomAnimation(
            this,
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
        showInterAll { startActivity(intent, option.toBundle()) }
    }

    private fun initData() {
        val handleExceptionCoroutine = CoroutineExceptionHandler { _, throwable ->
            CoroutineScope(Dispatchers.Main).launch {
                val dialogExit = YesNoDialog(
                    this@TrendingActivity,
                    R.string.error,
                    R.string.an_error_occurred
                )
                dialogExit.show()
                dialogExit.onNoClick = {
                    dialogExit.dismiss()
                    finish()
                }
                dialogExit.onYesClick = {
                    dialogExit.dismiss()
                    hideNavigation()
                    finish()
                }
            }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.Main + handleExceptionCoroutine).launch {
            val hasInternet = withContext(Dispatchers.IO) {
                InternetHelper.isInternetAvailable(this@TrendingActivity)
            }
            val filteredData = if (hasInternet) {
                dataViewModel.allData.value
            } else {
                dataViewModel.allData.value.filter { !it.isFromAPI }
            }
            if (filteredData.isEmpty()) return@launch

            suspend fun processCharacter(data: com.dragon.oc.avatar.creator.data.model.custom.CustomizeModel) {
                customizeCharacterViewModel.positionSelected =
                    dataViewModel.allData.value.indexOf(data)
                customizeCharacterViewModel.setDataCustomize(data)
                customizeCharacterViewModel.updateAvatarPath(data.avatar)
                customizeCharacterViewModel.resetDataList()
                customizeCharacterViewModel.addValueToItemNavList()
                customizeCharacterViewModel.setItemColorDefault()
                customizeCharacterViewModel.setBottomNavigationListDefault()
                for (j in 0 until ValueKey.RANDOM_QUANTITY) {
                    customizeCharacterViewModel.setClickRandomFullLayer()
                    val suggestion = customizeCharacterViewModel.getSuggestionList()
                    viewModel.updateRandomList(suggestion)
                }
            }

            // Xử lý character đầu tiên → show ngay
            withContext(Dispatchers.IO) {
                try { processCharacter(filteredData[0]) } catch (e: Exception) { e.printStackTrace() }
                viewModel.upsideDownList()
            }
            showRandomSuggestion { lifecycleScope.launch { dismissLoading() } }

            // Xử lý phần còn lại ở background
            if (filteredData.size > 1) {
                withContext(Dispatchers.IO) {
                    for (i in 1 until filteredData.size) {
                        try { processCharacter(filteredData[i]) } catch (e: Exception) { e.printStackTrace() }
                    }
                    viewModel.upsideDownList()
                }
            }
        }
    }

    private fun showRandomSuggestion(onComplete: (() -> Unit)? = null) {
        if (viewModel.randomList.isEmpty()) {
            onComplete?.invoke()
            return
        }
        val model = randomSuggestion(viewModel.randomList) ?: run {
            onComplete?.invoke()
            return
        }
        currentSuggestion = model
        renderSuggestion(model, onComplete)
    }

    private fun randomSuggestion(list: List<SuggestionModel>): SuggestionModel? {
        val current = currentSuggestion
        val candidates = if (current != null && list.size > 1) {
            list.filter { it !== current }
        } else {
            list
        }
        return candidates.randomOrNull()
    }

    private fun handleGenerate() {
        if (isAnimating) return
        isAnimating = true
        binding.btnGenerate.visibility = View.INVISIBLE
        binding.btnDownload.visibility = View.INVISIBLE
        val totalDuration = 800L

        // Dice: spin 3 full rounds, decelerating like a real dice roll
        val diceAnim = ObjectAnimator.ofFloat(binding.dices, "rotation", 0f, 1080f).apply {
            duration = totalDuration
            interpolator = DecelerateInterpolator(2f)
            start()
        }

        lifecycleScope.launch {
            showLoading()
            delay(totalDuration)

            diceAnim.cancel()
            binding.dices.rotation = 0f

            // Check internet sau khi delay xong, timeout 3s để tránh hang khi mất mạng
            isWaitingForRandomRender = true
            randomizeDragonWhenReady(onDone = {
                lifecycleScope.launch {
                    if (!isWaitingForRandomRender) return@launch
                    isWaitingForRandomRender = false
                    isAnimating = false
                    binding.btnGenerate.visibility = View.VISIBLE
                    binding.btnDownload.visibility = View.VISIBLE
                    dismissLoading()
                }
            })
        }
    }

    private fun renderSuggestion(model: SuggestionModel, onComplete: (() -> Unit)? = null) {
        android.util.Log.d("TrendingDebug", "renderSuggestion() called | pathInternalRandom='${model.pathInternalRandom}' | pathSelectedList.size=${model.pathSelectedList.size} | avatarPath='${model.avatarPath}'")

        if (model.pathInternalRandom.isNotEmpty()) {
            android.util.Log.d("TrendingDebug", "  → pathInternalRandom không rỗng, load trực tiếp")
            Glide.with(this)
                .load(model.pathInternalRandom)
                .listener(glideListener(onComplete))
                .into(binding.imvImage)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val paths = model.pathSelectedList.filter { it.isNotEmpty() }
                android.util.Log.d("TrendingDebug", "  → pathInternalRandom rỗng, tính từ pathSelectedList")
                android.util.Log.d("TrendingDebug", "     pathSelectedList raw (${model.pathSelectedList.size} item): ${model.pathSelectedList}")
                android.util.Log.d("TrendingDebug", "     paths sau filter notEmpty (${paths.size} item): $paths")
                if (paths.isEmpty()) {
                    android.util.Log.w("TrendingDebug", "  !! paths.isEmpty() → onComplete gọi không có ảnh nào, GIF sẽ còn quay!")
                    withContext(Dispatchers.Main) { onComplete?.invoke() }
                    return@launch
                }

                val bitmapDefault = Glide.with(this@TrendingActivity)
                    .asBitmap().load(paths.first()).submit().get()
                val width = bitmapDefault.width / 2
                val height = bitmapDefault.height / 2

                val listBitmap = ArrayList<Bitmap>()
                paths.forEach { path ->
                    listBitmap.add(
                        Glide.with(this@TrendingActivity)
                            .asBitmap().load(path).submit(width, height).get()
                    )
                }

                val combinedBitmap = createBitmap(width, height)
                val canvas = Canvas(combinedBitmap)
                for (bitmap in listBitmap) {
                    val left = (width - bitmap.width) / 2f
                    val top = (height - bitmap.height) / 2f
                    canvas.drawBitmap(bitmap, left, top, null)
                }

                MediaHelper.saveBitmapToInternalStorage(
                    this@TrendingActivity,
                    ValueKey.RANDOM_TEMP_ALBUM,
                    combinedBitmap
                ).collect { state ->
                    android.util.Log.d("TrendingDebug", "  → saveBitmapToInternalStorage state: $state")
                    if (state is SaveState.Success) {
                        model.pathInternalRandom = state.path
                        android.util.Log.d("TrendingDebug", "     Save thành công: path='${state.path}'")
                    }
                }

                android.util.Log.d("TrendingDebug", "  → Sau save: pathInternalRandom='${model.pathInternalRandom}'")
                withContext(Dispatchers.Main) {
                    if (model.pathInternalRandom.isEmpty()) {
                        android.util.Log.w("TrendingDebug", "  !! pathInternalRandom vẫn rỗng sau save → Glide.load('') sẽ fail, GIF còn quay!")
                    }
                    Glide.with(this@TrendingActivity)
                        .load(model.pathInternalRandom)
                        .listener(glideListener(onComplete))
                        .into(binding.imvImage)
                }
            } catch (e: Exception) {
                android.util.Log.e("TrendingDebug", "  !! Exception trong renderSuggestion: ${e::class.simpleName}: ${e.message}", e)
                withContext(Dispatchers.Main) { onComplete?.invoke() }
            }
        }
    }

    private fun glideListener(onComplete: (() -> Unit)?): RequestListener<android.graphics.drawable.Drawable> {
        return object : RequestListener<android.graphics.drawable.Drawable> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<android.graphics.drawable.Drawable>, isFirstResource: Boolean): Boolean {
                android.util.Log.e("TrendingDebug", "  !! Glide.onLoadFailed: model='$model' | cause=${e?.causes?.joinToString { it.message ?: it::class.simpleName ?: "?" }}")
                onComplete?.invoke()
                return false
            }
            override fun onResourceReady(resource: android.graphics.drawable.Drawable, model: Any, target: Target<android.graphics.drawable.Drawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                android.util.Log.d("TrendingDebug", "  ✓ Glide.onResourceReady: model='$model' | source=$dataSource")
                onComplete?.invoke()
                return false
            }
        }
    }

    private fun handleDownloadDragon() {
        lifecycleScope.launch { showLoading() }
        captureDownloadWhenReady()
    }

    private fun captureDownloadWhenReady(attempt: Int = 0) {
        binding.dragonWebView.evaluateJavascript(
            "(function(){try{return typeof window.dispatch === 'function' && typeof STATE !== 'undefined' && !!STATE.data;}catch(e){return false;}})()"
        ) { result ->
            if (result == "true") {
                hideDragonBuilderControls()
                binding.dragonWebView.evaluateJavascript(
                    """window.dispatch('{"type":"DOWNLOAD"}')""",
                    null
                )
            } else if (attempt < 20) {
                binding.dragonWebView.postDelayed({ captureDownloadWhenReady(attempt + 1) }, 100)
            } else {
                lifecycleScope.launch {
                    dismissLoading()
                    showToast(R.string.an_error_occurred)
                }
            }
        }
    }

    private fun downloadDragonToGallery(dataUrl: String) {
        val bytes = Base64.decode(dataUrl.substringAfter("base64,"), Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        lifecycleScope.launch {
            MediaHelper.saveBitmapToExternal(this@TrendingActivity, bitmap).collect { state ->
                when (state) {
                    HandleState.LOADING -> Unit
                    HandleState.SUCCESS -> {
                        dismissLoading()
                        showToast(R.string.download_success)
                    }

                    else -> {
                        dismissLoading()
                        showToast(R.string.download_failed_please_try_again_later)
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyUiCustomize()
            hideNavigation(true)
            window.decorView.removeCallbacks(reHideRunnable)
            window.decorView.postDelayed(reHideRunnable, 2000)
        } else {
            window.decorView.removeCallbacks(reHideRunnable)
        }
    }

    private val reHideRunnable = Runnable {
        applyUiCustomize()
        hideNavigation(true)
    }

    @Suppress("DEPRECATION")
    private fun applyUiCustomize() {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}
