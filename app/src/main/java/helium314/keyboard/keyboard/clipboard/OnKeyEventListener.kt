// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.keyboard.clipboard

interface OnKeyEventListener {

    fun onKeyDown(clipId: Long)

    fun onKeyUp(clipId: Long)

    /** Called when long-press should enter selection mode with the given clip pre-checked */
    fun onLongPressSelect(clipId: Long) {}

    /** Called when selection changes (item toggled) so the view can update count display */
    fun onSelectionChanged() {}

}