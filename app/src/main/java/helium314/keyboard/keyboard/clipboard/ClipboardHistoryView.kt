// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.keyboard.clipboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import helium314.keyboard.event.HapticEvent
import helium314.keyboard.keyboard.KeyboardActionListener
import helium314.keyboard.keyboard.KeyboardId
import helium314.keyboard.keyboard.KeyboardLayoutSet
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.MainKeyboardView
import helium314.keyboard.keyboard.PointerTracker
import helium314.keyboard.keyboard.internal.KeyDrawParams
import helium314.keyboard.keyboard.internal.KeyVisualAttributes
import helium314.keyboard.keyboard.internal.KeyboardIconsSet
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.AudioAndHapticFeedbackManager
import helium314.keyboard.latin.ClipboardHistoryManager
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.Colors
import helium314.keyboard.latin.common.Constants
import helium314.keyboard.latin.database.ClipboardDao
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.ResourceUtils
import helium314.keyboard.latin.utils.ToolbarKey
import helium314.keyboard.latin.utils.setToolbarButtonsActivatedStateOnPrefChange

@SuppressLint("CustomViewStyleable")
class ClipboardHistoryView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int = R.attr.clipboardHistoryViewStyle
) : LinearLayout(context, attrs, defStyle), View.OnClickListener,
    ClipboardDao.Listener, OnKeyEventListener,
    View.OnLongClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private val clipboardLayoutParams = ClipboardLayoutParams(context)
    private val pinIconId: Int
    private val keyBackgroundId: Int

    private lateinit var clipboardRecyclerView: ClipboardHistoryRecyclerView
    private lateinit var placeholderView: TextView
    private lateinit var clipboardAdapter: ClipboardAdapter

    lateinit var keyboardActionListener: KeyboardActionListener
    private lateinit var clipboardHistoryManager: ClipboardHistoryManager

    // Samsung-style toolbar buttons
    private lateinit var backButton: ImageButton
    private lateinit var selectionCountLabel: TextView
    private lateinit var selectAllButton: ImageButton
    private lateinit var pinButton: ImageButton
    private lateinit var deleteButton: ImageButton

    // Selection state — selectedIds lives on the adapter as the single source of truth
    private var isSelectionMode = false

    init {
        val clipboardViewAttr = context.obtainStyledAttributes(attrs,
                R.styleable.ClipboardHistoryView, defStyle, R.style.ClipboardHistoryView)
        pinIconId = clipboardViewAttr.getResourceId(R.styleable.ClipboardHistoryView_iconPinnedClip, 0)
        clipboardViewAttr.recycle()
        @SuppressLint("UseKtx") // suggestion does not work
        val keyboardViewAttr = context.obtainStyledAttributes(attrs, R.styleable.KeyboardView, defStyle, R.style.KeyboardView)
        keyBackgroundId = keyboardViewAttr.getResourceId(R.styleable.KeyboardView_keyBackground, 0)
        keyboardViewAttr.recycle()
        fitsSystemWindows = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val res = context.resources
        val width = ResourceUtils.getKeyboardWidth(context, Settings.getValues()) + paddingLeft + paddingRight
        val height = ResourceUtils.getSecondaryKeyboardHeight(res, Settings.getValues()) + paddingTop + paddingBottom
        setMeasuredDimension(width, height)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initialize() {
        if (this::clipboardAdapter.isInitialized) return
        val colors = Settings.getValues().mColors
        clipboardAdapter = ClipboardAdapter(clipboardLayoutParams, this).apply {
            itemBackgroundId = keyBackgroundId
            pinnedIconResId = pinIconId
        }
        placeholderView = findViewById(R.id.clipboard_empty_view)
        clipboardRecyclerView = findViewById<ClipboardHistoryRecyclerView>(R.id.clipboard_list).apply {
            val colCount = resources.getInteger(R.integer.config_clipboard_keyboard_col_count)
            layoutManager = GridLayoutManager(context, colCount)
            @Suppress("deprecation")
            persistentDrawingCache = PERSISTENT_NO_CACHE
            clipboardLayoutParams.setListProperties(this)
            placeholderView = this@ClipboardHistoryView.placeholderView
        }

        setupToolbarButtons(colors)
    }

    private fun setupToolbarButtons(colors: Colors) {
        val clipboardStrip = KeyboardSwitcher.getInstance().clipboardStrip
        val density = resources.displayMetrics.density
        val pillColor = colors.get(ColorType.KEY_BACKGROUND)

        // Create toolbar buttons
        backButton = createStyledButton(ToolbarKey.CLOSE_HISTORY)
        selectAllButton = createStyledButton(ToolbarKey.SELECT_ALL)
        pinButton = createPinButton()
        deleteButton = createStyledButton(ToolbarKey.CLEAR_CLIPBOARD)

        // Set back button to arrow_back initially
        ContextCompat.getDrawable(context, R.drawable.ic_arrow_back)?.mutate()?.let {
            backButton.setImageDrawable(it)
        }

        // Accessibility content descriptions
        backButton.contentDescription = context.getString(android.R.string.cancel)
        selectAllButton.contentDescription = context.getString(android.R.string.selectAll)
        pinButton.contentDescription = context.getString(R.string.clipboard_history_pinned_first)
        deleteButton.contentDescription = context.getString(R.string.clear_clipboard)

        // Selection count label (hidden until selection mode)
        selectionCountLabel = TextView(context).apply {
            textSize = 13f
            setTextColor(colors.get(ColorType.KEY_TEXT))
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
        }

        // Add to strip: [Back] [count] [---spacer---] [SelectAll] [Pin] [Delete]
        val spacer = View(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
        }

        clipboardStrip.addView(backButton)
        clipboardStrip.addView(selectionCountLabel)
        clipboardStrip.addView(spacer)
        clipboardStrip.addView(selectAllButton)
        clipboardStrip.addView(pinButton)
        clipboardStrip.addView(deleteButton)

        // Apply pill background and color to all buttons
        val buttons = listOf(backButton, selectAllButton, pinButton, deleteButton)
        buttons.forEach { btn ->
            colors.setColor(btn, ColorType.TOOL_BAR_KEY)
            val pill = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100f * density
                setColor(pillColor)
            }
            btn.background = pill
        }

        // Wire click handlers
        backButton.setOnClickListener { onBackClicked() }
        selectAllButton.setOnClickListener { onSelectAllClicked() }
        pinButton.setOnClickListener { onPinClicked() }
        deleteButton.setOnClickListener { onDeleteClicked() }
    }

    private fun createStyledButton(key: ToolbarKey): ImageButton {
        val button = ImageButton(context, null, R.attr.suggestionWordStyle)
        button.scaleType = android.widget.ImageView.ScaleType.CENTER
        button.tag = key
        button.setImageDrawable(KeyboardIconsSet.instance.getNewDrawable(key.name, context))
        return button
    }

    private fun createPinButton(): ImageButton {
        val button = ImageButton(context, null, R.attr.suggestionWordStyle)
        button.scaleType = android.widget.ImageView.ScaleType.CENTER
        button.tag = "pin_action"
        if (pinIconId != 0) {
            ContextCompat.getDrawable(context, pinIconId)?.mutate()?.let {
                button.setImageDrawable(it)
            }
        }
        return button
    }

    private fun setupToolbarLayout() {
        val density = resources.displayMetrics.density
        val hMargin = (3 * density).toInt()
        val vMargin = (4 * density).toInt()
        val buttonSize = (40 * density).toInt()

        // Back button: fixed size, no weight
        backButton.layoutParams = LayoutParams(buttonSize, LayoutParams.MATCH_PARENT).apply {
            setMargins(hMargin, vMargin, hMargin, vMargin)
        }

        // Selection count label: wrap content, sits next to back button
        selectionCountLabel.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT).apply {
            setMargins(0, vMargin, hMargin, vMargin)
        }

        // Right-side buttons: fixed size, no weight
        listOf(selectAllButton, pinButton, deleteButton).forEach {
            it.layoutParams = LayoutParams(buttonSize, LayoutParams.MATCH_PARENT).apply {
                setMargins(hMargin, vMargin, hMargin, vMargin)
            }
        }
    }

    // --- Action handlers ---

    private fun onBackClicked() {
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(KeyCode.NOT_SPECIFIED, this, HapticEvent.KEY_PRESS)
        if (isSelectionMode) {
            exitSelectionMode()
        } else {
            // Back to keyboard
            keyboardActionListener.onCodeInput(KeyCode.ALPHA, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
        }
    }

    private fun onSelectAllClicked() {
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(KeyCode.NOT_SPECIFIED, this, HapticEvent.KEY_PRESS)
        if (!isSelectionMode) {
            enterSelectionMode()
        }
        val ids = clipboardAdapter.selectedIds
        // Toggle: if all selected → deselect all, otherwise select all
        val totalCount = clipboardAdapter.itemCount
        val allSelected = ids.size == totalCount && totalCount > 0
        ids.clear()
        if (!allSelected) {
            for (i in 0 until totalCount) {
                clipboardHistoryManager.getHistoryEntry(i)?.let { ids.add(it.id) }
            }
        }
        clipboardAdapter.notifyDataSetChanged()
        updateSelectionCount()
    }

    private fun onPinClicked() {
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(KeyCode.NOT_SPECIFIED, this, HapticEvent.KEY_PRESS)
        if (!isSelectionMode) {
            enterSelectionMode()
            return
        }
        val ids = clipboardAdapter.selectedIds
        if (ids.isEmpty()) return

        // Determine pin action: pin if majority unpinned, unpin otherwise
        var pinnedCount = 0
        for (id in ids) {
            val entry = clipboardHistoryManager.getHistoryEntryById(id)
            if (entry?.isPinned == true) pinnedCount++
        }
        val shouldPin = pinnedCount <= ids.size / 2
        clipboardHistoryManager.bulkPin(ids.toSet(), shouldPin)
        exitSelectionMode()
    }

    private fun onDeleteClicked() {
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(KeyCode.NOT_SPECIFIED, this, HapticEvent.KEY_PRESS)
        if (!isSelectionMode) {
            enterSelectionMode()
            return
        }

        val ids = clipboardAdapter.selectedIds
        if (ids.isEmpty()) {
            // Nothing selected → offer to delete all non-pinned
            showConfirmBanner("Delete all non-pinned?") {
                clipboardHistoryManager.clearHistory()
                exitSelectionMode()
            }
            return
        }

        // Items selected → confirm then delete
        showConfirmBanner("Delete ${ids.size} item(s)?") {
            clipboardHistoryManager.bulkDelete(ids.toSet())
            exitSelectionMode()
        }
    }

    private fun enterSelectionMode() {
        isSelectionMode = true
        clipboardAdapter.isSelectionMode = true
        clipboardAdapter.selectedIds.clear()
        clipboardAdapter.notifyDataSetChanged()

        // Show count label
        selectionCountLabel.visibility = View.VISIBLE
        updateSelectionCount()

        // Change back icon to ✕ and re-tint
        KeyboardIconsSet.instance.getNewDrawable(ToolbarKey.CLOSE_HISTORY.name, context)?.let {
            backButton.setImageDrawable(it)
            Settings.getValues().mColors.setColor(backButton, ColorType.TOOL_BAR_KEY)
        }
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        clipboardAdapter.isSelectionMode = false
        clipboardAdapter.selectedIds.clear()
        clipboardAdapter.notifyDataSetChanged()
        dismissConfirmBanner()

        // Hide count label
        selectionCountLabel.visibility = View.GONE

        // Change back icon to ← and re-tint
        ContextCompat.getDrawable(context, R.drawable.ic_arrow_back)?.mutate()?.let {
            backButton.setImageDrawable(it)
            Settings.getValues().mColors.setColor(backButton, ColorType.TOOL_BAR_KEY)
        }
    }

    private fun updateSelectionCount() {
        val count = clipboardAdapter.selectedIds.size
        selectionCountLabel.text = if (count > 0) "$count selected" else "0 selected"
    }

    // --- Inline confirmation banner ---

    private var confirmBannerContainer: View? = null

    private fun showConfirmBanner(message: String, onConfirm: () -> Unit) {
        dismissConfirmBanner()
        val colors = Settings.getValues().mColors
        val density = resources.displayMetrics.density
        val pad = (12 * density).toInt()

        val container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(pad, (6 * density).toInt(), pad, (6 * density).toInt())
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * density
                setColor(colors.get(ColorType.KEY_BACKGROUND))
            }
            background = bg
        }

        val label = TextView(context).apply {
            text = message
            setTextColor(colors.get(ColorType.KEY_TEXT))
            textSize = 13f
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val confirmBtn = TextView(context).apply {
            text = "Confirm"
            setTextColor(colors.get(ColorType.KEY_TEXT))
            textSize = 13f
            setPadding((12 * density).toInt(), (4 * density).toInt(), (12 * density).toInt(), (4 * density).toInt())
            val pill = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100f * density
                setColor(colors.get(ColorType.TOOL_BAR_KEY_ENABLED_BACKGROUND))
            }
            background = pill
            setOnClickListener { onConfirm() }
        }

        val cancelBtn = TextView(context).apply {
            text = "Cancel"
            setTextColor(colors.get(ColorType.KEY_TEXT))
            textSize = 13f
            setPadding((12 * density).toInt(), (4 * density).toInt(), (12 * density).toInt(), (4 * density).toInt())
            setOnClickListener { dismissConfirmBanner() }
        }

        container.addView(label)
        container.addView(cancelBtn)
        container.addView(confirmBtn)

        confirmBannerContainer = container
        addView(container, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun dismissConfirmBanner() {
        confirmBannerContainer?.let {
            removeView(it)
            confirmBannerContainer = null
        }
    }

    // --- Existing setup methods ---

    private fun setupClipKey(params: KeyDrawParams) {
        clipboardAdapter.apply {
            itemBackgroundId = keyBackgroundId
            itemTypeFace = params.mTypeface
            itemTextColor = params.mTextColor
            itemTextSize = params.mLabelSize.toFloat()
        }
    }

    private fun setupBottomRowKeyboard(editorInfo: EditorInfo, listener: KeyboardActionListener) {
        val keyboardView = findViewById<MainKeyboardView>(R.id.bottom_row_keyboard)
        keyboardView.setKeyboardActionListener(listener)
        PointerTracker.switchTo(keyboardView)
        val kls = KeyboardLayoutSet.Builder.buildEmojiClipBottomRow(context, editorInfo)
        val keyboard = kls.getKeyboard(KeyboardId.ELEMENT_CLIPBOARD_BOTTOM_ROW)
        keyboardView.setKeyboard(keyboard)
    }

    fun setHardwareAcceleratedDrawingEnabled(enabled: Boolean) {
        if (!enabled) return
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun startClipboardHistory(
            historyManager: ClipboardHistoryManager,
            keyVisualAttr: KeyVisualAttributes?,
            editorInfo: EditorInfo,
            keyboardActionListener: KeyboardActionListener
    ) {
        clipboardHistoryManager = historyManager
        initialize()
        setupToolbarLayout()
        historyManager.prepareClipboardHistory()
        historyManager.setHistoryChangeListener(this)
        clipboardAdapter.clipboardHistoryManager = historyManager

        val params = KeyDrawParams()
        params.updateParams(clipboardLayoutParams.bottomRowKeyboardHeight, keyVisualAttr)
        val settings = Settings.getInstance()
        settings.getCustomTypeface()?.let { params.mTypeface = it }
        setupClipKey(params)
        setupBottomRowKeyboard(editorInfo, keyboardActionListener)

        placeholderView.apply {
            typeface = params.mTypeface
            setTextColor(params.mTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, params.mLabelSize.toFloat() * 2)
        }
        clipboardRecyclerView.apply {
            adapter = clipboardAdapter
            val keyboardWidth = ResourceUtils.getKeyboardWidth(context, settings.current)
            layoutParams.width = keyboardWidth

            val keyboardAttr = context.obtainStyledAttributes(
                null, R.styleable.Keyboard, R.attr.keyboardStyle, R.style.Keyboard)
            val leftPadding = (keyboardAttr.getFraction(R.styleable.Keyboard_keyboardLeftPadding,
                keyboardWidth, keyboardWidth, 0f)
                    * settings.current.mSidePaddingScale).toInt()
            val rightPadding =  (keyboardAttr.getFraction(R.styleable.Keyboard_keyboardRightPadding,
                keyboardWidth, keyboardWidth, 0f)
                    * settings.current.mSidePaddingScale).toInt()
            keyboardAttr.recycle()
            setPadding(leftPadding, paddingTop, rightPadding, paddingBottom)
        }

        // Ensure buttons reflect correct color state
        listOf(backButton, selectAllButton, pinButton, deleteButton).forEach {
            it.isEnabled = false; it.isEnabled = true
        }
    }

    fun stopClipboardHistory() {
        if (!this::clipboardAdapter.isInitialized) return
        exitSelectionMode()
        clipboardRecyclerView.adapter = null
        clipboardHistoryManager.setHistoryChangeListener(null)
        clipboardAdapter.clipboardHistoryManager = null
    }

    // --- Click handlers (unused now but kept for interface) ---

    override fun onClick(view: View) {
        // Toolbar buttons are wired directly via setOnClickListener, not through this
    }

    override fun onLongClick(view: View): Boolean {
        return false
    }

    // --- Clipboard key event listener ---

    override fun onKeyDown(clipId: Long) {
        keyboardActionListener.onPressKey(KeyCode.NOT_SPECIFIED, 0, true, HapticEvent.KEY_PRESS)
    }

    override fun onKeyUp(clipId: Long) {
        val clipContent = clipboardHistoryManager.getHistoryEntryContent(clipId)
        keyboardActionListener.onTextInput(clipContent?.text)
        keyboardActionListener.onReleaseKey(KeyCode.NOT_SPECIFIED, false)
        if (Settings.getValues().mAlphaAfterClipHistoryEntry)
            keyboardActionListener.onCodeInput(KeyCode.ALPHA, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
    }

    override fun onLongPressSelect(clipId: Long) {
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(KeyCode.NOT_SPECIFIED, this, HapticEvent.KEY_LONG_PRESS)
        if (!isSelectionMode) {
            enterSelectionMode()
        }
        clipboardAdapter.selectedIds.add(clipId)
        clipboardAdapter.notifyDataSetChanged()
        updateSelectionCount()
    }

    override fun onSelectionChanged() {
        updateSelectionCount()
    }

    // --- ClipboardDao.Listener ---

    override fun onClipInserted(position: Int) {
        clipboardAdapter.notifyItemInserted(position)
        clipboardRecyclerView.smoothScrollToPosition(position)
    }

    override fun onClipsRemoved(position: Int, count: Int) {
        clipboardAdapter.notifyItemRangeRemoved(position, count)
    }

    override fun onClipMoved(oldPosition: Int, newPosition: Int) {
        clipboardAdapter.notifyItemMoved(oldPosition, newPosition)
        clipboardAdapter.notifyItemChanged(newPosition)
        if (newPosition < oldPosition) clipboardRecyclerView.smoothScrollToPosition(newPosition)
    }

    override fun onDataSetChanged() {
        clipboardAdapter.notifyDataSetChanged()
    }

    // --- SharedPreferences listener ---

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        setToolbarButtonsActivatedStateOnPrefChange(KeyboardSwitcher.getInstance().clipboardStrip, key)

        if (::clipboardHistoryManager.isInitialized && key == Settings.PREF_CLIPBOARD_HISTORY_PINNED_FIRST) {
            Settings.getInstance().onSharedPreferenceChanged(prefs, key)
            clipboardHistoryManager.sortHistoryEntries()
            clipboardAdapter.notifyDataSetChanged()
        }
    }
}
