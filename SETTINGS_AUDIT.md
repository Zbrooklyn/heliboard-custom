# WhisperClick Settings Audit

Complete inventory of every setting, what the phone shows now, the original HeliBoard layout,
what we added, and a proposed reorganization.

---

## Part 1: Every Single Setting (Human-Readable)

### Voice & AI (NEW — WhisperClick addition)
| Setting | Type | What it does |
|---------|------|--------------|
| OpenAI API key | Text input | API key for cloud voice & AI rewrite (OpenAI) |
| Gemini API key | Text input | API key for cloud voice & AI rewrite (Google) |
| Test API key | Button | Validates entered API keys against their services |
| STT mode | List (local/cloud/google) | Speech-to-text engine: on-device Whisper, cloud API, or Google STT |
| Active model | List | Which Whisper model to use for local STT |
| AI provider | List (Gemini/OpenAI) | Which cloud provider for AI rewrite |
| Default rewrite style | List | Default style when using AI rewrite (clean, professional, casual, etc.) |
| Voice haptic feedback | List (off/light/medium/strong) | Vibration intensity during voice input |
| Voice benchmark | Button | Runs performance benchmark on Whisper model |
| Activity log | Button | Shows log of voice/AI activity this session |

### Toolbar
| Setting | Type | What it does |
|---------|------|--------------|
| Toolbar mode | List (hidden/toolbar keys/suggestion strip/expandable) | What the toolbar row shows |
| Hide clipboard and emoji toolbars too | Switch | When toolbar is hidden, also hide clipboard/emoji toolbars |
| Select toolbar keys | Reorder+toggle | Which keys appear in toolbar, and their order |
| Select pinned toolbar keys | Reorder+toggle | Which keys stay visible when toolbar is collapsed |
| Select clipboard toolbar keys | Reorder+toggle | Which keys appear in clipboard/emoji action row |
| Customize toolbar key codes | Dialog | Assign custom keycodes to toolbar keys |
| Pin toolbar key on long press | Switch | Long-pressing a toolbar key pins it |
| Auto show toolbar | Switch | Toolbar auto-expands in certain contexts |
| Auto hide toolbar | Switch | Toolbar auto-collapses after use |
| Variable toolbar direction | Switch | Toolbar scrolls in the direction of the swipe |

### Appearance
| Setting | Type | What it does |
|---------|------|--------------|
| Style | List | UI style (Material/Holo/Rounded) |
| Icon style | List | Icon set style |
| Customize icons | Dialog | Replace individual key icons |
| Colors | Picker | Color theme for the keyboard |
| Auto day/night mode | Switch | Switch themes by time of day (API 28+) |
| Colors (night) | Picker | Color theme for night mode |
| Key borders | Switch | Show borders around keys |
| Narrow key gaps | Switch | Reduce gap between keys (only if borders on) |
| Color navigation bar | Switch | Tint the system nav bar to match keyboard |
| Set background image | File picker | Custom keyboard background |
| Set background image (landscape) | File picker | Custom background for landscape |
| Enable split keyboard | Switch | Split keyboard in portrait |
| Enable split keyboard (landscape) | Switch | Split keyboard in landscape |
| Split spacer scale | Slider | Width of the gap in split keyboard |
| Keyboard height scale | Slider | Overall keyboard height |
| Bottom padding scale | Slider | Padding below the keyboard |
| Side padding scale | Slider | Padding on sides |
| Space bar text | Text input | Custom text on the spacebar |
| Custom font | File picker | Custom font for key labels |
| Keyboard font scale | Slider | Size of key label text |
| Custom emoji font | File picker | Custom font for emoji |
| Emoji font scale | Slider | Size of emoji |
| Emoji key fit | Switch | Fit emoji to key bounds (only if scale != 1) |
| Emoji skin tone | List | Default emoji skin tone (only if emoji SDK >= 24) |

