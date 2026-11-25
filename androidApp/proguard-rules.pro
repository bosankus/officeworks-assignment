# Ktor references JVM-only classes that don't exist on Android; they're never loaded at runtime.
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
