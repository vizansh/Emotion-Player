package com.vansh.ep.pipeline

import android.content.Context
import android.util.Log
import com.vansh.ep.backend.EmotionConstants
import com.vansh.ep.backend.FusionEngine
import com.vansh.ep.backend.Personalizer
import com.vansh.ep.models.GestureData
import com.vansh.ep.models.Song
import com.vansh.ep.network.SpotifyModule
import com.vansh.ep.network.WeatherModule
import com.google.gson.JsonParser

object VibePipeline {
    private const val TAG = "VibePipeline"

    suspend fun execute(
        context: Context,
        gestureData: GestureData? = null,
        manualQuery: String? = null
    ): List<Song> {
        try {
            val token = SpotifyModule.getValidToken()
            val personalizer = Personalizer(context)
            val seenIds = personalizer.getSeenIds()

            if (manualQuery != null) {
                val jsonResponse = SpotifyModule.searchTracks(token, manualQuery, limit = 50)
                val results = parseAndMap(jsonResponse, "search", isSearch = true)
                    .filter { it.id !in seenIds }
                
                personalizer.learnFromSearch(manualQuery)
                
                val sorted = results.sortedByDescending { it.popularity }
                val finalSearchList = mutableListOf<Song>()
                
                finalSearchList.addAll(sorted.take(2))
                val pool = sorted.drop(2).shuffled()
                finalSearchList.addAll(pool.take(10 - finalSearchList.size))
                
                if (finalSearchList.size < 10) {
                    val broad = SpotifyModule.searchTracks(token, manualQuery.split(" ").first(), limit = 50)
                    val extra = parseAndMap(broad, "search", true).filter { it.id !in seenIds && finalSearchList.none { f -> f.id == it.id } }
                    finalSearchList.addAll(extra.shuffled().take(10 - finalSearchList.size))
                }

                personalizer.markAsSeen(finalSearchList)
                return finalSearchList.take(10)
            } else {
                val weatherData = try { WeatherModule.getVibeWeather(context) } catch (_: Exception) { null }
                val gestureCandidates = FusionEngine.getGestureCandidates(gestureData!!)
                val weatherCandidates = weatherData?.let { FusionEngine.getWeatherCandidates(it.condition, it.temperature) } ?: emptyList()

                val top3 = personalizer.refineTopCandidates(gestureCandidates, weatherCandidates)
                val primaryEmotion = top3.firstOrNull() ?: "neutral"
                val params = EmotionConstants.emotionMap[primaryEmotion] ?: EmotionConstants.emotionMap["neutral"]!!

                Log.d(TAG, "🧠 Vibe Check: $primaryEmotion")

                // AGGRESSIVE POOLING
                val freshPool = mutableListOf<Song>()
                
                suspend fun fetchToPool(emotion: String) {
                    val p = EmotionConstants.emotionMap[emotion] ?: return
                    val resp = SpotifyModule.getRecommendations(
                        token, p.seedGenres.joinToString(","),
                        p.minValence, p.maxValence, p.minEnergy, p.maxEnergy, p.minTempo, p.maxTempo
                    )
                    freshPool.addAll(parseAndMap(resp, emotion, false).filter { it.id !in seenIds })
                }

                fetchToPool(primaryEmotion)

                // FALLBACK 1: Search genre if pool too small
                if (freshPool.size < 20) {
                    val fallbackGenre = params.seedGenres.random()
                    val searchResp = SpotifyModule.searchTracks(token, "genre:$fallbackGenre", limit = 50)
                    freshPool.addAll(parseAndMap(searchResp, primaryEmotion, true).filter { it.id !in seenIds })
                }

                // FALLBACK 2: Secondary emotions
                if (freshPool.size < 20 && top3.size > 1) fetchToPool(top3[1])

                val sortedFresh = freshPool.distinctBy { it.id }.sortedByDescending { it.popularity }
                val resultList = mutableListOf<Song>()
                
                // 1. TOP 2 (Most Popular Fresh)
                resultList.addAll(sortedFresh.take(2))
                
                // 2. NEXT 3 (History - Shuffled & Matching)
                var historyMatches = personalizer.getHistoryMatches(primaryEmotion)
                    .filter { h -> resultList.none { it.id == h.id } }
                    .shuffled()
                
                // If specific mood history is empty, pull from general history
                if (historyMatches.size < 3) {
                    val generalHistory = personalizer.getHistoryMatches("any") // Will return all history
                    val extraHistory = generalHistory.filter { h -> resultList.none { it.id == h.id } }.shuffled()
                    historyMatches = (historyMatches + extraHistory).distinctBy { it.id }
                }
                resultList.addAll(historyMatches.take(3))
                
                // 3. REMAINING 5 (or more if history was short)
                val needed = 10 - resultList.size
                val discoveryPool = sortedFresh.filter { s -> resultList.none { it.id == s.id } }.shuffled()
                resultList.addAll(discoveryPool.take(needed))
                
                // 4. ABSOLUTE SAFETY: Broad Discovery if still < 10
                if (resultList.size < 10) {
                    val broadSearch = SpotifyModule.searchTracks(token, "year:2024", limit = 50)
                    val broadSongs = parseAndMap(broadSearch, "neutral", true)
                        .filter { it.id !in seenIds && resultList.none { r -> r.id == it.id } }
                    resultList.addAll(broadSongs.shuffled().take(10 - resultList.size))
                }

                personalizer.markAsSeen(resultList)
                return resultList.take(10)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Pipeline Crash: ${e.message}")
            return emptyList()
        }
    }

    private fun parseAndMap(jsonResponse: String?, genre: String, isSearch: Boolean): List<Song> {
        if (jsonResponse.isNullOrBlank()) return emptyList()
        try {
            val json = JsonParser.parseString(jsonResponse).asJsonObject
            val tracksArray = if (isSearch) {
                json.getAsJsonObject("tracks")?.getAsJsonArray("items")
            } else {
                json.getAsJsonArray("tracks")
            } ?: return emptyList()

            return tracksArray.mapNotNull { element ->
                try {
                    val trackObj = element.asJsonObject
                    val album = trackObj.getAsJsonObject("album")
                    val artists = trackObj.getAsJsonArray("artists")
                    Song(
                        id = trackObj.get("id").asString,
                        title = trackObj.get("name").asString,
                        artist = artists[0].asJsonObject.get("name").asString,
                        albumArtUrl = album.getAsJsonArray("images").firstOrNull()?.asJsonObject?.get("url")?.asString ?: "",
                        spotifyUrl = trackObj.getAsJsonObject("external_urls").get("spotify").asString,
                        primaryGenre = genre,
                        durationMs = trackObj.get("duration_ms").asLong,
                        popularity = if (trackObj.has("popularity")) trackObj.get("popularity").asInt else 50
                    )
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) { return emptyList() }
    }
}
