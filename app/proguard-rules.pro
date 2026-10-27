# Keep the Glyph SDK surface intact once it is added (it is loaded reflectively by Nothing's
# Glyph service over AIDL). Harmless while the AAR is absent.
-keep class com.nothing.ketchum.** { *; }
-dontwarn com.nothing.ketchum.**
