package com.dragon.oc.avatar.creator.ui.dragon_webview

import android.content.res.AssetManager
import org.json.JSONObject

object PartsConfigParser {

    fun load(assets: AssetManager, tab: String): List<PartRow> {
        val json = assets.open("parts_config.json")
            .bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val partsObj = root.getJSONObject("parts")
        val rows = partsObj.getJSONArray(tab)

        val result = mutableListOf<PartRow>()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val optionsArr = row.getJSONArray("options")
            val options = mutableListOf<PartOption>()
            for (j in 0 until optionsArr.length()) {
                val opt = optionsArr.getJSONObject(j)
                options.add(PartOption(
                    label = opt.getString("label"),
                    value = opt.getString("value"),
                    thumb = if (opt.isNull("thumb")) null else opt.getString("thumb")
                ))
            }
            result.add(PartRow(
                partId = row.getString("partId"),
                label = row.getString("label"),
                colorId = if (row.isNull("colorId")) null else
                    row.getString("colorId"),
                options = options
            ))
        }
        return result
    }
}