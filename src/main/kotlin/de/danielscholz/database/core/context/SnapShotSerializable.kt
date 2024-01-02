package de.danielscholz.database.core.context

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.ID
import de.danielscholz.database.core.SNAPSHOT_VERSION
import de.danielscholz.database.core.SnapShot
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


@Serializable
internal class SnapShotSerializable(
    val time: Instant,
    val rootId: ID,
    val allEntries: Set<Base>,
    val changed: Set<ID>,
    val snapShotHistory: Map<SNAPSHOT_VERSION, SnapShotSerializable>?
)


internal fun SnapShot<*>.toFullSerialization(depth: Int = 0): SnapShotSerializable {
    return SnapShotSerializable(
        time,
        rootId,
        allEntries.values.toSet(),
        changed.map { it.id }.toSet(),
        if (depth == 0) snapShotHistory.map { it.key to it.value.toFullSerialization(1) }.toMap() else null
    )
}