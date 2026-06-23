# 🎵 Emotion Player (EP)
**Emotion Player** is an intelligent Android music discovery engine that bridges the gap between environmental sensors and Spotify playback. It uses a custom **VibePipeline** to translate physical gestures, real-time weather, and historical "Soul Profile" data into perfect musical matches.

---

## 🧠 Core Architecture: The VibePipeline
The app doesn't just search for genres; it executes a multi-stage **Aggressive Pooling** strategy to ensure a 0% failure rate in music discovery.

### 1. The Fusion Engine (`FusionEngine.kt`)
Combines multi-modal data into a unified "Emotion Vector":
*   **Gesture Analysis:** Calculates motion intensity using distance vectors ($\sqrt{dx^2 + dy^2}$) and touch pressure.
*   **Weather Integration:** Maps 50+ weather conditions (from OpenWeather) to psychological states (e.g., *Haze* → *Fearful*, *Spring* → *Joyful*).
*   **Temporal Bias:** Adjusts music suggestions based on time-of-day (e.g., *5 AM - 8 AM* → *Hopeful*).

### 2. The Personalizer (`Personalizer.kt`)
The "Soul Profile" (`soul_profile.json`) tracks your emotional affinity:
*   **Dynamic Weighting:** Every Skip or Like adjusts the 32-emotion weight map.
*   **Audio Feature Learning:** Uses Spotify's `valence`, `energy`, and `tempo` features to retroactively update your profile based on what you actually listen to.

### 3. Aggressive Pooling Strategy
To prevent empty playlists, the pipeline uses a 4-tier fallback:
1.  **Primary Match:** High-popularity tracks from the top-scored emotion.
2.  **Genre Expansion:** If the pool is < 20, it expands via genre-specific Spotify searches.
3.  **Secondary Injection:** Pulls from the next two most likely emotions.
4.  **Absolute Safety:** Uses a broad "Year: 2024" discovery pool to ensure the music never stops.

---

## 🎨 Creative UI & Math
The app features a **RandomRotatedGestureSwipeHint** system that uses procedural path generation on a Compose `Canvas`:
*   **Geometric Paths:** Includes math-heavy implementations for **Infinity (Lemniscate of Bernoulli)**, **Archimedean Spirals**, and **Sinusoidal Waves**.
*   **Material 3:** A dark-themed, immersive UI using Jetpack Compose and Coil for asynchronous image loading.
---
## Demo


https://github.com/user-attachments/assets/b070341d-e153-4a44-b9d9-6f40098a1ad3


<img width="1080" height="2400" alt="Screenshot_2026-06-17-22-39-17-100_com vansh ep" src="https://github.com/user-attachments/assets/c0a63a88-064b-4063-87d4-4815104b8235" />

<img width="1080" height="2400" alt="Screenshot_2026-06-17-22-39-09-915_com vansh ep" src="https://github.com/user-attachments/assets/3afccad9-8e7e-4402-8235-94e4d0241812" />

<img width="1080" height="2400" alt="Screenshot_2026-06-17-22-39-31-860_com vansh ep" src="https://github.com/user-attachments/assets/0b126c51-da6e-478c-9c83-6226a20f1049" />

---

## 🛠️ Tech Stack
- **Language:** 100% Kotlin
- **UI:** Jetpack Compose (Animations, Canvas, Material 3)
- **Networking:** OkHttp3, Retrofit, GSON
- **APIs:** Spotify (App Remote + Web API), OpenWeatherMaps
- **Location:** Google Play Services (FusedLocationProvider)
- **Conscurrency:** Kotlin Coroutines & Flow

---

## 🚀 Setup & Installation
1.  **Clone the Repo:** `git clone https://github.com/vizansh/Emotion-Player.git`
2.  **API Keys:** Create a `local.properties` file in the root directory and add your credentials:
3.  **Requirements:**
◦The official Spotify app must be installed and logged in on the device.
◦GPS must be enabled for the WeatherModule to sync the vibe.
◦Android Studio Ladybug (2024.2.1+) is recommended.
4.  **Spotify App Remote:** Ensure the official Spotify app is installed on your device, as this project utilizes the [Spotify App Remote SDK](https://developer.spotify.com/documentation/android/) for playback control.
5.  **Build:** Open in Android Studio (Ladybug 2024.2.1 or newer) and sync Gradle.
6.  **Android:** download the .apk

---
*Developed by Vansh — Where Code, Climate, and Chords Collide.*

    