### Keys & Feedback (originally "Preferences")
| Setting | Type | What it does |
|---------|------|--------------|
| Show key hints | Switch | Show hint characters on keys |
| Hint source (popup key labels order) | Reorder+toggle | Which hint sources to show and priority |
| Popup order | Reorder+toggle | Order of popup keys |
| Show functional hints | Switch | Show hints on functional keys |
| Show TLD popup keys | Switch | Show .com/.net etc. on URL fields |
| Popup on keypress | Switch | Show popup preview when pressing a key |
| Vibrate on keypress | Switch | Haptic feedback |
| Vibration duration | Slider | How long the vibration lasts |
| Vibrate in DND mode | Switch | Vibrate even in Do Not Disturb |
| Sound on keypress | Switch | Audible click feedback |
| Keypress sound volume | Slider | Volume of the click |
| Save subtype per app | Switch | Remember language/layout per app |
| Show emoji descriptions | Switch | Show text descriptions for emoji |
| Number row | Switch | Always show a number row |
| Localized number row | Switch | Use locale-specific digits |
| Number row hints | Switch | Show hints on number row keys |
| Number row in symbols | Switch | Show number row in symbol layouts |
| Language switch key | Switch | Show the globe/language key |
| Language switch key behavior | List | What the language key does |
| Emoji key | Switch | Show the emoji key |
| Remove redundant popups | Switch | Hide popup keys that duplicate visible keys |

### Clipboard History (originally inside Preferences)
| Setting | Type | What it does |
|---------|------|--------------|
| Enable clipboard history | Switch | Keep clipboard history |
| History retention time | Slider/List | How long to keep clips |
| Show pinned items on top | Switch | Pin important clips to top |

### Quick Text (NEW — WhisperClick addition)
| Setting | Type | What it does |
|---------|------|--------------|
| Quick text snippets | Dialog | Manage saved text snippets for quick insertion |

### Typing & Autocorrect (originally "Text Correction")
| Setting | Type | What it does |
|---------|------|--------------|
| Edit personal dictionary | Link | Opens personal dictionary editor |
| Block offensive words | Switch | Filter offensive word suggestions |
| Auto-correction | Switch | Enable autocorrect |
| More auto-correction | Switch | More aggressive autocorrection |
| Autocorrect shortcuts | Switch | Autocorrect on shortcut entries |
| Auto-correction confidence | List (modest/aggressive/very aggressive) | Threshold for autocorrect |
| Undo autocorrect on backspace | Switch | Backspace reverts the last autocorrect |
| Auto-capitalization | Switch | Capitalize after sentence end |
| Double-space period | Switch | Tap space twice for ". " |
| Autospace after punctuation | Switch | Auto-insert space after punctuation |
| Autospace after suggestion | Switch | Auto-insert space after picking a suggestion |
| Autospace before gesture typing | Switch | Insert space before a swipe word |
| Autospace after gesture typing | Switch | Insert space after a swipe word |
| Shift removes autospace | Switch | Shift key undoes an auto-inserted space |
| Show correction suggestions | Switch | Show the suggestion strip |
| Always show suggestions | Switch | Show suggestions even in password fields etc. |
| ...except web text fields | Switch | Exception for web input fields |
| Center suggestion to enter | Switch | Center suggestion aligned with enter key |
| Suggest emojis | Switch | Include emoji in suggestions |
| Inline emoji search | Switch | Search emoji by name in suggestions |
| Use personalized dictionaries | Switch | Learn from what you type |
| Bigram predictions | Switch | Use word pairs for better predictions |
| Suggest punctuation | Switch | Suggest punctuation marks |
| Suggest clipboard content | Switch | Suggest recently copied text |
| Use contacts | Switch | Suggest contact names |
| Use apps | Switch | Suggest app names |
| Add to personal dictionary | Switch | Auto-add learned words to personal dict |

### Swipe Typing (originally "Gesture Typing")
| Setting | Type | What it does |
|---------|------|--------------|
| Enable gesture typing | Switch | Master toggle for swipe input |
| Gesture preview trail | Switch | Show the swipe trail |
| Floating preview (static) | Switch | Show word preview while swiping |
| Floating preview (dynamic) | Switch | Animate the floating preview |
| Space-aware gestures | Switch | Lift finger for space, continue swiping |
| Fast typing cooldown | Slider | Delay before next gesture is recognized |
| Trail fadeout duration | Slider | How fast the trail disappears |

