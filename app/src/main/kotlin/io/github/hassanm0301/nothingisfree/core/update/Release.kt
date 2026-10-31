package io.github.hassanm0301.nothingisfree.core.update

/** A published GitHub release, trimmed to what an update needs. */
data class Release(
    val tag: String,
    val name: String,
    val notes: String,
    val pageUrl: String,
    val apk: Asset?,
    /** Marked as a pre-release on GitHub: only offered on the beta channel. */
    val prerelease: Boolean = false,
) {
    data class Asset(val name: String, val downloadUrl: String, val sizeBytes: Long)

    val version: String get() = tag.removePrefix("v")
}

object Versions {
    /**
     * Compares versions the semver way, ignoring a leading "v", "+build" metadata and the debug
     * build's "-debug" suffix. The dotted core compares numerically ("0.10.0" > "0.9.2"), missing
     * parts counting as zero ("1.2" == "1.2.0"). A pre-release comes before its release
     * ("0.5.0-beta.1" < "0.5.0"), and pre-releases compare part by part, numbers numerically and
     * below words ("beta.2" < "beta.10" < "rc.1").
     */
    fun compare(a: String, b: String): Int {
        val (coreA, preA) = split(a)
        val (coreB, preB) = split(b)
        for (i in 0 until maxOf(coreA.size, coreB.size)) {
            val c = coreA.getOrElse(i) { 0 }.compareTo(coreB.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return when {
            preA == null && preB == null -> 0
            preA == null -> 1
            preB == null -> -1
            else -> comparePre(preA, preB)
        }
    }

    fun isNewer(candidate: String, current: String): Boolean = compare(candidate, current) > 0

    /** The numeric core and the pre-release part (null for a release). */
    private fun split(v: String): Pair<List<Int>, List<String>?> {
        val plain = v.trim().removePrefix("v").substringBefore('+').removeSuffix("-debug")
        val core = plain.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val pre = plain.substringAfter('-', missingDelimiterValue = "").takeIf { it.isNotEmpty() }
        return core to pre?.split('.')
    }

    private fun comparePre(a: List<String>, b: List<String>): Int {
        for (i in 0 until minOf(a.size, b.size)) {
            val na = a[i].toIntOrNull()
            val nb = b[i].toIntOrNull()
            val c = when {
                na != null && nb != null -> na.compareTo(nb)
                na != null -> -1
                nb != null -> 1
                else -> a[i].compareTo(b[i])
            }
            if (c != 0) return c
        }
        return a.size.compareTo(b.size)
    }

    /** The newest of [releases] by version, or null if there are none. */
    fun newest(releases: List<Release>): Release? =
        releases.maxWithOrNull { x, y -> compare(x.version, y.version) }

    /** The APK to install from a release's assets: the first `.apk`, if any. */
    fun pickApk(assets: List<Release.Asset>): Release.Asset? =
        assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
}
