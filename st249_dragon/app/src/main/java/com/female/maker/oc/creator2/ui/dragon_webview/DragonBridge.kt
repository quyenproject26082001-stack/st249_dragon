package com.female.maker.oc.creator2.ui.dragon_webview

class DragonBridge(private val activity: DragonWebViewActivity) {

    @android.webkit.JavascriptInterface
    fun onEvent(eventJson: String) {
        try{
            val obj = org.json.JSONObject(eventJson)
            when (obj.getString("type")){
                "DOWNLOAD_READY" ->
                    activity.saveAndShare(obj.getString("data"))
                "EDIT_READY" ->
                    activity.openEdit(obj.getString("data"))
            }
        } catch (e: Exception){
            android.util.Log.e("DragonBridge","onEvent: ${e.message}")
        }
    }
}
