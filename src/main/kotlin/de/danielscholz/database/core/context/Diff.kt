package de.danielscholz.database.core.context

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.SnapShot
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


@Serializable
internal class Diff(
    val time: Instant,
    val changed: Collection<Base>
)


internal fun SnapShot<*>.toDiffSerialization(): Diff {
    return Diff(time, changed)
}