@file:UseSerializers(PersistentSetSerializer::class)

package de.danielscholz.database.core

import de.danielscholz.database.core.util.addOrReplace
import de.danielscholz.database.serializer.PersistentSetSerializer
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.UseSerializers


class Snapshot<ROOT : Base> internal constructor(
    val version: Long,
    val time: Instant,
    internal val rootId: ID,
    internal val allEntries: PersistentMap<ID, Base>,
    internal val changed: PersistentSet<Base>,
    internal val snapshotHistory: PersistentMap<SNAPSHOT_VERSION, Snapshot<ROOT>>,
    internal val backReferences: PersistentMap<BackRef, PersistentSet<ID>>,
) {

    companion object {

        fun <ROOT : Base> init(root: ROOT): Snapshot<ROOT> {
            return Snapshot(
                rootId = root.id,
                time = Clock.System.now(),
                version = 0,
                changed = persistentSetOf(root),
                allEntries = persistentMapOf(root.id to root),
                snapshotHistory = persistentMapOf(),
                backReferences = persistentMapOf() // TODO may root already contain referenced objects?
            )
        }
    }


    internal fun copyIntern(
        rootId: ID,
        changedEntries: Collection<Base>,
        backReferences: PersistentMap<BackRef, PersistentSet<ID>>,
    ): Snapshot<ROOT> {
        return Snapshot(
            version + 1,
            Clock.System.now(),
            rootId,
            allEntries.addOrReplace(changedEntries.toList()),
            changedEntries.toPersistentSet(),
            snapshotHistory.put(version, this),
            backReferences
        )
    }

    internal fun clearHistory(): Snapshot<ROOT> {
        return Snapshot(
            version,
            time,
            rootId,
            allEntries,
            persistentSetOf(),
            persistentMapOf(),
            backReferences
        )
    }

}