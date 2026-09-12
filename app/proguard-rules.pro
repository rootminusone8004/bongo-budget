# Gson reflection and serialization
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Preserve data models used by Room and Gson
-keep class com.budjet.app.data.model.** { *; }
-keepclassmembers class com.budjet.app.data.model.** { *; }

# Room Database
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**
