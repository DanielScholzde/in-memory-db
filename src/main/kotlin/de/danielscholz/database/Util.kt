package de.danielscholz.database

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet


fun PersistentSet<Long>.addOrReplaceEntry(entry: Long): PersistentSet<Long> {
    if (entry in this) return this
    return this.add(entry)
}

fun PersistentSet<Long>.addOrReplaceEntries(entries: Collection<Long>): PersistentSet<Long> {
    val missing = entries.any { it !in this }
    if (!missing) return this
    return this.addAll(entries)
}

//fun <T : Base> List<T>.addOrReplaceEntry(entry: T): List<T> {
//    val index = this.indexOfFirst { it.id == entry.id }
//    if (index >= 0) {
//        if (this[index] == entry) return this
//        return this.subList(0, index) + entry + this.subList(index + 1, this.lastIndex + 1)
//    }
//    return this + entry
//}
//
//
//fun <T : Base> List<T>.addOrReplaceEntries(entries: List<T>): List<T> {
//    val result = ArrayList<T>(this.size + entries.size)
//    val entriesMap = entries.associateBy { it.id }.toMutableMap()
//    for (e in this) {
//        if (e.id in entriesMap) {
//            val t = entriesMap[e.id]!!
//            entriesMap.remove(e.id)
//            result += t
//        } else {
//            result += e
//        }
//    }
//    entriesMap.values.forEach { v ->
//        result += v
//    }
//    return result
//}


//fun <T : Base> Map<Long, T>.addOrReplace(entry: T): Map<Long, T> {
//    val map = this.toMutableMap()
//    val old = map.put(entry.id, entry)
//    if (old != entry) {
//        return map
//    }
//    return this
//}


fun <T : Base> PersistentMap<Long, T>.addOrReplace(entries: List<T>): PersistentMap<Long, T> {
    val changed = entries.any { this[it.id] != it }
    if (!changed) return this
    return this.putAll(entries.associateBy { it.id })
}

//infix fun <T : Base> List<T>.idEquals(other: List<T>): Boolean {
//    if (this === other) return true
//    if (this.size != other.size) return false
//    if (this.isEmpty()) return true
//    this.forEachIndexed { index, t ->
//        if (t.id != other[index].id) {
//            return false
//        }
//    }
//    return true
//}

//infix fun <T> List<T>.refEquals(other: List<T>): Boolean {
//    if (this === other) return true
//    if (this.size != other.size) return false
//    if (this.isEmpty()) return true
//    this.forEachIndexed { index, t ->
//        if (t !== other[index]) return false
//    }
//    return true
//}
//
//infix fun <T> Map<Long, T>.refEquals(other: Map<Long, T>): Boolean {
//    if (this === other) return true
//    if (this.size != other.size) return false
//    if (this.isEmpty()) return true
//    this.forEach { (key, value) ->
//        val o = other[key]
//        if (o == null || value !== o) return false
//    }
//    return true
//}