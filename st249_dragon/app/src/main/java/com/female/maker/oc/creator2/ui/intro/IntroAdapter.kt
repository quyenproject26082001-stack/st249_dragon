package com.female.maker.oc.creator2.ui.intro

import android.content.Context
import com.female.maker.oc.creator2.core.base.BaseAdapter
import com.female.maker.oc.creator2.core.extensions.loadImage
import com.female.maker.oc.creator2.core.extensions.select
import com.female.maker.oc.creator2.core.extensions.strings
import com.female.maker.oc.creator2.data.model.IntroModel
import com.female.maker.oc.creator2.databinding.ItemIntroBinding

class IntroAdapter(val context: Context) : BaseAdapter<IntroModel, ItemIntroBinding>(
    ItemIntroBinding::inflate
) {
    override fun onBind(binding: ItemIntroBinding, item: IntroModel, position: Int) {
        binding.apply {
            loadImage(root, item.image, imvImage, false)
            tvContent.text = context.strings(item.content)
            tvContent.select()
        }
    }
}