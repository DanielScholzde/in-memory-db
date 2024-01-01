package de.danielscholz.database.core


class HistoryEntryContext<T : Base>(val snapShotContext: SnapShotContext, val entry: T) {

    fun perform(block: SnapShotContext.(T) -> Unit) {
        snapShotContext.block(entry)
    }

}