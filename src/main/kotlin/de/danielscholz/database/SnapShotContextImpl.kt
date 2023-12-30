package de.danielscholz.database

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

    override fun <T : Base> T.getVersionBefore(): Pair<SnapShotContext, T>? {
        val snapShot1 = snapShot.snapShotHistory[this.snapShotVersion - 1]
        val get = snapShot1?.allEntries?.get(this.id)
        if (get != null) {
            @Suppress("UNCHECKED_CAST")
            return SnapShotContextImpl(database, snapShot1) to get as T
        }
        return null
    }


    @Serializable
    internal class Diff(val changed: Collection<Base>)


    @Synchronized
    override fun update(update: ChangeContext.() -> Unit) {
        val change = ChangeContextImpl(database, snapShot)

        change.update()

        if (change.changed.isNotEmpty()) {

            val map = change.changed.map { it.key to it.value.setSnapShotVersion(snapShot.version + 1) }.toMap()

            val changedRoot = map[snapShot.root.id]?.let { it as Shop }
            val changedSnapShot = snapShot.copyIntern(changedRoot ?: snapShot.root, map.values)

            if (database.writeDiff()) {
                val file = File("database_v${changedSnapShot.version}_diff.json")
                Files.writeString(file.toPath(), database.json.encodeToString(Diff(map.values)))
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