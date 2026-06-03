package com.female.maker.oc.creator2.ui.add_character.adapter

import com.female.maker.oc.creator2.core.base.BaseAdapter
import com.female.maker.oc.creator2.core.extensions.loadImage
import com.female.maker.oc.creator2.core.extensions.loadImageSticker
import com.female.maker.oc.creator2.core.extensions.tap
import com.female.maker.oc.creator2.data.model.SelectedModel
import com.female.maker.oc.creator2.databinding.ItemStickerBinding

class StickerAdapter : BaseAdapter<SelectedModel, ItemStickerBinding>(ItemStickerBinding::inflate) {
    var onItemClick : ((String) -> Unit) = {}
    override fun onBind(binding: ItemStickerBinding, item: SelectedModel, position: Int) {
        binding.apply {
            loadImageSticker(root, item.path, imvSticker)
            root.tap { onItemClick.invoke(item.path) }
        }
    }
}