# R8-specific rules to prevent aggressive optimizations that break Retrofit

# CRITICAL: Keep all Retrofit-related classes with full signature information
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# CRITICAL: Keep API interface without any modifications
-keep interface edu.ucam.reservashack.data.remote.TakeASpotApi { *; }

# CRITICAL: Keep all DTOs and response models
-keep class edu.ucam.reservashack.data.remote.dto.** { *; }

# CRITICAL: Preserve generic type information at method level
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*,MethodParameters

# Keep Gson with full type information
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }

# Keep OkHttp classes
-keep class okhttp3.ResponseBody { *; }
-keep class okhttp3.RequestBody { *; }

# Prevent optimization of generic methods
-keepclassmembers interface ** {
    @retrofit2.http.* <methods>;
}

