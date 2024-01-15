package de.danielscholz.database.core.context

import de.danielscholz.database.core.Base


class HistoryEntryContext<T : Base, ROOT : Base>(private val snapshotContext: SnapshotContext<ROOT>, private val entry: T) {

    fun <R> perform(block: SnapshotContext<ROOT>.(T) -> R): R {
        with(snapshotContext) {
            return block(entry)
        }
    }

}