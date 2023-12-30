package de.danielscholz.database

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong


@Serializable
sealed class Base {

    companion object {
        private val idGen = AtomicLong()
        fun getNextId() = idGen.incrementAndGet()
    }

    abstract val id: ID

    abstract val version: Long

    abstract val snapShotVersion: SNAPSHOT_VERSION

    abstract val referencedIds: Set<ID>

    abstract fun setSnapShotVersion(snapShotVersion: SNAPSHOT_VERSION): Base


    final override fun equals(other: Any?) = this === other

    final override fun hashCode() = id.hashCode()
}
