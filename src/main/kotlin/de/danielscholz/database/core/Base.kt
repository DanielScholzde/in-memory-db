package de.danielscholz.database.core

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.serialization.Serializable


@Serializable
abstract class Base {

    abstract val id: ID

    abstract val version: Long

    abstract val snapshotVersion: SNAPSHOT_VERSION

    abstract val referencedIds: ImmutableMap<EXT_REF_IDX, ImmutableSet<ID>>


    final override fun equals(other: Any?) = this === other

    final override fun hashCode() = id.hashCode()

}
