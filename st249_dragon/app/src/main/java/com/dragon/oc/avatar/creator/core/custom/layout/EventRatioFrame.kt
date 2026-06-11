package com.dragon.oc.avatar.creator.core.custom.layout

import android.widget.ImageView
import com.dragon.oc.avatar.creator.core.custom.imageview.StrokeImageView

interface EventRatioFrame {
    fun onImageClick(image: StrokeImageView, btnEdit: ImageView)
}