package com.female.maker.oc.creator2.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lvt.ads.util.Admob
import com.female.maker.oc.creator2.R
import com.female.maker.oc.creator2.core.base.BaseActivity
import com.female.maker.oc.creator2.core.extensions.hideNavigation
import com.female.maker.oc.creator2.core.extensions.loadNativeCollabAds
import com.female.maker.oc.creator2.core.extensions.rateApp
import com.female.maker.oc.creator2.core.extensions.select
import com.female.maker.oc.creator2.core.extensions.setImageActionBar
import com.female.maker.oc.creator2.core.extensions.showInterAll
import com.female.maker.oc.creator2.core.extensions.startIntentRightToLeft
import com.female.maker.oc.creator2.core.helper.LanguageHelper
import com.female.maker.oc.creator2.core.helper.MediaHelper
import com.female.maker.oc.creator2.core.utils.key.ValueKey
import com.female.maker.oc.creator2.core.utils.state.RateState
import com.female.maker.oc.creator2.databinding.ActivityHomeBinding
import com.female.maker.oc.creator2.ui.SettingsActivity
import com.female.maker.oc.creator2.ui.my_creation.MyCreationActivity
import com.female.maker.oc.creator2.core.extensions.tap
import com.female.maker.oc.creator2.core.extensions.strings
import com.female.maker.oc.creator2.core.helper.InternetHelper
import com.female.maker.oc.creator2.core.utils.state.HandleState
import com.female.maker.oc.creator2.dialog.DialogType
import com.female.maker.oc.creator2.dialog.YesNoDialog
import com.female.maker.oc.creator2.ui.add_character.AddCharacterActivity
import com.female.maker.oc.creator2.ui.dragon_webview.DragonWebViewActivity
import com.female.maker.oc.creator2.ui.trending.TrendingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.exitProcess

class HomeActivity : BaseActivity<ActivityHomeBinding>() {

    override fun setViewBinding(): ActivityHomeBinding {
        return ActivityHomeBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        sharePreference.setCountBack(sharePreference.getCountBack() + 1)
        deleteTempFolder()
        binding.tv1.isSelected = true
      //  binding.tvTrending.isSelected = true
        binding.tv3.isSelected = true
        binding.tv2.isSelected = true

//        // Apply elastic bounce animation to app name
//        val elasticBounce = AnimationUtils.loadAnimation(this, R.anim.elastic_bounce)
//        binding.imvAppName.startAnimation(elasticBounce)
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarRight.tap(800) { startIntentRightToLeft(SettingsActivity::class.java) }
            btnMaker.tap(2000){ startIntentRightToLeft(DragonWebViewActivity::class.java)}
            btnMyCreation.tap(800) { showInterAll { startIntentRightToLeft(MyCreationActivity::class.java) } }
           btnRandom.tap(800) { checkDataInternet(this@HomeActivity){showInterAll {startIntentRightToLeft(
               TrendingActivity::class.java) }}}

        }
    }

    override fun initText() {
        super.initText()
        binding.actionBar.tvCenter.select()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarRight, R.drawable.ic_settings)
        }
    }

    // Enable background music for HomeActivity
    override fun shouldPlayBackgroundMusic(): Boolean = true

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() {
        if (!sharePreference.getIsRate(this) && sharePreference.getCountBack() % 2 == 0) {
            rateApp(sharePreference) { state ->
                if (state != RateState.CANCEL) {
                    showToast(R.string.have_rated)
                }
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        delay(1000)
                        exitProcess(0)
                    }
                }
            }
        } else {
            exitProcess(0)
        }
    }

    private fun deleteTempFolder() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dataTemp = MediaHelper.getImageInternal(this@HomeActivity, ValueKey.RANDOM_TEMP_ALBUM)
            if (dataTemp.isNotEmpty()) {
                dataTemp.forEach {
                    val file = File(it)
                    file.delete()
                }
            }
        }
    }

    private fun updateText() {
        binding.apply {
            tv1.text = strings(R.string.dragon_custom)
            tv3.text = strings(R.string.my_creation)
            tv2.text = strings(R.string.random_dragon)

        }
    }

    override fun onRestart() {
        super.onRestart()
        deleteTempFolder()
        LanguageHelper.setLocale(this)
        updateText()
        //initNativeCollab()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
        startStaggeredAnimations()

        }
    }


    fun checkDataInternet(context: AppCompatActivity, action:
    (() -> Unit)) {
        InternetHelper.checkInternet(context) { result ->
            if (result == HandleState.SUCCESS) {
                action.invoke()
            } else {
                val dialog = YesNoDialog(
                    context,
                    R.string.no_internet,
                    R.string.please_check_your_internet,
                    isError = true,
                    dialogType = DialogType.INTERNET
                )
                dialog.show()
                dialog.onYesClick = {
                    dialog.dismiss()
                }
            }
        }
    }

    private fun startStaggeredAnimations() {
        // Card 1: Slide from right (no delay)
        val slideFromRight1 = AnimationUtils.loadAnimation(this, R.anim.slide_in_right_home)
        binding.btnMaker.startAnimation(slideFromRight1)
        binding.tv1.startAnimation(slideFromRight1)


        // Card 2: Slide from left (200ms delay)
        val slideFromLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_home)
        binding.btnRandom.postDelayed({
            binding.btnRandom.startAnimation(slideFromLeft)
            binding.tv2.startAnimation(slideFromLeft)
        }, 200)

        // Card 3: Slide from right (400ms delay)
        val slideFromRight2 = AnimationUtils.loadAnimation(this, R.anim.slide_in_right_home)
        binding.btnMyCreation.postDelayed({
            binding.btnMyCreation.startAnimation(slideFromRight2)
            binding.tv3.startAnimation(slideFromRight2)
        }, 400)

    }

    fun initNativeCollab() {
        Admob.getInstance().loadNativeCollapNotBanner(this,getString(R.string.native_cl_home), binding.flNativeCollab)
    }

    override fun initAds() {
        initNativeCollab()
        Admob.getInstance().loadInterAll(this, getString(R.string.inter_all))
        Admob.getInstance().loadNativeAll(this, getString(R.string.native_all))
    }
}
