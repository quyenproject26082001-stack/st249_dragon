package com.dragon.oc.avatar.creator.ui.add_character.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.dragon.oc.avatar.creator.R
import com.dragon.oc.avatar.creator.core.extensions.gone
import com.dragon.oc.avatar.creator.core.extensions.tap
import com.dragon.oc.avatar.creator.core.extensions.visible
import com.dragon.oc.avatar.creator.data.model.SelectedModel
import com.dragon.oc.avatar.creator.databinding.ItemBackgroundImageBinding

class BackgroundImageAdapter :
    ListAdapter<SelectedModel, BackgroundImageAdapter.ViewHolder>(DIFF_CALLBACK) {

    var onAddImageClick: (() -> Unit) = {}
    var onBackgroundImageClick: ((String, Int) -> Unit) = { _, _ -> }
    var showAddItem: Boolean = true

    inner class ViewHolder(val binding: ItemBackgroundImageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemBackgroundImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        Log.d("BgImageAdapter", "onBind pos=$position isSelected=${item.isSelected} path=${item.path}")
        holder.binding.apply {
            imvFrame.setImageResource(if (item.isSelected) R.drawable.item_slt_bg else R.drawable.item_uslt_bg)
            if (showAddItem && position == 0) {
                lnlAddItem.visible()
                imvImage.gone()
                tvAddImg.isSelected = true
                lnlAddItem.tap(500) { onAddImageClick.invoke() }
            } else {
                lnlAddItem.gone()
                imvImage.visible()
                val cornerRadiusPx = (4 * root.context.resources.displayMetrics.density).toInt()
                Glide.with(root)
                    .load(item.path)
                    .transform(RoundedCorners(cornerRadiusPx))
                    .into(imvImage)
                imvImage.tap { onBackgroundImageClick.invoke(item.path, position) }
            }
        }
    }

    fun submitItem(position: Int, list: ArrayList<SelectedModel>) {
        val selectedPositions = list.mapIndexedNotNull { i, m -> if (m.isSelected) i else null }
        Log.d("BgImageAdapter", "submitItem pos=$position | selectedInList=$selectedPositions | listSize=${list.size}")
        submitList(list.map { it.copy() })
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SelectedModel>() {
            override fun areItemsTheSame(oldItem: SelectedModel, newItem: SelectedModel): Boolean {
                return oldItem.path == newItem.path && oldItem.color == newItem.color
            }

            override fun areContentsTheSame(oldItem: SelectedModel, newItem: SelectedModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}
