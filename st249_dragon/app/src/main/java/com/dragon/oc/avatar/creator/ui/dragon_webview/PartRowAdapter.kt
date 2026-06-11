package com.dragon.oc.avatar.creator.ui.dragon_webview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dragon.oc.avatar.creator.R

class PartRowAdapter(
    private val onOptionClick: (partId: String, value: String) -> Unit,
    private val onColorClick: (colorId: String) -> Unit
) : ListAdapter<PartRow, PartRowAdapter.PartRowViewHolder>(DIFF) {


    private val selectedValues = mutableMapOf<String, String>()


    companion object {
        val DIFF = object : DiffUtil.ItemCallback<PartRow>() {
            override fun areItemsTheSame(oldItem: PartRow, newItem: PartRow) =
                oldItem.partId == newItem.partId

            override fun areContentsTheSame(oldItem: PartRow, newItem: PartRow) =
                oldItem == newItem
        }
    }

    class PartRowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLabel: TextView = view.findViewById(R.id.tvPartLabel)
        val llOptions: LinearLayout = view.findViewById(R.id.llOptions)
        val btnColor: ImageView = view.findViewById(R.id.btnColor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartRowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_part_row, parent, false)
        return PartRowViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartRowViewHolder, position: Int) {
        val row = getItem(position)
        val selectedValue = selectedValues.getOrPut(row.partId) {
            row.options.firstOrNull()?.value.orEmpty()
        }
        holder.tvLabel.text = row.label
        holder.llOptions.removeAllViews()


        row.options.forEach { opt ->
            val ctx = holder.itemView.context
            val img = LayoutInflater.from(ctx)
                .inflate(R.layout.item_part_option, holder.llOptions, false) as ImageView
            img.isSelected = (opt.value == selectedValue)
            img.setOnClickListener {
                selectedValues[row.partId] = opt.value
                notifyItemChanged(holder.bindingAdapterPosition)
                onOptionClick(row.partId, opt.value)
            }
            if (opt.thumb != null) {
                Glide.with(ctx)
                    .load("file:///android_asset/${opt.thumb}")
                    .into(img)
            } else {
                img.setImageResource(R.drawable.ic_none)
            }
            holder.llOptions.addView(img)
        }

        if (row.colorId != null) {
            holder.btnColor.visibility = View.VISIBLE
            holder.btnColor.setOnClickListener { onColorClick(row.colorId) }
        } else {
            holder.btnColor.visibility = View.GONE
        }
    }

    fun submitRows(rows: List<PartRow>) {
        rows.forEach { row ->
            selectedValues.putIfAbsent(row.partId, row.options.firstOrNull()?.value.orEmpty())
        }
        submitList(rows)
    }

    fun setSelectedValues(values: Map<String, String>) {
        selectedValues.putAll(values)
        notifyDataSetChanged()
    }
}
