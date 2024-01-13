@file:UseSerializers(PersistentSetSerializer::class)

package de.danielscholz.database.core

import com.google.common.collect.MultimapBuilder
import com.google.common.collect.SetMultimap
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


class SnapShot<ROOT : Base> internal constructor(
    val version: Long,
    val time: Instant,
    internal val rootId: ID,
    internal val allEntries: PersistentMap<ID, Base>,
    internal val changed: PersistentSet<Base>,
    internal val snapShotHistory: PersistentMap<SNAPSHOT_VERSION, SnapShot<ROOT>>
) {

    companion object {
        fun <ROOT : Base> init(root: ROOT): SnapShot<ROOT> {
            return SnapShot(
                rootId = root.id,
                time = Clock.System.now(),
                version = 0,
                changed = persistentSetOf(root),
                allEntries = persistentMapOf(root.id to root),
                snapShotHistory = persistentMapOf()
            )
        }
    }

    // TODO
    internal val backReferences: SetMultimap<ID, ID> = MultimapBuilder.hashKeys().hashSetValues().build()

    init {
        // TODO
        allEntries.values.forEach {
            it.referencedIds.forEach { refId ->
                backReferences.put(refId, it.id)
            }
        }
    }


    internal fun copyIntern(
        rootId: ID,
        changedEntries: Collection<Base>
    ): SnapShot<ROOT> {
        return SnapShot(
            version + 1,
            Clock.System.now(),
            rootId,
            allEntries.addOrReplace(changedEntries.toList()),
            changedEntries.toPersistentSet(),
            snapShotHistory.put(version, this)
        )
    }

    internal fun clearHistory(): SnapShot<ROOT> {
        return SnapShot(
            version,
            time,
            rootId,
            allEntries,
            persistentSetOf(),
            persistentMapOf()
        )
    }

}