package de.danielscholz.database.core


interface SnapShotContext<ROOT : Base> {

    val database: Database<ROOT>

    val snapShot: SnapShot<ROOT>

    val root: ROOT

    fun ID.resolve(): Base

    fun Base.getReferencedBy(): Collection<Base>

    fun <T : Base> T.getVersionBefore(): HistoryEntryContext<T, ROOT>?

    fun update(update: ChangeContext<ROOT>.() -> Unit)

}


interface ChangeContext<ROOT : Base> : SnapShotContext<ROOT> {

    context(SnapShotContext<ROOT>)
    fun <T : Base> T.persist(): T

    val nextSnapShotVersion: SNAPSHOT_VERSION

}
