package com.lagradost.cloudstream3.utils

import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * StreamLogger — transparent session logger for CloudStream.
 *
 * Logs:
 *  - Stream URLs, type, referer, headers (from open-source extension repos)
 *  - DRM info: licenseUrl, kid, key, uuid, kty, keyRequestParameters
 *  - Repo/API link callbacks: source, url, quality, headers
 *
 * Auto-saves log to /storage/emulated/0/Download/CloudStream_Logs/
 * at session end (onDestroy) with a timestamped filename.
 */
object StreamLogger {

    private const val TAG = "StreamLogger"
    private val sessionStart = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
    private val logLines = mutableListOf<String>()
    private val lock = Any()

    // ── internal helpers ─────────────────────────────────────────────────────

    private fun timestamp(): String =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())

    private fun append(section: String, lines: List<Pair<String, String?>>) {
        val sb = StringBuilder()
        sb.appendLine("┌─── $section [${timestamp()}]")
        for ((k, v) in lines) {
            if (!v.isNullOrBlank()) sb.appendLine("│  $k: $v")
        }
        sb.append("└" + "─".repeat(60))
        val entry = sb.toString()
        Log.d(TAG, entry)
        synchronized(lock) { logLines.add(entry) }
    }

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Called from CS3IPlayer.loadOnlinePlayer() when a stream is about to play.
     */
    fun logStreamPlay(
        source: String?,
        name: String?,
        url: String?,
        referer: String?,
        type: String?,
        quality: Int?,
        headers: Map<String, String>?,
        // DRM fields from DrmExtractorLink (all exposed by open-source extensions)
        licenseUrl: String? = null,
        kid: String? = null,
        key: String? = null,
        uuid: String? = null,
        kty: String? = null,
        keyRequestParameters: Map<String, String>? = null
    ) {
        val fields = mutableListOf(
            "Source"    to source,
            "Name"      to name,
            "URL"       to url,
            "Referer"   to referer,
            "Type"      to type,
            "Quality"   to quality?.toString(),
        )
        headers?.forEach { (k, v) -> fields.add("Header[$k]" to v) }

        // DRM section — only appended when present (ClearKey / Widevine license info
        // that is publicly defined in the extension source code)
        if (!licenseUrl.isNullOrBlank() || !kid.isNullOrBlank() || !key.isNullOrBlank()) {
            fields.add("── DRM ──" to "")
            fields.add("LicenseURL"  to licenseUrl)
            fields.add("KID"         to kid)
            fields.add("Key"         to key)
            fields.add("UUID"        to uuid)
            fields.add("KTY"         to kty)
            keyRequestParameters?.forEach { (k, v) ->
                fields.add("KeyReqParam[$k]" to v)
            }
        }

        append("PLAY STREAM", fields)
    }

    /**
     * Called from RepoLinkGenerator when an ExtractorLink comes back from the API.
     */
    fun logExtractorLink(
        source: String?,
        name: String?,
        url: String?,
        referer: String?,
        type: String?,
        quality: Int?,
        headers: Map<String, String>?,
        extractorData: String? = null
    ) {
        val fields = mutableListOf(
            "Source"        to source,
            "Name"          to name,
            "URL"           to url,
            "Referer"       to referer,
            "Type"          to type,
            "Quality"       to quality?.toString(),
            "ExtractorData" to extractorData,
        )
        headers?.forEach { (k, v) -> fields.add("Header[$k]" to v) }
        append("EXTRACTOR LINK", fields)
    }

    /**
     * Called from RepoLinkGenerator when the API is invoked for a repo entry.
     */
    fun logApiCall(
        apiName: String?,
        data: String?,
        episodeTitle: String? = null
    ) {
        append("API CALL", listOf(
            "API"     to apiName,
            "Data"    to data,
            "Episode" to episodeTitle,
        ))
    }

    // ── save ──────────────────────────────────────────────────────────────────

    /**
     * Saves the current session log to
     *   /storage/emulated/0/Download/CloudStream_Logs/session_<timestamp>.txt
     *
     * Called from MainActivity.onDestroy().
     */
    fun saveAndFlush() {
        try {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "CloudStream_Logs"
            )
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, "session_$sessionStart.txt")
            val header = buildString {
                appendLine("═".repeat(64))
                appendLine("  CloudStream Session Log")
                appendLine("  Session started : $sessionStart")
                appendLine("  Saved at        : ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                appendLine("═".repeat(64))
                appendLine()
            }

            synchronized(lock) {
                file.writeText(header + logLines.joinToString("\n\n"))
                Log.i(TAG, "Session log saved → ${file.absolutePath}  (${logLines.size} entries)")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to save session log", t)
        }
    }
}
