package io.github.hassanm0301.nothingisfree.core.update

import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Reads releases of this app's public GitHub repository. Blocking; call off the main thread. */
object GitHubReleases {
    const val REPO = "hassanm0301/nothing-is-free"
    private const val LATEST_URL = "https://api.github.com/repos/$REPO/releases/latest"

    /** The newest non-draft, non-prerelease release, or null if none is published yet. */
    fun latest(): Release? {
        val conn = open(LATEST_URL).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        try {
            return when (val code = conn.responseCode) {
                HttpURLConnection.HTTP_OK -> parse(conn.inputStream.use { it.readBytes().decodeToString() })
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> error("GitHub answered $code")
            }
        } finally {
            conn.disconnect()
        }
    }

    /** Streams a release asset; GitHub redirects to its CDN, which HttpURLConnection follows. */
    fun <T> download(asset: Release.Asset, block: (InputStream) -> T): T {
        val conn = open(asset.downloadUrl).apply { setRequestProperty("Accept", "application/octet-stream") }
        try {
            check(conn.responseCode == HttpURLConnection.HTTP_OK) { "Download failed (${conn.responseCode})" }
            return conn.inputStream.use(block)
        } finally {
            conn.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
        }

    private fun parse(json: String): Release {
        val o = JSONObject(json)
        val assets = o.optJSONArray("assets")
        val all = (0 until (assets?.length() ?: 0)).map { i ->
            val a = assets!!.getJSONObject(i)
            Release.Asset(a.getString("name"), a.getString("browser_download_url"), a.optLong("size"))
        }
        return Release(
            tag = o.getString("tag_name"),
            name = o.optString("name").ifBlank { o.getString("tag_name") },
            notes = if (o.isNull("body")) "" else o.getString("body"),
            pageUrl = o.optString("html_url"),
            apk = Versions.pickApk(all),
        )
    }
}
