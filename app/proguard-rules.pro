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

# ML Kit and Firebase discover ComponentRegistrar implementations reflectively, by class name,
# from <meta-data> entries in the merged manifest. firebase-components ships
# "-keep class * implements com.google.firebase.components.ComponentRegistrar", which retained
# the default constructor under legacy ProGuard but does not under R8 full mode. Without the
# constructor, discovery fails silently, no components register, and
# TextRecognition.getClient() dereferences a null component.
# Only the constructor is added here; the library rule already keeps the classes.
-keepclassmembers class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}