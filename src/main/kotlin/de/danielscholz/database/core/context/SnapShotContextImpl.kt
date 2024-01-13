package de.danielscholz.database.core.context

import com.google.common.collect.MultimapBuilder
import com.google.common.collect.SetMultimap
import de.danielscholz.database.core.Base
import de.danielscholz.database.core.Database
import de.danielscholz.database.core.EntryNotFoundException
import de.danielscholz.database.core.ID
import de.danielscholz.database.core.SnapShot
import kotlinx.serialization.encodeToString
import java.io.File
import java.nio.file.Files


class SnapShotContextImpl<ROOT : Base>(override val database: Database<ROOT>, snapShot: SnapShot<ROOT>) : SnapShotContext<ROOT> {

    companion object {

        fun <T : Base, ROOT : Base> getVersionBefore(entry: T, snapShot: SnapShot<ROOT>, database: Database<ROOT>): HistoryEntryContext<T, ROOT>? {
            val snapShotBefore = snapShot.snapShotHistory[entry.snapShotVersion - 1]
            snapShotBefore?.allEntries?.get(entry.id)?.let { entryBefore ->
                if (entryBefore.snapShotVersion == snapShotBefore.version) {
                    @Suppress("UNCHECKED_CAST")
                    return HistoryEntryContext(SnapShotContextImpl(database, snapShotBefore), entryBefore as T)
                } else {
                    val snapShotHist = snapShot.snapShotHistory[entryBefore.snapShotVersion]
                    snapShotHist?.allEntries?.get(entry.id)?.let { entryHist ->
                        @Suppress("UNCHECKED_CAST")
                        return HistoryEntryContext(SnapShotContextImpl(database, snapShotHist), entryHist as T)
                    }
                }
            }
            return null
        }

    }


    @Volatile
    private var _snapShot: SnapShot<ROOT> = snapShot

    override val snapShot: SnapShot<ROOT>
        get() = _snapShot

    @Suppress("UNCHECKED_CAST")
    override val root: ROOT get() = snapShot.rootId.resolve() as ROOT


    override fun ID.resolve() =
        snapShot.allEntries[this] ?: throw EntryNotFoundException("Entry with id $this could not be found within snapShot ${snapShot.version}!")


    override fun Base.getReferencedBy(): Collection<Base> {
        val referencedByObjectIds = snapShot.backReferences[this.id]
        return referencedByObjectIds.map { it.resolve() }
    }

    override fun <T : Base> T.asRef(): Reference<ROOT, T> {
        return Reference(this.id)
    }

    override fun <T : Base> T.getVersionBefore(): HistoryEntryContext<T, ROOT>? {
        return getVersionBefore(this, snapShot, database)
    }


    override fun <T> update(update: ChangeContext<ROOT>.() -> T): T { // result may contain one or many references
        return database.makeChange {

            val snapShot = database.snapShot // database.snapShot may be newer than _snapShot!
            val changeContext = ChangeContextImpl(database, snapShot)

            val result = changeContext.update()

            if (changeContext.changed.isNotEmpty()) {

                val newEntries = mutableSetOf<Base>()
                val changedEntries = mutableSetOf<Base>()
                val backReferences: SetMultimap<ID, ID> = MultimapBuilder.hashKeys().hashSetValues().build()

                changeContext.changed.values.forEach {
                    val old = snapShot.allEntries[it.id]

                    it.referencedIds.forEach { refId ->
                        backReferences.put(refId, it.id)
                    }

                    if (old != null) {
                        val removed = old.referencedIds - it.referencedIds
                    } else {
                        newEntries += it
                    }
                }

                if (newEntries.isNotEmpty()) {
                    newEntries.forEach {
                        // TODO
                    }
                }

                val changedSnapShot = snapShot.copyIntern(snapShot.rootId, changeContext.changed.values)

                changeContext.changed.forEach {
                    if (it.value.snapShotVersion != changedSnapShot.version) throw Exception("Internal error")
                    if (it.value.version < 0) throw Exception("Internal error")
                }

                if (database.writeToFile) {
                    if (database.writeDiff(changedSnapShot.version)) {
                        val file = File("database_${database.name}_v${changedSnapShot.version}_diff.json")
                        Files.writeString(file.toPath(), database.json.encodeToString(changedSnapShot.toDiffSerialization()))
                        println(file.name)
                    } else {
                        val file = File("database_${database.name}_v${changedSnapShot.version}_full.json")
                        Files.writeString(file.toPath(), database.json.encodeToString(changedSnapShot.toFullSerialization()))
                        println(file.name)
                    }
                }

                database.snapShot = changedSnapShot
                _snapShot = changedSnapShot
            }

            result
        }
    }

}