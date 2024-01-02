package de.danielscholz.database.core.context

import de.danielscholz.database.core.Base


class HistoryEntryContext<T : Base, ROOT : Base>(internal val snapShotContext: SnapShotContext<ROOT>, internal val entry: T) {

    fun perform(block: SnapShotContext<ROOT>.(T) -> Unit) {
        snapShotContext.block(entry)
    }

}