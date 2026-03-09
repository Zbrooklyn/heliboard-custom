// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ai

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.Colors

/**
 * Samsung-style AI rewrite panel that replaces the keyboard area.
 * Built entirely programmatically (no XML layouts), themed via [Colors]/[ColorType].
 */
class RewritePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // Style definitions: internal key -> display label
    private data class StyleDef(val key: String, val label: String)

    private val styles = listOf(
        StyleDef("clean", "\u2728 Clean"),
        StyleDef("professional", "\uD83D\uDCBC Professional"),
        StyleDef("casual", "\uD83D\uDCAC Casual"),
        StyleDef("concise", "\uD83D\uDCDD Concise"),
        StyleDef("emojify", "\uD83D\uDE04 Emojify")
    )

    // Header
    private val titleText: TextView
    private val closeButton: TextView

    // Original text card
    private val originalLabel: TextView
    private val originalTextView: TextView
    private val originalCardContainer: LinearLayout

    // Style tabs
    private val styleTabsRow: LinearLayout
    private val styleChips = mutableMapOf<String, TextView>()
    private var selectedStyle: String = "clean"

    // Content area
    private val contentArea: FrameLayout
    private val loadingView: LinearLayout
    private val loadingSpinner: ProgressBar
    private val loadingText: TextView
    private val resultsScroller: ScrollView
    private val resultText: TextView

    // Bottom bar
    private val cancelButton: TextView
    private val undoButton: TextView
    private val applyButton: TextView

    // Listeners
    private var onCloseListener: (() -> Unit)? = null
    private var onApplyListener: ((String) -> Unit)? = null
    private var onUndoListener: (() -> Unit)? = null
    private var onStyleSelectedListener: ((String) -> Unit)? = null

    // Cached colors for chip updates
    private var cachedAccentColor: Int = Color.DKGRAY
    private var cachedTextColor: Int = Color.WHITE
    private var cachedBgColor: Int = Color.BLACK
    private var cachedCardColor: Int = 0x20808080

    private val density = resources.displayMetrics.density

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        val pad = (8 * density).toInt()
        setPadding(pad, pad, pad, pad)

        // ── Header Row ──────────────────────────────────────────────
        val headerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (6 * density).toInt()
            }
        }

        titleText = TextView(context).apply {
            text = "\u2728 AI Rewrite"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
        headerRow.addView(titleText)

        // Spacer
        headerRow.addView(View(context).apply {
            layoutParams = LayoutParams(0, 0, 1f)
        })

        closeButton = TextView(context).apply {
            text = "\u2715"
            textSize = 18f
            gravity = Gravity.CENTER
            val btnSize = (36 * density).toInt()
            layoutParams = LayoutParams(btnSize, btnSize)
            setOnClickListener { onCloseListener?.invoke() }
            contentDescription = "Close rewrite panel"
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        headerRow.addView(closeButton)

        addView(headerRow)

        // ── Original Text Card ──────────────────────────────────────
        originalCardContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            val cardPad = (10 * density).toInt()
            setPadding(cardPad, cardPad, cardPad, cardPad)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * density).toInt()
            }
        }

        originalLabel = TextView(context).apply {
            text = "Original:"
            textSize = 11f
            alpha = 0.6f
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (4 * density).toInt()
            }
        }
        originalCardContainer.addView(originalLabel)

        val originalScroller = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                // Max height ~4 lines at 14sp
                height = (4 * 20 * density).toInt()
            }
            isVerticalScrollBarEnabled = true
        }
        originalTextView = TextView(context).apply {
            textSize = 14f
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        originalScroller.addView(originalTextView)
        originalCardContainer.addView(originalScroller)

        addView(originalCardContainer)

        // ── Style Tabs ──────────────────────────────────────────────
        val styleTabsScroller = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * density).toInt()
            }
        }

        styleTabsRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        for (style in styles) {
            val chip = createStyleChip(style.key, style.label)
            styleChips[style.key] = chip
            styleTabsRow.addView(chip)
        }

        styleTabsScroller.addView(styleTabsRow)
        addView(styleTabsScroller)

        // ── Content Area (FrameLayout: loading / results) ───────────
        contentArea = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }

        // Loading view
        loadingView = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        loadingSpinner = ProgressBar(context).apply {
            isIndeterminate = true
            layoutParams = LayoutParams(
                (36 * density).toInt(),
                (36 * density).toInt()
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (8 * density).toInt()
            }
        }
        loadingView.addView(loadingSpinner)

        loadingText = TextView(context).apply {
            text = "Rewriting..."
            textSize = 13f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        loadingView.addView(loadingText)

        contentArea.addView(loadingView)

        // Results scroller
        resultsScroller = ScrollView(context).apply {
            visibility = View.GONE
            isVerticalScrollBarEnabled = true
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        val resultPad = (12 * density).toInt()
        resultText = TextView(context).apply {
            textSize = 15f
            setPadding(resultPad, resultPad, resultPad, resultPad)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        resultsScroller.addView(resultText)

        contentArea.addView(resultsScroller)

        addView(contentArea)

        // ── Bottom Bar ──────────────────────────────────────────────
        val bottomBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = (8 * density).toInt()
            }
        }

        cancelButton = createPillButton("Cancel", filled = false).apply {
            setOnClickListener { onCloseListener?.invoke() }
        }
        bottomBar.addView(cancelButton)

        undoButton = createPillButton("\u21A9 Undo", filled = false).apply {
            visibility = View.GONE
            setOnClickListener { onUndoListener?.invoke() }
        }
        bottomBar.addView(undoButton)

        applyButton = createPillButton("Apply", filled = true).apply {
            setOnClickListener {
                val text = resultText.text?.toString() ?: ""
                onApplyListener?.invoke(text)
            }
        }
        bottomBar.addView(applyButton)

        addView(bottomBar)

        // Default selection
        updateChipSelection()
    }

    // ── View Creation Helpers ───────────────────────────────────────

    private fun createStyleChip(styleKey: String, label: String): TextView {
        val chipH = (32 * density).toInt()
        val chipPadH = (14 * density).toInt()
        val chipMargin = (4 * density).toInt()

        return TextView(context).apply {
            text = label
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(chipPadH, 0, chipPadH, 0)
            minHeight = chipH
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, chipH).apply {
                marginStart = chipMargin
                marginEnd = chipMargin
            }
            setOnClickListener {
                selectedStyle = styleKey
                updateChipSelection()
                onStyleSelectedListener?.invoke(styleKey)
            }
            contentDescription = "Style: $label"
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
    }

    private fun createPillButton(label: String, filled: Boolean): TextView {
        val btnH = (38 * density).toInt()
        val btnPadH = (20 * density).toInt()
        val btnMargin = (6 * density).toInt()

        return TextView(context).apply {
            text = label
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(btnPadH, 0, btnPadH, 0)
            minHeight = btnH
            maxLines = 1
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, btnH).apply {
                marginStart = btnMargin
                marginEnd = btnMargin
            }
            // Background will be set in applyTheme
            tag = if (filled) "filled" else "outlined"
        }
    }

    // ── Theme Application ───────────────────────────────────────────

    fun applyTheme(colors: Colors) {
        val bgColor = colors.get(ColorType.MAIN_BACKGROUND)
        val textColor = colors.get(ColorType.KEY_TEXT)
        val accentColor = colors.get(ColorType.KEY_BACKGROUND)
        val stripColor = colors.get(ColorType.STRIP_BACKGROUND)

        cachedBgColor = bgColor
        cachedTextColor = textColor
        cachedAccentColor = accentColor

        // Determine a subtle card color: slightly different from main background
        cachedCardColor = blendColor(bgColor, if (isDark(bgColor)) 0x20FFFFFF else 0x18000000)

        setBackgroundColor(bgColor)

        // Header
        titleText.setTextColor(textColor)
        closeButton.setTextColor(textColor)

        // Original text card
        originalLabel.setTextColor(textColor)
        originalTextView.setTextColor(textColor)
        val cardBg = GradientDrawable().apply {
            setColor(cachedCardColor)
            cornerRadius = 12 * density
        }
        originalCardContainer.background = cardBg

        // Loading
        loadingText.setTextColor(textColor)

        // Result
        resultText.setTextColor(textColor)

        // Style chips
        updateChipSelection()

        // Bottom buttons
        applyButtonTheme(cancelButton, textColor, accentColor, bgColor)
        applyButtonTheme(undoButton, textColor, accentColor, bgColor)
        applyButtonTheme(applyButton, textColor, accentColor, bgColor)
    }

    private fun applyButtonTheme(button: TextView, textColor: Int, accentColor: Int, bgColor: Int) {
        val isFilled = button.tag == "filled"
        val cornerRadius = 19 * density

        if (isFilled) {
            val bg = GradientDrawable().apply {
                setColor(accentColor)
                this.cornerRadius = cornerRadius
            }
            button.background = bg
            // Text on accent background: pick contrasting color
            button.setTextColor(if (isDark(accentColor)) Color.WHITE else Color.BLACK)
        } else {
            val bg = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke((1.5f * density).toInt(), textColor)
                this.cornerRadius = cornerRadius
            }
            button.background = bg
            button.setTextColor(textColor)
        }
    }

    private fun updateChipSelection() {
        for ((key, chip) in styleChips) {
            val isSelected = key == selectedStyle
            val cornerRadius = 16 * density

            if (isSelected) {
                val bg = GradientDrawable().apply {
                    setColor(cachedAccentColor)
                    this.cornerRadius = cornerRadius
                }
                chip.background = bg
                chip.setTextColor(if (isDark(cachedAccentColor)) Color.WHITE else Color.BLACK)
                chip.typeface = Typeface.DEFAULT_BOLD
            } else {
                val bg = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    // Semi-transparent outline: keep RGB from text color, set alpha to ~25%
                    val outlineColor = (cachedTextColor and 0x00FFFFFF) or 0x40000000
                    setStroke((1 * density).toInt(), outlineColor)
                    this.cornerRadius = cornerRadius
                }
                chip.background = bg
                chip.setTextColor(cachedTextColor)
                chip.typeface = Typeface.DEFAULT
            }
        }
    }

    // ── Public API ──────────────────────────────────────────────────

    fun setOriginalText(text: String) {
        originalTextView.text = text
    }

    fun showLoading(providerName: String) {
        loadingText.text = "Rewriting with $providerName..."
        loadingView.visibility = View.VISIBLE
        resultsScroller.visibility = View.GONE
    }

    fun showResult(style: String, text: String) {
        resultText.text = text
        loadingView.visibility = View.GONE
        resultsScroller.visibility = View.VISIBLE
        // Auto-select the returned style tab
        selectedStyle = style
        updateChipSelection()
    }

    fun showError(message: String) {
        resultText.text = message
        resultText.setTextColor(Color.RED)
        loadingView.visibility = View.GONE
        resultsScroller.visibility = View.VISIBLE
    }

    fun setSelectedStyle(style: String) {
        selectedStyle = style
        updateChipSelection()
    }

    fun setOnCloseListener(listener: () -> Unit) {
        onCloseListener = listener
    }

    fun setOnApplyListener(listener: (String) -> Unit) {
        onApplyListener = listener
    }

    fun setOnUndoListener(listener: () -> Unit) {
        onUndoListener = listener
    }

    fun setOnStyleSelectedListener(listener: (String) -> Unit) {
        onStyleSelectedListener = listener
    }

    fun showUndo() {
        undoButton.visibility = View.VISIBLE
    }

    // ── Utility ─────────────────────────────────────────────────────

    /** Simple luminance check: true if the color is dark. */
    private fun isDark(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        // Perceived luminance
        return (0.299 * r + 0.587 * g + 0.114 * b) < 128
    }

    /** Blend a base color with an overlay (respecting overlay alpha). */
    private fun blendColor(base: Int, overlay: Int): Int {
        val a = Color.alpha(overlay) / 255f
        val r = ((1 - a) * Color.red(base) + a * Color.red(overlay)).toInt()
        val g = ((1 - a) * Color.green(base) + a * Color.green(overlay)).toInt()
        val b = ((1 - a) * Color.blue(base) + a * Color.blue(overlay)).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }
}
