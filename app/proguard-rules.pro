# Keep native methods
-keepclassmembers class * {
    native <methods>;
}

# Keep classes that are used as a parameter type of methods that are also marked as keep
# to preserve changing those methods' signature.
-keep class helium314.keyboard.latin.dictionary.Dictionary
-keep class helium314.keyboard.latin.NgramContext
-keep class helium314.keyboard.latin.makedict.ProbabilityInfo

# after upgrading to gradle 8, stack traces contain "unknown source"
-keepattributes SourceFile,LineNumberTable

# Keep Compose-related classes for settings UI
-keep class helium314.keyboard.settings.** { *; }
# Keep AI/voice classes accessed via reflection or from Java
-keep class helium314.keyboard.latin.ai.** { *; }
-keep class helium314.keyboard.latin.voice.** { *; }

# Google Tink (used by androidx.security:security-crypto) references annotation classes
# that aren't on the Android classpath — tell R8 to ignore them
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**
