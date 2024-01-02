package de.danielscholz.database.core

import com.google.common.collect.MultimapBuilder
import com.google.common.collect.SetMultimap
import de.danielscholz.database.core.util.addOrReplace
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
class SnapShot<ROOT : Base> private constructor(
    val version: Long,
    internal val root: ROOT,
    internal val allEntries: PersistentMap<ID, Base>,
    internal val changed: PersistentSet<Base>,
    internal val snapShotHistory: PersistentMap<SNAPSHOT_VERSION, SnapShot<ROOT>>
) {

    companion object {
        fun <ROOT : Base> init(root: ROOT): SnapShot<ROOT> {
            return SnapShot(
                root = root,
                version = 0,
                changed = persistentSetOf(),
                allEntries = persistentMapOf(),
                snapShotHistory = persistentMapOf()
            )
        }
    }

    @Transient
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
        root: ROOT,
        changedEntries: Collection<Base>
    ): SnapShot<ROOT> {
        return SnapShot(
            version + 1,
            root,
            allEntries.addOrReplace(changedEntries.toList()),
            changedEntries.toPersistentSet(),
            snapShotHistory.put(version, this)
        )
    }

    internal fun clearHistory(): SnapShot<ROOT> {
        return SnapShot(
            version,
            root,
            allEntries,
            persistentSetOf(),
            persistentMapOf()
        )
    }

}