package de.danielscholz.database.core.context

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.Database
import de.danielscholz.database.core.ID
import de.danielscholz.database.core.SNAPSHOT_VERSION
import de.danielscholz.database.core.SnapShot


interface SnapShotContext<ROOT : Base> {

    val database: Database<ROOT>

    val snapShot: SnapShot<ROOT>

    val root: ROOT

    fun ID.resolve(): Base

    fun <T : Base> T.asRef(): Reference<ROOT, T>

    fun Base.getReferencedBy(): Collection<Base>

    fun <T : Base> T.getVersionBefore(): HistoryEntryContext<T, ROOT>?

    fun <T : Base> T.getVersionsBefore(): List<HistoryEntryContext<T, ROOT>> {
        val result = mutableListOf<HistoryEntryContext<T, ROOT>>()
        var context = this@SnapShotContext
        var entry = this
        while (true) {
            with(context) {
                entry.getVersionBefore()
            }?.let { versionBefore ->
                result += versionBefore
                context = versionBefore.snapShotContext
                entry = versionBefore.entry
            } ?: break
        }
        return result
    }

    fun <T> update(update: ChangeContext<ROOT>.() -> T): T

}


interface ChangeContext<ROOT : Base> : SnapShotContext<ROOT> {

    context(SnapShotContext<ROOT>)
    fun <T : Base> T.persist(): T

    val nextSnapShotVersion: SNAPSHOT_VERSION

    fun Base.checkIsCurrent()

}
