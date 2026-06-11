package com.dragon.oc.avatar.creator.ui.choose_character

import com.dragon.oc.avatar.creator.core.base.BaseAdapter
import com.dragon.oc.avatar.creator.core.extensions.gone
import com.dragon.oc.avatar.creator.core.extensions.loadImage
import com.dragon.oc.avatar.creator.core.extensions.tap
import com.dragon.oc.avatar.creator.data.model.custom.CustomizeModel
import com.dragon.oc.avatar.creator.databinding.ItemChooseAvatarBinding

class ChooseCharacterAdapter : BaseAdapter<CustomizeModel, ItemChooseAvatarBinding>(ItemChooseAvatarBinding::inflate) {
    var onItemClick: ((position: Int) -> Unit) = {}
    override fun onBind(binding: ItemChooseAvatarBinding, item: CustomizeModel, position: Int) {
        binding.apply {
            loadImage(item.avatar, imvImage, onDismissLoading = {
                sflShimmer.stopShimmer()
                sflShimmer.gone()
            })
            root.tap { onItemClick.invoke(position) }
        }
    }
}