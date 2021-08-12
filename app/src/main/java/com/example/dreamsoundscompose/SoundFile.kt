package com.example.dreamsoundscompose

data class SoundFile (val imageId: Int, val description: String, val filename: String) {
}

val SOUNDS = mapOf(
    "Babbling Brook" to SoundFile(R.drawable.babbling_brook, "Babbling Brook", "babbling_brook.mp3"),
    "City Noise" to SoundFile(R.drawable.city_noise,"City Noise", "city_noise.mp3"),
    "Crickets" to SoundFile(R.drawable.crickets,"Crickets", "crickets.mp3"),
    "Rain" to SoundFile(R.drawable.rain,"Rain", "rain.mp3"),
    "Thunder Storm" to SoundFile(R.drawable.thunder_storm,"Thunder Storm", "thunder_storm.mp3"),
    "Tropical Rainforest" to SoundFile(R.drawable.tropical_rainforest,"Tropical Rainforest", "tropical_rainforest.mp3"),
    "Waterfall" to SoundFile(R.drawable.waterfall, "Waterfall", "waterfall.mp3"),
    "Waves" to SoundFile(R.drawable.waves, "Waves", "waves.mp3"),
    "White Noise" to SoundFile(R.drawable.white_noise, "White Noise", "white_noise.mp3"),
    "Wind Chimes" to SoundFile(R.drawable.wind_chimes, "Wind Chimes", "wind_chimes.mp3")
)
