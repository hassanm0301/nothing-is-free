// Adapted from Essential Key Tools by KoukeNeko (MIT License, see docs/licenses/EssentialKeyTools.txt).
package io.github.hassanm0301.nothingisfree.essentialkey

/** One [KeyAction] per [KeyGesture]; a gesture with none set does nothing. */
data class GestureActionMap(private val actions: Map<KeyGesture, KeyAction> = emptyMap()) {

    fun actionFor(gesture: KeyGesture): KeyAction = actions[gesture] ?: KeyAction.None

    fun with(gesture: KeyGesture, action: KeyAction): GestureActionMap =
        GestureActionMap(actions + (gesture to action))

    /** The gestures that do something; only these need recognising. */
    fun activeGestures(): Set<KeyGesture> =
        KeyGesture.entries.filterTo(mutableSetOf()) { actionFor(it) != KeyAction.None }

    fun uses(kind: KeyAction.Kind): Boolean = KeyGesture.entries.any { actionFor(it).kind == kind }

    companion object {
        val EMPTY = GestureActionMap()

        fun idKey(gesture: KeyGesture) = gesture.name.lowercase() + "_action_id"
        fun payloadKey(gesture: KeyGesture) = gesture.name.lowercase() + "_action_payload"

        /** Rebuilds the map from stored strings; [read] returns null for a key that isn't there. */
        fun decode(read: (String) -> String?): GestureActionMap = GestureActionMap(
            KeyGesture.entries.associateWith { KeyAction.fromStored(read(idKey(it)), read(payloadKey(it))) },
        )
    }
}

/** How each gesture reads in the app. */
val KeyGesture.title: String
    get() = when (this) {
        KeyGesture.SINGLE_PRESS -> "Single press"
        KeyGesture.DOUBLE_PRESS -> "Double press"
        KeyGesture.TRIPLE_PRESS -> "Triple press"
        KeyGesture.LONG_PRESS -> "Long press"
    }
