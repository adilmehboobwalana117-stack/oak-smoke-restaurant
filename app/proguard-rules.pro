# Oak & Smoke Restaurant Hasilpur — ProGuard / R8 Release Keep Rules
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,AnnotationDefault

# Preserve Firestore POJO data models & enums for reflection-based toObject() deserialization
-keep class com.example.data.** { *; }

# Preserve ZXing QR Code encoder classes
-keep class com.google.zxing.** { *; }
