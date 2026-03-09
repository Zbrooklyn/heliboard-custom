# Changelog

All notable changes to WhisperClick will be documented in this file.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
Versioning: [Semantic Versioning](https://semver.org/spec/v2.0.0.html)

---

## [Unreleased]

---

## [1.0.0-beta] - 2026-03-09

### Added
- **Whisper hallucination guard** — RMS energy check rejects near-silent recordings; known hallucination phrase filter blocks "Thank you for watching", "Subscribe", etc. (local mode only)
- **Audio focus management** — requests AUDIOFOCUS_GAIN_TRANSIENT before recording; pauses media playback; auto-stops recording if another app steals focus
- **WakeLock during recording** — PARTIAL_WAKE_LOCK prevents doze mode from suspending audio capture when screen is off
- **Minimum recording duration** — rejects recordings shorter than 0.5 seconds to prevent Whisper hallucinations on near-empty audio
- **Privacy guard for cloud STT** — blocks cloud transcription in password fields, fields with IME_FLAG_NO_PERSONALIZED_LEARNING, and incognito mode
- **EncryptedSharedPreferences for API keys** — OpenAI and Gemini keys stored with AES256-GCM encryption; one-time migration from plain prefs on upgrade
- **Google STT language control** — passes keyboard's current subtype locale to SpeechRecognizer; adds offline preference support
- **Whisper language parameter** — JNI `fullTranscribe` accepts language string; defaults to `"auto"` for multilingual model support
- **Voice cancel on keyboard dismiss** — force-stops recording without transcribing when keyboard hides
- **Google STT accumulated text cap** — 10,000 character limit prevents unbounded memory growth
- **Voice input pipeline** — three STT modes: Local (whisper.cpp), Cloud (OpenAI Whisper API), Google (Android SpeechRecognizer)
- **whisper.cpp integration** — JNI bridge, CPU-optimized .so loading (vfpv4, v8fp16), 8 quantized models (Tiny 31MB to Large-v3-Turbo 574MB)
- **Model manager** — download with resume (HTTP Range), progress tracking, active model persistence
- **Google STT continuous mode** — auto-restarts on silence, accumulates results, partial result preview
- **Voice input UI** — VoiceInputModeView with animated mic button, state display, provider label, network indicator, incognito badge
- **AI text rewrite** — Gemini and OpenAI clients with 5 style variants (Clean, Professional, Casual, Concise, Emojify)
- **Samsung theme** — dark theme with Samsung-style visual design
- **Settings restructure** — flat layout, collapsible Advanced sections, screen consolidation
- **Voice & AI settings screen** — STT mode toggle, model picker, API key input, on-device benchmark
- **Live keyboard resize** — real-time re-layout during drag
- **Live keyboard preview toggle** in Settings — debounced IME tracking, focus recovery
- **Toolbar mic key** with voice state feedback
- **Permission handling** — VoicePermissionActivity for RECORD_AUDIO with rationale dialog
- **Activity log** — in-memory event log (200-entry circular buffer) for diagnostics
- **Unit tests** — ModelManager (8 tests), WavEncoder (6 tests), ApiKeyValidator

### Fixed
- **TYPE_NULL fields (terminals)** — voice text was silently lost in Termux, ConnectBot, etc. Now sends single `ACTION_MULTIPLE`/`KEYCODE_UNKNOWN` KeyEvent preserving RTL bidi and surrogate pairs
- **Stale InputConnection** — voice commit wrapped in `beginBatchEdit()`/`endBatchEdit()` to refresh connection
- **Input-type-aware post-processing** — auto-cap/period/space skipped for password, URI, email, filter, TYPE_NULL fields
- **Safe undo** — verifies text before cursor matches committed text before deleting; graceful failure message
- **Sample rate validation** — checks `getMinBufferSize()` for ERROR before use; prevents crash
- **Recording memory guard** — MAX_SAMPLES cap (4,800,000) prevents OOM
- **Composing text cleanup** — explicit `finishComposingText()` before voice commit
- **Thread safety** — `state` field `@Volatile`; `whisperContext` captured before transcription (TOCTOU fix)
- **Keyboard state reset after voice** — resets mWordComposer, mSpaceState, suggestions, auto-caps
- **Keyboard preview toggle** — tracks IME visibility correctly
- **Resize overlay** — removed unused tabs, made top tab draggable
- **Height dialog** — OK button no longer reverts saved values
- **Settings reload** — soft reload prevents white flash on layout changes

### Security
- API keys in EncryptedSharedPreferences (AES256-GCM/AES256-SIV)
- Cloud STT blocked for password fields and incognito mode
- Privacy flags (IME_FLAG_NO_PERSONALIZED_LEARNING) respected

---

## Version Scheme

`MAJOR.MINOR.PATCH[-prerelease]`

- **MAJOR** — breaking changes or major release milestones
- **MINOR** — new features, significant enhancements
- **PATCH** — bug fixes, hardening, documentation
- **Pre-release** — `-alpha`, `-beta`, `-rc1`, etc.

Git tags: `v1.0.0-beta`, `v1.0.0`, `v1.1.0`, etc.
`versionCode` and `versionName` in `build.gradle.kts` derived from latest git tag automatically.

### Rules
1. Every tagged version MUST have a CHANGELOG entry
2. Every CHANGELOG entry MUST list what changed (Added/Fixed/Changed/Removed/Security)
3. [Unreleased] section tracks work-in-progress between tags
4. No exceptions
