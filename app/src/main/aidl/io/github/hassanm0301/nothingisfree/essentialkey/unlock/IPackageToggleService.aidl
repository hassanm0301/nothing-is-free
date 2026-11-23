// Adapted from Essential Key Tools by KoukeNeko (MIT License, see docs/licenses/EssentialKeyTools.txt).
package io.github.hassanm0301.nothingisfree.essentialkey.unlock;

/** Runs in the Shizuku user service process, which has shell privileges. */
interface IPackageToggleService {
    // Transaction code reserved by Shizuku for tearing the user service process down.
    void destroy() = 16777114;

    /** Null when `pm` accepted the change; otherwise what it said (or why it didn't run). */
    String setPackageEnabled(String packageName, boolean enabled, int userId) = 1;
}
