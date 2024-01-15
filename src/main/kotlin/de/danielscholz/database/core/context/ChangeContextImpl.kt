package de.danielscholz.database.core.context

import de.danielscholz.database.core.BackRef
import de.danielscholz.database.core.Base
import de.danielscholz.database.core.Database
import de.danielscholz.database.core.EXT_REF_IDX
import de.danielscholz.database.core.EntryNotFoundException
import de.danielscholz.database.core.ID
import de.danielscholz.database.core.SNAPSHOT_VERSION
import de.danielscholz.database.core.SnapShot
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentSetOf


/**
 * Context for an update.
 * All methods should only be called by one thread!
 */
class ChangeContextImpl<ROOT : Base>(
    override val database: Database<ROOT>,
    override val snapShot: SnapShot<ROOT>
) : ChangeContext<ROOT> {

    companion object {

        internal fun changedReferences(
            backReferences: PersistentMap<BackRef, PersistentSet<ID>>,
            sourceEntryId: ID,
            sourceRefIdx: EXT_REF_IDX,
            before: Set<ID>,
            after: Set<ID>
        ): PersistentMap<BackRef, PersistentSet<ID>> {
            val added = after - before
            val removed = before - after
            if (added.isEmpty() && removed.isEmpty()) return backReferences

            return backReferences.mutate { map ->
                added.forEach {
                    val ref = BackRef(it, sourceRefIdx)
                    val existingBackRefs = backReferences[ref] ?: persistentSetOf()
                    val changedBackRefs = existingBackRefs.add(sourceEntryId)
                    map[ref] = changedBackRefs
                }
                removed.forEach {
                    val ref = BackRef(it, sourceRefIdx)
                    val existingBackRefs = backReferences[ref] ?: persistentSetOf()
                    val changedBackRefs = existingBackRefs.add(sourceEntryId)
                    map[ref] = changedBackRefs
                }
            }
        }
    }

    internal val changed = mutableMapOf<ID, Base>()

    internal var backReferences: PersistentMap<BackRef, PersistentSet<ID>> = snapShot.backReferences


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


    override fun Base.checkIsCurrent() {
        if (this.asRef().get() != this) throw Exception("Used entry $this is an old version")
    }


    internal fun changedReferences(sourceEntryId: ID, sourceRefIdx: EXT_REF_IDX, before: Set<ID>, after: Set<ID>) {
        backReferences = Companion.changedReferences(backReferences, sourceEntryId, sourceRefIdx, before, after)
    }

    override fun Base.getReferencedBy(sourceRefIdx: EXT_REF_IDX): Collection<Base> {
        val referencedByObjectIds = backReferences[BackRef(this.id, sourceRefIdx)]
        return referencedByObjectIds?.map { it.resolve() } ?: setOf()
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

    override val context: SnapShotContext<ROOT>
        get() = this


    override fun <T> update(update: ChangeContext<ROOT>.() -> T): T {
        throw Exception("a nested update is not possible!")
    }

}