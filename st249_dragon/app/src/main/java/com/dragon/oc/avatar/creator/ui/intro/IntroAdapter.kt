package com.dragon.oc.avatar.creator.ui.intro

import android.content.Context
import com.dragon.oc.avatar.creator.core.base.BaseAdapter
import com.dragon.oc.avatar.creator.core.extensions.loadImage
import com.dragon.oc.avatar.creator.core.extensions.select
import com.dragon.oc.avatar.creator.core.extensions.strings
import com.dragon.oc.avatar.creator.data.model.IntroModel
import com.dragon.oc.avatar.creator.databinding.ItemIntroBinding

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