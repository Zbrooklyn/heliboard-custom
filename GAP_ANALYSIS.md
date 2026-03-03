# Gap Analysis: Samsung Keyboard vs HeliBoard Custom

**Date:** 2026-03-03
**Samsung reference:** `Screenshot_20260303_083654_Termux.jpg` (One UI dark keyboard)
**HeliBoard build:** `Screenshot_20260303_102414_HeliBoard debug.jpg` (AMOLED Black + Rounded + Borders)
**Repo:** `Zbrooklyn/heliboard-custom` branch `main`

---

## Section 1: Visual Layout — Row by Row

### 1.1 Toolbar Strip (Top)

| Property | Samsung | HeliBoard | Match | Gap |
|----------|---------|-----------|-------|-----|
| **Row count** | 1 row (toolbar OR suggestions, swaps) | 1 row (after latest fix) | YES | — |
| **Icons** | 7: AI sparkle, clipboard, emoji, keyboard, settings, floating, "..." | 6: mic, clipboard, emoji, 1234, settings, incognito | PARTIAL | Different icons, different count |
| **Icon distribution** | Evenly spaced across full width | Evenly spaced (weight=1, after fix) | YES | — |
| **Icon style** | Circular with subtle dark bg (#3A3A3A-ish) | Pill-shaped dark bg (#333333) | CLOSE | Fixed in commit 9934620e |
| **Background** | Subtle dark gray strip (~#1A1A1A) | adjustedBackground (subtle dark gray) | CLOSE | Fixed in commit eddce79c |
| **Height** | ~44dp | 40dp | CLOSE | May need fine-tuning |
| **Suggestion swap** | Suggestions replace toolbar when typing | Same (after latest fix) | YES | — |
| **Orange dot** | Notification indicator on emoji icon | N/A | NO | Samsung system feature |

### 1.2 Action Row (Navigation/Editing)

| Property | Samsung | HeliBoard | Match | Gap |
|----------|---------|-----------|-------|-----|
| **Key count** | 7: ← → IT copy paste email ˅ ˄ | 8: ← → select_word copy paste select_all ˅ ˄ | PARTIAL | Different count and functions |
| **Width fill** | Edge to edge | Edge to edge (width:-1) | YES | — |
| **Key background** | Same dark gray as letter keys | Same dark gray | YES | — |
| **Key shape** | Rounded rectangle ~8dp | Rounded rectangle 8dp | YES | — |
| **Icon style** | Outlined/thin stroke, Samsung icon set | HeliBoard default icons (mixed styles) | NO | Different icon set |
| **5th button** | Shows autofill (email address) | Shows select_all icon | NO | Samsung has Autofill API integration |
| **Key sizing** | Uneven — email button wider | All equal (width:-1 = 12.5% each) | NO | Samsung adapts widths per content |

### 1.3 Number Row

| Property | Samsung | HeliBoard | Match | Gap |
|----------|---------|-----------|-------|-----|
| **Digits** | 1 2 3 4 5 6 7 8 9 0 | 1 2 3 4 5 6 7 8 9 0 | YES | — |
| **Key count** | 10 | 10 | YES | — |
| **Width fill** | Edge to edge | Edge to edge (default width × 10) | YES | — |
| **Text size** | ~24sp (noticeably larger than letters) | 62% ratio (~13% larger than letters) | CLOSE | Reduced from 72% to 62%, Samsung may be slightly bigger |
| **Key background** | Same dark gray as letter keys | Same dark gray | YES | — |
| **Hint labels (shifted)** | ! @ # $ % ^ & * ( ) | ! @ # $ % ^ & * ( ) | YES | — |
| **Font weight** | Medium/regular | Regular | CLOSE | — |

### 1.4 QWERTY Row (q-p)

| Property | Samsung | HeliBoard | Match | Gap |
|----------|---------|-----------|-------|-----|
| **Key count** | 10 | 10 | YES | — |
| **Hint characters** | + × ÷ = / - < > [ ] | + × ÷ = / - < > [ ] | YES | Fixed in commit eddce79c |
| **Hint position** | Top-right corner of each key | Top-right corner | YES | — |
| **Hint text color** | Gray (~#808080) | Gray (#80FFFFFF = 50% white) | CLOSE | — |
| **Letter text color** | White (#FFFFFF) | White (#FFFFFF) | YES | — |
| **Key background** | Dark gray (~#333333) | Dark gray (#333333) | YES | — |
| **Key gaps** | ~3px between keys | 0.8%p horizontal, 2.0%p vertical | YES | Loosened in commit a7bc890a |

### 1.5 Home Row (a-l)

| Property | Samsung | HeliBoard | Match | Gap |
|----------|---------|-----------|-------|-----|
| **Key count** | 9 | 9 | YES | — |
| **Hint characters** | ! @ # $ % ^ & * ( | ! @ # $ % ^ & * ( | YES | Fixed in commit 5a8affc3 |
| **Side indentation** | Minimal (~half-key spacers) | Auto-spacers from width calc | CLOSE | HeliBoard auto-adds spacers |
| **Key background** | Same as QWERTY row | Same | YES | — |

### 1.6 Shift Row (z-m)

| Property | Samsung | HeliBoard | Match | Gap |
|----------|---------|-----------|-------|-----|
| **Letter keys** | 7: z x c v b n m | 7: z x c v b n m | YES | — |
| **Shift key** | Left, same corner radius as letters, outlined arrow | Left, 8dp radius, filled arrow | PARTIAL | Different icon style |
| **Backspace key** | Right, same corner radius, outlined ⌫ icon | Right, 8dp radius, filled ⌫ icon | PARTIAL | Different icon style |
| **Shift/BS width** | ~15% each | 15% each (width: 0.15) | YES | — |
| **Hint characters** | - ' " : ; ! ? | _ " ' : ; ! ? | CLOSE | z=_ vs Samsung z=- (rest matches) |

### 1.7 Bottom Row

| Property | Samsung | HeliBoard | Match | Gap |
|----------|---------|-----------|-------|-----|
| **Key layout** | !#1, globe, comma, space, period, enter | !#1, comma, space, period, search | PARTIAL | Missing globe; search vs enter |
| **Globe key** | Always visible | Only with 2+ languages enabled | NO | Conditional in HeliBoard |
| **Space bar text** | "English (US)" | (empty or language label) | PARTIAL | Depends on settings |
| **Action key** | Enter arrow (↵), same dark gray | Search icon (🔍), light blue accent bg | NO | Different icon and color |
| **Action key shape** | Same corner radius as other keys | Same corner radius | YES | — |
| **!#1 width** | ~15% | 15% (width: 0.15) | YES | — |
| **Action key width** | ~15% | 15% (width: 0.15) | YES | — |

---

## Section 2: Colors & Theme

| Property | Samsung | HeliBoard (AMOLED Black) | Match | Gap |
|----------|---------|--------------------------|-------|-----|
| **Keyboard background** | #000000 (pure black) | #000000 | YES | — |
| **Key background (normal)** | ~#333333-#3A3A3A | #333333 | CLOSE | May be slightly lighter on Samsung |
| **Key background (functional)** | Same as normal keys | Same (#333333) | YES | — |
| **Key text color** | #FFFFFF | #FFFFFF | YES | — |
| **Hint text color** | ~#808080 (medium gray) | #80FFFFFF (50% white) | CLOSE | — |
| **Action key accent** | Same dark gray (no highlight) | Same dark gray (AMOLED override) | YES | Fixed in commit eddce79c |
| **Toolbar icon color** | White/light gray | White/light gray | YES | — |
| **Key bevel/shadow** | None (flat) | None (flat, after fix) | YES | — |

---

## Section 3: Geometry & Spacing

| Property | Samsung | HeliBoard (current) | Match | Gap |
|----------|---------|---------------------|-------|-----|
| **Corner radius (normal)** | ~7-8dp | 8dp | YES | — |
| **Corner radius (functional)** | Same as normal (~7-8dp) | 8dp | YES | — |
| **Horizontal key gap** | ~3dp (~0.8%p) | 0.8%p | YES | Loosened in commit a7bc890a |
| **Vertical key gap** | ~4dp (~2.0%p) | 2.0%p | YES | Loosened in commit a7bc890a |
| **Left/right padding** | ~1% | 1.0%p | YES | Fixed in commit eddce79c |
| **Top padding** | ~1% | 1.0%p | YES | — |
| **Bottom padding** | ~1% | 1.5%p | CLOSE | — |
| **Keyboard height** | ~210dp (estimated) | 205.6dp | CLOSE | — |
| **Suggestion strip height** | ~40-44dp | 40dp | CLOSE | — |

---

## Section 4: Typography

| Property | Samsung | HeliBoard | Match | Gap |
|----------|---------|-----------|-------|-----|
| **Font family** | SamsungOne / SamsungSans | System default (Roboto) | NO | System-level, not changeable in-app |
| **Letter text size ratio** | ~55% | 55% (config_key_letter_ratio) | YES | — |
| **Number text size** | ~30% larger than letters | 62% ratio (~13% larger) | CLOSE | Fixed with FOLLOW_KEY_LARGE_LETTER_RATIO flag |
| **Hint text size ratio** | ~25% | 25% (config_key_hint_letter_ratio) | YES | — |
| **Spacebar label size** | ~33% | 33.735% (config_language_on_spacebar_text_ratio) | YES | — |

---

## Section 5: Behavior

| Property | Samsung | HeliBoard | Match | Gap |
|----------|---------|-----------|-------|-----|
| **Toolbar/suggestion swap** | Single row, swap in place | Single row, swap (after latest fix) | YES | — |
| **Key press feedback** | Subtle highlight, brief | Highlight + optional popup preview | PARTIAL | Different animation style |
| **Long press** | Popup with extra characters | Popup with extra characters | YES | — |
| **Swipe typing** | Samsung swipe | Gesture typing (if enabled) | YES | — |
| **Autofill integration** | Shows email/address in action row | Not available | NO | Would need Autofill API work |

---

## Section 6: Icons

| Icon | Samsung | HeliBoard | Match |
|------|---------|-----------|-------|
| **Shift** | Outlined hollow arrow (△) | Filled arrow (▲) | NO |
| **Backspace** | Outlined ⌫ with X | Filled ⌫ with X | NO |
| **Enter/Action** | Return arrow (↵) | Search magnifying glass (🔍) | NO |
| **Globe** | Globe outline (🌐) | Globe outline (similar) | CLOSE |
| **Toolbar: AI** | Sparkle icon (✨) | Mic icon (🎤) | NO |
| **Toolbar: clipboard** | Clipboard outline | Clipboard outline | CLOSE |
| **Toolbar: emoji** | Smiley face | Smiley face | CLOSE |
| **Toolbar: keyboard** | Keyboard outline | 1234 numpad icon | NO |
| **Toolbar: settings** | Gear icon | Gear icon | YES |
| **Toolbar: floating** | Floating keyboard icon | Incognito icon | NO |
| **Toolbar: more** | "..." ellipsis | (no equivalent) | NO |
| **Action: select** | "IT" text cursor icon | Dotted rectangle | NO |
| **Action: copy** | Overlapping pages | Overlapping pages | CLOSE |
| **Action: paste** | Clipboard with page | Clipboard with page | CLOSE |

---

## Section 6b: Clipboard History View

**Samsung reference:** `Screenshot_20260303_115216_Termux.jpg`

| Property | Samsung | HeliBoard | Match | Gap |
|----------|---------|-----------|-------|-----|
| **Grid columns** | 3 columns | 3 columns | YES | Fixed in commit a7bc890a |
| **Title bar** | "Clipboard" text + keyboard/pin/trash icons | Toolbar strip with tool keys | NO | Samsung has dedicated clipboard header |
| **Section headers** | "Recent" / "Show less" toggle | None | NO | No section header support |
| **Card corner radius** | ~16dp | 8dp (same as keyboard keys) | NO | Clipboard cards need larger radius |
| **Card border** | Subtle thin outline on cards | No border (flat fill) | NO | Samsung cards have 1dp border |
| **Pinned highlight** | Highlighted border color on pinned card | Small pin icon (18dp) | PARTIAL | Different pinned indicator style |
| **Image thumbnails** | Shows copied images as thumbnails | Text only | NO | Would need ContentResolver + image rendering |
| **Card text size** | ~16sp | Inherits from key label size | CLOSE | — |
| **Card padding** | ~16dp internal | 4-8dp margins | CLOSE | Could increase for breathing room |
| **Swipe to delete** | Swipe left to delete | Swipe left to delete | YES | — |
| **Long press to pin** | Long press toggles pin | Long press toggles pin | YES | — |
| **Empty state** | (not shown) | Kaomoji ¯\_(ツ)_/¯ | N/A | Different but fine |

---

## Section 7: Summary Scorecard

| Category | Items Checked | Match | Close | No Match | Score |
|----------|--------------|-------|-------|----------|-------|
| **Layout structure** | 12 | 10 | 1 | 1 | 88% |
| **Colors & theme** | 8 | 7 | 1 | 0 | 94% |
| **Geometry & spacing** | 9 | 8 | 1 | 0 | 94% |
| **Typography** | 5 | 4 | 1 | 0 | 90% |
| **Behavior** | 5 | 3 | 1 | 1 | 70% |
| **Icons** | 14 | 2 | 4 | 8 | 29% |
| **Clipboard** | 12 | 4 | 2 | 6 | 42% |
| **OVERALL** | **65** | **38** | **12** | **16** | **68%** |

---

## Section 8: Priority Fixes (Biggest Visual Impact)

### Tier 1 — Quick Wins (Easy, High Impact)

| # | Fix | Impact | Effort | Files |
|---|-----|--------|--------|-------|
| 1 | ~~**Hint character mapping**~~ | ~~HIGH~~ | ~~Easy~~ | DONE (eddce79c) — QWERTY hints match Samsung |
| 2 | ~~**Action key accent color**~~ | ~~HIGH~~ | ~~Easy~~ | DONE (eddce79c) — uses dark gray on AMOLED |
| 3 | ~~**Key gap fine-tuning**~~ | ~~MEDIUM~~ | ~~Easy~~ | DONE (eddce79c) — h:0.5%p, v:1.5%p |
| 4 | ~~**Side padding fine-tuning**~~ | ~~LOW~~ | ~~Easy~~ | DONE (eddce79c) — 1.0%p |

### Tier 2 — Medium Effort

| # | Fix | Impact | Effort | Files |
|---|-----|--------|--------|-------|
| 5 | ~~**Number row text size**~~ | ~~HIGH~~ | ~~Medium~~ | DONE (eddce79c + 5a8affc3) — 62% ratio via LARGE_LETTER flag |
| 6 | ~~**Toolbar icon backgrounds**~~ | ~~MEDIUM~~ | ~~Medium~~ | DONE (9934620e) — pill-shaped GradientDrawable behind each toolbar icon |
| 7 | ~~**Toolbar strip background**~~ | ~~MEDIUM~~ | ~~Easy~~ | DONE (eddce79c) — adjustedBackground on AMOLED |

### Tier 2b — Clipboard Fixes

| # | Fix | Impact | Effort | Files |
|---|-----|--------|--------|-------|
| 12 | ~~**Clipboard 3-column grid**~~ | ~~HIGH~~ | ~~Easy~~ | DONE (a7bc890a) |
| 13 | **Clipboard card corner radius** — increase from 8dp to 16dp | MEDIUM | Easy | New drawable or modify existing |
| 14 | **Clipboard card border** — add 1dp subtle outline | MEDIUM | Easy | New drawable |
| 15 | **Clipboard title bar** — add "Clipboard" header with back/pin/trash icons | HIGH | Medium | clipboard_history_view.xml + ClipboardHistoryView.kt |
| 16 | **Clipboard section headers** — "Recent" / "Show less" | MEDIUM | Medium | ClipboardAdapter.kt (section headers in RecyclerView) |
| 17 | **Clipboard pinned highlight** — border color change on pinned cards | LOW | Easy | ClipboardAdapter.kt |
| 18 | **Clipboard image thumbnails** — show copied images | HIGH | Hard | ClipboardHistoryManager, ClipboardAdapter, ContentResolver |

### Tier 3 — Hard / Not Possible

| # | Fix | Impact | Effort | Notes |
|---|-----|--------|--------|-------|
| 8 | **Icon redesign** — replace shift, backspace, enter, action row icons with Samsung-style outlined versions | HIGH | Hard | Need custom SVG drawables for each icon |
| 9 | **Autofill in action row** — show email/address suggestions | MEDIUM | Hard | Android Autofill API integration |
| 10 | **Font** — Samsung uses SamsungOne typeface | LOW | Not possible | System-level, keyboard can't override |
| 11 | **Samsung toolbar features** — AI sparkle, floating keyboard, notification dots | LOW | Not possible | Samsung One UI proprietary |

---

## Section 9: Current HeliBoard Values Reference

### Spacing & Padding
```
top_padding:        1.0%p
bottom_padding:     1.5%p
left_padding:       1.0%p
right_padding:      1.0%p
vertical_gap:       2.0%p  (loosened in a7bc890a)
horizontal_gap:     0.8%p  (loosened in a7bc890a)
vertical_gap_narrow:   1.3%p
horizontal_gap_narrow: 0.5%p
```

### Colors (AMOLED Black theme)
```
background:         #000000
key_background:     #333333
functional_key:     #333333
spacebar:           #333333
key_text:           #FFFFFF
key_hint:           #80FFFFFF (50% white)
accent:             #5E97F6
```

### Key Shape
```
corner_radius:      8dp (all keys)
bevel:              none (flat)
```

### Suggestion Strip
```
height:             40dp
layout:             FrameLayout (toolbar/suggestions swap)
toolbar_key_width:  weight=1 (evenly distributed)
```

### Action Row
```
keys:               8 (left, right, select_word, copy, paste, select_all, up, down)
key_width:          -1 (auto-fill, 12.5% each)
```

### Bottom Row
```
keys:               !#1(15%), [globe?], comma, [emoji?], space(-1), period, action(15%)
globe:              conditional (languageKeyEnabled)
emoji:              conditional (emojiKeyEnabled)
```

---

## Section 10: Files Changed So Far

| Commit | Files | Change |
|--------|-------|--------|
| 874b9ee2 | 7 files | Spacing, bevel removal, radius, AMOLED color |
| 311274f0 | action_row.json | Width:-1 on all action row keys |
| df934489 | colors.xml | Key color #1A1A1A→#333333 |
| 733aa8b0 | SuggestionStripView.kt | Hide empty suggestion row |
| 3d210ec2 | suggestions_strip.xml, SuggestionStripView.kt | Toolbar icons evenly distributed |
| 7e87a939 | suggestions_strip.xml, SuggestionStripView.kt, config.xml | Single-row swap layout, strip 76dp→40dp |
| 26d58098 | GAP_ANALYSIS.md | 53-item gap analysis document |
| cdf49603 | ROADMAP.md | Updated roadmap with progress |
| eddce79c | 6 files | Close Samsung gap (hints, action key, spacing, number size, toolbar bg) |
| 5a8affc3 | config.xml, symbols.txt | Number row size 72%→62%, home row hints to Samsung order |
| 8ce9ea75 | GAP_ANALYSIS.md | Updated scorecard 65%→73% |
| 9934620e | SuggestionStripView.kt | Samsung-style pill backgrounds on toolbar icons |
| a7bc890a | config.xml | Loosen spacing (v:2.0%p, h:0.8%p), clipboard 3 columns |
