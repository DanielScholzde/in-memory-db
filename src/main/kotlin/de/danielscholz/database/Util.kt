package de.danielscholz.database

import kotlinx.collections.immutable.PersistentMap


fun <T : Base> PersistentMap<Long, T>.addOrReplace(entries: List<T>): PersistentMap<Long, T> {
    val changed = entries.any { this[it.id] != it }
    if (!changed) return this
    return this.putAll(entries.associateBy { it.id })
}

