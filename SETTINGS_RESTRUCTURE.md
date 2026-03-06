# Settings Restructure — Final Plan

> Supersedes all previous Version A / Version B proposals.
> Based on full audit of original HeliBoard layout vs current WhisperClick state.
> See `SETTINGS_AUDIT.md` for the complete inventory and analysis.

---

## Settings Philosophy

1. **If a normal user wouldn't understand it, move it to Advanced.** Not delete — move.
2. **If we picked a look, own it.** Don't surface settings that break the design.
3. **Main settings = things people actually change.** Everything else behind a collapsible "Advanced" section per screen.
4. **Don't disable — relocate.** No grayed-out "Managed by WhisperClick" patterns.
5. **Fewer screens, fewer items per screen.** If a user feels overwhelmed, we failed.
6. **Don't delete useful settings during dev.** Move to Advanced, decide what to cut before launch. Settings for removed features (deprecated modes, unused toolbar states) stay in code with defaults but don't need UI placement.
7. **Each screen owns its niche settings.** Collapsible "Advanced" section at the bottom of each screen — not one giant Advanced junk drawer.

---

## Approach: Compact + Collapsible Advanced

The original HeliBoard settings were well-organized. We broke them by fragmenting
screens, orphaning settings, and creating an artificial WhisperClick/Classic split.

**Fix:** Restore the flat list, delete fragmented screens (Action Bar, Clipboard),
compact each screen to show only what normal users change, and put niche settings
in a collapsible `▶ Advanced` section per screen.

---

## Final Layout

```
Main Screen (flat list, no categories)
├── Voice & AI →              10 settings
├── Appearance →               9 + collapsible advanced
├── Toolbar →                  5 settings
├── Typing & Autocorrect →    14 + collapsible advanced
├── Keys & Feedback →         12 + collapsible advanced
├── (Swipe Typing) →           3 + collapsible advanced (conditional)
├── Languages & Layouts →     unchanged
├── Secondary Layouts →       unchanged
├── Dictionaries →            unchanged
├── Advanced →                system-level only (~20)
└── About →                   unchanged
```

Total: 11 entries. Same as original HeliBoard + Voice & AI.

---

## Screen Details

### Voice & AI (10 settings — no changes needed)
```
├── [API Keys]
│   ├── OpenAI API key
│   ├── Gemini API key
│   └── Test API key
├── [Voice Input]
│   ├── STT mode
│   └── Active model
├── [AI Rewrite]
│   ├── AI provider
│   └── Default rewrite style
├── [Behavior]
│   └── Voice haptic feedback
└── [Diagnostics]
    ├── Voice benchmark
    └── Activity log
```

### Appearance (9 main + collapsible advanced)
```
├── Colors
├── (Auto day/night mode)
├── (Colors night)
├── Keyboard height
├── Space below keyboard
├── Font scale
├── Space bar label
├── Emoji skin tone
├── Reset layout
└── ▶ Advanced
    ├── Theme style
    ├── Icon style
    ├── Customize icons
    ├── Key borders
    ├── (Reduce gaps between keys)
    ├── Tint phone nav bar
    ├── Background image
    ├── Background image (landscape)
    ├── Enable split keyboard
    ├── Enable split keyboard (landscape)
    ├── (Split keyboard gap size)
    ├── Space on sides
    ├── Custom font
    ├── Custom emoji font
    ├── Emoji font scale
    └── (Fit emoji to key size)
```

### Toolbar (5 settings — already compact, no collapsible needed)
```
├── [Toolbar]
│   ├── Show toolbar [switch]          ← maps to PREF_TOOLBAR_MODE (on=TOOLBAR_KEYS, off=HIDDEN)
│   └── (Select toolbar keys)          ← only if toolbar on
├── [Action Bar]
│   ├── Show action bar [switch]       ← NEW: PREF_SHOW_ACTION_BAR (default: true)
│   └── (Select action bar keys)       ← only if action bar on
└── Custom button actions               ← shared, covers both rows
```

**Hardcoded toolbar behavior (not shown in settings):**
- Toolbar mode (when on) = TOOLBAR_KEYS
- Auto-show toolbar = On
- Auto-hide toolbar = On
- Pinned toolbar keys = Disabled
- Quick pin = Off
- Variable toolbar direction = On (default)
- Toolbar hiding global = Off

