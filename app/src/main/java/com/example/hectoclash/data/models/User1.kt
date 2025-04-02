package com.example.hectoclash.data.models

data class User1(
    val id: String,
    val name: String,
    val playerId: String,
    val points: Int,
    val profilePicUrl: String,
    val isOnline: Boolean
)

// Sample dummy data for the friends list
object DummyData {
    val users = listOf(
        User1("1", "John Doe", "JD123", 1500, "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", true),
        User1("2", "Jane Smith", "JS456", 1800, "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", false),
        User1("3", "Mike Johnson", "MJ789", 2200, "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", true),
        User1("4", "Sarah Brown", "SB101", 1950, "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", true),
        User1("5", "Alex Wilson", "AW202", 1750, "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", false),
    )
}