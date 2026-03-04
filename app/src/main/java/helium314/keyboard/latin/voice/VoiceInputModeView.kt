// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import helium314.keyboard.keyboard.internal.KeyboardIconsSet
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.Colors

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
    private val providerText: TextView
    private val partialPreviewText: TextView
    private val progressBar: ProgressBar
    private val micButton: ImageButton
    private val networkIndicator: TextView
    private val incognitoIndicator: TextView
    private val backButton: TextView

    private var onMicClick: (() -> Unit)? = null
    private var onBackClick: (() -> Unit)? = null

    // Mic pulse animation
    private var micPulseAnimatorX: ObjectAnimator? = null
    private var micPulseAnimatorY: ObjectAnimator? = null
    private var micCircleDrawable: GradientDrawable? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val density = resources.displayMetrics.density

        // Status text ("Listening...", "Transcribing...", etc.)
        statusText = TextView(context).apply {
            text = context.getString(R.string.voice_tap_mic)
            textSize = 16f
            gravity = Gravity.CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (4 * density).toInt()
            }
        }
        addView(statusText)

        // Provider subtitle (e.g. "Local STT", "Cloud STT (OpenAI)", "Google STT")
        providerText = TextView(context).apply {
            textSize = 12f
            gravity = Gravity.CENTER
            alpha = 0.6f
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (4 * density).toInt()
            }
        }
        addView(providerText)

        // Network/offline indicator
        networkIndicator = TextView(context).apply {
            textSize = 10f
            gravity = Gravity.CENTER
            alpha = 0.5f
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (2 * density).toInt()
            }
        }
        addView(networkIndicator)

        // Incognito indicator (hidden by default)
        incognitoIndicator = TextView(context).apply {
            text = context.getString(R.string.voice_incognito_indicator)
            textSize = 10f
            gravity = Gravity.CENTER
            alpha = 0.5f
            visibility = View.GONE
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (4 * density).toInt()
            }
        }
        addView(incognitoIndicator)

        // Partial transcription preview (hidden by default, shown during Google STT)
        partialPreviewText = TextView(context).apply {
            textSize = 13f
            gravity = Gravity.CENTER
            alpha = 0.7f
            maxLines = 3
            visibility = View.GONE
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * density).toInt()
                marginStart = (16 * density).toInt()
                marginEnd = (16 * density).toInt()
            }
        }
        addView(partialPreviewText)

        // Progress bar for model downloads (hidden by default)
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            visibility = View.GONE
            layoutParams = LayoutParams((200 * density).toInt(), LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (8 * density).toInt()
            }
        }
        addView(progressBar)

        // Large circular mic button
        val micSize = (72 * density).toInt()
        micButton = ImageButton(context).apply {
            scaleType = ImageView.ScaleType.CENTER
            setImageDrawable(KeyboardIconsSet.instance.getNewDrawable("voice", context))
            contentDescription = context.getString(R.string.voice_idle_desc)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            layoutParams = LayoutParams(micSize, micSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (16 * density).toInt()
            }
            setOnClickListener { onMicClick?.invoke() }
        }
        addView(micButton)

        // "Back to keyboard" text button
        backButton = TextView(context).apply {
            text = context.getString(R.string.voice_back_to_keyboard)
            textSize = 14f
            gravity = Gravity.CENTER
            contentDescription = context.getString(R.string.voice_back_desc)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = (8 * density).toInt()
            }
            setOnClickListener { onBackClick?.invoke() }
        }
        addView(backButton)
    }

    fun applyTheme(colors: Colors) {
        val bgColor = colors.get(ColorType.MAIN_BACKGROUND)
        val textColor = colors.get(ColorType.KEY_TEXT)
        val accentColor = colors.get(ColorType.KEY_BACKGROUND)

        setBackgroundColor(bgColor)
        statusText.setTextColor(textColor)
        providerText.setTextColor(textColor)
        networkIndicator.setTextColor(textColor)
        incognitoIndicator.setTextColor(textColor)
        partialPreviewText.setTextColor(textColor)
        backButton.setTextColor(textColor)

        // Circular mic button background
        val circle = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(accentColor)
        }
        micCircleDrawable = circle
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
            VoiceInputManager.VoiceState.IDLE -> context.getString(R.string.voice_tap_mic)
            VoiceInputManager.VoiceState.LISTENING -> context.getString(R.string.voice_listening)
            VoiceInputManager.VoiceState.TRANSCRIBING -> context.getString(R.string.voice_transcribing)
            VoiceInputManager.VoiceState.ERROR -> context.getString(R.string.voice_error_tap_retry)
        }

        // Update accessibility content description for mic button
        micButton.contentDescription = when (state) {
            VoiceInputManager.VoiceState.IDLE -> context.getString(R.string.voice_idle_desc)
            VoiceInputManager.VoiceState.LISTENING -> context.getString(R.string.voice_recording_desc)
            VoiceInputManager.VoiceState.TRANSCRIBING -> context.getString(R.string.voice_transcribing_desc)
            VoiceInputManager.VoiceState.ERROR -> context.getString(R.string.voice_error_desc)
        }

        // Announce state change for screen readers
        announceForAccessibility(statusText.text)

        // Hide progress bar when actively recording/transcribing
        if (state != VoiceInputManager.VoiceState.IDLE) {
            progressBar.visibility = View.GONE
        }

        // Mic pulse animation — active only while listening
        if (state == VoiceInputManager.VoiceState.LISTENING) {
            startMicPulse()
        } else {
            stopMicPulse()
        }

        // Hide partial preview when not listening
        if (state != VoiceInputManager.VoiceState.LISTENING) {
            partialPreviewText.visibility = View.GONE
            partialPreviewText.text = ""
        }
    }

    /** Show which STT provider is active (grayed-out subtitle). */
    fun updateProvider(label: String) {
        providerText.text = label
    }

    /** Show network/offline indicator based on STT mode. */
    fun updateNetworkIndicator(sttMode: String) {
        networkIndicator.text = when (sttMode) {
            "local" -> context.getString(R.string.voice_network_offline)
            "cloud" -> context.getString(R.string.voice_network_openai)
            "google" -> context.getString(R.string.voice_network_google)
            else -> ""
        }
    }

    /** Show or hide the incognito indicator. */
    fun setIncognitoVisible(visible: Boolean) {
        incognitoIndicator.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /** Update partial transcription preview text (for Google STT live results). */
    fun updatePartialPreview(text: String?) {
        if (text.isNullOrBlank()) {
            partialPreviewText.visibility = View.GONE
            partialPreviewText.text = ""
        } else {
            partialPreviewText.visibility = View.VISIBLE
            partialPreviewText.text = text
        }
    }

    /** Show or update model download progress. Set percent = -1 to hide. */
    fun updateDownloadProgress(percent: Int) {
        if (percent < 0) {
            progressBar.visibility = View.GONE
            statusText.text = context.getString(R.string.voice_tap_mic)
        } else {
            progressBar.visibility = View.VISIBLE
            progressBar.progress = percent
            statusText.text = context.getString(R.string.voice_downloading_model, percent)
        }
    }

    /** Start a gentle breathing/pulse animation on the mic button to indicate recording. */
    private fun startMicPulse() {
        if (micPulseAnimatorX != null) return
        micPulseAnimatorX = ObjectAnimator.ofFloat(micButton, "scaleX", 1.0f, 1.15f).apply {
            duration = 600
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        micPulseAnimatorY = ObjectAnimator.ofFloat(micButton, "scaleY", 1.0f, 1.15f).apply {
            duration = 600
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        micPulseAnimatorX?.start()
        micPulseAnimatorY?.start()
    }

    /** Stop the mic pulse animation and reset scale. */
    private fun stopMicPulse() {
        micPulseAnimatorX?.cancel()
        micPulseAnimatorX = null
        micPulseAnimatorY?.cancel()
        micPulseAnimatorY = null
        micButton.scaleX = 1.0f
        micButton.scaleY = 1.0f
    }
}
