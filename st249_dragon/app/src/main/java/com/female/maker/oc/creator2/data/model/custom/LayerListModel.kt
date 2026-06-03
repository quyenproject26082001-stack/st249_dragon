package com.female.maker.oc.creator2.data.model.custom

data class LayerListModel(
    var positionCustom: Int = 0,
    var positionNavigation: Int = 0,
    var imageNavigation: String = "",
    var layer: ArrayList<LayerModel> = arrayListOf(),
)
