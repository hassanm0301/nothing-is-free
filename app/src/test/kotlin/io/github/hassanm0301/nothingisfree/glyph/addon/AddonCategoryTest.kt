package io.github.hassanm0301.nothingisfree.glyph.addon

import io.github.hassanm0301.nothingisfree.glyph.toy.ToyId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddonCategoryTest {

    private val digital = ToyId.DIGITAL_CLOCK
    private val binary = ToyId.BINARY_CLOCK

    private val start = mapOf(
        AddonId.NOTIFICATION_PULSE to setOf(digital),
        AddonId.BATTERY_LEVEL to setOf(digital, binary),
        AddonId.BINARY_BATTERY to emptySet(),
    )

    @Test
    fun `the addons come in two categories`() {
        assertEquals(AddonCategory.NOTIFICATION, AddonId.NOTIFICATION_PULSE.category)
        assertEquals(AddonCategory.BATTERY, AddonId.BATTERY_LEVEL.category)
        assertEquals(AddonCategory.BATTERY, AddonId.BINARY_BATTERY.category)
    }

    @Test
    fun `switching one on switches the other of its category off, on that toy only`() {
        val after = AddonPreferences.withAddon(start, AddonId.BINARY_BATTERY, digital, on = true)
        assertEquals(setOf(digital), after[AddonId.BINARY_BATTERY])
        assertEquals(setOf(binary), after[AddonId.BATTERY_LEVEL])
        // Another category is untouched.
        assertEquals(setOf(digital), after[AddonId.NOTIFICATION_PULSE])
    }

    @Test
    fun `switching one off leaves the rest alone`() {
        val after = AddonPreferences.withAddon(start, AddonId.BATTERY_LEVEL, binary, on = false)
        assertEquals(setOf(digital), after[AddonId.BATTERY_LEVEL])
        assertEquals(emptySet<ToyId>(), after[AddonId.BINARY_BATTERY])
        assertEquals(setOf(digital), after[AddonId.NOTIFICATION_PULSE])
    }

    @Test
    fun `settings saved with two of a category keep the first`() {
        val both = start + (AddonId.BINARY_BATTERY to setOf(digital))
        val fixed = AddonPreferences.onePerCategory(both)
        assertEquals(setOf(digital, binary), fixed[AddonId.BATTERY_LEVEL])
        assertEquals(emptySet<ToyId>(), fixed[AddonId.BINARY_BATTERY])
        assertEquals(setOf(digital), fixed[AddonId.NOTIFICATION_PULSE])
    }

    @Test
    fun `a toy draws the settings of its battery addon, or none`() {
        val bar = BatterySettings()
        val bcd = BinaryBatterySettings()
        assertEquals(bar, AddonPreferences.batteryLook(start, digital, bar, bcd))
        val switched = AddonPreferences.withAddon(start, AddonId.BINARY_BATTERY, digital, on = true)
        assertEquals(bcd, AddonPreferences.batteryLook(switched, digital, bar, bcd))
        val none = AddonPreferences.withAddon(start, AddonId.BATTERY_LEVEL, binary, on = false)
        assertNull(AddonPreferences.batteryLook(none, binary, bar, bcd))
        assertEquals(AddonId.NOTIFICATION_PULSE, AddonPreferences.active(start, AddonCategory.NOTIFICATION, digital))
        assertNull(AddonPreferences.active(start, AddonCategory.NOTIFICATION, binary))
    }
}
