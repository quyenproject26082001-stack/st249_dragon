package com.dragon.oc.avatar.creator.dialog

import android.animation.ObjectAnimator
import android.app.Activity
import android.view.animation.LinearInterpolator
import com.dragon.oc.avatar.creator.R
import com.dragon.oc.avatar.creator.core.base.BaseDialog
import com.dragon.oc.avatar.creator.databinding.DialogLoadingBinding

class WaitingDialog(val context: Activity) :
    BaseDialog<DialogLoadingBinding>(context, maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_loading
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false
    private var spinnerAnimator: ObjectAnimator? = null

    override fun initView() {
    }

    override fun onStart() {
        super.onStart()
        binding.Icspinning.post {
            spinnerAnimator?.cancel()
            binding.Icspinning.rotation = 0f
            spinnerAnimator = ObjectAnimator.ofFloat(binding.Icspinning, "rotation", 0f, 360f).apply {
                duration = 1000L
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        }
    }

    override fun initAction() {}

    override fun onDismissListener() {
        spinnerAnimator?.cancel()
        spinnerAnimator = null
        binding.Icspinning.rotation = 0f
    }

}
