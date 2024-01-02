package de.danielscholz.database.core

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong


@Serializable
abstract class Base {

    companion object {
        private val idGen = AtomicLong()
        fun getNextId() = idGen.incrementAndGet()
    }

    abstract val id: ID

    abstract val version: Long

    abstract val snapShotVersion: SNAPSHOT_VERSION

    abstract fun setSnapShotVersion(snapShotVersion: SNAPSHOT_VERSION): Base

    abstract val referencedIds: Set<ID>


    final override fun equals(other: Any?) = this === other

    final override fun hashCode() = id.hashCode()
}
