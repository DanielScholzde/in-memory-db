package de.danielscholz.database.core.util

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.context.HistoryEntryContext
import de.danielscholz.database.core.context.SnapshotContext
import kotlinx.collections.immutable.PersistentMap


fun <T : Base> PersistentMap<Long, T>.addOrReplace(entries: List<T>): PersistentMap<Long, T> {
    val changed = entries.any { this[it.id] != it }
    if (!changed) return this
    return this.putAll(entries.associateBy { it.id })
}


fun <T> Iterable<T>.withEach(block: T.() -> Unit) {
    this.forEach {
        it.block()
    }
}


fun <ROOT : Base, T : Base> List<HistoryEntryContext<T, ROOT>>.performEach(block: SnapshotContext<ROOT>.(T) -> Unit) {
    this.forEach {
        it.perform(block)
    }
}