### Advanced
| Setting | Type | What it does |
|---------|------|--------------|
| Force incognito mode | Switch | Never learn from typing |
| Key long press delay | Slider | Time before long-press triggers |
| Horizontal spacebar swipe | List | What horizontal swipe on spacebar does |
| Vertical spacebar swipe | List | What vertical swipe on spacebar does |
| Language swipe distance | Slider | How far to swipe to switch language |
| Delete swipe | Switch | Swipe left on delete to select & delete words |
| Long press space to change language | Switch | Long-press spacebar for language picker |
| Long press symbols for numpad | Switch | Long-press symbol key opens numpad |
| Emoji physical key (Alt) | Switch | Alt key on physical keyboard opens emoji |
| Show setup wizard icon | Switch | Show app icon in launcher (API < 29) |
| Switch to ABC after symbol+space | Switch | Return to alpha after typing symbol+space |
| Switch to ABC after numpad+space | Switch | Return to alpha after numpad+space |
| Switch to ABC after emoji | Switch | Return to alpha after emoji |
| Switch to ABC after clipboard | Switch | Return to alpha after clipboard |
| Customize currencies | Dialog | Set custom currency symbols |
| More popup keys | List | How many extra popup keys to show |
| Timestamp format | Text input | Custom format for timestamp insertion |
| Backup and restore | Button | Export/import all settings |
| Debug settings | Link | (conditional) Open debug screen |
| Enable split keyboard | Switch | (duplicate — also in Appearance) |
| Enable split keyboard (landscape) | Switch | (duplicate) |
| Split spacer scale | Slider | (duplicate) |
| Side padding scale | Slider | (duplicate) |
| Space bar text | Text input | (duplicate) |
| Custom font | File picker | (duplicate) |
| Custom emoji font | File picker | (duplicate) |
| Emoji font scale | Slider | (duplicate) |
| Emoji key fit | Switch | (duplicate) |
| Emoji skin tone | List | (duplicate) |
| Emoji max SDK | Slider | Max Android SDK for emoji support |
| URL detection | Switch | Detect URLs while typing |
| Load gesture library | File picker | Load a custom gesture typing library |

### Debug
| Setting | Type | What it does |
|---------|------|--------------|
| Show debug settings | Switch | Show debug screen in Advanced |
| Debug mode | Switch | Enable verbose logging |
| Show suggestion info | Switch | Show technical info about suggestions |
| Force non-distinct multitouch | Switch | Debug: treat all touches as same pointer |
| Sliding key input preview | Switch | Debug: show sliding key input trail |
| Dump dynamic dictionaries | Buttons | Export learned dictionary data |

---

## Part 2: What the Phone Shows Right Now

