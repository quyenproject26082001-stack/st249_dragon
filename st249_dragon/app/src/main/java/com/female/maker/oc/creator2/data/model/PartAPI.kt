package com.female.maker.oc.creator2.data.model

import android.os.Parcelable

data class PartAPI(
    val position: String,
    val parts: String,
    val colorArray: String,
    val quantity: String,
    val level: String
)

data class DataAPI(val name: String, val parts: List<PartAPI>)
