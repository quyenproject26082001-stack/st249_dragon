package com.dragon.oc.avatar.creator.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.util.Admob
import com.dragon.oc.avatar.creator.R
import com.dragon.oc.avatar.creator.core.base.BaseActivity
import com.dragon.oc.avatar.creator.core.extensions.checkShowSplashWhenFail
import com.dragon.oc.avatar.creator.core.extensions.loadNativeCollabAds
import com.dragon.oc.avatar.creator.core.extensions.loadSplashInterAds
import com.dragon.oc.avatar.creator.core.helper.InternetHelper
import com.dragon.oc.avatar.creator.core.utils.state.HandleState
import com.dragon.oc.avatar.creator.databinding.ActivitySplashBinding
import com.dragon.oc.avatar.creator.dialog.DialogType
import com.dragon.oc.avatar.creator.dialog.YesNoDialog
import com.dragon.oc.avatar.creator.ui.add_character.AddCharacterActivity
import com.dragon.oc.avatar.creator.ui.intro.IntroActivity
import com.dragon.oc.avatar.creator.ui.language.LanguageActivity
import com.dragon.oc.avatar.creator.ui.home.DataViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    var intentActivity: Intent? = null
    private val dataViewModel: DataViewModel by viewModels()
    var interCallBack: InterCallback? = null

    private val MIN_SPLASH_MS = 2000L  // Reduced from 3000ms to 1500ms for faster startup
    private var minTimePassed = false
    private var dataReady = false
    private var triggered = false



    override fun setViewBinding(): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(LayoutInflater.from(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_BaseProject)
        super.onCreate(savedInstanceState)
    }

    override fun initView() {
        // Start loading animation
        val rotateAnimation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.rotate_loading)
        binding.ivLoading.startAnimation(rotateAnimation)

        if (!isTaskRoot &&
            intent.hasCategory(Intent.CATEGORY_LAUNCHER) &&
            intent.action != null &&
            intent.action == Intent.ACTION_MAIN) {
            finish(); return
        }

        intentActivity = if (sharePreference.getIsFirstLang()) {
            Intent(this, LanguageActivity::class.java)
        } else {
            Intent(this, IntroActivity::class.java)
        }
        Admob.getInstance().setTimeLimitShowAds(30000)
        Admob.getInstance().setOpenShowAllAds(false)
        interCallBack = object : InterCallback() {
            override fun onNextAction() {
                super.onNextAction()
                startActivity(intentActivity)
                finishAffinity()
            }
        }
        dataViewModel.ensureData(this@SplashActivity)
        dataViewModel.preloadAddCharacterAssets(this)

        lifecycleScope.launch {
            kotlinx.coroutines.delay(MIN_SPLASH_MS)
            minTimePassed = true
            tryProceed()
        }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            dataViewModel.dataLoadFinished.collect { finished ->
                if (finished){
                    // Data is ready, no need to call API again
                    // (API already called in saveAndReadData if needed)
                    dataReady = true
                    tryProceed()
                }

            }
        }
    }




    private fun tryProceed() {
        if (triggered) return
        if (!minTimePassed || !dataReady) return

        triggered = true


        loadSplashInterAds(getString(R.string.inter_splash), 30000, 2000, interCallBack)
    }

    override fun viewListener() {
    }

    override fun initText() {}

    override fun initActionBar() {}

    @SuppressLint("GestureBackNavigation", "MissingSuperCall")
    override fun onBackPressed() {}

//    override fun initAds() {
//        initNativeCollab()
//    }

//    fun initNativeCollab() {
//
//        loadNativeCollabAds(R.string.native_splash, binding.flNativeCollab)
//
//
//    }

    override fun onResume() {
        super.onResume()
        checkShowSplashWhenFail(interCallBack, 1000)
    }

    override fun shouldPlayBackgroundMusic(): Boolean = false
}