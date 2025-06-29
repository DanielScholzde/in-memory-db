package de.danielscholz.database.core.context

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.ID
import de.danielscholz.database.core.Snapshot
import kotlinx.serialization.Serializable
import kotlin.time.Instant


@Serializable
internal class Diff(
    val time: Instant,
    val changed: Collection<Base>,
    val rootId: ID,
)


internal fun Snapshot<*>.toDiffSerialization() = Diff(time, changed, rootId)
