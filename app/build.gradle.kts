import com.android.build.api.variant.ApplicationVariant

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization") version "2.2.21"
    kotlin("plugin.compose") version "2.2.21"
}

// Derive version from git tags: v1.0.0 → versionCode=10000, versionName="1.0.0"
// Supports pre-release suffixes: v1.0.0-beta → versionCode=10000, versionName="1.0.0-beta"
// Uses providers.exec for Gradle configuration cache compatibility
val gitTagResult = providers.exec {
    commandLine("git", "describe", "--tags", "--abbrev=0")
    isIgnoreExitValue = true
}
val gitDescribeResult = providers.exec {
    commandLine("git", "describe", "--tags", "--always")
    isIgnoreExitValue = true
}

fun gitVersionCode(): Int {
    return try {
        val tag = gitTagResult.standardOutput.asText.get().trim().removePrefix("v")
        if (tag.isEmpty()) return 10000
        // Strip pre-release suffix (e.g., "1.0.0-beta" → "1.0.0")
        val version = tag.split("-").first()
        val parts = version.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        major * 10000 + minor * 100 + patch
    } catch (_: Exception) { 10000 }
}

fun gitVersionName(): String {
    return try {
        val name = gitDescribeResult.standardOutput.asText.get().trim().removePrefix("v")
        name.ifEmpty { "1.0.0-dev" }
    } catch (_: Exception) { "1.0.0-dev" }
}

android {
    compileSdk = 35

    defaultConfig {
        applicationId = "helium314.keyboard"
        minSdk = 21
        targetSdk = 35
        versionCode = gitVersionCode()
        versionName = gitVersionName()
        ndk {
            abiFilters.clear()
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }

    signingConfigs {
        create("custom") {
            val ksFile = file("${rootDir}/keystore.jks")
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: throw GradleException("KEYSTORE_PASSWORD env var not set")
                keyAlias = System.getenv("KEY_ALIAS") ?: "heliboard"
                keyPassword = System.getenv("KEY_PASSWORD") ?: throw GradleException("KEY_PASSWORD env var not set")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            isDebuggable = false
            isJniDebuggable = false
            val ksFile = file("${rootDir}/keystore.jks")
            if (ksFile.exists()) {
                signingConfig = signingConfigs.getByName("custom")
            }
        }
        create("nouserlib") { // same as release, but does not allow the user to provide a library
            isMinifyEnabled = true
            isShrinkResources = false
            isDebuggable = false
            isJniDebuggable = false
            matchingFallbacks += "release"
        }
        debug {
            // "normal" debug has minify for smaller APK to fit the GitHub 25 MB limit when zipped
            // and for better performance in case users want to install a debug APK
            isMinifyEnabled = true
            isJniDebuggable = false
            applicationIdSuffix = ".debug"
            val ksFile = file("${rootDir}/keystore.jks")
            if (ksFile.exists()) {
                signingConfig = signingConfigs.getByName("custom")
            }
        }
        create("runTests") { // build variant for running tests on CI that skips tests known to fail
            isMinifyEnabled = false
            isJniDebuggable = false
            matchingFallbacks += "debug"
        }
        create("debugNoMinify") { // for faster builds in IDE
            isDebuggable = true
            isMinifyEnabled = false
            isJniDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".debug"
            matchingFallbacks += "debug"
        }
        base.archivesBaseName = "HeliBoard_" + defaultConfig.versionName
        // got a little too big for GitHub after some dependency upgrades, so we remove the largest dictionary
        androidComponents.onVariants { variant: ApplicationVariant ->
            if (variant.buildType == "debug") {
                variant.androidResources.ignoreAssetsPatterns = listOf("main_ro.dict")
                variant.proguardFiles = emptyList()
                //noinspection ProguardAndroidTxtUsage we intentionally use the "normal" file here
                variant.proguardFiles.add(project.layout.buildDirectory.file(getDefaultProguardFile("proguard-android.txt").absolutePath))
                variant.proguardFiles.add(project.layout.buildDirectory.file(project.buildFile.parent + "/proguard-rules.pro"))
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    externalNativeBuild {
        ndkBuild {
            path = File("src/main/jni/Android.mk")
        }
    }
    ndkVersion = "28.0.13004108"

    packaging {
        jniLibs {
            // shrinks APK by 3 MB, zipped size unchanged
            useLegacyPackaging = true
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    // see https://github.com/Helium314/HeliBoard/issues/477
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    namespace = "helium314.keyboard.latin"
    lint {
        abortOnError = true
    }
}

dependencies {
    // whisper.cpp voice engine
    implementation(project(":whisper"))

    // androidx
    implementation("androidx.core:core-ktx:1.16.0") // 1.17 requires SDK 36
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.autofill:autofill:1.3.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // compose
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    // newer than 2025.11.01 contains androidx.compose.material:material-android:1.10.0, which requires minSdk 23
    // maybe it's possible to use tools:overrideLibrary="androidx.compose.material" as it's not used explicitly, but probably this is just going to crash
    implementation(platform("androidx.compose:compose-bom:2025.11.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.navigation:navigation-compose:2.9.6")
    implementation("sh.calvin.reorderable:reorderable:2.4.3") // for easier re-ordering, todo: check 3.0.0
    implementation("com.github.skydoves:colorpicker-compose:1.1.3") // for user-defined colors

    // test
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.17.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:runner:1.6.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
