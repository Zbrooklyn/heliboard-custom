// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.keyboard.clipboard

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import helium314.keyboard.latin.ClipboardHistoryEntry
import helium314.keyboard.latin.ClipboardHistoryManager
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings

class ClipboardAdapter(
       val clipboardLayoutParams: ClipboardLayoutParams,
       val keyEventListener: OnKeyEventListener
) : RecyclerView.Adapter<ClipboardAdapter.ViewHolder>() {

    var clipboardHistoryManager: ClipboardHistoryManager? = null

    var pinnedIconResId = 0
    var itemBackgroundId = 0
    var itemTypeFace: Typeface? = null
    var itemTextColor = 0
    var itemTextSize = 0f

    var isSelectionMode = false
    val selectedIds = mutableSetOf<Long>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.clipboard_entry_key, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setContent(getItem(position))
    }

    private fun getItem(position: Int) = clipboardHistoryManager?.getHistoryEntry(position)

    override fun getItemCount() = clipboardHistoryManager?.getHistorySize() ?: 0

    inner class ViewHolder(
            view: View
    ) : RecyclerView.ViewHolder(view), View.OnClickListener, View.OnTouchListener, View.OnLongClickListener {

        private val pinnedIconView: ImageView
        private val contentView: TextView
        private val checkBox: CheckBox

        init {
            view.apply {
                setOnClickListener(this@ViewHolder)
                setOnTouchListener(this@ViewHolder)
                setOnLongClickListener(this@ViewHolder)
                setBackgroundResource(R.drawable.clipboard_entry_background)
                isHapticFeedbackEnabled = false
            }
            Settings.getValues().mColors.setBackground(view, ColorType.KEY_BACKGROUND)
            pinnedIconView = view.findViewById<ImageView>(R.id.clipboard_entry_pinned_icon).apply {
                visibility = View.GONE
                setImageResource(pinnedIconResId)
            }
            contentView = view.findViewById<TextView>(R.id.clipboard_entry_content).apply {
                typeface = itemTypeFace
                setTextColor(itemTextColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, itemTextSize)
            }
            checkBox = view.findViewById(R.id.clipboard_entry_checkbox)
            clipboardLayoutParams.setItemProperties(view)
            val colors = Settings.getValues().mColors
            colors.setColor(pinnedIconView, ColorType.CLIPBOARD_PIN)
        }

        fun setContent(historyEntry: ClipboardHistoryEntry?) {
            itemView.tag = historyEntry?.id
            contentView.text = historyEntry?.text?.take(1000) // truncate displayed text for performance reasons
            pinnedIconView.visibility = if (historyEntry?.isPinned == true) View.VISIBLE else View.GONE

            val isSelected = isSelectionMode && historyEntry?.id in selectedIds
            // Selection mode: show/hide checkbox, set checked state
            if (isSelectionMode) {
                checkBox.visibility = View.VISIBLE
                checkBox.isChecked = isSelected
            } else {
                checkBox.visibility = View.GONE
            }
            // Highlight selected cards with reduced alpha
            itemView.alpha = if (isSelectionMode && !isSelected) 0.5f else 1.0f
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            if (isSelectionMode) return false
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                keyEventListener.onKeyDown(view.tag as Long)
            }
            return false
        }

        override fun onClick(view: View) {
            val id = view.tag as Long
            if (isSelectionMode) {
                if (id in selectedIds) selectedIds.remove(id) else selectedIds.add(id)
                checkBox.isChecked = id in selectedIds
                itemView.alpha = if (id in selectedIds) 1.0f else 0.5f
                keyEventListener.onSelectionChanged()
                return
            }
            keyEventListener.onKeyUp(id)
        }

        override fun onLongClick(view: View): Boolean {
            val id = view.tag as Long
            if (isSelectionMode) return true // suppress in selection mode
            // Long-press enters selection mode with this card pre-checked
            keyEventListener.onLongPressSelect(id)
            return true
        }
    }
}
