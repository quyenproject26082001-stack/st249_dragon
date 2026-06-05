package com.female.maker.oc.creator2.ui

import android.view.LayoutInflater
import com.female.maker.oc.creator2.R
import com.female.maker.oc.creator2.core.base.BaseActivity
import com.female.maker.oc.creator2.core.extensions.gone
import com.female.maker.oc.creator2.core.extensions.handleBackLeftToRight
import com.female.maker.oc.creator2.core.extensions.policy
import com.female.maker.oc.creator2.core.extensions.select
import com.female.maker.oc.creator2.core.extensions.setImageActionBar
import com.female.maker.oc.creator2.core.extensions.setTextActionBar
import com.female.maker.oc.creator2.core.extensions.shareApp
import com.female.maker.oc.creator2.core.extensions.startIntentRightToLeft
import com.female.maker.oc.creator2.core.extensions.visible
import com.female.maker.oc.creator2.core.utils.key.IntentKey
import com.female.maker.oc.creator2.core.utils.state.RateState
import com.female.maker.oc.creator2.databinding.ActivitySettingsBinding
import com.female.maker.oc.creator2.ui.language.LanguageActivity
import com.female.maker.oc.creator2.core.extensions.tap
import com.female.maker.oc.creator2.core.helper.MusicHelper
import com.female.maker.oc.creator2.core.helper.RateHelper
import kotlin.jvm.java

class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {
    override fun setViewBinding(): ActivitySettingsBinding {
        return ActivitySettingsBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        binding.tvMusic.select()
        initRate()
        initMusic()
    }

    private fun initMusic() {
        updateMusicUI(sharePreference.isMusicEnabled())
    }

    private fun updateMusicUI(isEnabled: Boolean) {
        binding.btnMusic.setImageResource(
            if (isEnabled) R.drawable.ic_sw_on else R.drawable.ic_sw_off_ms
        )
    }

    private fun toggleMusic() {
        val isEnabled = !sharePreference.isMusicEnabled()
        sharePreference.setMusicEnabled(isEnabled)
        updateMusicUI(isEnabled)
        if (isEnabled) {
            MusicHelper.play()
        } else {
            MusicHelper.pause()
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.tap { handleBackLeftToRight() }
            layoutMusic.tap { toggleMusic() }
            btnLang.tap { startIntentRightToLeft(LanguageActivity::class.java, IntentKey.INTENT_KEY) }
            btnShareApp.tap(1500) { shareApp() }
            btnRate.tap {
                RateHelper.showRateDialog(this@SettingsActivity, sharePreference){ state ->
                    if (state != RateState.CANCEL){
                        btnRate.gone()
                        showToast(R.string.have_rated)
                    }
                }
            }
            btnPolicy.tap(1500) { policy() }
        }
    }

    override fun initText() {
        binding.actionBar.tvCenter.select()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setTextActionBar(tvCenter, getString(R.string.settings))
            binding.actionBar.spTvCenter.visible()

        }
    }

    private fun initRate() {
        if (sharePreference.getIsRate(this)) {
            binding.btnRate.gone()
        } else {
            binding.btnRate.visible()
        }
    }
}