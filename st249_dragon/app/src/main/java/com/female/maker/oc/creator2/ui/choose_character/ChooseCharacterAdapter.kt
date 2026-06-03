package com.female.maker.oc.creator2.ui.choose_character

import com.female.maker.oc.creator2.core.base.BaseAdapter
import com.female.maker.oc.creator2.core.extensions.gone
import com.female.maker.oc.creator2.core.extensions.loadImage
import com.female.maker.oc.creator2.core.extensions.tap
import com.female.maker.oc.creator2.data.model.custom.CustomizeModel
import com.female.maker.oc.creator2.databinding.ItemChooseAvatarBinding

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