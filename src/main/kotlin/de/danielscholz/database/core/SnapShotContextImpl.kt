package de.danielscholz.database.core

import de.danielscholz.database.Shop
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import java.nio.file.Files


class SnapShotContextImpl(override val database: Database, snapShot: SnapShot) : SnapShotContext {

    @Volatile
    private var _snapShot: SnapShot = snapShot

    override val snapShot: SnapShot
        get() = _snapShot

    override val root: Shop get() = snapShot.root

    override fun ID.resolve() = snapShot.allEntries[this] ?: throw Exception()

    override fun Base.getReferencedBy(): Collection<Base> {
        val referencedByObjectIds = snapShot.backReferences[this.id]
        return referencedByObjectIds.map { it.resolve() }
    }

    override fun <T : Base> T.getVersionBefore(): HistoryEntryContext<T>? {
        val snapShot1 = snapShot.snapShotHistory[this.snapShotVersion - 1]
        snapShot1?.allEntries?.get(this.id)?.let {
            @Suppress("UNCHECKED_CAST")
            return HistoryEntryContext(SnapShotContextImpl(database, snapShot1), it as T)
        }
        return null
    }


    @Serializable
    internal class Diff(val changed: Collection<Base>)


    @Synchronized
    override fun update(update: ChangeContext.() -> Unit) {
        val snapShot = database.snapShot
        val change = ChangeContextImpl(database, snapShot)

        change.update()

        if (change.changed.isNotEmpty()) {

            val changedRoot = change.changed[snapShot.root.id]?.let { it as Shop }
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