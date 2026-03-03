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

---

## Remaining Gaps

### Fixable — Needs Work

| # | Gap | Difficulty | Notes |
|---|-----|-----------|-------|
| 1 | **Number row text size** — Samsung digits are ~20% larger than letter keys | Medium | Need a separate `config_key_large_letter_ratio` or per-row text size override. May require Kotlin changes if XML config doesn't support per-row sizing. |
| 2 | **Key icons** — shift, backspace, enter, globe icons don't match Samsung style | Medium | Samsung uses outlined/hollow icon style. Would need custom drawable SVGs replacing HeliBoard's defaults. Files are in `app/src/main/res/drawable/` (sym_keyboard_shift_*, sym_keyboard_delete_*, sym_keyboard_enter_*, sym_keyboard_language_switch_*). |
| 3 | **Action row content** — Samsung shows autofill suggestions (email, address) as a toolbar button | Hard | HeliBoard's action row is static function keys. Samsung integrates with Android Autofill framework to show context-aware suggestions inline. Would need Autofill API integration + dynamic action row key generation. |
| 4 | **Key color fine-tuning** — #333333 is close but Samsung may vary by device/One UI version (~#333333 to #3A3A3A) | Easy | One-line change in colors.xml. May need side-by-side testing on device to nail exact value. |

### Not Fixable (Samsung One UI / System Level)

| # | Gap | Why |
|---|-----|-----|
| 5 | **Samsung toolbar strip** — top row with AI sparkle, clipboard, emoji, keyboard switch, settings, floating keyboard, "..." icons | Samsung One UI proprietary feature. HeliBoard shows word suggestion strip instead. Would require building an entirely new toolbar UI from scratch. |
| 6 | **Font** — Samsung uses SamsungOne/SamsungSans typeface | System-level font. HeliBoard renders with whatever system font is active (Roboto on stock Android). User could install SamsungOne system-wide with a font manager on rooted devices. |
| 7 | **Bottom system bar** — keyboard picker dots (left) and dismiss chevron (right) below keyboard | Android system UI, not part of the keyboard app. Rendered by InputMethodManager. |
| 8 | **Key press animation** — Samsung has subtle highlight/ripple on press | HeliBoard has its own press highlight. Could potentially be customized via `btn_keyboard_key_pressed_*` drawables but matching Samsung's exact animation timing/style would be difficult. |

---

## Build & Test

1. Push to `Zbrooklyn/heliboard-custom` main branch
2. GitHub Actions builds debug APK automatically
3. Download APK artifact, install on device
4. Settings: Rounded style + Borders ON + AMOLED Black night theme
5. Compare side-by-side with Samsung screenshot

---

## Commits

| Hash | Change |
|------|--------|
| 874b9ee2 | Style keyboard to match Samsung dark theme (spacing, bevel, radius, color) |
| 311274f0 | Fill action row keys edge-to-edge (width:-1) |
| df934489 | Bump AMOLED key color to #333333 |
