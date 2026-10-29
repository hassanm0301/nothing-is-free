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
}
