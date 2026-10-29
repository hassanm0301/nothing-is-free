package io.github.hassanm0301.nothingisfree.core.update

/** A published GitHub release, trimmed to what an update needs. */
data class Release(
    val tag: String,
    val name: String,
    val notes: String,
    val pageUrl: String,
    val apk: Asset?,
) {
    data class Asset(val name: String, val downloadUrl: String, val sizeBytes: Long)

    val version: String get() = tag.removePrefix("v")
}

object Versions {
    /**
     * Compares dotted versions numerically ("0.10.0" > "0.9.2"), ignoring a leading "v" and any
     * "-suffix". Missing parts count as zero, so "1.2" == "1.2.0".
     */
    fun compare(a: String, b: String): Int {
        val pa = parts(a)
        val pb = parts(b)
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val c = pa.getOrElse(i) { 0 }.compareTo(pb.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }

    fun isNewer(candidate: String, current: String): Boolean = compare(candidate, current) > 0

    private fun parts(v: String): List<Int> =
        v.trim().removePrefix("v").substringBefore('-').substringBefore('+')
            .split('.').map { it.toIntOrNull() ?: 0 }

    /** The APK to install from a release's assets: the first `.apk`, if any. */
    fun pickApk(assets: List<Release.Asset>): Release.Asset? =
        assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
}
