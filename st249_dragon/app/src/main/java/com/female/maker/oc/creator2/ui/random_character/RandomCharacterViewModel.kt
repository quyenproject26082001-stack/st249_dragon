package com.female.maker.oc.creator2.ui.random_character

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import com.female.maker.oc.creator2.R
import com.female.maker.oc.creator2.core.helper.InternetHelper
import com.female.maker.oc.creator2.core.utils.state.HandleState
import com.female.maker.oc.creator2.data.model.custom.SuggestionModel
import kotlinx.coroutines.flow.MutableStateFlow

class RandomCharacterViewModel : ViewModel() {

    val randomList = ArrayList<SuggestionModel>()
    // Data từ API hay không
    private val _isDataAPI = MutableStateFlow(false)
    //-----------------------------------------------------------------------------------------------------------------

    suspend fun updateRandomList(suggestionModel: SuggestionModel){
        randomList.add(suggestionModel)
    }
    fun upsideDownList() = randomList.shuffle()

    fun setIsDataAPI(isAPI: Boolean) {
        _isDataAPI.value = isAPI
    }

    fun checkDataInternet(context: AppCompatActivity, action: (() -> Unit)) {
        if (!_isDataAPI.value) {
            action.invoke()
            return
        }
        InternetHelper.checkInternet(context) { result ->
            if (result == HandleState.SUCCESS) {
                action.invoke()
            } else {
                // Show No Internet dialog
                val dialog = com.female.maker.oc.creator2.dialog.YesNoDialog(
                    context,
                    R.string.no_internet,
                    R.string.please_check_your_internet,
                    isError = true,
                    dialogType = com.female.maker.oc.creator2.dialog.DialogType.INTERNET
                )
                dialog.show()
                dialog.onYesClick = {
                    dialog.dismiss()
                }
            }
        }
    }


}