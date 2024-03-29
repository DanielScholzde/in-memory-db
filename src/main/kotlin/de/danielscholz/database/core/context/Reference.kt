package de.danielscholz.database.core.context

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.ID


class Reference<ROOT : Base, T : Base>(private val id: ID) {

    context(SnapshotContext<ROOT>)
    fun get(): T {
        @Suppress("UNCHECKED_CAST")
        return id.resolve() as T
    }

}