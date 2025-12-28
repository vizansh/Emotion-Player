# 🎵 Emotion Player (EP)
An intelligent Android music discovery engine that synchronizes your Spotify playback with your real-time "vibe"—calculated from gestures, weather data, and personal history.

## 🧠 The Core: VibePipeline
The heart of this app is the `VibePipeline`, which utilizes a multi-stage **Aggressive Pooling** strategy:
1. **Sensor Fusion:** Combines `GestureData` and `WeatherModule` outputs via a custom `FusionEngine`.
2. **Refinement:** The `Personalizer` adjusts candidates based on your skip/play history.
3. **Discovery & Fallbacks:** 
   - Primary: High-popularity tracks matching the current emotion.
   - Fallback 1: Genre-specific search if recommendations are thin.
   - Fallback 2: Secondary emotion injection.
   - Absolute Safety: Broad 2024 discovery pool to ensure 0% failure rate.

## 🛠️ Tech Stack
- **UI:** Jetpack Compose (Material 3)
- **Backend:** Kotlin Coroutines & Flow
- **API Integration:** Spotify App Remote & Web API, OpenWeather Maps
- **Data:** GSON for complex JSON parsing of Spotify's recommendation clusters
- **Personalization:** Local preference learning algorithm

## 🚀 Key Features
- **Vibe Check:** Real-time emotion classification.
- **Aggressive Pooling:** Ensures a fresh 10-song playlist every time the environment changes.
- **Smart History:** The `Personalizer` ensures you don't hear the same tracks too often.

---
*Developed by Vansh - A fusion of Music, Data, and Emotion.*
