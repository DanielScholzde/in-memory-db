package de.danielscholz.database.core.context

import de.danielscholz.database.core.Base
import kotlinx.serialization.Serializable


@Serializable
internal class Diff(val changed: Collection<Base>)