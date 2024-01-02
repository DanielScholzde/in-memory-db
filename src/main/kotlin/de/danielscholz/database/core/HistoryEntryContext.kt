package de.danielscholz.database.core


class HistoryEntryContext<T : Base, ROOT : Base>(val snapShotContext: SnapShotContext<ROOT>, val entry: T) {

    fun perform(block: SnapShotContext<ROOT>.(T) -> Unit) {
        snapShotContext.block(entry)
    }

}