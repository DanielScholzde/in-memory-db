package de.danielscholz.database.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import java.nio.file.Files


class SnapShotContextImpl<ROOT : Base>(override val database: Database<ROOT>, snapShot: SnapShot<ROOT>) : SnapShotContext<ROOT> {

    @Volatile
    private var _snapShot: SnapShot<ROOT> = snapShot

    override val snapShot: SnapShot<ROOT>
        get() = _snapShot

    override val root: ROOT get() = snapShot.root

    override fun ID.resolve() = snapShot.allEntries[this] ?: throw Exception()

    override fun Base.getReferencedBy(): Collection<Base> {
        val referencedByObjectIds = snapShot.backReferences[this.id]
        return referencedByObjectIds.map { it.resolve() }
    }

    override fun <T : Base> T.getVersionBefore(): HistoryEntryContext<T, ROOT>? {
        val snapShot1 = snapShot.snapShotHistory[this.snapShotVersion - 1]
        snapShot1?.allEntries?.get(this.id)?.let {
            @Suppress("UNCHECKED_CAST")
            return HistoryEntryContext(SnapShotContextImpl<ROOT>(database, snapShot1), it as T)
        }
        return null
    }


    @Serializable
    internal class Diff(val changed: Collection<Base>)


    @Synchronized
    override fun update(update: ChangeContext<ROOT>.() -> Unit) {
        val snapShot = database.snapShot
        val change = ChangeContextImpl<ROOT>(database, snapShot)

        change.update()

        if (change.changed.isNotEmpty()) {

            val changedRoot = change.changed[snapShot.root.id]?.let {
                @Suppress("UNCHECKED_CAST")
                it as ROOT
            }
            val changedSnapShot = snapShot.copyIntern(changedRoot ?: snapShot.root, change.changed.values)

            if (database.writeDiff()) {
                val file = File("database_v${changedSnapShot.version}_diff.json")
                Files.writeString(file.toPath(), database.json.encodeToString(Diff(change.changed.values)))
                println(file.name)
            } else {
                val file = File("database_v${changedSnapShot.version}_full.json")
                Files.writeString(file.toPath(), database.json.encodeToString(changedSnapShot))
                println(file.name)
            }

            database.snapShot = changedSnapShot
            _snapShot = changedSnapShot
        }
    }

}