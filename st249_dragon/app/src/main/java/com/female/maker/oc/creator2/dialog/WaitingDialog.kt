package com.female.maker.oc.creator2.dialog

import android.app.Activity
import android.view.animation.AnimationUtils
import com.female.maker.oc.creator2.R
import com.female.maker.oc.creator2.core.base.BaseDialog
import com.female.maker.oc.creator2.core.extensions.setBackgroundConnerSmooth
import com.female.maker.oc.creator2.databinding.DialogLoadingBinding

class WaitingDialog(val context: Activity) :
    BaseDialog<DialogLoadingBinding>(context, maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_loading
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false

    override fun initView() {
        val rotate = AnimationUtils.loadAnimation(context, R.anim.rotate_loading)
        binding.Icspinning.startAnimation(rotate)
    }

    override fun initAction() {}

    override fun onDismissListener() {}

}