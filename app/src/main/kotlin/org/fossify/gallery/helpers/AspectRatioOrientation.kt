package org.fossify.gallery.helpers

import android.content.pm.ActivityInfo
import java.io.File

/**
 * Determines the screen orientation that matches the aspect ratio of a media file.
 *
 * The orientation of a file is decided only once, so that the media is never rotated twice, no
 * matter if the dimensions were read from the file itself or reported by the media player.
 */
object AspectRatioOrientation {
    private const val MAX_DECISIONS = 100
    private const val CACHE_INITIAL_CAPACITY = 16
    private const val CACHE_LOAD_FACTOR = 0.75f

    private val decisions = object : LinkedHashMap<String, Int>(CACHE_INITIAL_CAPACITY, CACHE_LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>) = size > MAX_DECISIONS
    }

    /**
     * The key of a file in the cache. It contains the modification time and the size of the file, so
     * that a changed file is not mistaken for the one that was already handled.
     */
    fun getKey(path: String): String {
        val file = File(path)
        return "$path|${file.lastModified()}|${file.length()}"
    }

    /**
     * The orientation that was already decided for the given file, if there is any.
     */
    fun getDecision(key: String): Int? = synchronized(decisions) { decisions[key] }

    /**
     * Stores the orientation decided for the given file. Returns false if an orientation was
     * already decided for it, in which case the existing one has to be used.
     */
    fun decide(key: String, orientation: Int): Boolean = synchronized(decisions) {
        if (decisions.containsKey(key)) {
            false
        } else {
            decisions[key] = orientation
            true
        }
    }

    /**
     * Returns the orientation matching the aspect ratio of the given file, 0 if the media is
     * square, or null if the dimensions of the file could not be determined. Must be called from a
     * background thread.
     */
    fun resolve(path: String): Int? {
        val file = File(path)
        if (!file.exists()) {
            return null
        }

        val size = when (MediaDimensions.getMediaType(path)) {
            TYPE_VIDEOS -> MediaDimensions.getVideoSize(path)
            TYPE_GIFS, TYPE_IMAGES -> MediaDimensions.getImageSize(path)
            else -> null
        } ?: return null

        // A square media keeps the current orientation, which is stored as no rotation at all.
        return getOrientation(size.x, size.y, MediaDimensions.getRotationDegrees(path)) ?: 0
    }

    /**
     * Returns the orientation matching the given dimensions, or null if the media is square, in
     * which case the current orientation should be kept.
     */
    fun getOrientation(width: Int, height: Int, rotationDegrees: Int): Int? {
        if (width <= 0 || height <= 0) {
            return null
        }

        val flipSides = MediaDimensions.flipsSides(rotationDegrees)
        val effectiveWidth = if (flipSides) height else width
        val effectiveHeight = if (flipSides) width else height

        return when {
            effectiveWidth > effectiveHeight -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            effectiveWidth < effectiveHeight -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> null
        }
    }
}
