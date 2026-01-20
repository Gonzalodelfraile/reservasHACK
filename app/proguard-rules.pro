# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================================================
# REGLAS GLOBALES CRÍTICAS (para evitar ParameterizedType errors)
# ============================================================================

# Preservar TODOS los atributos de firma genérica (MUY IMPORTANTE)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes MethodParameters

# ============================================================================
# FIREBASE & SERIALIZATION RULES
# ============================================================================

# Firebase Core y Google Play Services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase Analytics (evitar errores de SecurityException)
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.android.gms.measurement.** { *; }
-keep class com.google.android.gms.internal.** { *; }

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-keepattributes *Annotation*

# Firebase Firestore
-keep class com.google.firebase.firestore.** { *; }
-keepclassmembers class com.google.firebase.firestore.** { *; }

# Preservar constructores sin argumentos en clases de modelo para Firebase Firestore
# Necesario para la deserialización correcta en builds release
-keepclasseswithmembers class edu.ucam.reservashack.domain.model.** {
    <init>();
}

# Preservar constructores sin argumentos en DTOs para Gson/Retrofit
-keepclasseswithmembers class edu.ucam.reservashack.data.remote.dto.** {
    <init>();
}

# CRÍTICO: Preservar todas las clases de modelo y DTO (NO ofuscar nunca)
-keep class edu.ucam.reservashack.domain.model.** { *; }
-keep class edu.ucam.reservashack.data.remote.dto.** { *; }
-keepclassmembers class edu.ucam.reservashack.domain.model.** { *; }
-keepclassmembers class edu.ucam.reservashack.data.remote.dto.** { *; }

# CRÍTICO: Preservar OkHttp ResponseBody y RequestBody
-keep class okhttp3.ResponseBody { *; }
-keep class okhttp3.RequestBody { *; }
-keepclassmembers class okhttp3.ResponseBody { *; }
-keepclassmembers class okhttp3.RequestBody { *; }

# Preservar campos anotados con @SerializedName (usados por Gson)
-keepclassmembers class edu.ucam.reservashack.data.remote.dto.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================================================
# GSON RULES (para evitar errores de ParameterizedType)
# ============================================================================

# CRÍTICO: Preservar toda la infraestructura de reflexión de tipos
-keep class java.lang.reflect.** { *; }
-keep interface java.lang.reflect.** { *; }
-keep class sun.reflect.** { *; }
-dontwarn sun.reflect.**

# CRÍTICO: Preservar clases que usan reflexión de Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.JsonObject { *; }
-keep class com.google.gson.JsonArray { *; }
-keep class com.google.gson.JsonElement { *; }
-keepclassmembers class com.google.gson.** { *; }

# Ignorar advertencias de clases internas de Gson/Retrofit
-dontwarn com.google.gson.**
-dontwarn sun.misc.**

# Preservar nombres de campos para Gson
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Evitar que R8 elimine constructores sin argumentos necesarios para Gson
-keepclassmembers class * {
    <init>();
}

# Preservar tipos genéricos en métodos (para evitar ParameterizedType cast)
-keepclassmembers class * {
    *** *(...);
}

# Preservar clases usadas en reflexión de Gson (Response<T>, Result<T>, etc)
-keep class * implements java.lang.reflect.ParameterizedType { *; }
-keep class * implements java.lang.reflect.Type { *; }

# ============================================================================
# RETROFIT & OKHTTP RULES
# ============================================================================

# CRÍTICO: Retrofit hace uso intensivo de reflexión y tipos genéricos
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# CRÍTICO: Preservar TODAS las clases de Retrofit sin ofuscación
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepclassmembers class retrofit2.** { *; }

# CRÍTICO: Preservar Response<T> con información de tipo genérico completa
-keep class retrofit2.Response { *; }
-keepclassmembers class retrofit2.Response { *; }

# CRÍTICO: NO ofuscar la API interface completa
-keep interface edu.ucam.reservashack.data.remote.TakeASpotApi { *; }
-keepclassmembers interface edu.ucam.reservashack.data.remote.TakeASpotApi { *; }

# Preservar métodos anotados con Retrofit HTTP annotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# CRÍTICO: Preservar tipos genéricos en funciones suspend (Continuation)
-keep class kotlin.coroutines.Continuation { *; }
-keepclassmembers class kotlin.coroutines.Continuation { *; }

# CRÍTICO: Preservar toda la información de tipos genéricos en métodos
-keepattributes Signature,InnerClasses,EnclosingMethod

# Preservar parámetros de métodos para Retrofit
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp platform usado por Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Ignorar referencias a OkHttp antiguo (com.squareup.okhttp) usado por gRPC
# Firebase usa gRPC que tiene referencias opcionales a OkHttp v2 (obsoleto)
# Nuestra app usa OkHttp3, así que estas clases no existen y no son necesarias
-dontwarn com.squareup.okhttp.CipherSuite
-dontwarn com.squareup.okhttp.ConnectionSpec
-dontwarn com.squareup.okhttp.TlsVersion

# ============================================================================
# HILT / DAGGER RULES
# ============================================================================

# Preservar clases anotadas con Hilt/Dagger
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Preservar métodos anotados con @Inject
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# Preservar ViewModels inyectados con Hilt
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Preservar miembros inyectados
-keepclassmembers class * {
    @javax.inject.Inject <fields>;
    @javax.inject.Inject <init>(...);
}

# ============================================================================
# KOTLIN & COROUTINES
# ============================================================================

# Preservar metadata de Kotlin para reflexión
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ============================================================================
# JETPACK COMPOSE
# ============================================================================

# Preservar funciones Composable
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ============================================================================
# APP SPECIFIC RULES (Sealed Classes y Estados Genéricos)
# ============================================================================

# Preservar todas las sealed classes con tipos genéricos
-keep class edu.ucam.reservashack.ui.shared.UiState { *; }
-keep class edu.ucam.reservashack.ui.shared.UiState$* { *; }
-keep class edu.ucam.reservashack.domain.util.Resource { *; }
-keep class edu.ucam.reservashack.domain.util.Resource$* { *; }
-keep class edu.ucam.reservashack.ui.shared.LoginState { *; }
-keep class edu.ucam.reservashack.ui.shared.LoginState$* { *; }
-keep class edu.ucam.reservashack.ui.screens.mybookings.MyBookingsState { *; }
-keep class edu.ucam.reservashack.ui.screens.mybookings.MyBookingsState$* { *; }
-keep class edu.ucam.reservashack.ui.screens.home.HomeState { *; }
-keep class edu.ucam.reservashack.ui.screens.home.HomeState$* { *; }
-keep class edu.ucam.reservashack.ui.screens.profile.ProfileUiState { *; }
-keep class edu.ucam.reservashack.ui.screens.profile.ProfileUiState$* { *; }

# Preservar todos los ViewModels (ya están, pero reforzamos)
-keep class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
    public ** get*();
    public ** set*();
}

# Preservar métodos que retornan Flow<*>
-keepclassmembers class * {
    public kotlinx.coroutines.flow.Flow get*();
    public kotlinx.coroutines.flow.StateFlow get*();
}
