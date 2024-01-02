package de.danielscholz.database.core.context

import de.danielscholz.database.core.Base


class HistoryEntryContext<T : Base, ROOT : Base>(private val snapShotContext: SnapShotContext<ROOT>, private val entry: T) {

    fun perform(block: SnapShotContext<ROOT>.(T) -> Unit) {
        snapShotContext.block(entry)
    }

}