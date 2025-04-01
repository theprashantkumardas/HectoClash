package com.example.hectoclash.model

data class User(
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
        User("1", "John Doe", "JD123", 1500, "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", true),
        User("2", "Jane Smith", "JS456", 1800, "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", false),
        User("3", "Mike Johnson", "MJ789", 2200, "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", true),
        User("4", "Sarah Brown", "SB101", 1950, "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", true),
        User("5", "Alex Wilson", "AW202", 1750, "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", false),
        User("6", "Emily Davis", "ED303", 2100, "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", true),
        User("7", "Chris Martin", "CM404", 1600, "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", false),
        User("8", "Lisa Anderson", "LA505", 2000, "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", true),
        User("9", "David Taylor", "DT606", 1850, "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", true),
        User("10", "Olivia White", "OW707", 1700, "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", false)
    )
}