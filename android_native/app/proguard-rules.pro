# Keep Firestore models annotated with @Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Preserve domain models and navigation destinations
-keep class com.helal.accomplishmentplan.nativeapp.domain.model.** { *; }
-keep class com.helal.accomplishmentplan.nativeapp.ui.navigation.** { *; }
