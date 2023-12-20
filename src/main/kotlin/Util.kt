fun List<Long>.addOrReplaceEntry(entry: Long): List<Long> {
    val index = this.indexOf(entry)
    if (index >= 0) {
        return this
    }
    return this + entry
}

fun List<Long>.addOrReplaceEntries(entries: Collection<Long>): List<Long> {
    val result = ArrayList<Long>(this.size + entries.size)
    val set = entries.toMutableSet()
    this.forEach {
        if (it in set) {
            result += it
            set.remove(it)
        } else {
            result += it
        }
    }
    result.addAll(set)
    return result
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


fun <T : Base> Map<Long, T>.addOrReplace(entry: T): Map<Long, T> {
    val map = this.toMutableMap()
    val old = map.put(entry.id, entry)
    if (old != entry) {
        return map
    }
    return this
}


fun <T : Base> Map<Long, T>.addOrReplace(entries: List<T>): Map<Long, T> {
    val map = this.toMutableMap()
    var dirty = false
    for (entry in entries) {
        val old = map.put(entry.id, entry)
        if (old != entry) {
            dirty = true
        }
    }
    if (dirty) {
        return map
    }
    return this
}

infix fun <T : Base> List<T>.idEquals(other: List<T>): Boolean {
    if (this === other) return true
    if (this.size != other.size) return false
    if (this.isEmpty()) return true
    this.forEachIndexed { index, t ->
        if (t.id != other[index].id) {
            return false
        }
    }
    return true
}

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