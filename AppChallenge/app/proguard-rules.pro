# Flags needed for Firebase Auth
-keepattributes Signature
-keepattributes *Annotation*

# Flags needed for Firebase Database
-keepattributes Signature
-keepclassmembers class team.tr.permitlog.** {
    *;
}