package de.danielscholz.database.core


data class Config(
    val writeToFile: Boolean = true,
    val writeDiff: (SNAPSHOT_VERSION) -> Boolean = { true },
    val jsonPrettyPrint: Boolean = true,
)