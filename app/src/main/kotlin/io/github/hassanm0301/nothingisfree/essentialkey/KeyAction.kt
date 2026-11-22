// Adapted from Essential Key Tools by KoukeNeko (MIT License, see docs/licenses/EssentialKeyTools.txt).
package io.github.hassanm0301.nothingisfree.essentialkey

/**
 * Something the Essential Key can do for a gesture. Plain data, so it can be stored and tested on
 * the JVM; [ActionExecutor] runs it. Each kind has a stable [Kind.id] that is what gets stored, plus
 * a payload for the kinds that carry one (an app's package, a link).
 */
sealed interface KeyAction {

    val kind: Kind

    /** Does nothing: the gesture is left alone. */
    data object None : KeyAction { override val kind = Kind.NONE }

    /** Opens an installed app. */
    data class LaunchApp(val packageName: String) : KeyAction { override val kind = Kind.LAUNCH_APP }

    /** Opens a link or any URI: `https:`, `tel:`, `geo:`, an app's deep link, or an `intent:` URI. */
    data class OpenLink(val uri: String) : KeyAction { override val kind = Kind.OPEN_LINK }

    /** Every action without a payload: one per [Kind] that has none. */
    data class Simple(override val kind: Kind) : KeyAction {
        init {
            require(!kind.hasPayload && kind != Kind.NONE) { "$kind is not a simple action" }
        }
    }

    /**
     * What an action does. [id] is stored and must never change; [group] sorts the picker.
     * New kinds go anywhere in the list, since only [id] is stored.
     */
    enum class Kind(val id: String, val group: Group, val title: String, val summary: String) {
        NONE("none", Group.NONE, "Nothing", "The gesture does nothing"),

        LAUNCH_APP("launch_app", Group.OPEN, "Open an app", "Any app on the phone"),
        OPEN_LINK("open_link", Group.OPEN, "Open a link", "A web page, a deep link, a phone number or an intent: URI"),
        CAMERA("open_camera", Group.OPEN, "Camera", "Opens the camera, ready for a photo"),
        ASSISTANT("voice_assistant", Group.OPEN, "Voice assistant", "Starts the phone's voice assistant"),

        FLASHLIGHT("toggle_flashlight", Group.TOGGLE, "Flashlight", "Turns the torch on or off"),
        RINGER_CYCLE("ringer_cycle", Group.TOGGLE, "Ring · vibrate · silent", "Steps the ringer to its next mode"),
        DO_NOT_DISTURB("toggle_dnd", Group.TOGGLE, "Do Not Disturb", "Turns Do Not Disturb on or off"),
        MUTE_MEDIA("toggle_mute_media", Group.TOGGLE, "Mute media", "Mutes or unmutes music and video"),

        PLAY_PAUSE("media_play_pause", Group.MEDIA, "Play / pause", "Whatever is playing"),
        NEXT_TRACK("media_next", Group.MEDIA, "Next track", "Skips forward"),
        PREVIOUS_TRACK("media_previous", Group.MEDIA, "Previous track", "Skips back, or restarts the track"),
        VOLUME_PANEL("volume_panel", Group.MEDIA, "Volume panel", "Shows the volume bar"),

        SCREENSHOT("take_screenshot", Group.SYSTEM, "Screenshot", "Takes a screenshot"),
        LOCK_SCREEN("lock_screen", Group.SYSTEM, "Lock screen", "Turns the screen off and locks the phone"),
        NOTIFICATIONS("notifications", Group.SYSTEM, "Notifications", "Pulls down the notification shade"),
        QUICK_SETTINGS("quick_settings", Group.SYSTEM, "Quick settings", "Pulls down the quick settings"),
        RECENTS("recents", Group.SYSTEM, "Recent apps", "Opens the app switcher"),
        HOME("home", Group.SYSTEM, "Home", "Goes to the home screen"),
        BACK("back", Group.SYSTEM, "Back", "Goes back, like the back gesture"),
        POWER_MENU("power_menu", Group.SYSTEM, "Power menu", "Opens the power menu"),
        SPLIT_SCREEN("split_screen", Group.SYSTEM, "Split screen", "Splits the screen with the current app"),
        ;

        val hasPayload: Boolean get() = this == LAUNCH_APP || this == OPEN_LINK

        /** Needs the key's accessibility service to run (it always runs there, but the UI says so). */
        val usesGlobalAction: Boolean get() = group == Group.SYSTEM

        companion object {
            fun fromId(id: String?): Kind? = entries.firstOrNull { it.id == id }
        }
    }

    enum class Group(val title: String) {
        NONE("Off"),
        OPEN("Open"),
        TOGGLE("Switch"),
        MEDIA("Media"),
        SYSTEM("System"),
    }

    companion object {
        /** The action for a kind without a payload. */
        fun of(kind: Kind): KeyAction = when (kind) {
            Kind.NONE -> None
            Kind.LAUNCH_APP, Kind.OPEN_LINK -> error("$kind needs a payload")
            else -> Simple(kind)
        }

        /**
         * Rebuilds an action from what was stored. Anything unknown or missing its payload reads as
         * [None], so a change in what is stored can never crash the reader.
         */
        fun fromStored(id: String?, payload: String?): KeyAction {
            val kind = Kind.fromId(id) ?: return None
            return when (kind) {
                Kind.NONE -> None
                Kind.LAUNCH_APP -> if (payload.isNullOrBlank()) None else LaunchApp(payload)
                Kind.OPEN_LINK -> if (payload.isNullOrBlank()) None else OpenLink(payload.trim())
                else -> Simple(kind)
            }
        }

        /** The payload stored beside [action]'s id, or null if it has none. */
        fun payloadOf(action: KeyAction): String? = when (action) {
            is LaunchApp -> action.packageName
            is OpenLink -> action.uri
            else -> null
        }
    }
}
