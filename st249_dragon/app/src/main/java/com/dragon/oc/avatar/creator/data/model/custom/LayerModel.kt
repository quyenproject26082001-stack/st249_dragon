package com.dragon.oc.avatar.creator.data.model.custom

import com.dragon.oc.avatar.creator.data.model.custom.ColorModel

data class LayerModel(
    val image: String,
    val isMoreColors: Boolean = false,
    var listColor: ArrayList<ColorModel> = arrayListOf(),
    val thumb: String = ""
)