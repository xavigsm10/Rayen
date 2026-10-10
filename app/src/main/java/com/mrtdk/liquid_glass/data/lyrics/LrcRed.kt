package com.mrtdk.liquid_glass.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/**
 * lrc.red — Apple Music TTML, per-syllable, filed by ISRC.
 *
 * Every document lives at `https://lrc.red/s/{ISRC}.ttml`, so a track whose
 * recording is already known costs one request and cannot come back as the
 * wrong edit.
 */
object LrcRed {

    private const val BASE = "https://lrc.red/"

    private const val DURATION_TOLERANCE_SECONDS = 3.0

    /** `CC-XXX-YY-NNNNN` without the dashes — and nothing else goes into a path. */
    private val ISRC = Regex("""[A-Z]{2}[A-Z0-9]{3}\d{7}""")

    fun documentUrl(isrc: String): String? =
        isrc.trim().uppercase(Locale.ROOT).takeIf { ISRC.matches(it) }?.let { "${BASE}s/$it.ttml" }

    suspend fun lyrics(
        title: String,
        artist: String,
        durationMs: Long,
        isrc: String? = null,
        get: suspend (String) -> String? = { lyricsGet(it) },
    ): List<LyricLine>? = withContext(Dispatchers.IO) {
        val known = isrc?.let(::documentUrl)
        if (known != null) document(known, get)?.let { return@withContext it }

        val hit = search(title, artist, durationMs) ?: return@withContext null
        val found = hit.isrc?.let(::documentUrl)?.takeIf { it != known } ?: return@withContext null
        document(found, get)
    }

    private suspend fun document(url: String, get: suspend (String) -> String?): List<LyricLine>? =
        get(url)?.let(TtmlLyrics::parse)?.takeIf { it.isNotEmpty() }

    private fun search(title: String, artist: String, durationMs: Long): Hit? {
        val url = "${BASE}search.json".toHttpUrl().newBuilder()
            .addQueryParameter("q", "$title $artist".trim())
            .build()
        val body = lyricsGet(url.toString()) ?: return null
        val hits = runCatching { lyricsJson.decodeFromString<Response>(body) }.getOrNull()?.hits
            ?: return null
        return best(hits, title, artist, durationMs)
    }

    fun best(hits: List<Hit>, title: String, artist: String, durationMs: Long): Hit? {
        val wantedTitle = coreOf(title)
        val wantedVersion = versionOf(title)
        val wantedArtists = artistsOf(artist)
        val seconds = durationMs / 1000.0

        fun distance(hit: Hit): Double =
            if (durationMs > 0 && hit.duration != null) abs(hit.duration - seconds) else 0.0

        return hits
            .filter { hit ->
                val name = hit.title ?: return@filter false
                hit.isrc?.let(::documentUrl) != null &&
                    coreOf(name) == wantedTitle &&
                    versionOf(name) == wantedVersion &&
                    (wantedArtists.isEmpty() || artistsOf(hit.artist.orEmpty()).any { it in wantedArtists }) &&
                    distance(hit) <= DURATION_TOLERANCE_SECONDS
            }
            .minByOrNull(::distance)
    }

    private fun coreOf(title: String): String =
        normalized(title.replace(BRACKETED, " ").substringBefore(" - "))
            .ifEmpty { normalized(title) }

    private fun versionOf(title: String): Set<String> {
        val extras = BRACKETED.findAll(title).joinToString(" ") { it.value } +
            " " + title.substringAfter(" - ", "")
        return normalized(extras).split(' ').filter { it in VERSION_WORDS }.toSet()
    }

    private fun artistsOf(artist: String): Set<String> =
        artist.split(ARTIST_SEPARATORS).map(::normalized).filter { it.isNotEmpty() }.toSet()

    private fun normalized(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .lowercase(Locale.ROOT)
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .replace(WHITESPACE, " ")
            .trim()

    private val BRACKETED = Regex("""[(\[][^)\]]*[)\]]""")
    private val COMBINING_MARKS = Regex("""\p{Mn}+""")
    private val WHITESPACE = Regex("""\s+""")
    private val ARTIST_SEPARATORS = Regex(
        """\s*(?:,|&|;|/|\s+and\s+|\s+x\s+|\s+with\s+|\s+feat\.?\s+|\s+ft\.?\s+)\s*""",
        RegexOption.IGNORE_CASE,
    )

    private val VERSION_WORDS = setOf(
        "live", "remix", "remixed", "mix", "acoustic", "unplugged", "instrumental",
        "karaoke", "cappella", "acapella", "demo", "edit", "version", "cover",
        "sped", "slowed", "reverb", "nightcore", "lofi", "orchestral", "extended",
    )

    @Serializable
    data class Response(
        val hits: List<Hit>? = null,
        val next: String? = null,
    )

    @Serializable
    data class Hit(
        val isrc: String? = null,
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val year: Int? = null,
        val duration: Double? = null,
    )
}
