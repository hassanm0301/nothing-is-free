package io.github.hassanm0301.nothingisfree.essentialkey.unlock

import org.junit.Assert.assertEquals
import org.junit.Test

class PmFailureTest {

    @Test
    fun `an exception gives its message without the stack trace`() {
        val output = """
            Exception occurred while executing 'disable-user':
            java.lang.SecurityException: Cannot disable a protected package: com.nothing.ntessentialrecorder
            	at com.android.server.pm.PackageManagerService.setEnabledSettings(PackageManagerService.java:1)
        """.trimIndent()
        assertEquals("Cannot disable a protected package: com.nothing.ntessentialrecorder", pmFailure(output))
    }

    @Test
    fun `an error line is passed on as it is`() {
        assertEquals("Error: unknown package", pmFailure("Error: unknown package\n"))
    }

    @Test
    fun `silence still says something`() {
        assertEquals("pm failed without saying why", pmFailure(""))
    }

    @Test
    fun `the toggle command is the one App info uses`() {
        assertEquals(
            listOf("pm", "disable-user", "--user", "0", "com.nothing.ntessentialrecorder"),
            packageToggleCommand("com.nothing.ntessentialrecorder", enabled = false, userId = 0),
        )
    }
}
