package io.github.hassanm0301.nothingisfree.glyph.addon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddonCategoryTest {

    private val start = setOf(AddonId.NOTIFICATION_PULSE, AddonId.BATTERY_LEVEL)

    @Test
    fun `the addons come in two categories`() {
        assertEquals(AddonCategory.NOTIFICATION, AddonId.NOTIFICATION_PULSE.category)
        assertEquals(AddonCategory.BATTERY, AddonId.BATTERY_LEVEL.category)
        assertEquals(AddonCategory.BATTERY, AddonId.BINARY_BATTERY.category)
    }

    @Test
    fun `switching one on switches the other of its category off`() {
        val after = AddonPreferences.withAddon(start, AddonId.BINARY_BATTERY, on = true)
        assertEquals(setOf(AddonId.NOTIFICATION_PULSE, AddonId.BINARY_BATTERY), after)
    }

    @Test
    fun `switching one off leaves the rest alone`() {
        assertEquals(setOf(AddonId.NOTIFICATION_PULSE), AddonPreferences.withAddon(start, AddonId.BATTERY_LEVEL, on = false))
        assertEquals(setOf(AddonId.BATTERY_LEVEL), AddonPreferences.withAddon(start, AddonId.NOTIFICATION_PULSE, on = false))
        // Switching off one that is already off changes nothing.
        assertEquals(start, AddonPreferences.withAddon(start, AddonId.BINARY_BATTERY, on = false))
    }

    @Test
    fun `switching one on twice keeps it on`() {
        assertEquals(start, AddonPreferences.withAddon(start, AddonId.BATTERY_LEVEL, on = true))
    }

    @Test
    fun `two of a category carried over from per-toy switches keep the first`() {
        val fixed = AddonPreferences.onePerCategory(AddonId.entries.toSet())
        assertEquals(setOf(AddonId.NOTIFICATION_PULSE, AddonId.BATTERY_LEVEL), fixed)
    }

    @Test
    fun `every toy draws the settings of the battery addon that is on, or none`() {
        val bar = BatterySettings()
        val bcd = BinaryBatterySettings()
        assertEquals(bar, AddonPreferences.batteryLook(start, bar, bcd))
        val switched = AddonPreferences.withAddon(start, AddonId.BINARY_BATTERY, on = true)
        assertEquals(bcd, AddonPreferences.batteryLook(switched, bar, bcd))
        assertNull(AddonPreferences.batteryLook(setOf(AddonId.NOTIFICATION_PULSE), bar, bcd))
        assertEquals(AddonId.NOTIFICATION_PULSE, AddonPreferences.active(start, AddonCategory.NOTIFICATION))
        assertNull(AddonPreferences.active(setOf(AddonId.BINARY_BATTERY), AddonCategory.NOTIFICATION))
    }

    @Test
    fun `an addon starts as last switched, else on if any toy had it, else its default`() {
        // Switched since addons went global: that wins.
        assertTrue(addonStartsOn(saved = true, legacyToys = emptySet(), default = false))
        assertFalse(addonStartsOn(saved = false, legacyToys = setOf("DIGITAL_CLOCK"), default = true))
        // Only the old per-toy switches: on if it was on for any toy that still exists.
        assertTrue(addonStartsOn(saved = null, legacyToys = setOf("BINARY_CLOCK"), default = false))
        assertFalse(addonStartsOn(saved = null, legacyToys = emptySet(), default = true))
        assertFalse(addonStartsOn(saved = null, legacyToys = setOf("PULSE_CLOCK"), default = true))
        // Never set at all.
        assertTrue(addonStartsOn(saved = null, legacyToys = null, default = true))
        assertFalse(addonStartsOn(saved = null, legacyToys = null, default = false))
    }
}
