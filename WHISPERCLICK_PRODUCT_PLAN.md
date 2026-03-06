# WhisperClick Product Plan

## What is WhisperClick

A custom Android keyboard forked from HeliBoard. Samsung-style layout and UX with integrated voice AI (WhisperFlow). The goal is a clean, opinionated keyboard that works out of the box — not a settings playground.

HeliBoard is the engine. WhisperClick is the product.

---

## Keyboard Layout (top to bottom)

```
┌─────────────────────────────────────────────┐
│  [Mic] [Rw] [Clip] [Emoji] [#] [Aa] [...]  │  ← Toolbar (circular icons, swaps with suggestions)
├─────────────────────────────────────────────┤
│  [←] [→] [Select All] [Copy] [Paste] [↓][↑]│  ← Action Bar (edit keys)
├─────────────────────────────────────────────┤
│  1  2  3  4  5  6  7  8  9  0               │  ← Number Row (optional)
├─────────────────────────────────────────────┤
│                                             │
│              Keyboard Keys                  │  ← Main keyboard
│                                             │
└─────────────────────────────────────────────┘
```

**Toolbar / Suggestion bar** share one row. Toolbar shows when idle. Suggestions replace it when typing. They swap — never both visible.

**Action Bar** is always visible. Contains navigation and clipboard edit keys.

**Number Row** is optional, on by default.

---

## Features Built

| Feature | Status | Notes |
|---------|--------|-------|
| Samsung-style circular toolbar | Done | Expandable with 3-dots overflow |
| Toolbar / suggestion bar swap | Done | Auto-show when idle, auto-hide when typing |
| Action bar (edit keys) | Done | Left, right, select all, copy, paste, up, down, close |
| WhisperClick / Classic settings split | Done | Two sections in main settings |
| Clipboard settings screen | Done | Extracted from Preferences to own screen |
| Quick Text toolbar key | Done (patchy) | Tap inserts default, long-hold shows popup |
| Voice AI settings | Done | WhisperFlow integration |
| Theme & Size settings | Done | Own screen under WhisperClick |
| Number row | Done (HeliBoard stock) | Toggle in Preferences |

---

## Settings Architecture

### WhisperClick Section (curated)

| Setting | What it controls | Type |
|---------|-----------------|------|
| **Toolbar** | Which icons show in the circular toolbar | Reorderable key list |
| **Action Bar** | Which keys show in the edit row | Reorderable key list |
| **Number Row** | Show/hide number row | On/off toggle |
| **Theme & Size** | Keyboard appearance | Own screen |
| **Voice AI** | WhisperFlow voice input | Own screen |
| **Clipboard** | History + Quick Text snippets | Own screen |

### Hardcoded Behavior (no user toggles)

These are baked in for WhisperClick. No settings exposed:

| Behavior | Value | Why |
|----------|-------|-----|
| Toolbar mode | Expandable | Only mode that makes sense for the layout |
| Auto-show toolbar | On | Toolbar visible when not typing |
| Auto-hide toolbar | On | Suggestions replace toolbar when typing |
| Pinned toolbar keys | Disabled | No pinned keys concept — just toolbar + action bar |
| Quick pin | Off | No long-press-to-pin behavior |
| Variable toolbar direction | Follows system | No need for user toggle |

### Classic Section (legacy HeliBoard)

Stays as-is for now. Contains every HeliBoard setting. Over time these get:

1. **Phase 1: Disabled** — Toggle grayed out, tooltip says "Managed by WhisperClick"
2. **Phase 2: Hidden** — Setting removed from Classic UI but code still exists
3. **Phase 3: Removed** — Code deleted

### Deprecation Schedule

| Setting | Current State | Phase 1 (Disable) | Phase 2+ |
|---------|--------------|-------------------|----------|
| Toolbar Mode picker | Active in Classic | Disable — WhisperClick hardcodes Expandable | Hide |
| Auto Show Toolbar | Active in Classic | Disable — always on | Hide |
| Auto Hide Toolbar | Active in Classic | Disable — always on | Hide |
| Quick Pin Toolbar Keys | Active in Classic | Disable — off | Hide |
| Pinned Toolbar Keys list | Active in Classic | Disable — empty | Hide |
| Variable Toolbar Direction | Active in Classic | Disable — follows system | Hide |
| Toolbar Hiding Global | Active in Classic | Disable | Hide |

