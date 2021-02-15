package com.example.dreamsoundscompose

data class SoundFile (val imageId: Int, val filename: String)

val SOUNDS = mapOf(
    "Babbling Brook" to SoundFile(R.drawable.babbling_brook, "babbling_brook.mp3"),
    "City Noise" to SoundFile(R.drawable.city_noise, "city_noise.mp3"),
    "Crickets" to SoundFile(R.drawable.crickets, "crickets.mp3"),
    "Rain" to SoundFile(R.drawable.rain, "rain.mp3"),
    "Thunder Storm" to SoundFile(R.drawable.thunder_storm, "thunder_storm.mp3"),
    "Tropical Rainforest" to SoundFile(R.drawable.tropical_rainforest, "tropical_rainforest.mp3"),
    "Waterfall" to SoundFile(R.drawable.waterfall, "waterfall.mp3"),
    "Waves" to SoundFile(R.drawable.waves, "waves.mp3"),
    "White Noise" to SoundFile(R.drawable.white_noise, "white_noise.mp3"),
    "Wind Chimes" to SoundFile(R.drawable.wind_chimes, "wind_chimes.mp3")
)
