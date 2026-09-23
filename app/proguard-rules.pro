-keepattributes *Annotation*
-keep class com.jitelecom.productadviser.data.remote.** { *; }
-keep class com.jitelecom.productadviser.data.importexport.** { *; }

# These models are deserialized reflectively by Moshi from bundled assets.
-keep class com.jitelecom.productadviser.data.seed.Bundled** { *; }
