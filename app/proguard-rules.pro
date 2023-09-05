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

# For E2EE
-keep class com.wire.cryptobox.** { *; }

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# For SQLCipher
-keep,includedescriptorclasses class net.sqlcipher.** { *; }
-keep,includedescriptorclasses interface net.sqlcipher.** { *; }

# protobuf
#-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }


# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
#-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
#-keepattributes InnerClasses # Needed for `getDeclaredClasses`.

-keepnames class * extends com.ramcosta.composedestinations.spec.Route

# For JNA
-dontwarn java.awt.Component
-dontwarn java.awt.GraphicsEnvironment
-dontwarn java.awt.HeadlessException
-dontwarn java.awt.Window
