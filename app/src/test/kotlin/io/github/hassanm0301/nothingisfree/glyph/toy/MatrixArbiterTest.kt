package io.github.hassanm0301.nothingisfree.glyph.toy

import org.junit.Assert.assertEquals
import org.junit.Test

class MatrixArbiterTest {

    private val log = mutableListOf<String>()
    private var connect: ((String) -> Unit)? = null
    private var disconnect: (() -> Unit)? = null

    private val arbiter = MatrixArbiter<String>(
        open = { onConnected, onDisconnected ->
            log += "open"
            connect = onConnected
            disconnect = onDisconnected
        },
        close = { log += "close" },
    )

    private fun toy(name: String) = object : MatrixArbiter.Client<String> {
        override fun start(matrix: String) {
            log += "$name start"
        }

        override fun stop() {
            log += "$name stop"
        }
    }

    @Test
    fun `one toy opens on bind, draws once connected, closes on unbind`() {
        val pulse = toy("pulse")
        arbiter.acquire(pulse)
        connect!!("m")
        arbiter.release(pulse)
        assertEquals(listOf("open", "pulse start", "pulse stop", "close"), log)
    }

    @Test
    fun `a toy bound before the old one unbinds takes over without closing the matrix`() {
        val pulse = toy("pulse")
        val binary = toy("binary")
        arbiter.acquire(pulse)
        connect!!("m")
        arbiter.acquire(binary)
        arbiter.release(pulse)
        arbiter.release(binary)
        assertEquals(
            listOf("open", "pulse start", "pulse stop", "binary start", "binary stop", "close"),
            log,
        )
    }

    @Test
    fun `when the newest toy leaves first the older one takes the matrix back`() {
        val pulse = toy("pulse")
        val binary = toy("binary")
        arbiter.acquire(pulse)
        connect!!("m")
        arbiter.acquire(binary)
        log.clear()
        arbiter.release(binary)
        assertEquals(listOf("binary stop", "pulse start"), log)
    }

    @Test
    fun `a disconnect stops the owner and a reconnect starts it again`() {
        val binary = toy("binary")
        arbiter.acquire(binary)
        connect!!("m")
        disconnect!!()
        connect!!("m")
        assertEquals(listOf("open", "binary start", "binary stop", "binary start"), log)
    }

    @Test
    fun `unbinding before the connection arrives closes without drawing, and a late connect is ignored`() {
        val binary = toy("binary")
        arbiter.acquire(binary)
        arbiter.release(binary)
        connect!!("m")
        assertEquals(listOf("open", "close"), log)
    }

    @Test
    fun `rebinding after the last toy left opens a fresh connection`() {
        val binary = toy("binary")
        arbiter.acquire(binary)
        connect!!("m")
        arbiter.release(binary)
        arbiter.acquire(binary)
        assertEquals(listOf("open", "binary start", "binary stop", "close", "open"), log)
    }
}
