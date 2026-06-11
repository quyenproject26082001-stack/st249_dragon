package com.dragon.oc.avatar.creator.ui.random_character

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.graphics.createBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.dragon.oc.avatar.creator.core.base.BaseAdapter
import com.dragon.oc.avatar.creator.core.extensions.gone
import com.dragon.oc.avatar.creator.core.extensions.invisible
import com.dragon.oc.avatar.creator.core.extensions.tap
import com.dragon.oc.avatar.creator.core.extensions.visible
import com.dragon.oc.avatar.creator.core.helper.MediaHelper
import com.dragon.oc.avatar.creator.core.utils.key.ValueKey
import com.dragon.oc.avatar.creator.core.utils.state.SaveState
import com.dragon.oc.avatar.creator.data.model.custom.SuggestionModel
import com.dragon.oc.avatar.creator.databinding.ItemRandomCharacterBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RandomCharacterAdapter(val context: Context) :
    BaseAdapter<SuggestionModel, ItemRandomCharacterBinding>(ItemRandomCharacterBinding::inflate) {
    var onItemClick: ((SuggestionModel) -> Unit) = {}

    private val activeJobs = mutableMapOf<Int, kotlinx.coroutines.Job>()

    override fun onBind(binding: ItemRandomCharacterBinding, item: SuggestionModel, position: Int) {
        binding.apply {
            val holderKey = System.identityHashCode(root)
            val bindToken = Any()
            root.tag = bindToken

            val targetItem = item
            val targetPaths = ArrayList(item.pathSelectedList)

            Log.d("RandomAdapter", "onBind position=$position avatar=${targetItem.avatarPath}")

            activeJobs[holderKey]?.cancel()

            if (targetItem.pathInternalRandom.isNotEmpty()) {
                sflShimmer.gone()
                sflShimmer.stopShimmer()
                imvImage.visible()
                Glide.with(root).load(targetItem.pathInternalRandom).into(imvImage)
                root.tap { onItemClick.invoke(targetItem) }
                return@apply
            }

            sflShimmer.visible()
            sflShimmer.startShimmer()
            imvImage.invisible()

            var width = ValueKey.WIDTH_BITMAP
            var height = ValueKey.HEIGHT_BITMAP
            val layerBitmaps = arrayListOf<Bitmap>()
            val errorHandler = CoroutineExceptionHandler { _, throwable ->
                Log.e("RandomAdapter", "Render failed at position $position: ${throwable.message}", throwable)
            }

            val job = CoroutineScope(SupervisorJob() + Dispatchers.IO + errorHandler).launch {
                val loaded = async {
                    if (targetPaths.isEmpty()) return@async false

                    val defaultBitmap = Glide.with(context)
                        .asBitmap()
                        .load(targetPaths.first())
                        .submit()
                        .get()

                    width = (defaultBitmap.width / 2).takeIf { it > 0 } ?: ValueKey.WIDTH_BITMAP
                    height = (defaultBitmap.height / 2).takeIf { it > 0 } ?: ValueKey.HEIGHT_BITMAP

                    if (targetItem.pathInternalRandom.isEmpty()) {
                        targetPaths.forEach { path ->
                            layerBitmaps.add(
                                Glide.with(context)
                                    .asBitmap()
                                    .load(path)
                                    .submit(width, height)
                                    .get()
                            )
                        }
                    }
                    true
                }.await()

                withContext(Dispatchers.Main) {
                    if (!loaded || root.tag !== bindToken) return@withContext

                    if (targetItem.pathInternalRandom.isEmpty()) {
                        val combinedBitmap = createBitmap(width, height)
                        val canvas = Canvas(combinedBitmap)

                        layerBitmaps.forEach { bitmap ->
                            val left = (width - bitmap.width) / 2f
                            val top = (height - bitmap.height) / 2f
                            canvas.drawBitmap(bitmap, left, top, null)
                        }

                        MediaHelper.saveBitmapToInternalStorage(
                            context,
                            ValueKey.RANDOM_TEMP_ALBUM,
                            combinedBitmap
                        ).collect { state ->
                            if (state is SaveState.Success) {
                                targetItem.pathInternalRandom = state.path
                            }
                        }
                    }

                    if (root.tag !== bindToken) return@withContext
                    Glide.with(root)
                        .load(targetItem.pathInternalRandom)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable?>,
                                isFirstResource: Boolean
                            ): Boolean {
                                if (root.tag !== bindToken) return true
                                sflShimmer.stopShimmer()
                                sflShimmer.gone()
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable?>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                if (root.tag !== bindToken) return true
                                sflShimmer.stopShimmer()
                                sflShimmer.gone()
                                imvImage.visible()
                                return false
                            }
                        })
                        .into(imvImage)
                }
            }

            activeJobs[holderKey] = job
            root.tap { onItemClick.invoke(targetItem) }
        }
    }

    fun cancelAllJobs() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
    }
}
