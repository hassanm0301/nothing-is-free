// Adapted from Essential Key Tools by KoukeNeko (MIT License, see docs/licenses/EssentialKeyTools.txt).
package io.github.hassanm0301.nothingisfree.essentialkey

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.net.toUri
import io.github.hassanm0301.nothingisfree.essentialkey.KeyAction.Kind
import java.net.URISyntaxException

/**
 * Runs a [KeyAction] from [service]. Everything fails soft, with a log line and a toast rather than
 * an exception: actions run from a key press on the service's main thread, with no UI of ours up.
 */
class ActionExecutor(private val service: AccessibilityService) {

    private val audio = service.getSystemService(AudioManager::class.java)
    private val camera = service.getSystemService(CameraManager::class.java)
    private val notifications = service.getSystemService(NotificationManager::class.java)
    private val keyguard = service.getSystemService(KeyguardManager::class.java)

    /** The torch's real state, kept up to date so the quick settings tile and this agree. */
    private val torchOn = mutableMapOf<String, Boolean>()
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            torchOn[cameraId] = enabled
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            torchOn[cameraId] = false
        }
    }
    private val torchCamera: String? by lazy { backCameraWithFlash() }
    private var silentHintShown = false

    init {
        camera?.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
    }

    fun release() {
        camera?.unregisterTorchCallback(torchCallback)
    }

    fun execute(action: KeyAction) {
        when (action) {
            KeyAction.None -> Unit
            is KeyAction.LaunchApp -> launchApp(action.packageName)
            is KeyAction.OpenLink -> openLink(action.uri)
            is KeyAction.Simple -> run(action.kind)
        }
    }

    private fun run(kind: Kind) {
        when (kind) {
            Kind.CAMERA -> openCamera()
            Kind.ASSISTANT -> openAssistant()
            Kind.FLASHLIGHT -> toggleFlashlight()
            Kind.RINGER_CYCLE -> cycleRinger()
            Kind.DO_NOT_DISTURB -> toggleDoNotDisturb()
            Kind.MUTE_MEDIA -> audio?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_TOGGLE_MUTE,
                AudioManager.FLAG_SHOW_UI,
            ) ?: toast("Audio isn't available")
            Kind.VOLUME_PANEL -> audio?.adjustSuggestedStreamVolume(
                AudioManager.ADJUST_SAME,
                AudioManager.USE_DEFAULT_STREAM_TYPE,
                AudioManager.FLAG_SHOW_UI,
            ) ?: toast("Audio isn't available")
            Kind.PLAY_PAUSE -> mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            Kind.NEXT_TRACK -> mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            Kind.PREVIOUS_TRACK -> mediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            Kind.SCREENSHOT -> global(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT, kind)
            Kind.LOCK_SCREEN -> global(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, kind)
            Kind.NOTIFICATIONS -> global(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, kind)
            Kind.QUICK_SETTINGS -> global(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS, kind)
            Kind.RECENTS -> global(AccessibilityService.GLOBAL_ACTION_RECENTS, kind)
            Kind.HOME -> global(AccessibilityService.GLOBAL_ACTION_HOME, kind)
            Kind.BACK -> global(AccessibilityService.GLOBAL_ACTION_BACK, kind)
            Kind.POWER_MENU -> global(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG, kind)
            Kind.SPLIT_SCREEN -> global(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN, kind)
            Kind.NONE, Kind.LAUNCH_APP, Kind.OPEN_LINK -> Unit
        }
    }

    private fun launchApp(packageName: String) {
        val intent = service.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            Log.w(TAG, "No launch intent for $packageName")
            toast("That app isn't installed any more")
            return
        }
        start(intent, "Couldn't open the app")
    }

    /** `intent:` URIs are parsed as intents (an app's shortcut, say); anything else is viewed. */
    private fun openLink(uri: String) {
        val intent = try {
            if (uri.startsWith("intent:", ignoreCase = true) || uri.startsWith("#Intent;")) {
                Intent.parseUri(uri, Intent.URI_INTENT_SCHEME).apply {
                    // Only an activity the user could open anyway; never one granting our URI access.
                    selector = null
                    flags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION).inv()
                }
            } else {
                Intent(Intent.ACTION_VIEW, uri.toUri())
            }
        } catch (e: URISyntaxException) {
            Log.w(TAG, "Bad link $uri", e)
            toast("That link isn't valid")
            return
        }
        start(intent, "Nothing on the phone opens that link")
    }

    private fun openCamera() {
        // Over the lock screen the camera opens in its locked mode, as from the lock screen's shortcut.
        val action = if (keyguard?.isKeyguardLocked == true) {
            MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE
        } else {
            MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
        }
        start(Intent(action), "No camera app found")
    }

    private fun openAssistant() {
        if (tryStart(Intent(Intent.ACTION_VOICE_COMMAND))) return
        start(Intent(Intent.ACTION_ASSIST), "No voice assistant is set up")
    }

    private fun start(intent: Intent, failure: String) {
        if (!tryStart(intent)) toast(failure)
    }

    private fun tryStart(intent: Intent): Boolean = try {
        service.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "Nothing handles $intent", e)
        false
    } catch (e: SecurityException) {
        Log.w(TAG, "Not allowed to start $intent", e)
        false
    }

    private fun toggleFlashlight() {
        val manager = camera
        val id = torchCamera
        if (manager == null || id == null) {
            toast("The flashlight isn't available")
            return
        }
        try {
            manager.setTorchMode(id, torchOn[id] != true)
        } catch (e: CameraAccessException) {
            // E.g. the camera app has the camera: the torch can't be used meanwhile.
            Log.w(TAG, "Torch toggle failed", e)
            toast("The flashlight is in use")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Torch toggle rejected", e)
            toast("The flashlight isn't available")
        }
    }

    private fun backCameraWithFlash(): String? = try {
        val manager = camera
        manager?.cameraIdList?.firstOrNull { id ->
            val c = manager.getCameraCharacteristics(id)
            c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    } catch (e: CameraAccessException) {
        Log.w(TAG, "Couldn't list cameras", e)
        null
    }

    private fun global(action: Int, kind: Kind) {
        if (!service.performGlobalAction(action)) {
            Log.w(TAG, "Global action $action wasn't performed")
            toast("Couldn't do ${kind.title.lowercase()}")
        }
    }

    /**
     * One media key to whichever app owns the media buttons. Down then up: play/pause is held
     * until its up arrives, so a lone down would never be delivered. Nothing listening is a no-op.
     */
    private fun mediaKey(code: Int) {
        val manager = audio ?: return toast("Audio isn't available")
        manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    private val policyAccess: Boolean get() = notifications?.isNotificationPolicyAccessGranted == true

    private fun cycleRinger() {
        val manager = audio ?: return toast("Audio isn't available")
        val access = policyAccess
        val next = nextRingerMode(manager.ringerMode, access)
            ?: return toast("Leaving silent needs Do Not Disturb access for Nothing is Free")
        try {
            manager.ringerMode = next
        } catch (e: SecurityException) {
            // OEM builds draw the Do Not Disturb line in slightly different places.
            Log.w(TAG, "Ringer change to $next denied", e)
            return toast("Android didn't allow the ringer change")
        }
        if (!access && !silentHintShown) {
            silentHintShown = true
            toast("Ring and vibrate only: silent needs Do Not Disturb access")
        }
    }

    private fun toggleDoNotDisturb() {
        val manager = notifications ?: return
        if (!manager.isNotificationPolicyAccessGranted) {
            return toast("Do Not Disturb needs access: Essential key › Permissions")
        }
        val next = if (manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        } else {
            NotificationManager.INTERRUPTION_FILTER_ALL
        }
        try {
            manager.setInterruptionFilter(next)
        } catch (e: SecurityException) {
            Log.w(TAG, "Do Not Disturb change denied", e)
            toast("Android didn't allow the Do Not Disturb change")
        }
    }

    private fun toast(message: String) {
        Toast.makeText(service, message, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val TAG = "EssentialKeyAction"
    }
}
