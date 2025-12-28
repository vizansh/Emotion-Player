package com.vansh.ep.backend

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.vansh.ep.models.Song
import com.vansh.ep.network.SpotifyModule
import org.json.JSONObject
import java.io.File
import kotlin.math.abs

class Personalizer(private val context: Context) {
    private val weightsFile by lazy { File(context.filesDir, "soul_profile.json") }
    private val historyFile by lazy { File(context.filesDir, "listening_history.json") }
    private val seenFile by lazy { File(context.filesDir, "seen_songs.json") }
    private var weights = JSONObject()
    private val gson = Gson()

    init {
        if (weightsFile.exists()) {
            try {
                weights = JSONObject(weightsFile.readText())
            } catch (e: Exception) {
                Log.e("Personalizer", "Weights load failed, resetting")
                EmotionConstants.emotionMap.keys.forEach { weights.put(it, 1.0) }
            }
        } else {
            EmotionConstants.emotionMap.keys.forEach { weights.put(it, 1.0) }
        }
    }

    fun refineTopCandidates(
        gestureCandidates: List<Pair<String, Float>>,
        weatherCandidates: List<Pair<String, Float>>
    ): List<String> {
        val finalScores = mutableMapOf<String, Float>()
        EmotionConstants.emotionMap.keys.forEach { emo ->
            val gScore = gestureCandidates.find { it.first == emo }?.second ?: 0f
            val wScore = weatherCandidates.find { it.first == emo }?.second ?: 0f
            val userWeight = weights.optDouble(emo, 1.0).toFloat()
            finalScores[emo] = ((0.6f * gScore) + (0.4f * wScore)) * userWeight
        }
        return finalScores.toList().sortedByDescending { it.second }.take(3).map { it.first }
    }

    fun saveFeedback(emotion: String, liked: Boolean) {
        val current = weights.optDouble(emotion, 1.0)
        val delta = if (liked) 0.15 else -0.3
        weights.put(emotion, (current + delta).coerceIn(0.1, 5.0))
        saveWeights()
    }

    private fun saveWeights() {
        try {
            weightsFile.writeText(weights.toString())
        } catch (e: Exception) {
            Log.e("Personalizer", "Weights save failed", e)
        }
    }

    fun addToHistory(song: Song) {
        try {
            val type = object : TypeToken<MutableList<Song>>() {}.type
            val history: MutableList<Song> = if (historyFile.exists()) {
                gson.fromJson(historyFile.readText(), type) ?: mutableListOf()
            } else mutableListOf()
            
            history.removeAll { it.id == song.id }
            history.add(0, song)
            
            val limited = history.take(200)
            historyFile.writeText(gson.toJson(limited))
        } catch (e: Exception) {
            Log.e("Personalizer", "History save failed", e)
        }
    }

    fun getHistoryMatches(emotion: String): List<Song> {
        return try {
            if (!historyFile.exists()) return emptyList()
            val type = object : TypeToken<List<Song>>() {}.type
            val history: List<Song> = gson.fromJson(historyFile.readText(), type) ?: return emptyList()
            if (emotion == "any") return history
            history.filter { it.primaryGenre == emotion }.shuffled()
        } catch (e: Exception) { emptyList() }
    }

    fun markAsSeen(songs: List<Song>) {
        try {
            val type = object : TypeToken<MutableSet<String>>() {}.type
            val seenIds: MutableSet<String> = if (seenFile.exists()) {
                gson.fromJson(seenFile.readText(), type) ?: mutableSetOf()
            } else mutableSetOf()
            seenIds.addAll(songs.map { it.id })
            seenFile.writeText(gson.toJson(seenIds))
        } catch (e: Exception) {
            Log.e("Personalizer", "Seen save failed", e)
        }
    }

    fun getSeenIds(): Set<String> {
        return try {
            if (!seenFile.exists()) return emptySet()
            val type = object : TypeToken<Set<String>>() {}.type
            gson.fromJson(seenFile.readText(), type) ?: emptySet()
        } catch (e: Exception) { emptySet() }
    }

    suspend fun learnFromPlayedSong(song: Song) {
        try {
            val token = SpotifyModule.getValidToken()
            val featuresJson = SpotifyModule.getAudioFeatures(token, song.id) ?: return
            val features = JsonParser.parseString(featuresJson).asJsonObject

            fun getSafeFloat(key: String, default: Float): Float {
                return if (features.has(key) && !features.get(key).isJsonNull) {
                    try { features.get(key).asFloat } catch (e: Exception) { default }
                } else default
            }

            val valence = getSafeFloat("valence", 0.5f)
            val energy = getSafeFloat("energy", 0.5f)
            val tempo = getSafeFloat("tempo", 100f)

            val matchedEmotion = EmotionConstants.emotionMap.minByOrNull { (_, params) ->
                val avgVal = (params.minValence + params.maxValence) / 2f
                val avgEn = (params.minEnergy + params.maxEnergy) / 2f
                val avgTempo = (params.minTempo + params.maxTempo) / 2f
                abs(valence - avgVal) + abs(energy - avgEn) + (abs(tempo - avgTempo) / 200f)
            }?.key ?: "neutral"

            val current = weights.optDouble(matchedEmotion, 1.0)
            weights.put(matchedEmotion, (current + 0.5).coerceIn(0.1, 5.0))
            
            EmotionConstants.emotionMap.keys.forEach { emo ->
                if (emo != matchedEmotion) {
                    val w = weights.optDouble(emo, 1.0)
                    weights.put(emo, (w * 0.98).coerceIn(0.1, 5.0))
                }
            }
            saveWeights()
            
            val updatedSong = song.copy(primaryGenre = matchedEmotion)
            addToHistory(updatedSong)
            
        } catch (e: Exception) {
            Log.e("Personalizer", "Playback learning failed", e)
        }
    }

    suspend fun learnFromSearch(query: String) {
        Log.d("Personalizer", "Search observed: $query")
    }
}
