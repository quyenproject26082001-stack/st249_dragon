package com.female.maker.oc.creator2.dialog

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.DrawableWrapper
import android.widget.LinearLayout
import com.female.maker.oc.creator2.core.extensions.gone
import com.female.maker.oc.creator2.core.extensions.hideNavigation
import com.female.maker.oc.creator2.core.extensions.tap
import com.female.maker.oc.creator2.R
import com.female.maker.oc.creator2.core.base.BaseDialog
import com.female.maker.oc.creator2.core.extensions.strings
import com.female.maker.oc.creator2.databinding.DialogConfirmBinding

enum class DialogType {
    DELETE_EXIT,
    RESET,
    LOADING,
    INTERNET,
    PERMISSION
}

class YesNoDialog(
    val context: Activity,
    val title: Int,
    val description: Int,
    val isError: Boolean = false,
    val dialogType: DialogType = DialogType.DELETE_EXIT
) : BaseDialog<DialogConfirmBinding>(context, maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_confirm
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false

    var onNoClick: (() -> Unit) = {}
    var onYesClick: (() -> Unit) = {}
    var onDismissClick: (() -> Unit) = {}

    override fun initView() {
        initText()
        initBackground()
        if (isError) {
            binding.btnNo.gone()
            if(dialogType == DialogType.INTERNET){
                val params = binding.btnYes.layoutParams as LinearLayout.LayoutParams

                val density = context.resources.displayMetrics.density

                params.width = (100 * density).toInt()
                params.height = (40* density).toInt()
                params.weight =0f
                params.marginStart =0
                binding.btnYes.layoutParams = params

            }
        }
        context.hideNavigation()
        binding.tvTitle.isSelected = true
        binding.tvDescription.isSelected = true
    }

    private fun initBackground() {
        val bgRes = when (dialogType) {
            DialogType.DELETE_EXIT -> R.drawable.bg_dialog_delete_exit
            DialogType.RESET -> R.drawable.bg_dialog_reset
            DialogType.LOADING -> R.drawable.bg_dialog_loading
            DialogType.INTERNET -> R.drawable.bg_dialog_internet
            DialogType.PERMISSION -> R.drawable.bg_dialog_loading
        }


        val textColor = when (dialogType) {
            DialogType.DELETE_EXIT -> Color.parseColor("#01579B")
            DialogType.RESET -> Color.parseColor("#01579B")
            DialogType.LOADING -> Color.parseColor("#01579B")
            DialogType.INTERNET -> Color.parseColor("#01579B")
            DialogType.PERMISSION -> Color.parseColor("#01579B")
        }



    }

    override fun initAction() {
        binding.apply {
            btnNo.tap { onNoClick.invoke() }
            btnYes.tap { onYesClick.invoke() }
            flOutSide.tap { onDismissClick.invoke() }
        }
    }

    override fun onDismissListener() {

    }

    private fun initText() {
        binding.apply {
            tvTitle.text = context.strings(title)
            tvDescription.text = context.strings(description)
            // Use "Ok" text for error dialogs (like internet check)
            if (isError) {
                btnYes.text = context.strings(R.string.ok)
            }
        }
    }
}
