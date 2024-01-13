package de.danielscholz.database.core.context

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.Database
import de.danielscholz.database.core.EntryNotFoundException
import de.danielscholz.database.core.ID
import de.danielscholz.database.core.SNAPSHOT_VERSION
import de.danielscholz.database.core.SnapShot


class ChangeContextImpl<ROOT : Base>(override val database: Database<ROOT>, override val snapShot: SnapShot<ROOT>) : ChangeContext<ROOT> {

    internal val changed = mutableMapOf<ID, Base>()


    @Suppress("UNCHECKED_CAST")
    override val root: ROOT
        get() = snapShot.rootId.resolve() as ROOT // this is only valid when root ID never changes


    override fun ID.resolve() =
        changed[this]
            ?: snapShot.allEntries[this]
            ?: throw EntryNotFoundException("Entry with id $this could not be found within snapShot ${snapShot.version}!")


    override fun <T : Base> T.asRef(): Reference<ROOT, T> {
        return Reference(this.id)
    }


    context(ChangeContext<ROOT>)
    override fun <T : Base> T.persist(): T {
        val existing = snapShot.allEntries[this.id]
        if (existing == this) return this
        if (existing != null && this.version <= existing.version) throw Exception("Updating an entry with an old version is not possible")
        changed[this.id] = this
        return this
    }


    override val nextSnapShotVersion: SNAPSHOT_VERSION
        get() = snapShot.version + 1

    /**
     * Checks if given entry is the last (current) version
     */
    override fun Base.checkIsCurrent() {
        if (this.asRef().get() != this) throw Exception()
    }


    override fun Base.getReferencedBy(): Collection<Base> {
        val result = mutableSetOf<ID>()

        val referencedByObjectIds = snapShot.backReferences[this.id]
        referencedByObjectIds.forEach { referencedByObjectId ->
            if (referencedByObjectId !in changed.keys) {
                result.add(referencedByObjectId)
            } else {
                val referencedIds = changed[referencedByObjectId]!!.referencedIds
                if (referencedByObjectId in referencedIds) result += referencedByObjectId
            }
        }

        changed.values.forEach {
            if (this.id in it.referencedIds) {
                result.add(it.id)
            }
        }
        return result.map { it.resolve() }
    }


    override fun <T : Base> T.getVersionBefore(): HistoryEntryContext<T, ROOT>? {
        if (changed[this.id] != null) {
            snapShot.allEntries[this.id]?.let {
                @Suppress("UNCHECKED_CAST")
                return HistoryEntryContext(SnapShotContextImpl(database, snapShot), it as T)
            }
            return null // entry is new
        }
        return SnapShotContextImpl.getVersionBefore(this, snapShot, database)
    }


    override fun <T> update(update: ChangeContext<ROOT>.() -> T): T {
        throw Exception("a nested update is not possible!")
    }

}