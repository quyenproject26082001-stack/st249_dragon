package com.dragon.oc.avatar.creator.ui.edit

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.dragon.oc.avatar.creator.R
import com.dragon.oc.avatar.creator.core.extensions.gone
import com.dragon.oc.avatar.creator.core.extensions.handleBackLeftToRight
import com.dragon.oc.avatar.creator.core.extensions.hideNavigation
import com.dragon.oc.avatar.creator.core.extensions.createBitmapFromView
import com.dragon.oc.avatar.creator.core.extensions.focusAndShowKeyboard
import com.dragon.oc.avatar.creator.core.extensions.setImageActionBar
import com.dragon.oc.avatar.creator.core.extensions.setFont
import com.dragon.oc.avatar.creator.core.extensions.tap
import com.dragon.oc.avatar.creator.core.extensions.visible
import com.dragon.oc.avatar.creator.core.helper.AssetHelper
import com.dragon.oc.avatar.creator.core.utils.DataLocal
import com.dragon.oc.avatar.creator.core.helper.MediaHelper
import com.dragon.oc.avatar.creator.core.utils.key.IntentKey
import com.dragon.oc.avatar.creator.core.utils.key.ValueKey
import com.dragon.oc.avatar.creator.data.model.SelectedModel
import com.dragon.oc.avatar.creator.data.model.custom.DragonCardEditModel
import com.dragon.oc.avatar.creator.databinding.ActivityEditBinding
import com.dragon.oc.avatar.creator.dialog.ChooseColorDialog
import com.dragon.oc.avatar.creator.dialog.DialogType
import com.dragon.oc.avatar.creator.dialog.YesNoDialog
import com.dragon.oc.avatar.creator.ui.add_character.adapter.BackgroundImageAdapter
import com.dragon.oc.avatar.creator.ui.add_character.adapter.TextColorAdapter
import com.dragon.oc.avatar.creator.ui.add_character.adapter.TextFontAdapter
import com.dragon.oc.avatar.creator.ui.success.SuccessActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBinding
    private val bgImageAdapter by lazy { BackgroundImageAdapter().apply { showAddItem = false } }
    private val bgTagAdapter by lazy { BackgroundImageAdapter().apply { showAddItem = false } }
    private val nameFontAdapter by lazy { TextFontAdapter(this) }
    private val nameColorAdapter by lazy { TextColorAdapter() }
    private val describeFontAdapter by lazy { TextFontAdapter(this) }
    private val describeColorAdapter by lazy { TextColorAdapter() }
    private var bgImageAssets = arrayListOf<SelectedModel>()
    private var bgTagAssets = arrayListOf<SelectedModel>()
    private var nameFonts = arrayListOf<SelectedModel>()
    private var nameColors = arrayListOf<SelectedModel>()
    private var describeFonts = arrayListOf<SelectedModel>()
    private var describeColors = arrayListOf<SelectedModel>()
    private var currentDragonImagePath = ""
    private var currentSourcePath = ""
    private var currentModelId = ""
    private var currentNameFont = 0
    private var currentNameColor = 0
    private var currentDescribeFont = 0
    private var currentDescribeColor = 0
    private var currentBgImagePath = ""
    private var currentBgTagPath = ""
    private var currentSelectionState = ""
    private var currentStarRating = 2
    private var currentStarStyle = 1
    private var currentAtk = ""
    private var currentDef = ""
    private var pickingBgTag = false

    private val pickBgImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                if (pickingBgTag) {
                    addCustomBgTag(it.toString())
                } else {
                    addCustomBgImage(it.toString())
                }
            }
            pickingBgTag = false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        setupActionBar()
        setupTabs()
        setupTextEditors()
        setupStyleEditors()
        setupPowerEditors()
        setupBackgroundEditors()
        restoreState()
        showTab(EditTab.NAME_TAG)
        hideNavigation(true)
    }

    override fun onResume() {
        super.onResume()
        hideNavigation(true)
    }

    private fun setupActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setImageActionBar(btnActionBarCenter, R.drawable.ic_reset)
            btnActionBarLeft.tap { handleBackLeftToRight() }
            btnActionBarCenter.visible()
            btnActionBarCenter.tap { confirmReset() }
            btnActionBarRight.gone()
            btnActionBarRightText.visible()
            tvRightText.visible()
            tvRightText.text = getString(R.string.save)
            btnActionBarRightText.tap { handleDone() }
        }
    }

    private fun setupTabs() {
        binding.btnNameTag.tap { showTab(EditTab.NAME_TAG) }
        binding.btnPower.tap { showTab(EditTab.POWER) }
        binding.btnBg.tap { showTab(EditTab.BG) }
        binding.btnDescrible.tap { showTab(EditTab.DESCRIBE) }
    }

    private fun confirmReset() {
        val dialog = YesNoDialog(
            this,
            R.string.reset,
            R.string.change_your_whole_design_are_you_sure,
            dialogType = DialogType.RESET
        )
        dialog.show()

        fun closeDialog() {
            dialog.dismiss()
            hideNavigation(true)
        }

        dialog.onNoClick = { closeDialog() }
        dialog.onYesClick = {
            closeDialog()
            resetEditState()
        }
    }

    private fun resetEditState() {
        binding.tabNameTag.edtText.setText("")
        binding.tabDescribe.edtDescribe.setText("")

        nameFonts.selectItem(0)
        nameColors.selectItem(1)
        describeFonts.selectItem(0)
        describeColors.selectItem(1)
        applyNameFont(nameFonts[0].color)
        applyNameColor(nameColors[1].color)
        applyDescribeFont(describeFonts[0].color)
        applyDescribeColor(describeColors[1].color)
        nameFontAdapter.submitListReset(nameFonts)
        nameColorAdapter.submitListReset(nameColors)
        describeFontAdapter.submitListReset(describeFonts)
        describeColorAdapter.submitListReset(describeColors)

        applyStarRating(2)
        applyStarStyle(1)
        binding.tabPower.edtAtk.setText("")
        binding.tabPower.edtDef.setText("")

        currentBgImagePath = bgImageAssets.getOrNull(1)?.path.orEmpty()
        currentBgTagPath = bgTagAssets.getOrNull(1)?.path.orEmpty()
        if (currentBgImagePath.isNotEmpty()) {
            bgImageAssets.selectItem(1)
            applyBgImageAsset(currentBgImagePath)
        }
        if (currentBgTagPath.isNotEmpty()) {
            bgTagAssets.selectItem(1)
            applyBgTagAsset(currentBgTagPath)
        }
        bgImageAdapter.submitList(bgImageAssets.map { it.copy() })
        bgTagAdapter.submitList(bgTagAssets.map { it.copy() })
        showBackgroundList(isImageBackground = true)
        showTab(EditTab.NAME_TAG)
    }

    private fun setupTextEditors() {
        binding.tabNameTag.edtText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tvNameTag.text = s?.toString().orEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.tabDescribe.edtDescribe.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tvDescrible.text = s?.toString().orEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.tabNameTag.btnDoneText.tap { binding.tabNameTag.edtText.clearFocus() }
        binding.tabDescribe.btnDoneDescribe.tap { binding.tabDescribe.edtDescribe.clearFocus() }
    }

    private fun setupStyleEditors() {
        nameFonts = DataLocal.getTextFontDefault().apply { selectItem(0) }
        nameColors = DataLocal.getTextColorDefault(this).apply { selectItem(1) }
        describeFonts = DataLocal.getTextFontDefault().apply { selectItem(0) }
        describeColors = DataLocal.getTextColorDefault(this).apply { selectItem(1) }

        binding.tabNameTag.rcvFont.adapter = nameFontAdapter
        binding.tabNameTag.rcvTextColor.adapter = nameColorAdapter
        binding.tabDescribe.rcvDescribeFont.adapter = describeFontAdapter
        binding.tabDescribe.rcvDescribeTextColor.adapter = describeColorAdapter

        nameFontAdapter.submitListReset(nameFonts)
        nameColorAdapter.submitListReset(nameColors)
        describeFontAdapter.submitListReset(describeFonts)
        describeColorAdapter.submitListReset(describeColors)

        currentNameFont = nameFonts[0].color
        currentNameColor = nameColors[1].color
        currentDescribeFont = describeFonts[0].color
        currentDescribeColor = describeColors[1].color
        applyNameFont(currentNameFont)
        applyNameColor(currentNameColor)
        applyDescribeFont(currentDescribeFont)
        applyDescribeColor(currentDescribeColor)

        nameFontAdapter.onTextFontClick = { font, position ->
            nameFonts.selectItem(position)
            applyNameFont(font)
            nameFontAdapter.submitItem(position, nameFonts)
        }
        nameColorAdapter.onChooseColorClick = {
            showChooseColorDialog { color ->
                nameColors.updateCustomColor(color)
                applyNameColor(color)
                nameColorAdapter.submitItem(0, nameColors)
            }
        }
        nameColorAdapter.onTextColorClick = { color, position ->
            nameColors.selectItem(position)
            applyNameColor(color)
            nameColorAdapter.submitItem(position, nameColors)
        }

        describeFontAdapter.onTextFontClick = { font, position ->
            describeFonts.selectItem(position)
            applyDescribeFont(font)
            describeFontAdapter.submitItem(position, describeFonts)
        }
        describeColorAdapter.onChooseColorClick = {
            showChooseColorDialog { color ->
                describeColors.updateCustomColor(color)
                applyDescribeColor(color)
                describeColorAdapter.submitItem(0, describeColors)
            }
        }
        describeColorAdapter.onTextColorClick = { color, position ->
            describeColors.selectItem(position)
            applyDescribeColor(color)
            describeColorAdapter.submitItem(position, describeColors)
        }
    }

    private fun setupPowerEditors() {
        binding.tabPower.btnRatingStar.setOnRatingChangeListener { _, rating, _ ->
            applyStarRating(rating.toInt().coerceIn(0, 7))
        }

        binding.tabPower.edtAtk.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyAtk(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.tabPower.edtDef.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyDef(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.tabPower.edtAtk.setOnClickListener { focusAndShowKeyboard(binding.tabPower.edtAtk) }
        binding.tabPower.edtDef.setOnClickListener { focusAndShowKeyboard(binding.tabPower.edtDef) }
        binding.tabPower.edtAtk.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) focusAndShowKeyboard(binding.tabPower.edtAtk)
        }
        binding.tabPower.edtDef.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) focusAndShowKeyboard(binding.tabPower.edtDef)
        }

        listOf(
            binding.tabPower.btnStarStyle1 to 1,
            binding.tabPower.btnStarStyle2 to 2,
            binding.tabPower.btnStarStyle3 to 3,
            binding.tabPower.btnStarStyle4 to 4,
            binding.tabPower.btnStarStyle5 to 5,
            binding.tabPower.btnStarStyle6 to 6
        ).forEach { (view, style) ->
            view.tap { applyStarStyle(style) }
        }

        applyStarRating(currentStarRating)
        applyStarStyle(currentStarStyle)
        applyAtk(currentAtk)
        applyDef(currentDef)
    }

    private fun setupBackgroundEditors() {
        bgImageAdapter.showAddItem = true
        bgTagAdapter.showAddItem = true
        bgImageAssets = loadAssetItems("assets/bg_image").apply {
            add(0, SelectedModel())
        }
        bgTagAssets = loadAssetItems("assets/bg_tag").apply {
            add(0, SelectedModel())
        }
        currentBgImagePath = bgImageAssets.getOrNull(1)?.path.orEmpty()
        currentBgTagPath = bgTagAssets.getOrNull(1)?.path.orEmpty()

        binding.tabBg.sectionTab.visible()
        binding.tabBg.rcvBackgroundImage.adapter = bgImageAdapter
        binding.tabBg.rcvBackgroundTag.adapter = bgTagAdapter

        bgImageAdapter.onAddImageClick = {
            pickingBgTag = false
            pickBgImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        bgImageAdapter.onBackgroundImageClick = { path, position ->
            selectColor(bgImageAssets, position)
            applyBgImageAsset(path)
            bgImageAdapter.submitItem(position, bgImageAssets)
        }

        bgTagAdapter.onAddImageClick = {
            pickingBgTag = true
            pickBgImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        bgTagAdapter.onBackgroundImageClick = { path, position ->
            selectColor(bgTagAssets, position)
            applyBgTagAsset(path)
            bgTagAdapter.submitItem(position, bgTagAssets)
        }

        if (currentBgImagePath.isNotEmpty()) {
            bgImageAssets.selectItem(1)
            applyBgImageAsset(currentBgImagePath)
        }
        if (currentBgTagPath.isNotEmpty()) {
            bgTagAssets.selectItem(1)
            applyBgTagAsset(currentBgTagPath)
        }

        binding.tabBg.btnBackgroundImage.tap { showBackgroundList(isImageBackground = true) }
        binding.tabBg.btnBackgroundTag.tap { showBackgroundList(isImageBackground = false) }
        showBackgroundList(isImageBackground = true)
    }

    private fun loadAssetItems(folder: String): ArrayList<SelectedModel> {
        return AssetHelper.getSubfoldersAsset(this, folder)
            .map { SelectedModel(path = it) }
            .toCollection(ArrayList())
    }

    private fun addCustomBgImage(path: String) {
        val existingIndex = bgImageAssets.indexOfFirst { it.path == path }
        val position = if (existingIndex >= 0) {
            existingIndex
        } else {
            bgImageAssets.add(1, SelectedModel(path = path))
            1
        }
        selectColor(bgImageAssets, position)
        applyBgImageAsset(path)
        bgImageAdapter.submitItem(position, bgImageAssets)
    }

    private fun addCustomBgTag(path: String) {
        val existingIndex = bgTagAssets.indexOfFirst { it.path == path }
        val position = if (existingIndex >= 0) {
            existingIndex
        } else {
            bgTagAssets.add(1, SelectedModel(path = path))
            1
        }
        selectColor(bgTagAssets, position)
        applyBgTagAsset(path)
        bgTagAdapter.submitItem(position, bgTagAssets)
    }

    private fun showBackgroundList(isImageBackground: Boolean) {
        if (isImageBackground) {
            binding.tabBg.sectionTab.setBackgroundResource(R.drawable.bg_select_tab_edit)
            binding.tabBg.rcvBackgroundImage.visible()
            binding.tabBg.rcvBackgroundTag.gone()
            binding.tabBg.tvBackgroundImage.setTextColor(getColor(R.color.app))
            binding.tabBg.tvBackgroundColor.setTextColor(Color.WHITE)
            bgImageAdapter.submitList(bgImageAssets.map { it.copy() })
        } else {
            binding.tabBg.sectionTab.setBackgroundResource(R.drawable.bg_unselect_tab_edit)
            binding.tabBg.rcvBackgroundImage.gone()
            binding.tabBg.rcvBackgroundTag.visible()
            binding.tabBg.tvBackgroundImage.setTextColor(Color.WHITE)
            binding.tabBg.tvBackgroundColor.setTextColor(getColor(R.color.app))
            bgTagAdapter.submitList(bgTagAssets.map { it.copy() })
        }
    }

    private fun selectColor(list: ArrayList<SelectedModel>, position: Int) {
        list.forEachIndexed { index, item -> item.isSelected = index == position }
    }

    private fun ArrayList<SelectedModel>.selectItem(position: Int) {
        forEachIndexed { index, item -> item.isSelected = index == position }
    }

    private fun ArrayList<SelectedModel>.selectValue(value: Int) {
        val index = indexOfFirst { it.color == value }.takeIf { it >= 0 } ?: return
        selectItem(index)
    }

    private fun ArrayList<SelectedModel>.selectPath(path: String) {
        val index = indexOfFirst { it.path == path }.takeIf { it >= 0 } ?: return
        selectItem(index)
    }

    private fun ArrayList<SelectedModel>.updateCustomColor(color: Int) {
        if (isEmpty()) return
        this[0] = this[0].copy(color = color)
        selectItem(0)
    }

    private fun showChooseColorDialog(onColorSelected: (Int) -> Unit) {
        val dialog = ChooseColorDialog(this)

        fun closeDialog() {
            dialog.dismiss()
            hideNavigation(true)
        }

        dialog.onCloseEvent = { closeDialog() }
        dialog.onDoneEvent = { color ->
            closeDialog()
            onColorSelected(color)
        }
        dialog.show()
    }

    private fun applyNameFont(font: Int) {
        currentNameFont = font
        binding.tvNameTag.setFont(font)
        binding.tabNameTag.edtText.setFont(font)
    }

    private fun applyNameColor(color: Int) {
        currentNameColor = color
        binding.tvNameTag.setTextColor(color)
        binding.tabNameTag.edtText.setTextColor(color)
    }

    private fun applyDescribeFont(font: Int) {
        currentDescribeFont = font
        binding.tvDescrible.setFont(font)
        binding.tabDescribe.edtDescribe.setFont(font)
    }

    private fun applyDescribeColor(color: Int) {
        currentDescribeColor = color
        binding.tvDescrible.setTextColor(color)
        binding.tabDescribe.edtDescribe.setTextColor(color)
    }

    private fun applyBgImageAsset(path: String) {
        currentBgImagePath = path
        Glide.with(this)
            .load(path)
            .into(binding.imvBgImageAsset)
    }

    private fun applyBgTagAsset(path: String) {
        currentBgTagPath = path
        Glide.with(this)
            .load(path)
            .into(binding.imvBgTagAsset)
    }

    private fun applyStarRating(rating: Int) {
        currentStarRating = rating
        binding.rbPreviewStars.rating = rating.toFloat()
        binding.tabPower.btnRatingStar.rating = rating.toFloat()
    }

    private fun applyStarStyle(style: Int) {
        currentStarStyle = style.coerceIn(1, 6)
        val filledRes = starStyleRes(currentStarStyle)
        binding.rbPreviewStars.setFilledDrawableRes(filledRes)
        binding.tabPower.btnRatingStar.setFilledDrawableRes(filledRes)
        updateStarStyleSelection()
        applyStarRating(currentStarRating)
    }

    private fun applyAtk(value: String) {
        currentAtk = value
        binding.tvAtk.text = "ATK/ ${value.ifBlank { "?" }}"
    }

    private fun applyDef(value: String) {
        currentDef = value
        binding.tvDef.text = "DEF/ ${value.ifBlank { "?" }}"
    }

    private fun updateStarStyleSelection() {
        listOf(
            binding.tabPower.btnStarStyle1,
            binding.tabPower.btnStarStyle2,
            binding.tabPower.btnStarStyle3,
            binding.tabPower.btnStarStyle4,
            binding.tabPower.btnStarStyle5,
            binding.tabPower.btnStarStyle6
        ).forEachIndexed { index, imageView ->
            val selected = index + 1 == currentStarStyle
            imageView.setBackgroundResource(if (selected) R.drawable.item_slt_bg else R.drawable.item_uslt_bg)
            imageView.alpha = 1f
            imageView.isSelected = selected
        }
    }

    private fun starStyleRes(style: Int): Int {
        return when (style.coerceIn(1, 6)) {
            1 -> R.drawable.style1_small
            2 -> R.drawable.style2_small
            3 -> R.drawable.style3_small
            4 -> R.drawable.style4_small
            5 -> R.drawable.style5_small
            else -> R.drawable.style6_small
        }
    }

    private fun restoreState() {
        currentSourcePath = intent.getStringExtra(IntentKey.EDIT_SOURCE_PATH).orEmpty()
        currentModelId = intent.getStringExtra(IntentKey.EDIT_MODEL_ID).orEmpty()
        val imagePathFromIntent = intent.getStringExtra(IntentKey.EDIT_IMAGE_PATH).orEmpty()
        val selectionStateFromIntent = intent.getStringExtra(IntentKey.EDIT_SELECTION_STATE).orEmpty()
        currentSelectionState = selectionStateFromIntent
        val savedModel = findDragonCardModel(currentModelId, currentSourcePath)

        if (savedModel != null) {
            currentModelId = savedModel.id
            currentSelectionState = selectionStateFromIntent.ifEmpty { savedModel.selectionState }
            currentDragonImagePath = if (selectionStateFromIntent.isNotEmpty() && imagePathFromIntent.isNotEmpty()) {
                imagePathFromIntent
            } else {
                savedModel.dragonImagePath.ifEmpty { imagePathFromIntent }
            }
            binding.tabNameTag.edtText.setText(savedModel.nameTag)
            binding.tabDescribe.edtDescribe.setText(savedModel.describe)
            restoreNameStyle(savedModel.nameFont, savedModel.nameColor)
            restoreDescribeStyle(savedModel.describeFont, savedModel.describeColor)
            restorePowerStyle(savedModel.starRating, savedModel.starStyle, savedModel.atk, savedModel.def)
            restoreBackgroundStyle(savedModel.bgImagePath, savedModel.bgTagPath)
        } else {
            currentDragonImagePath = imagePathFromIntent
            binding.tvNameTag.text = binding.tabNameTag.edtText.text?.toString().orEmpty()
            binding.tvDescrible.text = binding.tabDescribe.edtDescribe.text?.toString().orEmpty()
        }

        if (currentDragonImagePath.isNotEmpty()) {
            Glide.with(this)
                .load(File(currentDragonImagePath))
                .into(binding.imvImage)
        }
    }

    private fun restorePowerStyle(starRating: Int, starStyle: Int, atk: String, def: String) {
        applyStarRating(starRating.coerceIn(0, 7))
        applyStarStyle(starStyle.coerceIn(1, 6))
        binding.tabPower.edtAtk.setText(atk)
        binding.tabPower.edtDef.setText(def)
        applyAtk(atk)
        applyDef(def)
    }

    private fun restoreNameStyle(font: Int, color: Int) {
        if (font != 0) {
            nameFonts.selectValue(font)
            nameFontAdapter.submitListReset(nameFonts)
            applyNameFont(font)
        }
        if (color != 0) {
            nameColors.selectValue(color)
            nameColorAdapter.submitListReset(nameColors)
            applyNameColor(color)
        }
    }

    private fun restoreDescribeStyle(font: Int, color: Int) {
        if (font != 0) {
            describeFonts.selectValue(font)
            describeFontAdapter.submitListReset(describeFonts)
            applyDescribeFont(font)
        }
        if (color != 0) {
            describeColors.selectValue(color)
            describeColorAdapter.submitListReset(describeColors)
            applyDescribeColor(color)
        }
    }

    private fun restoreBackgroundStyle(bgImagePath: String, bgTagPath: String) {
        if (bgImagePath.isNotEmpty()) {
            if (bgImageAssets.none { it.path == bgImagePath }) {
                bgImageAssets.add(1, SelectedModel(path = bgImagePath))
            }
            bgImageAssets.selectPath(bgImagePath)
            applyBgImageAsset(bgImagePath)
        }
        if (bgTagPath.isNotEmpty()) {
            if (bgTagAssets.none { it.path == bgTagPath }) {
                bgTagAssets.add(1, SelectedModel(path = bgTagPath))
            }
            bgTagAssets.selectPath(bgTagPath)
            applyBgTagAsset(bgTagPath)
        }
        bgImageAdapter.submitList(bgImageAssets.map { it.copy() })
        bgTagAdapter.submitList(bgTagAssets.map { it.copy() })
    }

    private fun findDragonCardModel(modelId: String, previewPath: String): DragonCardEditModel? {
        if (modelId.isEmpty() && previewPath.isEmpty()) return null
        val list = MediaHelper
            .readListFromFile<DragonCardEditModel>(this, ValueKey.DRAGON_CARD_EDIT_FILE_INTERNAL)
        return when {
            modelId.isNotEmpty() -> list.firstOrNull { it.id == modelId }
            else -> list.firstOrNull { it.previewPath == previewPath }
        }
    }

    private fun showTab(tab: EditTab) {
        binding.tabNameTag.lnlText.visibility = if (tab == EditTab.NAME_TAG) View.VISIBLE else View.GONE
        binding.tabPower.lnlPower.visibility = if (tab == EditTab.POWER) View.VISIBLE else View.GONE
        binding.tabBg.lnlBg.visibility = if (tab == EditTab.BG) View.VISIBLE else View.GONE
        binding.tabDescribe.lnlDescribe.visibility = if (tab == EditTab.DESCRIBE) View.VISIBLE else View.GONE

        binding.btnNameTag.isSelected = tab == EditTab.NAME_TAG
        binding.btnPower.isSelected = tab == EditTab.POWER
        binding.btnBg.isSelected = tab == EditTab.BG
        binding.btnDescrible.isSelected = tab == EditTab.DESCRIBE

        updateEditNavigation(tab)
    }

    private fun updateEditNavigation(tab: EditTab) {
        val items = listOf(
            EditTab.NAME_TAG to NavItem(binding.icHead, binding.tvNameTagTab, R.drawable.navi1, R.drawable.ic_nametag),
            EditTab.POWER to NavItem(binding.icTorso, binding.tvPowerTab, R.drawable.navi2, R.drawable.ic_power),
            EditTab.BG to NavItem(binding.icLegs, binding.tvBgTab, R.drawable.navi3, R.drawable.ic_bg),
            EditTab.DESCRIBE to NavItem(binding.icWings, binding.tvDescribeTab, R.drawable.navi4, R.drawable.ic_des)
        )
        items.forEach { (itemTab, item) ->
            val selected = itemTab == tab
            item.icon.setImageResource(if (selected) item.selectedIcon else item.unselectedIcon)
            item.icon.alpha = 1f
            item.icon.scaleX = if (selected) 1.12f else 0.7f
            item.icon.scaleY = if (selected) 1.12f else 0.7f
            item.label.visibility = if (selected) View.GONE else View.VISIBLE
        }
    }

    private data class NavItem(
        val icon: ImageView,
        val label: View,
        val unselectedIcon: Int,
        val selectedIcon: Int
    )

    private fun handleDone() {
        try {
            val path = savePreviewToCreation()
            registerDragonCardModel(path)
            val intent = Intent(this, SuccessActivity::class.java).apply {
                putExtra(IntentKey.INTENT_KEY, path)
            }
            val options = ActivityOptions.makeCustomAnimation(
                this,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            startActivity(intent, options.toBundle())
        } catch (e: Exception) {
            Log.e("EditActivity", "handleDone failed", e)
            Toast.makeText(this, R.string.an_error_occurred, Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePreviewToCreation(): String {
        if (binding.bgTag.width <= 0 || binding.bgTag.height <= 0) {
            throw IllegalStateException("bgTag is not laid out: ${binding.bgTag.width}x${binding.bgTag.height}")
        }
        val bitmap = createBitmapFromView(binding.bgTag)
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
        val dir = File(filesDir, ValueKey.DOWNLOAD_ALBUM).also { it.mkdirs() }
        val file = File(dir, "edited_dragon_$ts.png")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return file.absolutePath
    }

    private fun registerDragonCardModel(previewPath: String) {
        val list = MediaHelper
            .readListFromFile<DragonCardEditModel>(this, ValueKey.DRAGON_CARD_EDIT_FILE_INTERNAL)
            .toCollection(ArrayList())

        val index = when {
            currentModelId.isNotEmpty() -> list.indexOfFirst { it.id == currentModelId }
            currentSourcePath.isNotEmpty() -> list.indexOfFirst { it.previewPath == currentSourcePath }
            else -> -1
        }

        val model = DragonCardEditModel(
            id = if (index >= 0) list[index].id else "dragon_card_${System.currentTimeMillis()}",
            previewPath = previewPath,
            dragonImagePath = persistDragonSource(),
            nameTag = binding.tabNameTag.edtText.text?.toString().orEmpty(),
            nameFont = currentNameFont,
            nameColor = currentNameColor,
            describe = binding.tabDescribe.edtDescribe.text?.toString().orEmpty(),
            describeFont = currentDescribeFont,
            describeColor = currentDescribeColor,
            starRating = currentStarRating,
            starStyle = currentStarStyle,
            atk = currentAtk,
            def = currentDef,
            bgImagePath = currentBgImagePath,
            bgTagPath = currentBgTagPath,
            selectionState = currentSelectionState
        )

        if (index >= 0) {
            list[index] = model
        } else {
            list.add(0, model)
        }
        MediaHelper.writeListToFile(this, ValueKey.DRAGON_CARD_EDIT_FILE_INTERNAL, list)
        currentModelId = model.id
        currentSourcePath = model.previewPath
        currentDragonImagePath = model.dragonImagePath
    }

    private fun persistDragonSource(): String {
        if (currentDragonImagePath.isEmpty()) return ""
        val source = File(currentDragonImagePath)
        if (!source.exists()) return currentDragonImagePath

        val sourceDir = File(filesDir, "dragon_card_sources").also { it.mkdirs() }
        if (source.absolutePath.startsWith(sourceDir.absolutePath)) return source.absolutePath

        val target = File(sourceDir, "dragon_source_${System.currentTimeMillis()}.png")
        source.copyTo(target, overwrite = true)
        currentDragonImagePath = target.absolutePath
        return target.absolutePath
    }

    private enum class EditTab {
        NAME_TAG,
        POWER,
        BG,
        DESCRIBE
    }
}
