package de.danielscholz.database


interface SnapShotContext {

    val database: Database

    val snapShot: SnapShot

    val root: Shop

    fun ID.resolve(): Base

    fun Base.getReferencedBy(): Collection<Base>

    fun <T : Base> T.getVersionBefore(): Pair<SnapShotContext, T>?

    fun update(update: ChangeContext.() -> Unit)

}

interface ChangeContext : SnapShotContext {

    context(SnapShotContext)
    fun <T : Base> T.persist(): T

}


fun <T> Pair<SnapShotContext, T>.perform(block: SnapShotContext.(T) -> Unit) {
    this.first.block(second)
}
