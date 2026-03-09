# Changelog

All notable changes to WhisperClick will be documented in this file.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
Versioning: [Semantic Versioning](https://semver.org/spec/v2.0.0.html)

---

## [Unreleased]

---

## [1.0.0-beta] - 2026-03-09

### Added
- Whisper hallucination guard — RMS energy check + known phrase filter (local mode only)
- Audio focus management — pauses media, auto-stops on focus loss
- WakeLock during recording — prevents doze mode audio gaps
- Minimum recording duration — rejects < 0.5s recordings
- Privacy guard for cloud STT — blocks for password/incognito/no-learning fields
- EncryptedSharedPreferences for API keys (AES256-GCM)
- Google STT language control — passes keyboard locale, offline preference
- Whisper language parameter — JNI accepts language string, defaults to "auto"
- Voice cancel on keyboard dismiss — force-stops orphaned recordings
- Google STT text cap — 10K character limit on accumulated text

### Fixed
- TYPE_NULL fields (terminals) — sends single KeyEvent preserving RTL/surrogate pairs
- Stale InputConnection — batch edit wrapping refreshes connection
- Input-type-aware post-processing — skips auto-cap/period for passwords, URIs, emails, etc.
- Safe undo — verifies text matches before deleting
- Sample rate validation — prevents crash on unsupported audio config
- Recording memory guard — MAX_SAMPLES cap prevents OOM
- Composing text cleanup — explicit finishComposingText before voice commit
- Thread safety — @Volatile on state, whisperContext TOCTOU fix
- Keyboard state reset after voice — caps, suggestions, spacing all synced
- Force-stop recording on keyboard dismiss — prevents background orphan

### Security
- API keys migrated to EncryptedSharedPreferences (AES256-GCM/AES256-SIV)
- Cloud STT blocked for password fields and incognito mode

---

## [0.5.0] - 2026-03-07

### Added
- Live keyboard resize — real-time re-layout during drag
- Live keyboard preview toggle in Settings — debounced IME tracking, focus recovery
- Resize instruction in suggestion strip (clears on Done tap)

### Fixed
- Keyboard preview toggle tracks IME visibility correctly
- Resize overlay: removed unused left/right tabs, made top tab draggable
- Keyboard height dialog OK button was reverting saved values
- Soft reload for layout-only settings (prevents white flash)
- Removed setBackgroundColor calls causing black screen on keyboard launch

---

## [0.4.0] - 2026-03-05

### Added
- Quick Text toolbar key (Samsung-style custom text snippets)
- Action Bar settings screen
- Simplified Toolbar with hardcoded auto-show/auto-hide

### Changed
- Flat settings layout with collapsible Advanced sections
- Screen consolidation — deleted ThemeSize, curated Appearance
- Removed pinned toolbar — hardcoded defaults

### Fixed
- Dead string removal and IME padding cleanup
- Missing LocalContext import in SearchScreen

---

## [0.3.0] - 2026-03-04

### Added
- Google STT continuous mode — auto-restarts on silence, partial results
- Soft Light day theme
- Samsung-style resize overlay with drag handle, Reset/Done buttons
- Samsung-style clipboard history management overhaul
- Inline API key fields with password masking and eye toggle
- Comprehensive feature registry (FEATURES.md)
- CI: merged workflows, debug APK artifact upload on every push

### Fixed
- Google STT gets stuck after error — mic tap does nothing
- Local model loading shows "Loading..." forever with no feedback
- OpenAI API key validation fails for project-scoped keys
- Removed stale SSL certificate pins blocking API calls
- API key validation — use query param for Gemini, show error details
- Suppress "the" on empty fields
- Resize audit fixes + STT consistency across all 3 modes

---

## [0.2.0] - 2026-02-28

### Added
- Voice input pipeline — 3 STT modes: Local (whisper.cpp), Cloud (OpenAI Whisper API), Google (SpeechRecognizer)
- whisper.cpp JNI integration — CPU-optimized .so loading (vfpv4, v8fp16)
- 8 quantized whisper models (Tiny 31MB to Large-v3-Turbo 574MB)
- Model manager — download with resume (HTTP Range), progress tracking
- AI text rewrite — Gemini and OpenAI clients, 5 style variants
- Voice benchmark button + unit tests (ModelManager, WavEncoder, ApiKeyValidator)
- Circular toolbar, feature drawer, voice input mode
- WhisperClick / Classic Settings section dividers
- Activity log — 200-entry circular buffer for diagnostics
- Rebrand HeliBoard → WhisperClick in all user-facing strings
- CI on push with full test suite

### Fixed
- CMakeLists.txt hardened for whisper.cpp FetchContent
- @JvmStatic for Java interop on isModelDownloaded
- Recording status bar made tappable to stop recording
- CI build failures (icon tint, Gradle config cache, lambda return type)
- WavEncoderTest matched to implementation behavior
- Guard voice init in onCreate for Robolectric test compat
- Show mic button in TYPE_NULL fields (Termux, terminals)
- 3 compilation errors (suspend release, coroutine callback, ColorType)

---

## [0.1.0] - 2026-02-25

### Added
- Samsung-style dark keyboard theme (AMOLED)
- Action row above number row
- Samsung key colors, spacing, and hints (pixel-matched from screenshots)
- Toolbar with pill icon backgrounds, evenly distributed
- Samsung-style single row: toolbar and suggestions swap in place
- Clipboard 3-column grid with card styling
- Gap analysis tracking (65% → 73% Samsung match)
- CI: debug APK build on push to main

---

## Version Scheme

`MAJOR.MINOR.PATCH[-prerelease]`

- **MAJOR** — breaking changes or major release milestones
- **MINOR** — new features, significant enhancements
- **PATCH** — bug fixes, hardening, documentation
- **Pre-release** — `-alpha`, `-beta`, `-rc1`, etc.

Git tags: `v0.1.0`, `v0.2.0`, ..., `v1.0.0-beta`, `v1.0.0`, etc.

### Rules
1. Every tagged version MUST have a CHANGELOG entry
2. Every CHANGELOG entry MUST list what changed (Added/Fixed/Changed/Removed/Security)
3. [Unreleased] section tracks work-in-progress between tags
4. No exceptions
