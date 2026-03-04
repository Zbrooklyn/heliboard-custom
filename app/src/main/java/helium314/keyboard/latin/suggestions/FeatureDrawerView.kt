// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import helium314.keyboard.keyboard.internal.KeyboardIconsSet
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.Colors
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.ToolbarKey
import helium314.keyboard.latin.utils.getCodeForToolbarKey

/**
 * Samsung-style 4x3 feature drawer grid that appears when "..." overflow is tapped.
 * Each cell has an icon + label. Tapping a cell dispatches the corresponding KeyCode.
 */
class FeatureDrawerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    /** Items shown in the 4x3 grid — order matches Samsung keyboard layout */
    data class DrawerItem(val key: ToolbarKey, val labelRes: Int, val code: Int)

    private var codeInputListener: ((Int) -> Unit)? = null
    private var onBackClick: (() -> Unit)? = null

    private val gridLayout = GridLayout(context).apply {
        columnCount = 4
        rowCount = 3
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private val backButton: TextView

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        addView(gridLayout)

        // "← Keyboard" back button at the bottom
        val density = resources.displayMetrics.density
        backButton = TextView(context).apply {
            text = "← Keyboard"
            textSize = 14f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = (8 * density).toInt()
                bottomMargin = (8 * density).toInt()
            }
            setPadding(0, (10 * density).toInt(), 0, (10 * density).toInt())
            setOnClickListener { onBackClick?.invoke() }
        }
        addView(backButton)
    }

    fun setOnBackClickListener(listener: () -> Unit) {
        onBackClick = listener
    }

    fun setCodeInputListener(listener: (Int) -> Unit) {
        codeInputListener = listener
    }

    fun populateGrid(colors: Colors) {
        gridLayout.removeAllViews()

        val items = getDrawerItems()
        val density = resources.displayMetrics.density
        val circleColor = colors.get(ColorType.KEY_BACKGROUND)
        val textColor = colors.get(ColorType.KEY_TEXT)
        val iconColor = colors.get(ColorType.TOOL_BAR_KEY)
        backButton.setTextColor(textColor)

        for ((index, item) in items.withIndex()) {
            val cell = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                val cellParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(index % 4, 1f)
                    rowSpec = GridLayout.spec(index / 4)
                    setMargins(
                        (4 * density).toInt(),
                        (8 * density).toInt(),
                        (4 * density).toInt(),
                        (8 * density).toInt()
                    )
                }
                layoutParams = cellParams
            }

            // Circular icon button
            val iconSize = (40 * density).toInt()
            val button = ImageButton(context).apply {
                scaleType = ImageView.ScaleType.CENTER
                val circle = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(circleColor)
                }
                background = circle
                layoutParams = LayoutParams(iconSize, iconSize).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                setImageDrawable(KeyboardIconsSet.instance.getNewDrawable(item.key.name, context))
                setColorFilter(iconColor)
                contentDescription = context.getString(item.labelRes)
                setOnClickListener { codeInputListener?.invoke(item.code) }
            }
            cell.addView(button)

            // Label below icon
            val label = TextView(context).apply {
                text = context.getString(item.labelRes)
                textSize = 10f
                setTextColor(textColor)
                gravity = Gravity.CENTER
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (3 * density).toInt()
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                maxLines = 1
            }
            cell.addView(label)

            gridLayout.addView(cell)
        }
    }

    companion object {
        /** Default 4x3 grid items. Row 1: core features, Row 2: layout, Row 3: modes */
        fun getDrawerItems(): List<DrawerItem> = listOf(
            // Row 1: Core features
            DrawerItem(ToolbarKey.VOICE, R.string.voice, KeyCode.VOICE_INPUT),
            DrawerItem(ToolbarKey.REWRITE, R.string.rewrite, KeyCode.REWRITE),
            DrawerItem(ToolbarKey.CLIPBOARD, R.string.clipboard, KeyCode.CLIPBOARD),
            DrawerItem(ToolbarKey.SETTINGS, R.string.settings_screen_preferences, KeyCode.SETTINGS),
            // Row 2: Layout & navigation
            DrawerItem(ToolbarKey.NUMPAD, R.string.numpad, KeyCode.NUMPAD),
            DrawerItem(ToolbarKey.ONE_HANDED, R.string.one_handed, KeyCode.TOGGLE_ONE_HANDED_MODE),
            DrawerItem(ToolbarKey.EMOJI, R.string.emoji, KeyCode.EMOJI),
            DrawerItem(ToolbarKey.SPLIT, R.string.enable_split_keyboard, KeyCode.SPLIT_LAYOUT),
            // Row 3: Modes
            DrawerItem(ToolbarKey.INCOGNITO, R.string.incognito, KeyCode.TOGGLE_INCOGNITO_MODE),
            DrawerItem(ToolbarKey.AUTOCORRECT, R.string.autocorrect, KeyCode.TOGGLE_AUTOCORRECT),
            DrawerItem(ToolbarKey.SELECT_ALL, R.string.select_all, KeyCode.CLIPBOARD_SELECT_ALL),
            DrawerItem(ToolbarKey.ONE_HANDED, R.string.resize_keyboard, KeyCode.TOGGLE_RESIZE_KEYBOARD),
        )
    }
}
