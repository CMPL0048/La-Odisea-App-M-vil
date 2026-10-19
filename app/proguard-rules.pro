# Modelos serializados con kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.laodisea.app.**$$serializer { *; }
-keepclassmembers class com.laodisea.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.laodisea.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn org.slf4j.**
