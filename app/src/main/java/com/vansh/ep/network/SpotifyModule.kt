package com.vansh.ep.network

import android.util.Base64
import android.util.Log
import com.google.gson.JsonParser
import com.vansh.ep.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException

object SpotifyModule {
    private const val TAG = "SpotifyModule"

    private const val CLIENT_ID = BuildConfig.SPOTIFY_CLIENT_ID
    private const val CLIENT_SECRET = BuildConfig.SPOTIFY_CLIENT_SECRET
    private val client = OkHttpClient()

    private var accessToken: String? = null
    private var tokenExpiryTime: Long = 0

    suspend fun getValidToken(): String = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        if (accessToken != null && currentTime < tokenExpiryTime) {
            return@withContext "Bearer $accessToken"
        }

        if (CLIENT_ID.isEmpty() || CLIENT_SECRET.isEmpty()) {
            Log.e(TAG, "❌ Spotify Credentials Missing in local.properties!")
            throw IOException("Missing Credentials")
        }

        val authString = "$CLIENT_ID:$CLIENT_SECRET"
        val encodedAuth = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)

        val request = Request.Builder()
            .url("https://accounts.spotify.com/api/token")
            .header("Authorization", "Basic $encodedAuth")
            .post(FormBody.Builder().add("grant_type", "client_credentials").build())
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IOException("Empty auth body")
            if (!response.isSuccessful) {
                Log.e(TAG, "❌ Auth failed: $body")
                throw IOException("Auth failed: $body")
            }

            val json = JsonParser.parseString(body).asJsonObject
            accessToken = json.get("access_token").asString
            val expiresIn = json.get("expires_in").asLong
            tokenExpiryTime = currentTime + (expiresIn * 1000) - 60000
            "Bearer $accessToken"
        }
    }

    suspend fun searchTracks(token: String, query: String, limit: Int = 25): String? = withContext(Dispatchers.IO) {
        val url = "https://api.spotify.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("type", "track")
            .addQueryParameter("limit", limit.toString())
            .build()

        val request = Request.Builder().url(url).header("Authorization", token).build()
        client.newCall(request).execute().use { it.body?.string() }
    }

    suspend fun getRecentlyPlayed(token: String): String? = withContext(Dispatchers.IO) {
        // NOTE: This requires 'user-read-recently-played' scope which client_credentials doesn't have.
        // However, for this demo, we'll simulate history using search learning or 
        // if you use a full User Token later, this is the endpoint.
        val url = "https://api.spotify.com/v1/me/player/recently-played?limit=50".toHttpUrl()
        val request = Request.Builder().url(url).header("Authorization", token).build()
        client.newCall(request).execute().use { it.body?.string() }
    }

    suspend fun getAudioFeatures(token: String, trackId: String): String? = withContext(Dispatchers.IO) {
        val url = "https://api.spotify.com/v1/audio-features/$trackId".toHttpUrl()
        val request = Request.Builder().url(url).header("Authorization", token).build()
        client.newCall(request).execute().use { it.body?.string() }
    }

    suspend fun getRecommendations(
        token: String,
        genres: String,
        minVal: Float, maxVal: Float,
        minEn: Float, maxEn: Float,
        minTempo: Float, maxTempo: Float
    ): String? = withContext(Dispatchers.IO) {
        if (genres.isEmpty()) return@withContext null

        val targetVal = ((minVal + maxVal) / 2).coerceIn(0.1f, 0.9f)
        val targetEn = ((minEn + maxEn) / 2).coerceIn(0.1f, 0.9f)
        val targetTemp = (minTempo + maxTempo) / 2

        val url = "https://api.spotify.com/v1/recommendations".toHttpUrl().newBuilder()
            .addQueryParameter("limit", "30") 
            .addEncodedQueryParameter("seed_genres", genres)
            .addQueryParameter("target_valence", targetVal.toString())
            .addQueryParameter("target_energy", targetEn.toString())
            .addQueryParameter("target_tempo", targetTemp.toString())
            .build()

        val request = Request.Builder().url(url).header("Authorization", token).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful) {
                Log.e(TAG, "❌ Recommendations failed: ${response.code}")
                return@withContext null
            }
            body
        }
    }
}
