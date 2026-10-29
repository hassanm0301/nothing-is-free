package io.github.hassanm0301.nothingisfree.glyph.toy

/**
 * Shares one matrix connection between toys. The Glyph SDK's `GlyphMatrixManager` is a
 * process-wide singleton whose `init`/`unInit` are not reference counted, and the Glyph service
 * may bind a newly picked toy before unbinding the old one. So: [open] runs for the first toy and
 * [close] after the last one leaves, and only the most recently bound toy draws. If it leaves
 * first, the one before it takes the matrix back.
 *
 * SDK-free so it can be tested; [M] is the connected matrix. Main thread only, like the service
 * callbacks that drive it.
 */
class MatrixArbiter<M>(
    private val open: (onConnected: (M) -> Unit, onDisconnected: () -> Unit) -> Unit,
    private val close: () -> Unit,
) {
    interface Client<M> {
        fun start(matrix: M)
        fun stop()
    }

    /** Bound toys, oldest first; the last one owns the matrix. */
    private val clients = ArrayDeque<Client<M>>()
    private var matrix: M? = null
    private var opened = false

    fun acquire(client: Client<M>) {
        if (client in clients) return
        matrix?.let { clients.lastOrNull()?.stop() }
        clients.addLast(client)
        if (!opened) {
            opened = true
            open(::onConnected, ::onDisconnected)
        } else {
            matrix?.let(client::start)
        }
    }

    fun release(client: Client<M>) {
        val wasOwner = clients.lastOrNull() === client
        if (!clients.remove(client)) return
        if (wasOwner) matrix?.let { client.stop() }
        if (clients.isEmpty()) {
            matrix = null
            opened = false
            close()
        } else if (wasOwner) {
            matrix?.let { clients.last().start(it) }
        }
    }

    private fun onConnected(connected: M) {
        if (!opened) return
        matrix = connected
        clients.lastOrNull()?.start(connected)
    }

    private fun onDisconnected() {
        if (matrix == null) return
        matrix = null
        clients.lastOrNull()?.stop()
    }
}
