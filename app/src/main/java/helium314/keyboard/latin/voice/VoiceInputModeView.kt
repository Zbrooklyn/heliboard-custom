// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import helium314.keyboard.keyboard.internal.KeyboardIconsSet
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.Colors
import helium314.keyboard.latin.settings.Settings

/**
 * Minimal voice keyboard mode — large mic button, status text, and a back button.
 * Replaces the full QWERTY keyboard for voice-first input.
 */
class VoiceInputModeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val statusText: TextView
    private val micButton: ImageButton
    private val backButton: TextView

    private var onMicClick: (() -> Unit)? = null
    private var onBackClick: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val density = resources.displayMetrics.density

        // Status text ("Listening...", "Transcribing...", etc.)
        statusText = TextView(context).apply {
            text = context.getString(R.string.voice_ai_category_voice)
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (16 * density).toInt()
            }
        }
        addView(statusText)

        // Large circular mic button
        val micSize = (72 * density).toInt()
        micButton = ImageButton(context).apply {
            scaleType = ImageView.ScaleType.CENTER
            setImageDrawable(KeyboardIconsSet.instance.getNewDrawable("voice", context))
            contentDescription = "Voice input"
            layoutParams = LayoutParams(micSize, micSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (16 * density).toInt()
            }
            setOnClickListener { onMicClick?.invoke() }
        }
        addView(micButton)

        // "Back to keyboard" text button
        backButton = TextView(context).apply {
            text = "← Keyboard"
            textSize = 14f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = (8 * density).toInt()
            }
            setOnClickListener { onBackClick?.invoke() }
        }
        addView(backButton)
    }

    fun applyTheme(colors: Colors) {
        val density = resources.displayMetrics.density
        val bgColor = colors.get(ColorType.MAIN_BACKGROUND)
        val textColor = colors.get(ColorType.KEY_TEXT)
        val accentColor = colors.get(ColorType.KEY_BACKGROUND)

        setBackgroundColor(bgColor)
        statusText.setTextColor(textColor)
        backButton.setTextColor(textColor)

        // Circular mic button background
        val circle = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(accentColor)
        }
        micButton.background = circle
        colors.setColor(micButton, ColorType.TOOL_BAR_KEY)
    }

    fun setOnMicClickListener(listener: () -> Unit) {
        onMicClick = listener
    }

    fun setOnBackClickListener(listener: () -> Unit) {
        onBackClick = listener
    }

    fun updateState(state: VoiceInputManager.VoiceState) {
        statusText.text = when (state) {
            VoiceInputManager.VoiceState.IDLE -> "Tap mic to speak"
            VoiceInputManager.VoiceState.LISTENING -> "Listening..."
            VoiceInputManager.VoiceState.TRANSCRIBING -> "Transcribing..."
            VoiceInputManager.VoiceState.ERROR -> "Error — tap to retry"
        }
    }
}
