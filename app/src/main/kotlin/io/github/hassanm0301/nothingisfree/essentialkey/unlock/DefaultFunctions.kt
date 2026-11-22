// Adapted from Essential Key Tools by KoukeNeko (MIT License, see docs/licenses/EssentialKeyTools.txt).
package io.github.hassanm0301.nothingisfree.essentialkey.unlock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.net.toUri

/**
 * Nothing's own Essential Key functions (Essential Space, Essential Recorder, Essential
 * intelligence) as installed on this phone. They take the single press before any app sees it;
 * disabling them is what frees it. Read straight from [PackageManager], so it needs no privilege.
 */
object DefaultFunctions {

    fun states(context: Context): List<PackageState> {
        val pm = context.packageManager
        return NothingConsumerPackages.CANDIDATES.map { name -> stateOf(pm, name) }
    }

    fun status(context: Context): UnlockStatus = UnlockStatus.fromPackageStates(states(context))

    /** The name the phone shows for [packageName], or the package name if it has none. */
    fun label(context: Context, packageName: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName
    }

    /** The package's App info page, where Disable or Enable may be pressed by hand. */
    fun appInfoIntent(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun stateOf(pm: PackageManager, name: String): PackageState {
        try {
            pm.getApplicationInfo(name, PackageManager.MATCH_DISABLED_COMPONENTS)
        } catch (_: PackageManager.NameNotFoundException) {
            return PackageState.notInstalled(name)
        }
        val enabled = when (pm.getApplicationEnabledSetting(name)) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> false
            // DEFAULT and ENABLED both leave the package's own manifest state: still active.
            else -> true
        }
        return PackageState(name, installed = true, enabled = enabled)
    }
}
