# WhisperClick — Complete Feature Registry

> Every custom feature built on top of HeliBoard, organized by category.
> Use this to audit, test, and track what's been shipped.

**Last updated:** 2026-03-05
**Total custom commits:** 62 (starting from `1b73f98b`)
**Repo:** `Zbrooklyn/heliboard-custom` branch `main`

---

## 1. Samsung Visual Theme

The entire keyboard is restyled to match Samsung One UI dark keyboard.

| Feature | Status | Key Commits | Files |
|---------|--------|-------------|-------|
| **AMOLED black background** (#000000) | Done | `874b9ee2`, `cc100de6` | colors.xml, Defaults.kt |
| **Key color** #333333 (Samsung match) | Done | `df934489`, `78a8be67` | colors.xml |
| **Two-tone key colors** (functional vs normal) | Done | `5987be06` | colors.xml |
| **Flat keys** — removed bottom bevel | Done | `874b9ee2` | config.xml drawables |
| **Unified corner radius** (8dp all keys) | Done | `874b9ee2` | config.xml |
| **Key spacing** — horizontal 0.8%p, vertical 2.0%p | Done | `a7bc890a`, `78a8be67` | config.xml |
| **Edge-to-edge padding** — 1.0%p sides | Done | `eddce79c` | config.xml |
| **Number row text sizing** — 62% ratio | Done | `5a8affc3`, `eddce79c` | config.xml |
| **Hint character mapping** — matches Samsung | Done | `eddce79c`, `5a8affc3` | symbols layout |
| **Action key accent** — dark gray on AMOLED | Done | `eddce79c` | colors |
| **Default theme = THEME_BLACK** | Done | `cc100de6` | Defaults.kt |
| **Soft Light theme** as default day theme | Done | `b3829173` | Defaults.kt |

**Test:** Settings → Appearance → Theme = Rounded, Colors = Black, Key borders = ON, Dark mode = ON

---

## 2. Toolbar & Suggestion Strip

| Feature | Status | Key Commits | Files |
|---------|--------|-------------|-------|
| **Single-row swap** — toolbar ↔ suggestions in place | Done | `7e87a939` | suggestions_strip.xml, SuggestionStripView.kt |
| **Even icon distribution** — weighted layout | Done | `3d210ec2` | suggestions_strip.xml, SuggestionStripView.kt |
| **Pill-shaped icon backgrounds** | Done | `9934620e` | SuggestionStripView.kt |
| **Toolbar background** — subtle dark gray | Done | `eddce79c` | suggestions_strip.xml |
| **Hide empty suggestion row** | Done | `733aa8b0` | SuggestionStripView.kt |
| **Circular toolbar icons** | Done | `24872bef` | SuggestionStripView.kt |
| **Rewrite button** on default toolbar | Done | `2f9aa74c` | ToolbarUtils.kt |
| **Mic button** on default toolbar | Done | `e365f6fc` | ToolbarUtils.kt |

**Test:** Open keyboard → toolbar should show evenly-spaced circular icons. Start typing → suggestions replace toolbar.

---

## 3. Samsung-Style Action Row

| Feature | Status | Key Commits | Files |
|---------|--------|-------------|-------|
| **Action row above number row** (← → select copy paste select_all ↑ ↓) | Done | `323ad1b3`, `1af4c314` | action_row layout JSON |
| **Edge-to-edge fill** (width:-1) | Done | `311274f0` | action_row layout |

**Test:** Open keyboard → action row should be visible above number row with 8 evenly-spaced keys.

---

## 4. Clipboard History

| Feature | Status | Key Commits | Files |
|---------|--------|-------------|-------|
| **3-column grid layout** | Done | `a7bc890a` | config.xml |
| **Card styling** — dark background, rounded | Done | `21c058bc` | clipboard_entry_background.xml |
| **Curated toolbar pills** | Done | `21c058bc` | SuggestionStripView.kt |

**Test:** Copy text → open clipboard (toolbar button) → should show 3-column grid with styled cards.

---

## 5. Feature Drawer (Samsung-style 4×3 grid)

| Feature | Status | Key Commits | Files |
|---------|--------|-------------|-------|
| **4×3 grid drawer** — opens from toolbar "..." key | Done | `24872bef` | FeatureDrawerView.kt (new), KeyboardSwitcher.java |
| **Back button** in drawer | Done | `56f26999` | FeatureDrawerView.kt |
| **Themed to match keyboard** | Done | `24872bef` | FeatureDrawerView.kt |

**Test:** Tap toolbar "..." icon → feature drawer should open with grid of keyboard actions. Tap any action → executes and closes drawer.

---

## 6. Keyboard Resize Mode

| Feature | Status | Key Commits | Files |
|---------|--------|-------------|-------|
| **Original resize handle** — thin pill drag bar | Done | `b06cc72b` | KeyboardSwitcher.java, main_keyboard_frame.xml |
| **Samsung-style resize overlay** — blue border, center drag circle, edge tabs | Done | `4caa5b3e` | resize_mode_overlay.xml (new), KeyboardSwitcher.java |
| **Reset button** — restore 1.0x default | Done | `4caa5b3e` | KeyboardSwitcher.java |
| **Done button** — commit resize and rebuild | Done | `4caa5b3e` | KeyboardSwitcher.java |
| **Keyboard dimming** during resize (40% alpha) | Done | `4caa5b3e` | KeyboardSwitcher.java |
| **4-direction arrow icon** (vector drawable) | Done | `4caa5b3e` | ic_resize_drag.xml (new) |
| **Height scale persistence** (portrait/landscape) | Done | `b06cc72b` | Settings.java |

**Test:** Tap resize toolbar key → blue border overlay, drag handle, Reset/Done buttons over dimmed keyboard. Drag up/down to resize. Tap Done to commit. Tap Reset for default.

---

## 7. Voice Input (Whisper.cpp)

| Feature | Status | Key Commits | Files |
|---------|--------|-------------|-------|
| **Whisper.cpp integration** — on-device speech-to-text | Done | `ee3ae826` | whisper module, VoiceInputManager.kt |
| **Voice input mode view** — full-screen mic + status | Done | `24872bef` | VoiceInputModeView.kt (new) |
| **Model picker** — select Whisper model size | Done | `ce01da74` | VoiceAIScreen.kt |
| **Large-v3-turbo model** added | Done | `0f89963c` | model list |
| **Recording status bar** — tappable to stop | Done | `e223e324` | VoiceInputModeView.kt |
| **Mic button in TYPE_NULL fields** (Termux) | Done | `22964d45` | LatinIME.java |
| **Auto-capitalize** after voice input | Done | `e365f6fc` | VoiceInputManager.kt |
| **Voice benchmark button** | Done | `4da75ab2` | VoiceAIScreen.kt |
| **Copy button on benchmark results** | Done | `c94edf30` | VoiceAIScreen.kt |

**Test:** Tap mic icon → voice mode with large microphone. Speak → text appears. Settings → Voice & AI → model picker, benchmark button.

---

## 8. Google STT (Cloud Speech-to-Text)

| Feature | Status | Key Commits | Files |
|---------|--------|-------------|-------|
| **Google STT client** — cloud speech recognition | Done | `b06cc72b` | GoogleSttClient.kt |
| **API key validation** — test button with error details | Done | `7a63ffe6` | GoogleSttClient.kt, VoiceAIScreen.kt |
| **Gemini API key validation** (query param auth) | Done | `7a63ffe6` | VoiceAIScreen.kt |

**Test:** Settings → Voice & AI → enter Google STT API key → tap Test → should validate and show status.

---

## 9. AI Text Rewrite

| Feature | Status | Key Commits | Files |
|---------|--------|-------------|-------|
| **AI rewrite helper** — rewrite selected text via API | Done | `2f9aa74c`, `b06cc72b` | RewriteHelper.kt |
| **Rewrite toolbar button** | Done | `2f9aa74c` | ToolbarUtils.kt |

**Test:** Select text in any app → tap Rewrite toolbar button → text should be rewritten via configured AI API.

---

## 10. Settings & UX

| Feature | Status | Key Commits | Files |
|---------|--------|-------------|-------|
| **Rebrand: HeliBoard → WhisperClick** | Done | `34b6d87c` | strings.xml (all user-facing) |
| **Voice & AI settings screen** | Done | `ce01da74` | VoiceAIScreen.kt |
| **API key fields** — inline with password masking + eye toggle | Done | `9e28e336` | VoiceAIScreen.kt |
| **API Keys section** reorganized to top | Done | `202a0d04` | VoiceAIScreen.kt |
| **Factory defaults / reset layout** | Done | `d6e64651`, `e9d876d3` | Settings.java |
| **Keyboard height presets** (Compact/Normal/Large/Custom) | Done | `b06cc72b` | KeyboardHeightPreference.kt |
| **Haptic feedback improvements** | Done | `68335f36` | settings |
| **Share button** | Done | `68335f36` | settings |
| **API call counter** | Done | `e365f6fc` | settings |
| **Settings restructure** | Planned | — | Restore HeliBoard structure, simplify toolbar (see SETTINGS_RESTRUCTURE.md) |

**Test:** Open settings → flat list (no WhisperClick/Classic split), Voice & AI with API key fields (masked), Toolbar with on/off toggles for toolbar + action bar.

**Note:** Previous settings overhaul (Batch A) introduced fragmentation — Action Bar screen (1 setting), Clipboard screen (4 settings), orphan Number Row switch, WhisperClick/Classic categories. These are being undone in the settings restructure. See `SETTINGS_AUDIT.md` for full analysis.

---

## 11. CI/CD

| Feature | Status | Key Commits | Files |
|---------|--------|-------------|-------|
| **GitHub Actions: debug APK on push** | Done | `b4b369b7`, `615afddb` | .github/workflows/ |
| **Unit tests** — voice, WavEncoder, CI on push | Done | `4da75ab2`, `a95654ad` | test files |
| **All 6 CI tests passing** | Done | `24872bef` | various fixes |

**Test:** Push to main → GitHub Actions builds APK → download artifact.

---

## 12. Bug Fixes (Standalone)

| Fix | Commit | Details |
|-----|--------|---------|
| Java→Kotlin lambda interop (`Unit.INSTANCE`) | `77e5eb06` | FeatureDrawerView callbacks |
| Suspend/coroutine compilation errors | `5171f70c` | VoiceInputManager |
| `withContext` import restored | `5ef87423` | VoiceInputManager |
| `@JvmStatic` on `isModelDownloaded` | `512b91d9` | Whisper Java interop |
| CMakeLists.txt hardening | `e2f39de5` | whisper native build |
| Gradle matchingFallbacks | `d0ee30d8` | whisper dependency |
| CI icon tint + config cache | `83f3bebe` | build fixes |
| Lambda return type + git-tag fallback | `1a8200e6` | CI fixes |
| Guard voice init for Robolectric | `7454e21c` | test compat |
| WavEncoderTest empty input | `a95654ad` | test fix |

---

## Quick Audit Checklist

Use this to verify all features are working after a build:

- [ ] **Theme:** AMOLED black, #333333 keys, 8dp radius, no bevel, 0.8%p/2.0%p gaps
- [ ] **Toolbar:** Circular icons, even spacing, swaps with suggestions, mic + rewrite buttons
- [ ] **Action row:** 8 keys above number row, edge-to-edge
- [ ] **Number row:** 62% text ratio, hint chars match Samsung
- [ ] **Clipboard:** 3-column grid, styled cards
- [ ] **Feature drawer:** Opens from toolbar, 4×3 grid, back button, themed
- [ ] **Resize mode:** Blue border overlay, drag handle, Reset/Done buttons, dimmed keyboard
- [ ] **Voice (Whisper):** Mic → voice mode → speech transcribed → auto-capitalized
- [ ] **Voice (Google STT):** API key → test validates → cloud transcription works
- [ ] **AI Rewrite:** Select text → rewrite button → text rewritten
- [ ] **Settings:** WhisperClick branding, Voice & AI screen, API key fields masked, height presets
- [ ] **CI:** Push triggers APK build, all tests pass
- [ ] **Persistence:** Height scale, theme, API keys survive restart
- [ ] **Landscape:** Separate height scale, layout adapts
- [ ] **One-handed + resize:** Both modes work together

---

## Files Changed (Summary)

See `GAP_ANALYSIS.md` Section 10 for Samsung theme file details.
See individual commit messages for full file lists.

Key new files created by this fork:
- `FeatureDrawerView.kt` — Feature drawer grid view
- `VoiceInputModeView.kt` — Full-screen voice input UI
- `GoogleSttClient.kt` — Google Cloud STT client
- `RewriteHelper.kt` — AI text rewrite helper
- `VoiceAIScreen.kt` — Voice & AI settings screen
- `resize_mode_overlay.xml` — Samsung-style resize overlay
- `ic_resize_drag.xml` — 4-direction arrow icon
- `.github/workflows/` — CI pipeline
