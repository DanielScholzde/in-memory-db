package de.danielscholz.database.core.context

import de.danielscholz.database.core.Base


class HistoryEntryContext<T : Base, ROOT : Base>(private val snapShotContext: SnapShotContext<ROOT>, private val entry: T) {

    fun <R> perform(block: SnapShotContext<ROOT>.(T) -> R): R {
        with(snapShotContext) {
            return block(entry)
        }
    }

}