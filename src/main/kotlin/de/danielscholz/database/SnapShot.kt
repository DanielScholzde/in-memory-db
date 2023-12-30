package de.danielscholz.database

import com.google.common.collect.MultimapBuilder
import com.google.common.collect.SetMultimap
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
class SnapShot(
    val version: Long = 0,
    internal val root: Shop,
    internal val allEntries: PersistentMap<ID, Base> = persistentMapOf(),
    val changed: PersistentSet<Base>,
    val snapShotHistory: PersistentMap<SNAPSHOT_VERSION, SnapShot>
) {

    @Transient
    internal val backReferences: SetMultimap<ID, ID> = MultimapBuilder.hashKeys().hashSetValues().build()

    init {
        allEntries.values.forEach {
            it.referencedIds.forEach { refId ->
                backReferences.put(refId, it.id)
            }
        }
    }


    internal fun copyIntern(
        root: Shop,
        changedEntries: Collection<Base>
    ): SnapShot {
        return SnapShot(
            version + 1,
            root,
            allEntries.addOrReplace(changedEntries.toList()),
            changedEntries.toPersistentSet(),
            snapShotHistory.put(version, this)
        )
    }

}