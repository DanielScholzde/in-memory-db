package de.danielscholz.database.core.context

import de.danielscholz.database.core.Base


class HistoryEntryContext<T : Base, ROOT : Base>(internal val snapShotContext: SnapShotContext<ROOT>, internal val entry: T) {

    fun <R> perform(block: SnapShotContext<ROOT>.(T) -> R): R {
        with(snapShotContext) {
            return block(entry)
        }
    }

}