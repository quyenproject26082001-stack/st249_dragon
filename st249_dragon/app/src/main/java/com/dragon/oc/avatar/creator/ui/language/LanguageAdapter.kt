package com.dragon.oc.avatar.creator.ui.language

import android.annotation.SuppressLint
import android.content.Context
import com.dragon.oc.avatar.creator.R
import com.dragon.oc.avatar.creator.core.base.BaseAdapter
import com.dragon.oc.avatar.creator.core.extensions.gone
import com.dragon.oc.avatar.creator.core.extensions.loadImage
import com.dragon.oc.avatar.creator.core.extensions.tap
import com.dragon.oc.avatar.creator.core.extensions.visible
import com.dragon.oc.avatar.creator.data.model.LanguageModel
import com.dragon.oc.avatar.creator.databinding.ItemLanguageBinding

class LanguageAdapter(val context: Context) : BaseAdapter<LanguageModel, ItemLanguageBinding>(
    ItemLanguageBinding::inflate
) {
    var onItemClick: ((code: String) -> Unit) = {}
    var isFirstLanguage: Boolean = false

    override fun submitList(list: List<LanguageModel>) {
        if (items.isEmpty()) {
            items.addAll(list)
            notifyDataSetChanged()
        } else {
            val oldList = items.toList()
            items.clear()
            items.addAll(list)

            // Find changed items
            val changedPositions = mutableListOf<Int>()
            for (i in list.indices) {
                if (i < oldList.size && oldList[i].activate != list[i].activate) {
                    changedPositions.add(i)
                }
            }

            // Only notify changed items
            if (changedPositions.isNotEmpty()) {
                changedPositions.forEach { notifyItemChanged(it) }
            }
        }
    }

    override fun onBind(
        binding: ItemLanguageBinding, item: LanguageModel, position: Int
    ) {
        binding.apply {
            loadImage(root, item.flag, imvFlag, false)
            tvLang.text = "\u00a0${item.name}\u00a0"
            val strokePx = context.resources.displayMetrics.density * 1f

            if (item.activate) {
                tvLang.setTextColor(android.graphics.Color.parseColor("#FFD700"))
                tvLang.setOuterStroke(strokePx, android.graphics.Color.parseColor("#2B1A00"))
            } else {
                tvLang.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                tvLang.setOuterStroke(strokePx, android.graphics.Color.parseColor("#2B1A00"))

            }

            val ratio = if (item.activate) {
                R.drawable.ic_tick_lang
            } else {
                R.drawable.ic_not_tick_lang
            }
            loadImage(root, ratio, btnRadio, false)

            // Apply color tint when activated and not first language
//            if (item.activate && !isFirstLanguage) {
//                btnRadio.setColorFilter(
//                    android.graphics.Color.parseColor("#01579B"),
//                    android.graphics.PorterDuff.Mode.SRC_IN
//                )
//            } else {
//                btnRadio.clearColorFilter()
//            }

            // Set selected state to trigger the selector drawable
            flMain.isSelected = item.activate

            root.tap {
                onItemClick.invoke(item.code)
            }
        }
    }
}