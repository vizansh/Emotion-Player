package com.vansh.ep.network

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.vansh.ep.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class WeatherInfo(
    val condition: String,
    val temperature: Float
)

object WeatherModule {
    private const val TAG = "WeatherModule"
    private const val API_KEY = BuildConfig.OPENWEATHERMAP_API_KEY
    private const val PREFS_NAME = "weather_cache"
    private val client = OkHttpClient()

    @SuppressLint("MissingPermission")
    suspend fun getVibeWeather(context: Context): WeatherInfo = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        var lat = prefs.getString("last_lat", "0.0")?.toDoubleOrNull() ?: 0.0
        var lon = prefs.getString("last_lon", "0.0")?.toDoubleOrNull() ?: 0.0

        try {
            val location = withTimeoutOrNull(3000) {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            }

            if (location != null) {
                lat = location.latitude
                lon = location.longitude
                prefs.edit().apply {
                    putString("last_lat", lat.toString())
                    putString("last_lon", lon.toString())
                    apply()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Location fetch failed, using cache: ${e.message}")
        }

        fetchWeatherData(lat, lon)
    }

    private fun fetchWeatherData(lat: Double, lon: Double): WeatherInfo {
        if (lat == 0.0 && lon == 0.0) return WeatherInfo("clear", 25f)

        val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=metric&appid=$API_KEY"

        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return WeatherInfo("clear", 25f)
                val body = response.body?.string() ?: return WeatherInfo("clear", 25f)
                val json = JSONObject(body)
                val condition = json.getJSONArray("weather").getJSONObject(0).getString("main").lowercase()
                val temp = json.getJSONObject("main").getDouble("temp").toFloat()
                WeatherInfo(condition, temp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "API fetch failed: ${e.message}")
            WeatherInfo("clear", 25f)
        }
    }
}
