package com.female.maker.oc.creator2.ui.view

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.female.maker.oc.creator2.R
import com.female.maker.oc.creator2.core.base.BaseActivity
import com.female.maker.oc.creator2.core.extensions.checkPermissions
import com.female.maker.oc.creator2.core.extensions.goToSettings
import com.female.maker.oc.creator2.core.extensions.gone
import com.female.maker.oc.creator2.core.extensions.handleBackLeftToRight
import com.female.maker.oc.creator2.core.extensions.hideNavigation
import com.female.maker.oc.creator2.core.extensions.invisible
import com.female.maker.oc.creator2.core.extensions.loadImage
import com.female.maker.oc.creator2.core.extensions.loadNativeCollabAds
import com.female.maker.oc.creator2.core.extensions.requestPermission
import com.female.maker.oc.creator2.core.extensions.select
import com.female.maker.oc.creator2.core.extensions.setImageActionBar
import com.female.maker.oc.creator2.core.extensions.setTextActionBar
import com.female.maker.oc.creator2.core.extensions.showInterAll
import com.female.maker.oc.creator2.core.extensions.strings
import com.female.maker.oc.creator2.core.extensions.tap
import com.female.maker.oc.creator2.core.helper.LanguageHelper
import com.female.maker.oc.creator2.core.helper.MediaHelper
import com.female.maker.oc.creator2.core.helper.UnitHelper
import com.female.maker.oc.creator2.core.utils.key.IntentKey
import com.female.maker.oc.creator2.core.utils.key.RequestKey
import com.female.maker.oc.creator2.core.utils.key.ValueKey
import com.female.maker.oc.creator2.core.utils.state.HandleState
import com.female.maker.oc.creator2.data.model.custom.DragonCardEditModel
import com.female.maker.oc.creator2.databinding.ActivityViewBinding
import com.female.maker.oc.creator2.dialog.YesNoDialog
import com.female.maker.oc.creator2.ui.dragon_webview.DragonWebViewActivity
import com.female.maker.oc.creator2.ui.edit.EditActivity
import com.female.maker.oc.creator2.ui.my_creation.fragment.MyAvatarFragment
import com.female.maker.oc.creator2.ui.my_creation.MyCreationActivity
import com.female.maker.oc.creator2.ui.permission.PermissionViewModel
import com.google.protobuf.value
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewActivity : BaseActivity<ActivityViewBinding>() {
    private val viewModel: ViewViewModel by viewModels()
    private val permissionViewModel: PermissionViewModel by viewModels()

    override fun setViewBinding(): ActivityViewBinding {
        return ActivityViewBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        viewModel.setPath(intent.getStringExtra(IntentKey.INTENT_KEY)!!)
        viewModel.updateStatusFrom(intent.getIntExtra(IntentKey.STATUS_KEY, ValueKey.AVATAR_TYPE))
        resolveDisplayPath()
        binding.lnlBottom.isSelected = true

        setButtonBackgrounds()
        setupUI()
    }

    private fun resolveDisplayPath() {
        if (viewModel.statusFrom != ValueKey.AVATAR_TYPE) return
        val dragonCard = MediaHelper
            .readListFromFile<DragonCardEditModel>(this, ValueKey.DRAGON_CARD_EDIT_FILE_INTERNAL)
            .firstOrNull { it.previewPath == viewModel.pathInternal.value }
        val dragonImagePath = dragonCard?.dragonImagePath.orEmpty()
        if (dragonImagePath.isNotEmpty() && File(dragonImagePath).exists()) {
            viewModel.setDisplayPath(dragonImagePath)
        }
    }

    private fun setButtonBackgrounds() {

    }

    private fun setupUI() {
        binding.apply {
            actionBar.apply {
             //   setTextActionBar(tvCenter, getString(R.string.my_work))
                //  setImageActionBar(btnActionBarNextRight, R.drawable.ic_edit_view)
                setImageActionBar(btnActionBarRight, R.drawable.ic_edit_view)

                // Hide edit icon when coming from design section
                if (viewModel.statusFrom == ValueKey.MY_DESIGN_TYPE||viewModel.statusFrom == ValueKey.PRIDE_OVERLAY_TYPE) {
                    btnActionBarRight.invisible()
                }

            }

            // Set scaleType based on content type
            if (viewModel.statusFrom == ValueKey.AVATAR_TYPE) {
                // For avatars, use fitCenter to show full character without cropping
                imvImage.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            } else {
                // For designs, use center to maintain original size
                imvImage.scaleType = android.widget.ImageView.ScaleType.CENTER
            }
        }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.displayPath.collect { path ->
                    loadImage(this@ViewActivity, path, binding.imvImage)
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.apply {
                btnActionBarLeft.tap { handleBack() }
                btnActionBarNextRight.tap { handleEditClick(viewModel.pathInternal.value) }
                btnActionBarRight.tap{ handleDelete() }
            }

            includeLayoutBottom.btnWhatsapp.tap(2590) {
                viewModel.shareFiles(this@ViewActivity)
            }
            includeLayoutBottom.btnTelegram.tap(2000) {
                checkStoragePermission()
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
           // tvCenter.select()

            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            if(viewModel.statusFrom == ValueKey.AVATAR_TYPE)
            {
                setImageActionBar(btnActionBarNextRight, R.drawable.ic_edit)
            }
            else{
                btnActionBarNextRight.gone()
            }
            setImageActionBar(btnActionBarRight, R.drawable.ic_delete_item)
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            handleDownload()
        } else {
            val perms = permissionViewModel.getStoragePermissions()
            if (checkPermissions(perms)) {
                handleDownload()
            } else if (permissionViewModel.needGoToSettings(sharePreference, true)) {
                goToSettings()
            } else {
                requestPermission(perms, RequestKey.STORAGE_PERMISSION_CODE)
            }
        }
    }

    private fun handleDownload() {
        lifecycleScope.launch {
            viewModel.downloadFiles(this@ViewActivity).collect { state ->
                when (state) {
                    HandleState.LOADING -> showLoading()
                    HandleState.SUCCESS -> {
                        dismissLoading()
                        showToast(R.string.download_success)
                    }

                    else -> {
                        dismissLoading()
                        showToast(R.string.download_failed_please_try_again_later)
                    }
                }
            }
        }
    }

    private fun handleDelete() {
        val dialog =
            YesNoDialog(this, R.string.delete, R.string.are_you_sure_want_to_delete_this_item)
        LanguageHelper.setLocale(this)
        dialog.show()
        dialog.onNoClick = {
            dialog.dismiss()
            hideNavigation()
        }
        dialog.onYesClick = {
            dialog.dismiss()
            lifecycleScope.launch {
                viewModel.deleteFile(this@ViewActivity, viewModel.pathInternal.value)
                    .collect { state ->
                        when (state) {
                            HandleState.LOADING -> showLoading()
                            HandleState.SUCCESS -> {
                                dismissLoading()
                                resetMyCreationSelectionMode()

                                setResult(Activity.RESULT_OK, Intent().apply {
                                    putExtra("DELETED_PATH", viewModel.pathInternal.value)
                                })
                                finish()
                            }

                            else -> {
                                dismissLoading()
                                showToast(R.string.delete_failed_please_try_again)
                            }
                        }
                    }
            }
        }
    }

    private fun handleBack() {
        resetMyCreationSelectionMode()
        handleBackLeftToRight()
    }

    private fun resetMyCreationSelectionMode() {
        val myCreationActivity = MyCreationActivity.getInstance()
        if (myCreationActivity != null) {
            android.util.Log.d("ViewActivity", "Resetting selection mode in MyCreationActivity")

            val designFragment =
                myCreationActivity.supportFragmentManager.findFragmentByTag("MyDesignFragment")
            if (designFragment is com.female.maker.oc.creator2.ui.my_creation.fragment.MyDesignFragment) {
                designFragment.resetSelectionMode()
            }

            val avatarFragment =
                myCreationActivity.supportFragmentManager.findFragmentByTag("MyAvatarFragment")
            if (avatarFragment is MyAvatarFragment) {
                avatarFragment.resetSelectionMode()
            }

            myCreationActivity.exitSelectionMode()
        } else {
            android.util.Log.w(
                "ViewActivity",
                "MyCreationActivity instance not found - unable to reset selection mode"
            )
        }
    }

    private fun handleEditClick(pathInternal: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dragonCard = MediaHelper
                .readListFromFile<DragonCardEditModel>(
                    this@ViewActivity,
                    ValueKey.DRAGON_CARD_EDIT_FILE_INTERNAL
                )
                .firstOrNull { it.previewPath == pathInternal }

            withContext(Dispatchers.Main) {
                val intent = if (!dragonCard?.selectionState.isNullOrBlank()) {
                    Intent(this@ViewActivity, DragonWebViewActivity::class.java).apply {
                        putExtra(DragonWebViewActivity.EXTRA_SELECTION_STATE, dragonCard?.selectionState)
                        putExtra(IntentKey.EDIT_SOURCE_PATH, pathInternal)
                    }
                } else {
                    Intent(this@ViewActivity, EditActivity::class.java).apply {
                        putExtra(IntentKey.EDIT_IMAGE_PATH, pathInternal)
                        putExtra(IntentKey.EDIT_SOURCE_PATH, pathInternal)
                    }
                }
                showInterAll { startActivity(intent) }
                overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == RequestKey.STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                permissionViewModel.updateStorageGranted(sharePreference, true)
                handleDownload()
            } else {
                permissionViewModel.updateStorageGranted(sharePreference, false)
            }
        }
    }

//    override fun initAds() {
//        initNativeCollab()
//    }
//
//    fun initNativeCollab() {
//
//        loadNativeCollabAds(R.string.native_cl_detail, binding.flNativeCollab)
//
//
//    }

    @android.annotation.SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        handleBack()
    }
}
