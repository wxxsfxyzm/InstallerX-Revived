# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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

###############################
# kotlinx serialization rules #
###############################
# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Serializer for classes with named companion objects are retrieved using `getDeclaredClasses`.
# If you have any, uncomment and replace classes with those containing named companion objects.
#-keepattributes InnerClasses # Needed for `getDeclaredClasses`.
#-if @kotlinx.serialization.Serializable class
#com.example.myapplication.HasNamedCompanion, # <-- List serializable classes with named companions.
#com.example.myapplication.HasNamedCompanion2
#{
#    static **$* *;
#}
#-keepnames class <1>$$serializer { # -keepnames suffices; class is kept when serializer() is kept.
#    static <1>$$serializer INSTANCE;
#}

-keep public interface ** extends android.os.IInterface {*;}
-keep public class com.rosan.installer.App {*;}
-keep public class com.rosan.installer.ui.activity.** extends android.app.Activity
# 保留 sealed class 的 metadata（sealedSubclasses 用到）
-keepattributes KotlinMetadata

# 保留 InstallOption 本体
-keep class com.rosan.installer.data.app.util.InstallOption { *; }

# 保留所有 InstallOption 的 object 子类（完整保留，不只是 INSTANCE）
-keep class com.rosan.installer.data.app.util.InstallOption$* { *; }

#-keep public class com.rosan.installer.data.process.model.impl.** extends com.rosan.installer.data.process.repo.ProcessRepo {
#public static void main(java.lang.String[]);
#}
#-keep public class com.rosan.installer.** extends android.app.Service
#-keep public class com.rosan.installer.** extends android.content.BroadcastReceiver
#-keep public class com.rosan.installer.** extends android.content.ContentProvider
#-keep class androidx.core.content.FileProvider {*;}
#-keep interface androidx.core.content.FileProvider$PathStrategy {*;}

-keep class rikka.shizuku.ShizukuProvider

-dontwarn **