```
Main Screen
├── [WhisperClick]
│   ├── Voice & AI →
│   ├── Appearance →
│   ├── Toolbar & Actions →
│   ├── Action Bar →                    ← 1 setting inside
│   ├── Clipboard →                     ← 4 settings inside
│   └── Number row [switch]             ← orphan, no sub-screen
│
├── [Classic Settings]
│   ├── Languages & Layouts →
│   ├── Typing & Autocorrect →
│   ├── Swipe Typing →
│   ├── (Gesture Data) →               ← conditional
│   ├── Keys & Feedback →
│   ├── Secondary Layouts →
│   ├── Dictionaries →
│   ├── Advanced →
│   └── About →

Voice & AI (10 settings)
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

Appearance (12-16 settings, conditional)
├── [Theme]
│   ├── Style
│   ├── Icon style
│   ├── Customize icons
│   ├── Colors
│   ├── (Auto day/night mode)
│   ├── (Colors night)
│   ├── Color navigation bar
│   ├── (Key borders)
│   ├── (Narrow key gaps)
│   ├── Set background image
│   └── Set background image (landscape)
└── [Size]
    ├── Keyboard height scale
    ├── Bottom padding scale
    ├── Font scale
    └── Reset layout

Toolbar (2 settings)                    ← was 10, we disabled 8
├── Select toolbar keys
└── Customize toolbar key codes

Action Bar (1 setting)                  ← was inside Toolbar
└── Select clipboard toolbar keys

Clipboard (4 settings)                  ← was inside Preferences
├── Enable clipboard history
├── (History retention time)
├── (Show pinned items on top)
└── [Quick Text]
    └── Quick text snippets

Typing & Autocorrect (19-26 settings, conditional)
├── Edit personal dictionary →
├── [Corrections]
│   ├── Block offensive words
│   ├── Auto-correction
│   ├── (More auto-correction)
│   ├── (Autocorrect shortcuts)
│   ├── (Auto-correction confidence)
│   ├── (Undo autocorrect on backspace)
│   └── Auto-capitalization
├── [Space]
│   ├── Double-space period
│   ├── Autospace after punctuation
│   ├── Autospace after suggestion
│   ├── (Autospace before gesture typing)
│   ├── (Autospace after gesture typing)
│   └── Shift removes autospace
└── [Suggestions]
    ├── (Show correction suggestions)
    ├── (Always show suggestions)
    ├── (...except web text fields)
    ├── (Center suggestion to enter)
    ├── (Suggest emojis)
    ├── (Inline emoji search)
    ├── Use personalized dictionaries
    ├── Bigram predictions
    ├── Suggest punctuation
    ├── Suggest clipboard content
    ├── Use contacts
    ├── Use apps
    └── (Add to personal dictionary)

Keys & Feedback (14-22 settings, conditional)
├── [Input]
│   ├── Show key hints
│   ├── (Hint source)
│   ├── Popup order
│   ├── Show functional hints
│   ├── Show TLD popup keys
│   ├── Popup on keypress
│   ├── (Vibrate on keypress)
│   ├── (Vibration duration)
│   ├── (Vibrate in DND mode)
│   ├── Sound on keypress
│   ├── (Keypress sound volume)
│   ├── Save subtype per app
│   └── Show emoji descriptions
└── [Additional Keys]
    ├── (Localized number row)
    ├── (Number row hints)
    ├── (Number row in symbols)
    ├── Language switch key
    ├── Language switch key behavior
    ├── Emoji key
    └── Remove redundant popups

Swipe Typing (1-7 settings, conditional)
├── Enable gesture typing
├── (Gesture preview trail)
├── (Floating preview static)
├── (Floating preview dynamic)
├── (Space-aware gestures)
├── (Fast typing cooldown)
└── (Trail fadeout duration)

Advanced (18-24 settings, conditional)
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
├── More popup keys
├── Timestamp format
├── Backup and restore
├── (Debug settings →)
├── [Customization]
│   ├── Enable split keyboard
│   ├── Enable split keyboard (landscape)
│   ├── (Split spacer scale)
│   ├── Side padding scale
│   ├── Space bar text
│   ├── Custom font
│   ├── Custom emoji font
│   ├── Emoji font scale
│   ├── (Emoji key fit)
│   └── (Emoji skin tone)
├── [Experimental]
│   ├── Emoji max SDK
│   ├── URL detection
│   └── (Load gesture library)
```

---

## Part 3: Original HeliBoard Layout

