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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ignore rx obfuscation
-keep class rx.** { *; }

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


# Library Hilt
# Keep class names of Hilt injected ViewModels since their name are used as a multibinding map key.
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel


# Library Retrofit
# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep annotation default values (e.g., retrofit2.http.Field.encoded).
-keepattributes AnnotationDefault
# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
#-dontwarn retrofit2.KotlinExtensions$*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Needed to avoid crash on minified app release with Maps SDK v3.1 beta (https://issuetracker.google.com/issues/148084488)
-keep,allowoptimization class com.google.android.libraries.maps.** { *; }


-keep class com.google.ads.** # Don't proguard AdMob classes
-dontwarn com.google.ads.** # Temporary workaround for v6.2.1. It gives a warning that you can ignore

-ignorewarnings

# -keepclassmembers class * extends org.greenrobot.greendao.AbstractDao {
# public static java.lang.String TABLENAME;
# }
-keep class **$Properties

# If you do not use SQLCipher:
# -dontwarn org.greenrobot.greendao.database.**
# If you do not use RxJava:
#-dontwarn rx.**

-keepattributes *Annotation*

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}


# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }

-dontwarn com.squareup.**
-dontwarn com.squareup.okhttp.**

-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

-dontwarn okhttp3.**
-dontwarn okio.**

-keepattributes Signature


-dontwarn com.google.android.material.snackbar.**

# Rxjava Rules
-dontwarn sun.misc.**

-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}

-dontnote rx.internal.util.PlatformDependent

#Android Room Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

#KotlinX
-dontwarn kotlinx.coroutines.flow.**


-keepclassmembers enum * { *; }
-keep,allowobfuscation interface <1>

# For Google Play Services
-keep public class com.google.android.gms.ads.**{
   public *;
}

# For old ads classes
-keep public class com.google.ads.**{
   public *;
}

# For mediation
-keepattributes *Annotation*

#-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
#   public static final *** NULL;
#}

#-keepnames @com.google.android.gms.common.annotation.KeepName class *
#-keepclassmembernames class * {
#   @com.google.android.gms.common.annotation.KeepName *;
#}

-keep class org.json.** { *; }
-keepclasseswithmembers class org.json.** { *; }


#-keep @com.jimbovpn.jimbo2023.app.dto.LocationModel public class *
#-keep @com.jimbovpn.jimbo2023.app.dto.LocationsListModel public class *
#-keep @com.jimbovpn.jimbo2023.data.remote.entities.ResponseEntity public class *
#-keep @com.jimbovpn.jimbo2023.app.dto.OperatorModel public class *
#-keep class com.jimbovpn.jimbo2023.app.dto.OperatorModel {
#    *;
#}
#-keep class com.jimbovpn.jimbo2023.app.dto.LocationModel {
#    *;
#}
#-keep class com.jimbovpn.jimbo2023.app.dto.ServerModel {
#    *;
#}




-dontnote com.google.gson.annotations.Expose
    -keepclassmembers class * {
        @com.google.gson.annotations.Expose <fields>;
    }

    -keepclasseswithmembers,allowobfuscation,includedescriptorclasses class * {
        @com.google.gson.annotations.Expose <fields>;
    }

    -dontnote com.google.gson.annotations.SerializedName
    -keepclasseswithmembers,allowobfuscation,includedescriptorclasses class * {
        @com.google.gson.annotations.SerializedName <fields>;
    }

-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers.
-keep public class * extends java.lang.Exception  # Optional: Keep custom exceptions.

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

#-keep public class * extends libv2ray.Libv2ray
-keep class go.** { *; }
-keep class libv2ray.** { *; }
-keep class com.tencent.mmkv.MMKV.** { *; }
-keep class com.jimbovpn.jimbo2023.app.v2ray.dto.** { *; }
-keep class com.jimbovpn.jimbo2023.app.services.** { *; }
-keep class com.jimbovpn.jimbo2023.app.v2ray.service.** { *; }
-keep public class * extends android.app.Service

-keep class com.example.fantasyproject.v2ray.service.V2RayServiceManager.** { *; }


-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

#for pangel ubfusecate
-keep class com.bytedance.sdk.** { *; }

-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

#noinspection ShrinkerUnresolvedReference
#unity
-keep class com.ironsource.unity.androidbridge.** { *;}
-keep class com.google.android.gms.ads.** {public *;}
-keep class com.google.android.gms.appset.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
#adapters
-keep class com.ironsource.adapters.** { *; }
#sdk
-dontwarn com.ironsource.**
-dontwarn com.ironsource.adapters.**
-keepclassmembers class com.ironsource.** { public *; }
-keep public class com.ironsource.**
-keep class com.ironsource.adapters.** { *;
}
#omid
-dontwarn com.iab.omid.**
-keep class com.iab.omid.** {*;}
#javascript
-keepattributes JavascriptInterface
-keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }
