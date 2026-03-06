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
| WhisperClick / Classic settings split | Removing | Being replaced with flat list (Phase 1) |
| Clipboard settings screen | Removing | Being merged back into Keys & Feedback (Phase 1) |
| Quick Text toolbar key | Done (patchy) | Tap inserts default, long-hold shows popup |
| Voice AI settings | Done | WhisperFlow integration |
| Theme & Size settings | Done | Own screen under WhisperClick |
| Number row | Done (HeliBoard stock) | Toggle in Preferences |

---

## Settings Architecture

> Full details in `SETTINGS_RESTRUCTURE.md`. Summary here.

### Philosophy

1. If a normal user wouldn't understand it, move it to Advanced — not delete.
2. If we picked a look, own it. Don't surface settings that break the design.
3. Main settings = things people actually change. Niche = collapsible `▶ Advanced` per screen.
4. Don't disable — relocate. No "Managed by WhisperClick" patterns.
5. Fewer screens, fewer items per screen.
6. Don't delete during dev — move to Advanced, cut before launch.
7. Each screen owns its niche settings in a collapsible section — not one giant Advanced junk drawer.

### Approach: Compact + Collapsible Advanced

No WhisperClick/Classic split. Flat list like original HeliBoard. Each screen shows only
what normal users change, with niche settings in a collapsible `▶ Advanced` section at
the bottom. Two fragmented screens (Action Bar, Clipboard) deleted — settings merged back.

### Main Screen (11 entries, flat)

| Screen | Main | Advanced | Notes |
|--------|------|----------|-------|
| **Voice & AI** | 10 | — | NEW — only addition over original HeliBoard |
| **Appearance** | 9 | 16 | Compacted — most-used settings up front |
| **Toolbar** | 5 | — | On/off toggles for toolbar & action bar |
| **Typing & Autocorrect** | 14 | 12 | Compacted from ~26 |
| **Keys & Feedback** | 12 | 13 | Clipboard + number row restored, compacted |
| **(Swipe Typing)** | 3 | 4 | Conditional |
| **Languages & Layouts** | — | — | Unchanged |
| **Secondary Layouts** | — | — | Unchanged |
| **Dictionaries** | — | — | Unchanged |
| **Advanced** | ~20 | — | System-level only, NOT a junk drawer |
| **About** | — | — | Unchanged |

### Toolbar Screen (simplified)

```
├── [Toolbar]
│   ├── Show toolbar [on/off switch]
│   └── (Select toolbar keys)
├── [Action Bar]
│   ├── Show action bar [on/off switch]
│   └── (Select action bar keys)
└── Custom button actions
```

Same pattern for both rows. On/off master toggle, pick your keys. One shared key code customizer.

### Hardcoded Behavior (no user toggles)

These stay in code with their defaults but are not shown in settings:

| Behavior | Value | Why |
|----------|-------|-----|
| Toolbar mode (when on) | TOOLBAR_KEYS | Only mode we use |
| Auto-show toolbar | On | Toolbar visible when not typing |
| Auto-hide toolbar | On | Suggestions replace toolbar when typing |
| Pinned toolbar keys | Disabled | No pinned keys — just toolbar + action bar |
| Quick pin | Off | No long-press-to-pin |
| Variable toolbar direction | On (default) | RTL support automatic, no toggle needed |
| Toolbar hiding global | Off | Not relevant with on/off switch |

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

### Phase 0 — Audit & Stabilize (DONE)
- ~~Fix settings gray screen~~
- ~~Fix QUICK_TEXT default-on migration~~
- ~~Build disabled preference UI pattern~~
- ~~Update documentation~~

### Phase 1 — Settings Restructure + Quick Text Hardening
- Create `ExpandableSection` composable for collapsible `▶ Advanced` per screen
- Compact each screen: main settings up front, niche in collapsible advanced
- Toolbar: on/off switches for toolbar + action bar, shared key codes
- Delete Action Bar screen, Clipboard screen
- Move number row, clipboard history, quick text back to Keys & Feedback
- Harden Quick Text during move: snippet limits, validation, reordering
- Fix listener cast workaround (SuggestionStripView.kt — drive-by)
- Remove WhisperClick/Classic categories from main screen
- Add PREF_SHOW_ACTION_BAR pref + wire into getActionRow()
- Rename 22 setting labels in strings.xml for clarity (see `SETTINGS_RESTRUCTURE.md` Label Renames table)
- Full checklist in `SETTINGS_RESTRUCTURE.md`

### Phase 2 — Feature Polish
- Quick Text: proper popup rewrite, haptic feedback
- Voice input reliability
- Wire Rewrite toolbar key to WhisperClick rewrite engine
- Build rewrite menu panel (like clipboard history)
- Text selection + replace-in-place

### Phase 3 — Testing & Security
- Manual test plan for every feature
- Edge cases, performance, landscape, RTL, accessibility
- Audit autofill/password manager support
- Authentication code handling
- Privacy audit

### Phase 4 — Branding & Production
- App identity (name, icon, about)
- Final QA + distribution

**Full implementation details:** See plan file or `QUICK_TEXT_TECH_DEBT.md` for technical debt specifics.

**Existing feature inventory:** 100+ settings across 10 screens. Complete mapping of every feature to its WhisperClick home is in the plan file.

---

## Design Principles

Every change should pass these:

1. **Would a normal user understand this setting?** If not, move it to Advanced — not delete.
2. **Does this serve the WhisperClick layout?** Toolbar, action bar, number row, keyboard. If it doesn't fit, it doesn't ship.
3. **Is this robust or a patch?** If it's a patch, document it and plan the real version.
4. **Main settings = things people actually change.** Everything else goes in collapsible Advanced.
5. **Don't delete useful settings during dev.** Move to Advanced, decide what to cut before launch. Settings for removed features (deprecated modes, unused toolbar states) stay in code with defaults but don't need UI placement.
6. **Fewer screens, fewer items per screen.** If a user feels overwhelmed, we failed.