### Typing & Autocorrect (14 main + collapsible advanced)
```
├── Edit personal dictionary →
├── [Corrections]
│   ├── Auto-correction
│   ├── Undo autocorrect on backspace
│   ├── Auto-capitalization
│   └── Block offensive words
├── [Space]
│   ├── Double-space period
│   └── Autospace after punctuation
├── [Suggestions]
│   ├── Show correction suggestions
│   ├── Suggest emojis
│   ├── Inline emoji search
│   ├── Use personalized dictionaries
│   ├── Suggest clipboard content
│   ├── Use contacts
│   └── App-based suggestions
└── ▶ Advanced
    ├── Aggressive auto-correction
    ├── Expand text shortcuts
    ├── Correction threshold
    ├── Autospace after suggestion
    ├── Autospace before/after gesture
    ├── Shift removes autospace
    ├── Always show suggestions
    ├── ...except web text fields
    ├── Always use middle suggestion
    ├── Next-word suggestions
    ├── Suggest punctuation
    └── Add to personal dictionary
```

### Keys & Feedback (12 main + collapsible advanced)
```
├── [Input]
│   ├── Show key hints
│   ├── Popup on keypress
│   ├── Vibrate on keypress
│   ├── (Vibration duration)
│   ├── Sound on keypress
│   └── (Sound volume)
├── [Keys]
│   ├── Number row
│   ├── Language switch key
│   └── Emoji key
├── [Clipboard]
│   ├── Enable clipboard history
│   ├── (History retention time)
│   └── Quick text snippets
└── ▶ Advanced
    ├── Hint characters from
    ├── Long-press popup order
    ├── Show shift/delete hints
    ├── Show .com/.org keys
    ├── Vibrate in DND mode
    ├── Remember language per app
    ├── Show emoji descriptions
    ├── Language-specific numbers
    ├── Number row hints
    ├── Number row in symbols
    ├── Language switch key behavior
    ├── Remove redundant popups
    └── Show pinned items on top
```

### Swipe Typing (3 main + collapsible advanced, conditional)
```
├── Enable gesture typing
├── Gesture preview trail
├── Floating preview text
└── ▶ Advanced
    ├── Animated floating preview
    ├── Space-aware gestures
    ├── Pause before next gesture
    └── Trail fadeout duration
```

### Advanced (system-level settings only — NOT a junk drawer)
```
├── Force incognito mode
├── Key long press delay
├── Horizontal spacebar swipe
├── Vertical spacebar swipe
├── (Language swipe distance)
├── Delete swipe
├── Long press space to change language
├── Long press symbols for numpad
├── Emoji physical key (Alt)
├── (Show setup wizard icon)
├── Switch to ABC after symbol+space
├── Switch to ABC after numpad+space
├── Switch to ABC after emoji
├── Switch to ABC after clipboard
├── Customize currencies
├── Extra long-press characters
├── Timestamp format
├── Backup and restore
├── (Debug settings →)
└── [Experimental]
    ├── Emoji compatibility level
    ├── URL detection
    └── (Load gesture library)
```

### Languages & Layouts, Secondary Layouts, Dictionaries, About
No changes from current state.

---

## Label Renames

Settings renamed for clarity. Old labels were developer jargon or ambiguous.

| Screen | Old Label | New Label |
|--------|-----------|-----------|
| Appearance | Bottom padding | Space below keyboard |
| Appearance | Space bar text | Space bar label |
| Appearance (Adv) | Style | Theme style |
| Appearance (Adv) | Narrow key gaps | Reduce gaps between keys |
| Appearance (Adv) | Color navigation bar | Tint phone nav bar |
| Appearance (Adv) | Split spacer scale | Split keyboard gap size |
| Appearance (Adv) | Side padding | Space on sides |
| Appearance (Adv) | Emoji key fit | Fit emoji to key size |
| Toolbar | Customize key codes | Custom button actions |
| Typing | Use apps | App-based suggestions |
| Typing (Adv) | More auto-correction | Aggressive auto-correction |
| Typing (Adv) | Autocorrect shortcuts | Expand text shortcuts |
| Typing (Adv) | Auto-correction confidence | Correction threshold |
| Typing (Adv) | Center suggestion to enter | Always use middle suggestion (already in strings.xml) |
| Typing (Adv) | Bigram predictions | Next-word suggestions (already in strings.xml) |
| Keys (Adv) | Hint source | Hint characters from |
| Keys (Adv) | Popup order | Long-press popup order |
| Keys (Adv) | Show functional hints | Show shift/delete hints |
| Keys (Adv) | Show TLD popup keys | Show .com/.org keys |
| Keys (Adv) | Save subtype per app | Remember language per app (already in strings.xml) |
| Keys (Adv) | Localized number row | Language-specific numbers |
| Swipe (Adv) | Floating preview dynamic | Animated floating preview |
| Swipe (Adv) | Fast typing cooldown | Pause before next gesture |
| Advanced | More popup keys | Extra long-press characters |
| Advanced | Emoji max SDK | Emoji compatibility level |

