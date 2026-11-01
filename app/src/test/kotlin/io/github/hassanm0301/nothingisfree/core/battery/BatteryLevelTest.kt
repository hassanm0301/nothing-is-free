package io.github.hassanm0301.nothingisfree.core.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryLevelTest {

    @Test
    fun `level over scale is a whole percentage, rounded down`() {
        assertEquals(73, BatteryLevel.percent(73, 100))
        assertEquals(100, BatteryLevel.percent(100, 100))
        assertEquals(0, BatteryLevel.percent(0, 100))
        assertEquals(49, BatteryLevel.percent(99, 200))
    }

    @Test
    fun `missing or nonsense extras mean unknown, and overshoots are capped`() {
        assertNull(BatteryLevel.percent(-1, 100))
        assertNull(BatteryLevel.percent(50, -1))
        assertNull(BatteryLevel.percent(50, 0))
        assertEquals(100, BatteryLevel.percent(120, 100))
    }
}
