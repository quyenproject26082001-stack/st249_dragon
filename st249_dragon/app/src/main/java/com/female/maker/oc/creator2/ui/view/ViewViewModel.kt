package com.female.maker.oc.creator2.ui.view

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.female.maker.oc.creator2.core.extensions.shareImagesPaths
import com.female.maker.oc.creator2.core.helper.MediaHelper
import com.female.maker.oc.creator2.core.utils.key.ValueKey
import com.female.maker.oc.creator2.core.utils.state.HandleState
import com.female.maker.oc.creator2.data.model.custom.DragonCardEditModel
import com.female.maker.oc.creator2.data.model.custom.SuggestionModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class ViewViewModel : ViewModel() {
    private val _pathInternal = MutableStateFlow<String>("")
    val pathInternal: StateFlow<String> = _pathInternal.asStateFlow()
    private val _displayPath = MutableStateFlow<String>("")
    val displayPath: StateFlow<String> = _displayPath.asStateFlow()

    var statusFrom = ValueKey.AVATAR_TYPE

    fun setPath(path: String) {
        _pathInternal.value = path
        _displayPath.value = path
    }

    fun setDisplayPath(path: String) {
        _displayPath.value = path
    }

    fun deleteFile(context: Context, path: String): Flow<HandleState> = flow {
        if (statusFrom == ValueKey.MY_DESIGN_TYPE || statusFrom == ValueKey.PRIDE_OVERLAY_TYPE) {
            emitAll(MediaHelper.deleteFileByPath(arrayListOf(path)))
        } else {
            emit(HandleState.LOADING)
            try {
                val originList = MediaHelper
                    .readListFromFile<SuggestionModel>(context, ValueKey.EDIT_FILE_INTERNAL)
                    .toCollection(ArrayList())
                val dragonCardList = MediaHelper
                    .readListFromFile<DragonCardEditModel>(context, ValueKey.DRAGON_CARD_EDIT_FILE_INTERNAL)
                    .toCollection(ArrayList())

                originList.removeAll { it.pathInternalEdit == path }
                dragonCardList.removeAll { it.previewPath == path }

                MediaHelper.writeListToFile(context, ValueKey.EDIT_FILE_INTERNAL, originList)
                MediaHelper.writeListToFile(context, ValueKey.DRAGON_CARD_EDIT_FILE_INTERNAL, dragonCardList)

                emit(HandleState.SUCCESS)
            } catch (e: Exception) {
                Log.e("nbhieu", "deleteFile: $e")
                emit(HandleState.FAIL)
            }
        }
    }

    fun shareFiles(context: Activity) {
        viewModelScope.launch {
            context.shareImagesPaths(arrayListOf(_displayPath.value))
        }
    }

    fun downloadFiles(context: Activity): Flow<HandleState> = flow {
        emitAll(
            MediaHelper.downloadPartsToExternal(
                context, arrayListOf(_displayPath.value)
            )
        )
    }

    fun updateStatusFrom(status: Int) {
        statusFrom = status
    }
}
