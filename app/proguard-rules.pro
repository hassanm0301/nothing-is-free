# Keep the Glyph SDK surface intact once it is added (it is loaded reflectively by Nothing's
# Glyph service over AIDL). Harmless while the AAR is absent.
-keep class com.nothing.ketchum.** { *; }
-dontwarn com.nothing.ketchum.**

# Essential key: Shizuku starts the package-toggle user service by class name in its own process,
# so nothing in the app references its constructor.
-keep class io.github.hassanm0301.nothingisfree.essentialkey.unlock.PackageToggleService {
    <init>();
}
