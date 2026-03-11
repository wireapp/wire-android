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
-keep class com.sun.jna.** { *; }
-keep class * extends com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.Pointer { long peer; }

# AVS/WebRTC classes are accessed from native code (FlowManager_attach).
# Keep concrete names and members to avoid NoSuchMethodError/ClassNotFoundException at runtime.
-keep class org.webrtc.** { *; }
-keep class com.waz.call.FlowManager { *; }
-keep class com.waz.avs.VideoRenderer { *; }
-keep class com.waz.call.CaptureDevice { *; }
-keep class com.waz.media.manager.** { *; }
-keep class com.waz.service.call.** { *; }
-keep class com.waz.soundlink.SoundLinkAPI { *; }
-dontwarn org.webrtc.CalledByNative
-dontwarn org.webrtc.JniCommon
-dontwarn org.webrtc.audio.AudioDeviceModule

# Room/WorkManager instantiate generated DB classes via reflection.
# Keep *_Impl classes (including constructors) to avoid startup crash
# when R8 strips default constructors.
-keep class **_Impl extends androidx.room.RoomDatabase { *; }

# WrapperWorkerFactory resolves inner workers by class name from input data.
# Keep names stable so existing enqueued work remains resolvable after minification.
# See docs/minification-workmanager-compat.md
-keepnames class com.wire.kalium.logic.sync.PendingMessagesSenderWorker
-keepnames class com.wire.kalium.logic.sync.periodic.UserConfigSyncWorker
-keepnames class com.wire.kalium.logic.sync.periodic.UpdateApiVersionsWorker
-keepnames class com.wire.kalium.logic.sync.receiver.asset.AudioNormalizedLoudnessWorker

# WorkManager reflection
# InputMerger is created reflectively via getDeclaredConstructor() and may lose a visible no-arg ctor after shrinking.
-keep class androidx.work.OverwritingInputMerger { <init>(); *; }
-keep class androidx.work.ArrayCreatingInputMerger { <init>(); *; }
