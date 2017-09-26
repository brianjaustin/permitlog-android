# Flags needed for Firebase Auth
-keepattributes Signature
-keepattributes *Annotation*

# Flags needed for Firebase Database
-keepattributes Signature
-keepclassmembers class team.tr.permitlog.** { *; }

# Keeps line numbers and file name obfuscation
# https://stackoverflow.com/a/10159080/3740708
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

-dontwarn javax.naming.**