```
Main Screen (flat list, no categories)
├── Languages & Layouts →
├── Preferences →
├── Appearance →
├── Toolbar →
├── (Gesture Typing) →
├── (Gesture Data) →
├── Text Correction →
├── Secondary Layouts →
├── Dictionaries →
├── Advanced →
└── About →

Toolbar (up to 10 settings, all conditionally shown based on toolbar mode)
├── Toolbar mode                             ← controls which settings appear below
├── (Hide clipboard/emoji toolbars too)      ← only if mode=hidden
├── (Select toolbar keys)                    ← only if mode=toolbar_keys or expandable
├── (Select pinned toolbar keys)             ← only if mode=expandable or suggestion_strip
├── (Select clipboard toolbar keys)          ← only if clipboard toolbar visible
├── (Customize toolbar key codes)            ← only if clipboard toolbar visible
├── (Pin toolbar key on long press)          ← only if mode=expandable
├── (Auto show toolbar)                      ← only if mode=expandable
├── (Auto hide toolbar)                      ← only if mode=expandable
└── (Variable toolbar direction)             ← only if mode!=hidden

Appearance (up to 24 settings)
├── [Theme]
│   ├── Style
│   ├── Icon style
│   ├── Customize icons
│   ├── Colors
│   ├── Key borders
│   ├── (Auto day/night mode)
│   ├── (Colors night)
│   ├── Color navigation bar
│   ├── Set background image
│   └── Set background image (landscape)
├── [Miscellaneous]                          ← note: "Miscellaneous", not "Customization"
│   ├── Enable split keyboard
│   ├── Enable split keyboard (landscape)
│   ├── (Split spacer scale)
│   ├── (Narrow key gaps)
│   ├── Keyboard height scale
│   ├── Bottom padding scale
│   ├── Side padding scale
│   ├── Space bar text
│   ├── Custom font
│   ├── Font scale
│   ├── Custom emoji font
│   ├── Emoji font scale
│   ├── (Emoji key fit)
│   └── (Emoji skin tone)

Preferences (up to 26 settings)
├── [Input]
│   ├── Show key hints
│   ├── (Hint source)
│   ├── Popup order
│   ├── Show functional hints
│   ├── Show TLD popup keys
│   ├── Popup on keypress
│   ├── (Vibrate on keypress)
│   ├── (Vibration duration)
│   ├── (Vibrate in DND mode)
│   ├── Sound on keypress
│   ├── (Keypress sound volume)
│   ├── Save subtype per app
│   └── Show emoji descriptions
├── [Additional Keys]
│   ├── NUMBER ROW                           ← lived here with its siblings
│   ├── (Localized number row)
│   ├── (Number row hints)
│   ├── (Number row in symbols)
│   ├── Language switch key
│   ├── Language switch key behavior
│   ├── Emoji key
│   └── Remove redundant popups
└── [Clipboard History]                      ← lived here, not its own screen
    ├── Enable clipboard history
    ├── (History retention time)
    └── (Show pinned items on top)

Text Correction (same as now, minus gesture-related conditions)
├── Edit personal dictionary →
├── [Corrections] ...
├── [Space] ...
└── [Suggestions] ...

Gesture Typing (same as now)

Advanced (18 settings, NO customization/size section)
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
├── More popup keys
├── Timestamp format
├── Backup and restore
├── (Debug settings →)
├── [Experimental]                           ← just 3 items, not 10+
│   ├── Emoji max SDK
│   ├── URL detection
│   └── (Load gesture library)
```

---

## Part 4: What We Added / Changed

### New settings (didn't exist in HeliBoard)
| Setting | Where we put it |
|---------|----------------|
| OpenAI API key | Voice & AI (new screen) |
| Gemini API key | Voice & AI |
| Test API key | Voice & AI |
| STT mode | Voice & AI |
| Active model | Voice & AI |
| AI provider | Voice & AI |
| Default rewrite style | Voice & AI |
| Voice haptic feedback | Voice & AI |
| Voice benchmark | Voice & AI |
| Activity log | Voice & AI |
| Quick text snippets | Clipboard (new screen) |

### Structural changes (moved/split/disabled existing settings)
| What we did | Original location | Current location | Problem |
|-------------|-------------------|------------------|---------|
| Created "WhisperClick" vs "Classic" categories on main screen | Flat list | Two categories | Artificial split — confuses more than helps |
| Disabled 7 of 10 toolbar settings | Toolbar (conditional visibility) | Toolbar (disabled, then removed) | Lost all toolbar configuration — mode, pinning, auto-show, etc. |
| Moved clipboard toolbar keys out of Toolbar | Toolbar screen | Action Bar (new screen, 1 setting) | Fragmented — not worth its own screen |
| Moved clipboard history out of Preferences | Preferences > Clipboard History section | Clipboard (new screen) | Was fine where it was — 3 settings don't need their own screen |
| Added Quick Text to Clipboard screen | N/A (new) | Clipboard > Quick Text | Makes sense if Clipboard stays its own screen |
| Pulled Number Row out of Preferences | Preferences > Additional Keys | Main screen (orphan switch) | Separated from its siblings (localized, hints, in-symbols) |
| Moved split keyboard, sizing, fonts from Appearance to Advanced | Appearance > Miscellaneous | Advanced > Customization | Made Advanced bloated, Appearance incomplete |
| Renamed "Preferences" to "Keys & Feedback" | "Preferences" | "Keys & Feedback" | Fine rename, but lost Clipboard History and Number Row |
| Renamed "Text Correction" to "Typing & Autocorrect" | "Text Correction" | "Typing & Autocorrect" | Fine rename |
| Renamed "Gesture Typing" to "Swipe Typing" | "Gesture Typing" | "Swipe Typing" | Fine rename |
| Renamed Appearance sections | "Theme" + "Miscellaneous" | "Theme" + "Size" | Lost split keyboard, sizing, fonts — they're in Advanced now |

### Summary of problems
1. **Action Bar screen has 1 setting** — was inside Toolbar
2. **Clipboard screen has 4 settings** — was inside Preferences
3. **Number Row is an orphan switch** on main screen — was inside Preferences > Additional Keys
4. **Toolbar lost 8 settings** — conditional visibility was replaced with disabling/removal
5. **Appearance lost half its settings** — split keyboard, sizing, fonts moved to Advanced
6. **Advanced gained 10+ settings** — now has a "Customization" section that doesn't belong
7. **Main screen has 15 entries** — original had 11, and the new ones are mostly thin wrappers
8. **WhisperClick vs Classic split** is confusing — users don't think in terms of "what's new"

---

## Part 5: Proposed New Layout

### Settings Philosophy

1. **If a normal user wouldn't understand it, move it to Advanced.** Not delete — move.
2. **If we picked a look, own it.** Don't surface settings that break the design.
3. **Main settings = things people actually change.** Everything else behind a collapsible "Advanced" section per screen.
4. **Don't disable — relocate.** No grayed-out "Managed by WhisperClick" patterns.
5. **Fewer screens, fewer items per screen.** If a user feels overwhelmed, we failed.
6. **Don't delete useful settings during dev.** Move to Advanced, decide what to cut before launch. Settings for removed features (deprecated modes, unused toolbar states) stay in code with defaults but don't need UI placement.
7. **Each screen owns its niche settings.** Collapsible "Advanced" section at the bottom of each screen — not one giant Advanced junk drawer.

---

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


Voice & AI (10 settings — no changes needed)
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


Appearance (9 main + collapsible advanced)
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


Toolbar (5 settings — already compact, no collapsible needed)
├── [Toolbar]
│   ├── Show toolbar [switch]
│   └── (Select toolbar keys)
├── [Action Bar]
│   ├── Show action bar [switch]
│   └── (Select action bar keys)
└── Custom button actions


Typing & Autocorrect (14 main + collapsible advanced)
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


Keys & Feedback (12 main + collapsible advanced)
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


Swipe Typing (3 main + collapsible advanced, conditional)
├── Enable gesture typing
├── Gesture preview trail
├── Floating preview text
└── ▶ Advanced
    ├── Animated floating preview
    ├── Space-aware gestures
    ├── Pause before next gesture
    └── Trail fadeout duration


Advanced (system-level settings only — NOT a junk drawer)
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

### What changes in the proposed layout

| Action | Effect |
|--------|--------|
| Delete Action Bar screen | Merge into Toolbar with same on/off + key picker pattern |
| Delete Clipboard screen | Move clipboard + quick text back into Keys & Feedback |
| Move Number Row back to Keys & Feedback | Reunite with siblings |
| Remove WhisperClick/Classic categories | Flat list |
| Simplify Toolbar to on/off toggles | Replace 4-option mode with show/hide switch |
| Compact every screen | Main settings = what normal users change. Niche = collapsible Advanced per screen |
| Advanced = system only | No more junk drawer — only system/behavior settings |
| Add PREF_SHOW_ACTION_BAR | New pref for action bar toggle |

### New prefs needed
| Pref | Type | Default | What it does |
|------|------|---------|--------------|
| `PREF_SHOW_ACTION_BAR` | Boolean | true | Master toggle for the action row above the keyboard |

### Toolbar mode mapping
- **Switch ON** → `PREF_TOOLBAR_MODE` = `TOOLBAR_KEYS`
- **Switch OFF** → `PREF_TOOLBAR_MODE` = `HIDDEN`

### Collapsible Advanced pattern
Each screen that has niche settings gets a `▶ Advanced` section at the bottom.
Collapsed by default. Tap to expand. This keeps screens clean without losing access.
Implementation: a simple `ExpandableSection` composable wrapping the niche `Setting` entries.

### Screens deleted: 2 (Action Bar, Clipboard)
### Main screen items: 11 (same as original HeliBoard)
### Nothing deleted from codebase — niche settings relocated, not removed
