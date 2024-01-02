package de.danielscholz.database.core.context

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.Database
import de.danielscholz.database.core.ID
import de.danielscholz.database.core.SnapShot
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import java.nio.file.Files


class SnapShotContextImpl<ROOT : Base>(override val database: Database<ROOT>, snapShot: SnapShot<ROOT>) : SnapShotContext<ROOT> {

    companion object {
        fun <T : Base, ROOT : Base> getVersionBefore(entry: T, snapShot: SnapShot<ROOT>, database: Database<ROOT>): HistoryEntryContext<T, ROOT>? {
            val snapShot1 = snapShot.snapShotHistory[entry.snapShotVersion - 1]
            snapShot1?.allEntries?.get(entry.id)?.let {
                if (it.snapShotVersion == snapShot1.version) {
                    @Suppress("UNCHECKED_CAST")
                    return HistoryEntryContext(SnapShotContextImpl(database, snapShot1), it as T)
                } else {
                    val snapShot2 = snapShot.snapShotHistory[it.snapShotVersion]
                    snapShot2?.allEntries?.get(entry.id)?.let {
                        @Suppress("UNCHECKED_CAST")
                        return HistoryEntryContext(SnapShotContextImpl(database, snapShot2), it as T)
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


    override fun ID.resolve() = snapShot.allEntries[this] ?: throw Exception("Entry could not be found!")


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


    @Serializable
    internal class Diff(val changed: Collection<Base>)


    override fun <T> update(update: ChangeContext<ROOT>.() -> T): T {
        return database.makeChange {

            val snapShot = database.snapShot // snapShot may be newer than _snapShot!
            val changeContext = ChangeContextImpl(database, snapShot)

            val result = changeContext.update()

            if (changeContext.changed.isNotEmpty()) {

                val changedSnapShot = snapShot.copyIntern(snapShot.rootId, changeContext.changed.values)

                changeContext.changed.forEach {
                    if (it.value.snapShotVersion != changedSnapShot.version) throw Exception()
                    if (it.value.version < 0) throw Exception()
                }

                if (database.writeToFile) {
                    if (database.writeDiff(changedSnapShot.version)) {
                        val file = File("database_${database.name}_v${changedSnapShot.version}_diff.json")
                        Files.writeString(file.toPath(), database.json.encodeToString(Diff(changeContext.changed.values)))
                        println(file.name)
                    } else {
                        val file = File("database_${database.name}_v${changedSnapShot.version}_full.json")
                        Files.writeString(file.toPath(), database.json.encodeToString(changedSnapShot))
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