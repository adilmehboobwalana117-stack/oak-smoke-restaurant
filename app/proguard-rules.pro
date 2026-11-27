# Oak & Smoke Restaurant Hasilpur — ProGuard / R8 Release Keep Rules
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,AnnotationDefault

# Preserve Firestore POJO data models & enums for reflection-based toObject() deserialization
-keep class com.example.data.** { *; }

# Preserve ZXing QR Code encoder classes
-keep class com.google.zxing.** { *; }

# Preserve Credential Manager & Google Sign-In ID token classes for Play Console release builds
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
