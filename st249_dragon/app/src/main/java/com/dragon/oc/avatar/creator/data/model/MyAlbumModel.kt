package com.dragon.oc.avatar.creator.data.model

data class MyAlbumModel(
    val path: String,
    val displayPath: String = path,
    var isShowSelection: Boolean = false,
    var isSelected: Boolean = false
)
