package io.github.hassanm0301.nothingisfree.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionsTest {

    @Test
    fun `compares numerically, not as text`() {
        assertTrue(Versions.isNewer("0.10.0", "0.9.2"))
        assertTrue(Versions.isNewer("1.0.0", "0.99.99"))
        assertFalse(Versions.isNewer("0.1.0", "0.1.0"))
        assertFalse(Versions.isNewer("0.1.0", "0.2.0"))
    }

    @Test
    fun `ignores a leading v, suffixes and missing parts`() {
        assertEquals(0, Versions.compare("v0.1.0", "0.1.0"))
        assertEquals(0, Versions.compare("1.2", "1.2.0"))
        assertEquals(0, Versions.compare("0.2.0-debug", "0.2.0"))
        assertTrue(Versions.isNewer("v0.2.0", "0.1.0"))
    }

    @Test
    fun `release version drops the tag prefix`() {
        assertEquals("0.1.0", Release("v0.1.0", "", "", "", null).version)
    }

    @Test
    fun `picks the first apk asset`() {
        val notes = Release.Asset("notes.txt", "u1", 1)
        val apk = Release.Asset("nothing-is-free-v0.1.0-debug.APK", "u2", 2)
        assertEquals(apk, Versions.pickApk(listOf(notes, apk)))
        assertNull(Versions.pickApk(listOf(notes)))
    }

    @Test
    fun `a pre-release comes before its release and after the one before`() {
        assertTrue(Versions.isNewer("0.5.0", "0.5.0-beta.1"))
        assertTrue(Versions.isNewer("0.5.0-beta.1", "0.4.0"))
        assertFalse(Versions.isNewer("0.5.0-beta.1", "0.5.0"))
        assertEquals(0, Versions.compare("v0.5.0-beta.1", "0.5.0-beta.1"))
        assertEquals(0, Versions.compare("0.5.0-beta.1-debug", "0.5.0-beta.1"))
    }

    @Test
    fun `pre-releases compare part by part, numbers numerically`() {
        assertTrue(Versions.isNewer("0.5.0-beta.10", "0.5.0-beta.2"))
        assertTrue(Versions.isNewer("0.5.0-rc.1", "0.5.0-beta.10"))
        assertTrue(Versions.isNewer("0.5.0-beta.1", "0.5.0-beta"))
        assertTrue(Versions.isNewer("0.5.0-beta", "0.5.0-1"))
    }

    @Test
    fun `newest picks the highest version, not the latest published`() {
        fun r(tag: String, pre: Boolean = false) = Release(tag, tag, "", "", null, prerelease = pre)
        val releases = listOf(r("v0.6.0-beta.1", true), r("v0.5.1"), r("v0.6.0-beta.2", true), r("v0.5.0"))
        assertEquals("v0.6.0-beta.2", Versions.newest(releases)?.tag)
        assertEquals("v0.6.0", Versions.newest(releases + r("v0.6.0"))?.tag)
        assertNull(Versions.newest(emptyList()))
    }
}
