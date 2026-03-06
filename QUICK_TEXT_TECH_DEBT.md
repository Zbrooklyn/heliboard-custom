# Quick Text — Technical Debt & Patch Points

What works, what's patchy, and what a robust version looks like.

---

## 1. Popup is raw `PopupWindow`, not keyboard-native

**File:** `SuggestionStripView.kt:439-492` — `showQuickTextPopup()`

**What it does now:**
- Builds a `LinearLayout` + `TextView` list manually
- Wraps it in a `PopupWindow` positioned above the toolbar button
- Pulls colors from `ColorType.KEY_TEXT` / `KEY_BACKGROUND`

**What's wrong:**
- Hardcoded values: `14f` text size, `8dp` padding, `120dp` min width, `8dp` corner radius
- Doesn't use the keyboard's own `MoreKeysKeyboard` / `MoreKeysKeyboardView` popup system
- No ripple/press feedback on snippet items
- No accessibility: missing content descriptions, no TalkBack announce
- No scroll — if someone adds 20 snippets, it overflows off screen
- Position math is fragile — `showAsDropDown(anchor, 0, -(height + anchorHeight))` can clip at screen top

**Robust version:**
- Use `MoreKeysKeyboardView` (what every key popup uses) or a `ListPopupWindow` with theme-aware adapter
- Add `ScrollView` wrapper or use `RecyclerView` for long lists
- Add snippet count limit or truncation in popup
- Use `?attr/selectableItemBackground` for ripple feedback
- Add `contentDescription` to each item

---

## 2. Listener cast workaround

**File:** `SuggestionStripView.kt:446, 470`

```kotlin
(listener as? KeyboardActionListener)?.onTextInput(snippet)
```

**What's wrong:**
- `SuggestionStripView.Listener` interface doesn't have `onTextInput()`
- We safe-cast to `KeyboardActionListener` (which LatinIME also implements) to get it
- If `listener` ever stops being a `KeyboardActionListener`, text insertion silently fails

**Robust version:**
- Add `onTextInput(text: String)` to `SuggestionStripView.Listener`
- Implement it in LatinIME (just delegates to existing `onTextInput`)
- Call `listener.onTextInput(snippet)` directly — no cast needed

---

## 3. No snippet size limits

**File:** `QuickTextUtils.kt`, `ClipboardSettingsScreen.kt`

**What's wrong:**
- No max snippet count — user could add hundreds
- No max snippet length — a user could paste a whole document
- JSON blob stored in SharedPreferences grows unbounded
- Popup can't display long snippets usefully (no truncation, no ellipsis)

**Robust version:**
- Cap at ~20 snippets in settings UI (disable "Add" button)
- Cap snippet length at ~500 chars (show counter in text field)
- Truncate display text in popup to ~50 chars with ellipsis
- Show full text only on insert

---

## 4. No haptic/audio feedback on tap

**File:** `SuggestionStripView.kt:469-471`

**What's wrong:**
- Tapping a snippet in the popup has no haptic or sound feedback
- Every other key press in HeliBoard plays a click and/or vibrates
- Feels dead compared to normal keyboard interaction

**Robust version:**
- Call `AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback()` on snippet tap
- Or at minimum `view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)`

---

## 5. Settings mixed into Clipboard screen

**File:** `ClipboardSettingsScreen.kt`

**What it does now:**
- Quick Text snippets editor lives under "Clipboard" settings with a category header

**What's wrong:**
- Quick Text isn't clipboard. It's its own feature that happens to share a screen.
- If Quick Text grows (reordering, import/export, per-app snippets), it'll outgrow this screen

**Robust version:**
- Fine for now. If the feature expands, move to its own `QuickTextSettingsScreen`

---

## 6. No reordering in snippet editor

**File:** `ClipboardSettingsScreen.kt` — `QuickTextEditDialog()`

**What's wrong:**
- Snippet order matters — first snippet is the tap default
- No drag-to-reorder, no move up/down buttons
- User has to delete and re-add to change order

**Robust version:**
- Add drag handles or up/down arrow buttons per row
- Or at minimum document that "first snippet = default tap" in the UI

---

## Priority

| # | Issue | Severity | Effort |
|---|-------|----------|--------|
| 1 | Raw PopupWindow | Medium | High — needs MoreKeysKeyboard integration or RecyclerView |
| 2 | Listener cast | Low | Low — add one method to interface + LatinIME |
| 3 | No size limits | Medium | Low — add constants + UI constraints |
| 4 | No haptic feedback | Low | Low — one line |
| 5 | Settings location | Low | None now — revisit if feature grows |
| 6 | No reordering | Medium | Medium — drag-to-reorder or arrow buttons |

---

## Files touched by Quick Text

| File | What | Clean? |
|------|------|--------|
| `ToolbarUtils.kt` | Enum + code mapping | Yes |
| `KeyCode.kt` | `QUICK_TEXT = -10056` | Yes |
| `QuickTextUtils.kt` | JSON snippet storage | Yes (needs limits) |
| `InputLogic.java:813-816` | Tap handler | Yes |
| `SuggestionStripView.kt:439-492` | Popup (long-press) | Patchy |
| `KeyboardIconsSet.kt` | 3 icon mappings | Yes |
| `ic_quick_text_*.xml` (3 files) | "Aa" vector icons | Yes |
| `Settings.java` | Pref key constant | Yes |
| `Defaults.kt` | Default `"[]"` | Yes |
| `ClipboardSettingsScreen.kt` | Settings UI + editor dialog | Yes (needs limits) |
| `strings.xml` | 7 string resources | Yes |
