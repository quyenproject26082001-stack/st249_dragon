package com.dragon.oc.avatar.creator.ui.dragon_webview

class DragonBridge(private val activity: DragonWebViewActivity) {

    @android.webkit.JavascriptInterface
    fun onEvent(eventJson: String) {
        try{
            val obj = org.json.JSONObject(eventJson)
            when (obj.getString("type")){
                "RENDER_COMPLETE" ->
                    activity.onRenderComplete()
                "DOWNLOAD_READY" ->
                    activity.saveAndShare(obj.getString("data"))
                "EDIT_READY" ->
                    activity.openEdit(
                        obj.getString("data"),
                        obj.optString("selectionState")
                    )
                "SUCCESS_READY" ->
                    activity.openSuccess(
                        obj.getString("data"),
                        obj.optString("selectionState")
                    )
            }
        } catch (e: Exception){
            android.util.Log.e("DragonBridge","onEvent: ${e.message}")
        }
    }
}
