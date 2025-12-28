package com.vansh.ep.models

/**
 * Represents a song fetched from Spotify
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String,
    val previewUrl: String? = null,
    val spotifyUrl: String,
    val primaryGenre: String,
    val durationMs: Long,
    val popularity: Int = 0 // Added for ranking
)

/**
 * Represents the 32-emotion profile with specific Spotify search parameters
 */
data class SpotifyParams(
    val minValence: Float, val maxValence: Float,
    val minEnergy: Float, val maxEnergy: Float,
    val minTempo: Float, val maxTempo: Float,
    val minAcousticness: Float, val maxAcousticness: Float,
    val minDanceability: Float, val maxDanceability: Float,
    val seedGenres: List<String>
)

/**
 * Data captured during the 2-second gesture window
 */
data class GestureData(
    val xPositions: List<Float>,
    val yPositions: List<Float>,
    val pressures: List<Float>,
    val startTime: Long
)
