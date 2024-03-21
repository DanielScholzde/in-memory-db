package de.danielscholz.database.core.context

import de.danielscholz.database.core.BackRef
import de.danielscholz.database.core.Base
import de.danielscholz.database.core.Database
import de.danielscholz.database.core.EXT_REF_IDX
import de.danielscholz.database.core.EntryNotFoundException
import de.danielscholz.database.core.ID
import de.danielscholz.database.core.Snapshot


/**
 * Context holding one database snapshot.
 * All methods and values provided here are thread-safe.
 */
class SnapshotContextImpl<ROOT : Base>(override val database: Database<ROOT>, snapshot: Snapshot<ROOT>) : SnapshotContextFinal<ROOT> {

    companion object {

        // Method needed to be within companion object, because it is used from ChangeContext too
        internal fun <T : Base, ROOT : Base> getVersionBefore(
            entry: T,
            snapshot: Snapshot<ROOT>,
            database: Database<ROOT>
        ): HistoryEntryContext<T, ROOT>? {
            val snapshotBefore = snapshot.snapshotHistory[entry.snapshotVersion - 1]
            snapshotBefore?.allEntries?.get(entry.id)?.let { entryBefore ->
                if (entryBefore.snapshotVersion == snapshotBefore.version) {
                    @Suppress("UNCHECKED_CAST")
                    return HistoryEntryContext(SnapshotContextImpl(database, snapshotBefore), entryBefore as T)
                } else {
                    val snapshotHist = snapshot.snapshotHistory[entryBefore.snapshotVersion]
                    snapshotHist?.allEntries?.get(entry.id)?.let { entryHist ->
                        @Suppress("UNCHECKED_CAST")
                        return HistoryEntryContext(SnapshotContextImpl(database, snapshotHist), entryHist as T)
                    }
                }
            }
            return null
        }

    }


    @Volatile
    override var snapshot: Snapshot<ROOT> = snapshot
        private set

    @Suppress("UNCHECKED_CAST")
    override val root: ROOT get() = snapshot.rootId.resolve() as ROOT


    override fun ID.resolve() =
        snapshot.allEntries[this] ?: throw EntryNotFoundException("Entry with id $this could not be found within snapshot ${snapshot.version}!")


    override fun Base.getReferencedBy(sourceRefIdx: EXT_REF_IDX): Collection<Base> {
        val referencedByObjectIds = snapshot.backReferences[BackRef(this.id, sourceRefIdx)]
        return referencedByObjectIds?.map { it.resolve() } ?: setOf()
    }

    override fun <T : Base> T.asRef(): Reference<ROOT, T> {
        return Reference(this.id)
    }

    override fun Base.checkIsCurrent() {
        if (this.asRef().get() != this) throw Exception("Used entry $this is an old version")
    }

    override fun <T : Base> T.getVersionBefore(): HistoryEntryContext<T, ROOT>? {
        return getVersionBefore(this, snapshot, database)
    }

    override val context: SnapshotContext<ROOT>
        get() = this


    override fun <T> update(update: ChangeContext<ROOT>.() -> T): T { // result may contain one or many references
        var changedSnapshot: Snapshot<ROOT>? = null

        val makeChange = database.makeChange {

            val snapshot = database.snapshot // database.snapshot may be newer than snapshot from SnapshotContextImpl!
            val changeContext = ChangeContextImpl(database, snapshot)

            val result = changeContext.update()

            if (changeContext.changed.isNotEmpty()) {

                changedSnapshot = snapshot.copyIntern(snapshot.rootId, changeContext.changed.values, changeContext.backReferences)

                changeContext.changed.forEach {
                    if (it.value.snapshotVersion != changedSnapshot!!.version) throw Exception("Internal error")
                    if (it.value.version < 0) throw Exception("Internal error")
                }

                Database.MakeChangeResult(changedSnapshot, result)
            } else {
                Database.MakeChangeResult(null, result)
            }
        }
        changedSnapshot?.let {
            snapshot = it
        }
        return makeChange
    }

}