package io.github.hassanm0301.nothingisfree.essentialkey

import io.github.hassanm0301.nothingisfree.essentialkey.KeyAction.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyActionTest {

    @Test
    fun `every action survives being stored and read back`() {
        val actions = Kind.entries.map { kind ->
            when (kind) {
                Kind.LAUNCH_APP -> KeyAction.LaunchApp("com.example.app")
                Kind.OPEN_LINK -> KeyAction.OpenLink("https://nothing.tech")
                else -> KeyAction.of(kind)
            }
        }
        actions.forEach { action ->
            assertEquals(action, KeyAction.fromStored(action.kind.id, KeyAction.payloadOf(action)))
        }
    }

    @Test
    fun `stored ids are unique`() {
        assertEquals(Kind.entries.size, Kind.entries.map { it.id }.toSet().size)
    }

    @Test
    fun `ids shared with Essential Key Tools keep their spelling`() {
        assertEquals("launch_app", Kind.LAUNCH_APP.id)
        assertEquals("toggle_flashlight", Kind.FLASHLIGHT.id)
        assertEquals("take_screenshot", Kind.SCREENSHOT.id)
        assertEquals("ringer_cycle", Kind.RINGER_CYCLE.id)
    }

    @Test
    fun `unknown, missing or payload-less actions read as none`() {
        assertEquals(KeyAction.None, KeyAction.fromStored(null, null))
        assertEquals(KeyAction.None, KeyAction.fromStored("no_such_action", null))
        assertEquals(KeyAction.None, KeyAction.fromStored("launch_app", null))
        assertEquals(KeyAction.None, KeyAction.fromStored("open_link", "  "))
    }

    @Test
    fun `links are trimmed`() {
        assertEquals(KeyAction.OpenLink("tel:123"), KeyAction.fromStored("open_link", " tel:123 "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a simple action can't be made for a kind with a payload`() {
        KeyAction.Simple(Kind.LAUNCH_APP)
    }

    @Test
    fun `the map decodes what was stored and treats the rest as none`() {
        val stored = mapOf(
            GestureActionMap.idKey(KeyGesture.DOUBLE_PRESS) to "toggle_flashlight",
            GestureActionMap.idKey(KeyGesture.LONG_PRESS) to "launch_app",
            GestureActionMap.payloadKey(KeyGesture.LONG_PRESS) to "com.example.app",
        )
        val map = GestureActionMap.decode(stored::get)
        assertEquals(KeyAction.None, map.actionFor(KeyGesture.SINGLE_PRESS))
        assertEquals(KeyAction.of(Kind.FLASHLIGHT), map.actionFor(KeyGesture.DOUBLE_PRESS))
        assertEquals(KeyAction.LaunchApp("com.example.app"), map.actionFor(KeyGesture.LONG_PRESS))
        assertEquals(setOf(KeyGesture.DOUBLE_PRESS, KeyGesture.LONG_PRESS), map.activeGestures())
        assertTrue(map.uses(Kind.FLASHLIGHT))
        assertFalse(map.uses(Kind.DO_NOT_DISTURB))
    }
}
