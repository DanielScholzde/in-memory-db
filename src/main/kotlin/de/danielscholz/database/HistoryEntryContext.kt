package de.danielscholz.database


class HistoryEntryContext<T : Base>(val snapShotContext: SnapShotContext, val entry: T) {

    fun perform(block: SnapShotContext.(T) -> Unit) {
        snapShotContext.block(entry)
    }

}