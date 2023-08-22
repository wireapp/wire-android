-dontobfuscate
-dontwarn java.awt.Component
-dontwarn java.awt.GraphicsEnvironment
-dontwarn java.awt.HeadlessException
-dontwarn java.awt.Window
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
