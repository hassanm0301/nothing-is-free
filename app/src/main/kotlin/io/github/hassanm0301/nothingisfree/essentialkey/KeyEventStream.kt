// Adapted from Essential Key Tools by KoukeNeko (MIT License, see docs/licenses/EssentialKeyTools.txt).
package io.github.hassanm0301.nothingisfree.essentialkey

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** A key event the service saw, flattened for the UI. */
data class ObservedKeyEvent(val keyCode: Int, val scanCode: Int, val down: Boolean, val atMs: Long)

/** A gesture the service recognised on the Essential Key. */
data class ObservedGesture(val gesture: KeyGesture, val atMs: Long)

/**
 * From [EssentialKeyService] to the screens that watch the key (same process, so a singleton).
 *
 * [learning] is set by Key setup: the service then publishes every key (bar the volume keys) so the
 * Essential Key's scan code can be picked out, and runs nothing. [testing] is set by Key test:
 * gestures are recognised and published, but their actions don't run.
 */
object KeyEventStream {
    private val _keys = MutableSharedFlow<ObservedKeyEvent>(extraBufferCapacity = BUFFER)
    private val _gestures = MutableSharedFlow<ObservedGesture>(extraBufferCapacity = BUFFER)

    /** Only events seen while collecting; nothing is replayed. */
    val keys: SharedFlow<ObservedKeyEvent> = _keys.asSharedFlow()
    val gestures: SharedFlow<ObservedGesture> = _gestures.asSharedFlow()

    @Volatile var learning: Boolean = false
    @Volatile var testing: Boolean = false

    fun publish(event: ObservedKeyEvent) {
        _keys.tryEmit(event)
    }

    fun publish(gesture: ObservedGesture) {
        _gestures.tryEmit(gesture)
    }

    private const val BUFFER = 32
}
