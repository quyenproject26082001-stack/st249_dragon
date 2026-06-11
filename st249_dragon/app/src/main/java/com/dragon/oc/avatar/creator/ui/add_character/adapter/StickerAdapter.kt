package com.dragon.oc.avatar.creator.ui.add_character.adapter

import com.dragon.oc.avatar.creator.core.base.BaseAdapter
import com.dragon.oc.avatar.creator.core.extensions.loadImage
import com.dragon.oc.avatar.creator.core.extensions.loadImageSticker
import com.dragon.oc.avatar.creator.core.extensions.tap
import com.dragon.oc.avatar.creator.data.model.SelectedModel
import com.dragon.oc.avatar.creator.databinding.ItemStickerBinding

class StickerAdapter : BaseAdapter<SelectedModel, ItemStickerBinding>(ItemStickerBinding::inflate) {
    var onItemClick : ((String) -> Unit) = {}
    override fun onBind(binding: ItemStickerBinding, item: SelectedModel, position: Int) {
        binding.apply {
            loadImageSticker(root, item.path, imvSticker)
            root.tap { onItemClick.invoke(item.path) }
        }
    }
}