---

## Implementation Checklist

### Collapsible pattern (new UI component)
- [ ] Create `ExpandableSection` composable — collapsed by default, tap to expand
- [ ] Use in Appearance, Typing & Autocorrect, Keys & Feedback, Swipe Typing screens

### New prefs
- [ ] Add `PREF_SHOW_ACTION_BAR` to `Settings.java` and `Defaults.kt` (default: `true`)
- [ ] Wire `PREF_SHOW_ACTION_BAR` into `KeyboardParser.getActionRow()` — if false, return empty
- [ ] Wire `PREF_TOOLBAR_MODE` show/hide switch — on sets TOOLBAR_KEYS, off sets HIDDEN
- [ ] Add `PREF_SHOW_ACTION_BAR` string resources

### Toolbar screen rewrite
- [ ] Rewrite `ToolbarScreen.kt` — two sections (Toolbar, Action Bar) + shared key codes
- [ ] Rewrite `createToolbarSettings()` — 5 settings total
- [ ] Delete `ActionBarScreen.kt`
- [ ] Remove `createActionBarSettings()` from `SettingsContainer.kt`
- [ ] Remove Action Bar route from `SettingsNavHost.kt`

### Compact screens with collapsible advanced
- [ ] `AppearanceScreen.kt` — 9 main settings, rest into `▶ Advanced`
- [ ] `TextCorrectionScreen.kt` — 14 main settings, rest into `▶ Advanced`
- [ ] `PreferencesScreen.kt` — 12 main settings (restore clipboard, number row, quick text), rest into `▶ Advanced`
- [ ] `GestureTypingScreen.kt` — 3 main settings, rest into `▶ Advanced`

### Restore original groupings
- [ ] Move clipboard history + quick text back into `PreferencesScreen.kt` (Keys & Feedback)
- [ ] Move number row back into `PreferencesScreen.kt` Keys section
- [ ] Delete `ClipboardSettingsScreen.kt`
- [ ] Remove Clipboard route from `SettingsNavHost.kt`

### Main screen cleanup
- [ ] Remove WhisperClick/Classic category headers
- [ ] Remove Action Bar, Clipboard, Number Row entries
- [ ] Flat list: Voice & AI, Appearance, Toolbar, Typing, Keys & Feedback, ...
- [ ] Remove `onClickActionBar`, `onClickClipboard` params

### Label renames (strings.xml)
- [ ] Rename 25 setting labels for clarity (see Label Renames table above)
- [ ] Update `strings.xml` with new user-facing labels
- [ ] Keep pref key names unchanged (only UI labels change)

### Cleanup
- [ ] Remove unused imports across all modified files
- [ ] Remove `managed_by_whisperclick` string if no longer referenced
- [ ] Verify settings search still works for all moved prefs

---

## Files Changed

| File | Change |
|------|--------|
| **NEW** `ExpandableSection.kt` | Collapsible "▶ Advanced" composable |
| `MainSettingsScreen.kt` | Remove categories, remove Action Bar/Clipboard/Number Row entries, flat list |
| `ToolbarScreen.kt` | Rewrite: 2 sections (Toolbar + Action Bar) + key codes |
| `AppearanceScreen.kt` | Compact to 9 main + collapsible advanced |
| `TextCorrectionScreen.kt` | Compact to 14 main + collapsible advanced |
| `PreferencesScreen.kt` | Restore clipboard/number row/quick text, compact to 12 main + collapsible advanced |
| `GestureTypingScreen.kt` | Compact to 3 main + collapsible advanced |
| `AdvancedScreen.kt` | Remove Customization section (settings moved to per-screen advanced) |
| `ActionBarScreen.kt` | **DELETE** |
| `ClipboardSettingsScreen.kt` | **DELETE** |
| `SettingsNavHost.kt` | Remove Action Bar + Clipboard routes |
| `SettingsContainer.kt` | Remove Action Bar + Clipboard registrations, update Toolbar + Preferences |
| `Settings.java` | Add `PREF_SHOW_ACTION_BAR` |
| `Defaults.kt` | Add `PREF_SHOW_ACTION_BAR = true` |
| `KeyboardParser.kt` | Check `PREF_SHOW_ACTION_BAR` in `getActionRow()` |
| `strings.xml` | Add show_action_bar string, possibly remove managed_by_whisperclick |
