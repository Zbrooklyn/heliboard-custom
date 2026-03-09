// Minimal whisper.cpp benchmark — runs on-device without Android SDK.
// Tests: model load time, inference with language="en" vs "auto",
// varying thread counts, and varying audio durations.

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <math.h>
#include "whisper.h"

#define WHISPER_SR 16000  // 16kHz sample rate

static double now_ms(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return ts.tv_sec * 1000.0 + ts.tv_nsec / 1e6;
}

// Generate silence (zeros) — baseline for model overhead
static float *gen_silence(int samples) {
    float *buf = calloc(samples, sizeof(float));
    return buf;
}

// Generate a 440Hz sine wave — tests actual audio processing
static float *gen_tone(int samples) {
    float *buf = malloc(samples * sizeof(float));
    for (int i = 0; i < samples; i++) {
        buf[i] = 0.3f * sinf(2.0f * 3.14159f * 440.0f * i / WHISPER_SR);
    }
    return buf;
}

typedef struct {
    const char *label;
    const char *language;
    int n_threads;
    int duration_sec;
    int use_tone;  // 0=silence, 1=tone
} bench_config;

static void run_bench(struct whisper_context *ctx, bench_config *cfg) {
    int n_samples = WHISPER_SR * cfg->duration_sec;
    float *audio = cfg->use_tone ? gen_tone(n_samples) : gen_silence(n_samples);

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime   = false;
    params.print_progress   = false;
    params.print_timestamps = false;
    params.print_special    = false;
    params.translate        = false;
    params.language         = cfg->language;
    params.n_threads        = cfg->n_threads;
    params.no_context       = true;
    params.single_segment   = true;

    whisper_reset_timings(ctx);

    printf("  %-35s ... ", cfg->label);
    fflush(stdout);

    double t0 = now_ms();
    int ret = whisper_full(ctx, params, audio, n_samples);
    double elapsed = now_ms() - t0;

    if (ret != 0) {
        printf("FAILED (ret=%d)\n", ret);
    } else {
        int n_seg = whisper_full_n_segments(ctx);
        const char *text = (n_seg > 0) ? whisper_full_get_segment_text(ctx, 0) : "(empty)";
        double rtf = elapsed / (cfg->duration_sec * 1000.0);
        printf("%7.0f ms  (%.1fx RT)  \"%s\"\n", elapsed, rtf, text);
    }

    free(audio);
}

int main(int argc, char **argv) {
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <model.bin> [threads]\n", argv[0]);
        fprintf(stderr, "  threads: number of CPU threads (default: auto)\n");
        return 1;
    }

    const char *model_path = argv[1];
    int max_threads = (argc >= 3) ? atoi(argv[2]) : 0;

    // Detect if English-only model
    int is_english = (strstr(model_path, ".en") != NULL) ? 1 : 0;

    printf("=== WhisperClick Local Benchmark ===\n");
    printf("Model:   %s\n", model_path);
    printf("English: %s\n", is_english ? "yes (will skip auto-detect test)" : "no (multilingual)");

    // Load model
    printf("\n--- Model Load ---\n");
    double t0 = now_ms();
    struct whisper_context_params cparams = whisper_context_default_params();
    struct whisper_context *ctx = whisper_init_from_file_with_params(model_path, cparams);
    double load_ms = now_ms() - t0;

    if (!ctx) {
        fprintf(stderr, "ERROR: Failed to load model: %s\n", model_path);
        return 1;
    }
    printf("  Load time: %.0f ms\n", load_ms);
    printf("  System:    %s\n", whisper_print_system_info());

    // Determine thread counts to test
    int thread_counts[4];
    int n_thread_tests = 0;
    if (max_threads > 0) {
        thread_counts[0] = max_threads;
        n_thread_tests = 1;
    } else {
        // Test 2, 4, 6 threads
        thread_counts[0] = 2;
        thread_counts[1] = 4;
        thread_counts[2] = 6;
        n_thread_tests = 3;
    }

    // --- Language detection overhead test ---
    if (!is_english) {
        printf("\n--- Language Detection Overhead (3s silence) ---\n");
        bench_config cfg_en   = {"lang=\"en\"  (no detection)", "en",   4, 3, 0};
        bench_config cfg_auto = {"lang=\"auto\" (with detection)", "auto", 4, 3, 0};
        run_bench(ctx, &cfg_en);
        run_bench(ctx, &cfg_auto);
    }

    // --- Thread scaling ---
    printf("\n--- Thread Scaling (3s silence, lang=\"%s\") ---\n", is_english ? "en" : "auto");
    for (int i = 0; i < n_thread_tests; i++) {
        char label[64];
        snprintf(label, sizeof(label), "%d threads", thread_counts[i]);
        const char *lang = is_english ? "en" : "auto";
        bench_config cfg = {label, lang, thread_counts[i], 3, 0};
        run_bench(ctx, &cfg);
    }

    // --- Duration scaling ---
    printf("\n--- Duration Scaling (%d threads, lang=\"%s\") ---\n",
           thread_counts[0], is_english ? "en" : "auto");
    int durations[] = {1, 3, 5, 10};
    for (int i = 0; i < 4; i++) {
        char label[64];
        snprintf(label, sizeof(label), "%ds silence", durations[i]);
        const char *lang = is_english ? "en" : "auto";
        bench_config cfg = {label, lang, thread_counts[0], durations[i], 0};
        run_bench(ctx, &cfg);
    }

    // --- Tone vs silence (actual audio processing) ---
    printf("\n--- Tone vs Silence (3s, %d threads) ---\n", thread_counts[0]);
    {
        const char *lang = is_english ? "en" : "auto";
        bench_config cfg_sil  = {"3s silence", lang, thread_counts[0], 3, 0};
        bench_config cfg_tone = {"3s 440Hz tone", lang, thread_counts[0], 3, 1};
        run_bench(ctx, &cfg_sil);
        run_bench(ctx, &cfg_tone);
    }

    printf("\n--- Done ---\n");
    whisper_free(ctx);
    return 0;
}
