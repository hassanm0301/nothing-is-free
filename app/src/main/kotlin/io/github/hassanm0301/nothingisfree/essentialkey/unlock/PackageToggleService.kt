// Adapted from Essential Key Tools by KoukeNeko (MIT License, see docs/licenses/EssentialKeyTools.txt).
package io.github.hassanm0301.nothingisfree.essentialkey.unlock

import kotlin.system.exitProcess

/**
 * Enables or disables a Nothing consumer package through `pm`. Shizuku instantiates this class by
 * name in its own shell-privileged process, which is why the system-app Disable button being greyed
 * out in App Info does not apply here.
 */
class PackageToggleService : IPackageToggleService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun setPackageEnabled(packageName: String, enabled: Boolean, userId: Int): String? {
        // The process holds shell privileges, so it only ever touches the packages this app manages.
        if (packageName !in NothingConsumerPackages.CANDIDATES) {
            return "$packageName isn't one of Nothing's key functions"
        }
        return try {
            val process = ProcessBuilder(packageToggleCommand(packageName, enabled, userId))
                .redirectErrorStream(true)
                .start()
            // Drain the output so the child cannot block on a full pipe before it exits.
            val output = process.inputStream.bufferedReader().use { reader -> reader.readText() }.trim()
            // pm can print an exception and still exit 0 on some builds, so check what it said too.
            if (process.waitFor() == 0 && !output.contains("Exception") && !output.startsWith("Error")) {
                null
            } else {
                pmFailure(output)
            }
        } catch (error: java.io.IOException) {
            "pm didn't run: ${error.message}"
        }
    }
}

/** `disable-user` is the reversible disable that App Info's Disable button performs. */
internal fun packageToggleCommand(packageName: String, enabled: Boolean, userId: Int): List<String> =
    listOf("pm", if (enabled) "enable" else "disable-user", "--user", userId.toString(), packageName)

/** The line of `pm`'s output that says why it failed, without the stack trace. */
internal fun pmFailure(output: String): String {
    val lines = output.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("at ") }
    // pm first says it hit an exception, then names it with its message: the message is the reason.
    return lines.firstOrNull { "Exception: " in it }?.substringAfter("Exception: ")
        ?: lines.firstOrNull { it.startsWith("Error") }
        ?: lines.firstOrNull()
        ?: "pm failed without saying why"
}
