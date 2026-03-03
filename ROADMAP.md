# HeliBoard Samsung Theme — Roadmap

Reference: `/storage/emulated/0/DCIM/Screenshots/Screenshot_20260303_083654_Termux.jpg`

---

## Done

- [x] Flat keys — removed 1dp bottom bevel from normal + functional keys
- [x] Corner radius — unified to 8dp (functional keys were 500dp pill-shaped)
- [x] Tight key spacing — vertical 6.1%→2.5%, horizontal 1.7%→0.8%
- [x] Edge-to-edge side padding — 8%→1.5%
- [x] Action row fills full width — keys set to width:-1 (auto-fill)
- [x] AMOLED key color — #1A1A1A→#333333 to match Samsung contrast on #000000 background
- [x] Top/bottom padding reduced — 2.3%/4.7%→1.0%/1.5%
- [x] Key layout matches Samsung (number row + action row + QWERTY + bottom row)
- [x] Hide empty suggestion row — bottom strip row set to GONE when no suggestions, eliminates black gap between toolbar and keyboard
- [x] Toolbar icons distributed evenly — replaced HorizontalScrollView with weighted LinearLayout so icons spread across full width like Samsung

---

## Remaining Gaps

### Fixable — Needs Work

| # | Gap | Difficulty | Notes |
|---|-----|-----------|-------|
| 1 | **Number row text size** — Samsung digits are ~20% larger than letter keys | Medium | Need a separate `config_key_large_letter_ratio` or per-row text size override. May require Kotlin changes if XML config doesn't support per-row sizing. |
| 2 | **Key icons** — shift, backspace, enter, globe icons don't match Samsung style | Medium | Samsung uses outlined/hollow icon style. Would need custom drawable SVGs replacing HeliBoard's defaults. Files are in `app/src/main/res/drawable/` (sym_keyboard_shift_*, sym_keyboard_delete_*, sym_keyboard_enter_*, sym_keyboard_language_switch_*). |
| 3 | **Action row content** — Samsung shows autofill suggestions (email, address) as a toolbar button | Hard | HeliBoard's action row is static function keys. Samsung integrates with Android Autofill framework to show context-aware suggestions inline. Would need Autofill API integration + dynamic action row key generation. |
| 4 | **Key color fine-tuning** — #333333 is close but Samsung may vary by device/One UI version (~#333333 to #3A3A3A) | Easy | One-line change in colors.xml. May need side-by-side testing on device to nail exact value. |
| 5 | **Hint text characters** — HeliBoard shows `% \ \| = [ ] < > { }`, Samsung shows `+ × ÷ = / - < > [ ]` | Medium | Defined in popup keys / hint labels in the main layout JSON. Would need to remap hint characters to match Samsung's symbol assignments. |
| 6 | **Toolbar background** — Samsung toolbar has subtle dark gray background strip, HeliBoard toolbar sits on pure black | Easy | Add a background color to the toolbar_container or toolbar LinearLayout in suggestions_strip.xml. |

### Not Fixable (Samsung One UI / System Level)

| # | Gap | Why |
|---|-----|-----|
| 7 | **Samsung toolbar icons** — AI sparkle, clipboard, emoji, keyboard switch, settings, floating keyboard, "..." | Samsung One UI proprietary. HeliBoard has its own toolbar keys (mic, clipboard, emoji, numpad, settings, incognito). Icons differ but function is similar. |
| 8 | **Font** — Samsung uses SamsungOne/SamsungSans typeface | System-level font. HeliBoard renders with whatever system font is active (Roboto on stock Android). User could install SamsungOne system-wide with a font manager on rooted devices. |
| 9 | **Bottom system bar** — keyboard picker dots (left) and dismiss chevron (right) below keyboard | Android system UI, not part of the keyboard app. Rendered by InputMethodManager. |
| 10 | **Key press animation** — Samsung has subtle highlight/ripple on press | HeliBoard has its own press highlight. Could potentially be customized via `btn_keyboard_key_pressed_*` drawables but matching Samsung's exact animation timing/style would be difficult. |
| 11 | **Globe key on bottom row** — Samsung shows globe (language switch), HeliBoard only shows it with multiple languages configured | By design — HeliBoard's `language_switch` key is conditional on `languageKeyEnabled` in functional_keys.json. Add a second language in settings to show it. |

---

## Required Settings

To activate the Samsung-like appearance:
1. **Appearance > Theme style** → Rounded
2. **Appearance > Theme colors (night)** → **Black**
3. **Appearance > Key borders** → ON
4. **Appearance > Day/night mode** → ON
5. Phone must be in system dark mode

---

## Build & Test

1. Push to `Zbrooklyn/heliboard-custom` main branch
2. GitHub Actions builds debug APK automatically
3. Download APK artifact, install on device
4. Apply settings above
5. Compare side-by-side with Samsung screenshot

---

## Commits

| Hash | Change |
|------|--------|
| 874b9ee2 | Style keyboard to match Samsung dark theme (spacing, bevel, radius, color) |
| 311274f0 | Fill action row keys edge-to-edge (width:-1) |
| df934489 | Bump AMOLED key color to #333333 |
| 9129534f | Add Samsung theme roadmap |
| 733aa8b0 | Hide empty suggestion row to eliminate black gap |
| 3d210ec2 | Distribute toolbar icons evenly across full width |
