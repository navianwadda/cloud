package com.lagradost.cloudstream3.utils

import com.lagradost.cloudstream3.utils.downloader.DownloadObjects

/**
 * Compatibility shim — VideoDownloadHelper classes were moved to DownloadObjects.
 */
object VideoDownloadHelper {
    typealias DownloadCached        = DownloadObjects.DownloadCached
    typealias DownloadEpisodeCached = DownloadObjects.DownloadEpisodeCached
    typealias DownloadHeaderCached  = DownloadObjects.DownloadHeaderCached
    typealias ResumeWatching        = DownloadObjects.ResumeWatching
}
