package com.vansh.ep.backend

import com.vansh.ep.models.SpotifyParams

/**
 * 100% Verified Spotify Seed Genres only.
 * Maps 32 emotions to specific Spotify audio parameters.
 */
object EmotionConstants {
    val emotionMap: Map<String, SpotifyParams> = mapOf(
        "excited" to SpotifyParams(0.8f, 1.0f, 0.8f, 1.0f, 130f, 160f, 0.0f, 0.2f, 0.7f, 0.9f, listOf("dance", "pop", "happy")),
        "delighted" to SpotifyParams(0.7f, 0.9f, 0.7f, 0.9f, 115f, 140f, 0.0f, 0.3f, 0.6f, 0.8f, listOf("pop", "indie-pop", "happy")),
        "happy" to SpotifyParams(0.7f, 1.0f, 0.6f, 0.9f, 100f, 130f, 0.0f, 0.4f, 0.6f, 0.9f, listOf("happy", "pop", "summer")),
        "astonished" to SpotifyParams(0.6f, 0.8f, 0.8f, 1.0f, 130f, 170f, 0.1f, 0.4f, 0.5f, 0.8f, listOf("electronic", "techno", "edm")),

        "relaxed" to SpotifyParams(0.5f, 0.8f, 0.1f, 0.4f, 60f, 95f, 0.6f, 1.0f, 0.2f, 0.5f, listOf("ambient", "acoustic", "chill")),
        "content" to SpotifyParams(0.5f, 0.7f, 0.2f, 0.5f, 80f, 110f, 0.5f, 0.8f, 0.4f, 0.6f, listOf("indie", "folk", "acoustic")),
        "calm" to SpotifyParams(0.4f, 0.6f, 0.0f, 0.2f, 60f, 85f, 0.8f, 1.0f, 0.1f, 0.4f, listOf("ambient", "piano", "study")),
        "serene" to SpotifyParams(0.5f, 0.7f, 0.1f, 0.3f, 50f, 85f, 0.8f, 1.0f, 0.1f, 0.4f, listOf("new-age", "sleep", "chill")),

        "angry" to SpotifyParams(0.0f, 0.3f, 0.8f, 1.0f, 140f, 190f, 0.0f, 0.2f, 0.3f, 0.5f, listOf("metal", "hardcore", "grunge")),
        "frustrated" to SpotifyParams(0.1f, 0.4f, 0.7f, 0.9f, 120f, 150f, 0.0f, 0.3f, 0.4f, 0.7f, listOf("rock", "punk", "alt-rock")),
        "alarmed" to SpotifyParams(0.2f, 0.5f, 0.8f, 1.0f, 130f, 180f, 0.0f, 0.2f, 0.4f, 0.6f, listOf("techno", "drum-and-bass", "trance")),
        "tense" to SpotifyParams(0.2f, 0.4f, 0.6f, 0.9f, 110f, 140f, 0.1f, 0.5f, 0.3f, 0.6f, listOf("industrial", "psych-rock", "alternative")),

        "sad" to SpotifyParams(0.0f, 0.3f, 0.0f, 0.3f, 60f, 90f, 0.6f, 1.0f, 0.1f, 0.4f, listOf("acoustic", "blues", "emo")),
        "depressed" to SpotifyParams(0.0f, 0.2f, 0.0f, 0.2f, 40f, 80f, 0.7f, 1.0f, 0.1f, 0.3f, listOf("sad", "piano", "blues")),
        "bored" to SpotifyParams(0.3f, 0.5f, 0.1f, 0.4f, 70f, 105f, 0.4f, 0.7f, 0.3f, 0.5f, listOf("chill", "hip-hop", "trip-hop")),
        "tired" to SpotifyParams(0.4f, 0.6f, 0.0f, 0.2f, 60f, 90f, 0.7f, 1.0f, 0.2f, 0.4f, listOf("ambient", "sleep", "piano")),

        "neutral" to SpotifyParams(0.4f, 0.6f, 0.4f, 0.6f, 90f, 115f, 0.2f, 0.5f, 0.4f, 0.7f, listOf("indie", "pop", "rock")),
        "nostalgic" to SpotifyParams(0.3f, 0.6f, 0.2f, 0.5f, 75f, 110f, 0.5f, 0.9f, 0.3f, 0.6f, listOf("folk", "acoustic", "singer-songwriter")),
        "longing" to SpotifyParams(0.2f, 0.5f, 0.3f, 0.6f, 80f, 115f, 0.4f, 0.8f, 0.4f, 0.6f, listOf("soul", "r-n-b", "blues")),
        "gloomy" to SpotifyParams(0.1f, 0.3f, 0.1f, 0.4f, 65f, 100f, 0.5f, 0.9f, 0.2f, 0.5f, listOf("blues", "jazz", "sad")),

        "fearful" to SpotifyParams(0.1f, 0.4f, 0.5f, 0.8f, 110f, 150f, 0.2f, 0.6f, 0.3f, 0.6f, listOf("soundtrack", "ambient", "industrial")),
        "disgusted" to SpotifyParams(0.1f, 0.4f, 0.5f, 0.8f, 90f, 130f, 0.1f, 0.4f, 0.4f, 0.6f, listOf("grunge", "punk", "metal")),
        "sleepy" to SpotifyParams(0.4f, 0.7f, 0.0f, 0.1f, 40f, 75f, 0.9f, 1.0f, 0.1f, 0.3f, listOf("sleep", "ambient", "chill")),
        "satisfied" to SpotifyParams(0.6f, 0.9f, 0.3f, 0.6f, 90f, 120f, 0.3f, 0.7f, 0.5f, 0.8f, listOf("chill", "jazz", "soul")),
        "miserable" to SpotifyParams(0.0f, 0.3f, 0.1f, 0.4f, 50f, 90f, 0.7f, 1.0f, 0.1f, 0.4f, listOf("sad", "blues", "acoustic")),
        "annoyed" to SpotifyParams(0.2f, 0.5f, 0.5f, 0.9f, 110f, 145f, 0.1f, 0.4f, 0.5f, 0.7f, listOf("rock", "alternative", "punk")),
        "droopy" to SpotifyParams(0.2f, 0.4f, 0.0f, 0.2f, 50f, 85f, 0.8f, 1.0f, 0.1f, 0.4f, listOf("piano", "acoustic", "ambient")),
        "hopeful" to SpotifyParams(0.6f, 1.0f, 0.4f, 0.8f, 95f, 130f, 0.2f, 0.6f, 0.5f, 0.9f, listOf("indie-pop", "folk", "happy")),
        "energetic" to SpotifyParams(0.6f, 1.0f, 0.8f, 1.0f, 128f, 160f, 0.0f, 0.2f, 0.7f, 1.0f, listOf("edm", "techno", "work-out")),
        "pleased" to SpotifyParams(0.7f, 1.0f, 0.4f, 0.7f, 100f, 130f, 0.2f, 0.5f, 0.6f, 0.9f, listOf("soul", "pop", "funk")),
        "dreamy" to SpotifyParams(0.5f, 0.9f, 0.1f, 0.5f, 70f, 110f, 0.6f, 1.0f, 0.3f, 0.7f, listOf("indie-pop", "alternative", "ambient"))
    )
}
