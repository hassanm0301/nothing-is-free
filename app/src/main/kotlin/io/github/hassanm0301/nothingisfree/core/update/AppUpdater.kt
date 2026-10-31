package io.github.hassanm0301.nothingisfree.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.provider.Settings
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Self-update from GitHub releases: [check] looks for a release newer than the installed
 * version, [install] streams its APK into a [PackageInstaller] session. Android asks the user to
 * confirm (see [InstallResultReceiver]); once this app has installed itself once it is the
 * installer of record and later updates can go through without a prompt.
 *
 * The release APK must be signed with the same key as the installed app, or Android refuses it.
 */
object AppUpdater {

    sealed interface State {
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        data class Available(val release: Release) : State
        /** [progress] is 0..1, or null when the size is unknown. */
        data class Downloading(val release: Release, val progress: Float?) : State
        data class Installing(val release: Release) : State
        data class Failed(val message: String, val release: Release? = null) : State
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val busy: Boolean
        get() = _state.value.let { it is State.Checking || it is State.Downloading || it is State.Installing }

    private var checkJob: Job? = null

    fun currentVersion(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"

    /**
     * Looks for an update on the channel set in [UpdatePreferences]: releases only, or with beta
     * on, pre-releases too. A check already running is started over, so a channel switched
     * mid-check doesn't answer for the old one; a download or install is left alone.
     */
    fun check(context: Context) {
        if (_state.value.let { it is State.Downloading || it is State.Installing }) return
        val current = currentVersion(context)
        val beta = UpdatePreferences.beta.value
        checkJob?.cancel()
        _state.value = State.Checking
        checkJob = scope.launch {
            val result = runCatching { GitHubReleases.latest(includePrereleases = beta) }
            ensureActive()
            _state.value = result.fold(
                onSuccess = { release ->
                    when {
                        release == null || !Versions.isNewer(release.version, current) -> State.UpToDate
                        release.apk == null -> State.Failed("${release.tag} has no APK attached", release)
                        else -> State.Available(release)
                    }
                },
                onFailure = { State.Failed(it.message ?: "Couldn't reach GitHub") },
            )
        }
    }

    /** Whether the user has let this app install packages ("Install unknown apps"). */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())

    fun install(context: Context, release: Release) {
        val apk = release.apk ?: return
        if (busy) return
        val app = context.applicationContext
        _state.value = State.Downloading(release, 0f.takeIf { apk.sizeBytes > 0 })
        scope.launch {
            val installer = app.packageManager.packageInstaller
            var sessionId = -1
            try {
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(app.packageName)
                    if (apk.sizeBytes > 0) setSize(apk.sizeBytes)
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
                sessionId = installer.createSession(params)
                installer.openSession(sessionId).use { session ->
                    GitHubReleases.download(apk) { input ->
                        session.openWrite(apk.name, 0, apk.sizeBytes.takeIf { it > 0 } ?: -1).use { out ->
                            val buf = ByteArray(64 * 1024)
                            var total = 0L
                            var lastReported = -1
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                total += n
                                if (apk.sizeBytes > 0) {
                                    val pct = (total * 100 / apk.sizeBytes).toInt()
                                    if (pct != lastReported) {
                                        lastReported = pct
                                        _state.value = State.Downloading(release, pct / 100f)
                                    }
                                }
                            }
                            session.fsync(out)
                        }
                    }
                    _state.value = State.Installing(release)
                    session.commit(InstallResultReceiver.statusSender(app, sessionId))
                }
            } catch (e: Exception) {
                if (sessionId != -1) runCatching { installer.abandonSession(sessionId) }
                _state.value = State.Failed(e.message ?: "Update failed", release)
            }
        }
    }

    /** Called by [InstallResultReceiver] when the installer reports back. */
    internal fun onInstallResult(status: Int, message: String?) {
        val release = (_state.value as? State.Installing)?.release
        _state.value = when (status) {
            // On success the system restarts this process with the new version; nothing to show.
            PackageInstaller.STATUS_SUCCESS -> State.UpToDate
            PackageInstaller.STATUS_FAILURE_ABORTED -> release?.let(State::Available) ?: State.Idle
            else -> State.Failed(message ?: "Install failed ($status)", release)
        }
    }
}
