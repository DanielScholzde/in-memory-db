package de.danielscholz.database.core

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong


@Serializable
abstract class Base {

    companion object {
        private val idGen = AtomicLong() // global unique? or database unique?
        fun getNextId() = idGen.incrementAndGet()
        internal fun setMaxAssignedId(id: Long) = idGen.set(id)
    }

    abstract val id: ID

    abstract val version: Long

    abstract val snapShotVersion: SNAPSHOT_VERSION

    abstract val referencedIds: ImmutableSet<ID>


    final override fun equals(other: Any?) = this === other

    final override fun hashCode() = id.hashCode()

}