Settings NOT being deprecated (they live in WhisperClick):
- Toolbar key list → moves to WhisperClick Toolbar
- Clipboard toolbar key list → moves to WhisperClick Action Bar
- Number row toggle → moves to WhisperClick Number Row
- All Clipboard, Voice AI, Theme & Size settings → already in WhisperClick

---

## Technical Debt

### Patchy (needs hardening)

| Item | File | Issue | Fix effort |
|------|------|-------|------------|
| Quick Text popup | SuggestionStripView.kt:439-492 | Raw PopupWindow, hardcoded styles, no scroll, no accessibility | High |
| Listener cast | SuggestionStripView.kt:446,470 | `as? KeyboardActionListener` instead of proper interface | Low |
| No snippet limits | QuickTextUtils.kt, ClipboardSettingsScreen.kt | Unbounded count/length | Low |
| No haptic feedback | SuggestionStripView.kt | Snippet tap has no feedback | Low |
| No snippet reorder | ClipboardSettingsScreen.kt | Can't reorder, first = default | Medium |
| QUICK_TEXT not default-on | ToolbarUtils.kt | Missing migration for existing installs | Low |

### Clean

| Item | File |
|------|------|
| ToolbarKey enum + code mapping | ToolbarUtils.kt |
| KeyCode constant | KeyCode.kt |
| Snippet storage (JSON/SharedPreferences) | QuickTextUtils.kt |
| Tap handler | InputLogic.java:813-816 |
| Icon sets (3 styles) | KeyboardIconsSet.kt, ic_quick_text_*.xml |
| Preference constants | Settings.java, Defaults.kt |
| String resources | strings.xml |

---

## Roadmap

### Phase 0 — Audit & Stabilize
- Fix settings gray screen (startup blocking)
- Fix QUICK_TEXT default-on migration
- Review clipboard migration fragility
- Build disabled preference UI pattern (`enabled: Boolean` on all preference composables)
- Update documentation

### Phase 1 — Settings Reorganization
- Simplify Toolbar screen (key list + custom codes only)
- Create Action Bar screen (edit key list)
- Add inline Number Row toggle to main screen
- Disable legacy toolbar toggles (grayed out, "Managed by WhisperClick")
- Enforce hardcoded defaults (toolbar mode, auto-show/hide, no pinned keys)

### Phase 2 — Harden Existing Features
- Quick Text: proper popup, haptic feedback, snippet limits, reordering
- Fix listener cast workaround
- Voice input reliability

### Phase 3 — Rewrite Button Integration
- Wire Rewrite toolbar key to WhisperClick rewrite engine
- Build rewrite menu panel (like clipboard history)
- Text selection + replace-in-place

### Phase 4 — Testing & QA
- Manual test plan for every feature
- Edge cases, performance, landscape, RTL, accessibility

### Phase 5 — Deprecation Round 1
- Hide disabled Classic settings from UI
- Clean up pinned keys rendering code
- Consolidate toolbar modes

### Phase 6 — Absorb Classic Features
- 6A: Input & Feedback → WhisperClick screen
- 6B: Typing & Autocorrect → WhisperClick screen
- 6C: Swipe Typing → WhisperClick screen
- 6D: Languages → WhisperClick screen
- 6E: Appearance extras → absorb into Theme & Size

### Phase 7 — Security & Privacy
- Audit autofill/password manager support
- Authentication code handling
- Privacy audit

### Phase 8 — Branding & Production
- App identity (name, icon, about)
- Remove or minimize Classic section
- Final QA + distribution

**Full implementation details:** See plan file or `QUICK_TEXT_TECH_DEBT.md` for technical debt specifics.

**Existing feature inventory:** 100+ settings across 10 screens. Complete mapping of every feature to its WhisperClick home is in the plan file.

---

## Design Principles

Every change should pass these:

1. **Would a normal user understand this setting?** If not, hardcode it or remove it.
2. **Does this serve the WhisperClick layout?** Toolbar, action bar, number row, keyboard. If it doesn't fit, it doesn't ship.
3. **Is this robust or a patch?** If it's a patch, document it and plan the real version.
4. **Does this break Classic?** WhisperClick changes shouldn't break users who stay in Classic settings.
