package com.female.maker.oc.creator2.data.model.custom

data class DragonCardEditModel(
    val id: String = "",
    var previewPath: String = "",
    var dragonImagePath: String = "",
    var nameTag: String = "",
    var nameFont: Int = 0,
    var nameColor: Int = 0,
    var describe: String = "",
    var describeFont: Int = 0,
    var describeColor: Int = 0,
    var starRating: Int = 2,
    var starStyle: Int = 1,
    var atk: String = "",
    var def: String = "",
    var bgImageColor: Int = 0,
    var bgTagColor: Int = 0,
    var bgImagePath: String = "",
    var bgTagPath: String = "",
    var selectionState: String = ""
)
