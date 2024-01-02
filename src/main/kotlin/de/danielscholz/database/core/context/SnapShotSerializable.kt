package de.danielscholz.database.core.context

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.ID
import de.danielscholz.database.core.SNAPSHOT_VERSION
import de.danielscholz.database.core.SnapShot
import kotlinx.serialization.Serializable


@Serializable
class SnapShotSerializable(
    val rootId: ID,
    val allEntries: Set<Base>,
    val changed: Set<ID>,
    val snapShotHistory: Map<SNAPSHOT_VERSION, SnapShotSerializable>?
)


fun SnapShot<*>.toSerializable(depth: Int = 0): SnapShotSerializable {
    return SnapShotSerializable(
        rootId,
        allEntries.values.toSet(),
        changed.map { it.id }.toSet(),
        if (depth == 0) snapShotHistory.map { it.key to it.value.toSerializable(1) }.toMap() else null
    )
}