package com.vansh.ep.backend

import com.vansh.ep.models.GestureData
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.sqrt

object FusionEngine {
    /**
     * Extracts top 3 emotion candidates from weather conditions.
     * Incorporates condition, temperature, and time.
     */
    fun getWeatherCandidates(condition: String, temp: Float): List<Pair<String, Float>> {
        val key = condition.lowercase().replace(" ", "_")
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // 1. Base primary emotion from condition
        val primary = BASE_CONDITION_MAP[key] ?: "calm"
        
        // 2. Adjust based on temperature
        val tempEmotion = when {
            temp > 35 -> "angry"
            temp > 28 -> "energetic"
            temp > 20 -> "happy"
            temp < 5 -> "relaxed"
            else -> null
        }

        // 3. Adjust based on time
        val timeEmotion = when (hour) {
            in 5..8 -> "hopeful"
            in 17..19 -> "romantic"
            in 20..23 -> "serene"
            in 0..4 -> "spiritual"
            else -> null
        }

        val candidates = mutableListOf<String>()
        candidates.add(primary)
        SECONDARY_MAP[key]?.let { candidates.addAll(it) }
        tempEmotion?.let { if (!candidates.contains(it)) candidates.add(it) }
        timeEmotion?.let { if (!candidates.contains(it)) candidates.add(it) }

        // Build top 3 with scores: 0.85, 0.75, 0.65
        return candidates.distinct().take(3).mapIndexed { index, emo ->
            val score = 0.85f - (index * 0.1f)
            emo to score
        }
    }

    /**
     * Extracts top 3 emotion candidates from gesture data.
     */
    fun getGestureCandidates(data: GestureData): List<Pair<String, Float>> {
        val intensity = calculateIntensity(data)
        
        return EmotionConstants.emotionMap.map { (name, params) ->
            val avgEnergy = (params.minEnergy + params.maxEnergy) / 2f
            // Similarity score: 1.0 is perfect match
            val score = 1f - abs(intensity - avgEnergy)
            name to score
        }.sortedByDescending { it.second }.take(3)
    }

    private fun calculateIntensity(data: GestureData): Float {
        if (data.xPositions.size < 2) return 0.3f
        var dist = 0f
        for (i in 1 until data.xPositions.size) {
            val dx = data.xPositions[i] - data.xPositions[i - 1]
            val dy = data.yPositions[i] - data.yPositions[i - 1]
            dist += sqrt(dx * dx + dy * dy)
        }
        val avgPressure = if (data.pressures.isNotEmpty()) data.pressures.average().toFloat() else 0.5f
        return ((dist / 5000f).coerceIn(0f, 0.7f) + (avgPressure * 0.3f).coerceIn(0f, 0.3f))
    }

    private val BASE_CONDITION_MAP = mapOf(
        "clear" to "happy", "mostly_clear" to "confident", "partly_cloudy" to "calm",
        "overcast" to "melancholic", "fog" to "confused", "haze" to "fearful",
        "smoke" to "anxious", "dust" to "angry", "drizzle" to "nostalgic",
        "light_rain" to "calm", "moderate_rain" to "inspired", "heavy_rain" to "sad",
        "showers" to "nostalgic", "freezing_rain" to "fearful", "light_snow" to "joyful",
        "moderate_snow" to "relaxed", "heavy_snow" to "joyful", "blizzard" to "fearful",
        "isolated_thunderstorm" to "adventurous", "thunderstorm" to "anxious",
        "severe_thunderstorm" to "fearful", "lightning" to "surprised",
        "tropical_storm" to "fearful", "breezy" to "adventurous", "windy" to "curious",
        "gusty" to "anxious", "gale" to "fearful", "heatwave" to "angry",
        "very_hot" to "energetic", "warm" to "happy", "mild" to "calm",
        "cool" to "nostalgic", "cold" to "spiritual", "very_cold" to "relaxed",
        "sunrise" to "hopeful", "sunset" to "romantic", "twilight" to "spiritual",
        "night_clear" to "calm", "night_overcast" to "lonely", "very_humid" to "melancholic",
        "dry_air" to "focused", "pressure_rising" to "hopeful", "pressure_falling" to "anxious",
        "uv_high" to "fearful", "uv_low" to "relaxed", "visibility_low" to "anxious",
        "visibility_excellent" to "proud", "aqi_good" to "grateful",
        "aqi_unhealthy" to "angry", "aqi_hazardous" to "fearful", "monsoon" to "nostalgic",
        "spring" to "joyful", "autumn" to "romantic", "winter" to "melancholic"
    )

    private val SECONDARY_MAP = mapOf(
        "clear" to listOf("energetic", "joyful"),
        "overcast" to listOf("sad", "lonely"),
        "fog" to listOf("calm", "nostalgic"),
        "dust" to listOf("fearful", "disgusted"),
        "light_rain" to listOf("nostalgic", "relaxed"),
        "heavy_rain" to listOf("melancholic", "anxious"),
        "blizzard" to listOf("anxious", "confused"),
        "thunderstorm" to listOf("fearful", "angry"),
        "heatwave" to listOf("angry", "sad"),
        "cold" to listOf("lonely", "calm"),
        "sunset" to listOf("nostalgic", "romantic"),
        "spring" to listOf("hopeful", "inspired"),
        "autumn" to listOf("nostalgic", "calm"),
        "winter" to listOf("sad", "lonely")
    )
}
