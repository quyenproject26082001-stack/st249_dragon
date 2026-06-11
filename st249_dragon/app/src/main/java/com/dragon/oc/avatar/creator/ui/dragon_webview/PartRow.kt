package com.dragon.oc.avatar.creator.ui.dragon_webview

data class PartOption(val label: String, val value: String, val thumb: String?)

data class PartRow(
    val partId: String,
    val label: String,
    val colorId: String?,
    val options: List<PartOption>
)
