# Add project specific ProGuard rules here.
# Keep DTO/model fields for Gson if needed.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.example.eventia.data.remote.dto.** { *; }
