@file:UseSerializers(PersistentSetSerializer::class)

package de.danielscholz.database.demo

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.EXT_REF_IDX
import de.danielscholz.database.core.ID
import de.danielscholz.database.core.SNAPSHOT_VERSION
import de.danielscholz.database.core.context.ChangeContext
import de.danielscholz.database.core.context.SnapshotContext
import de.danielscholz.database.serializer.PersistentSetSerializer
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers


@Serializable
@SerialName("Item")
class Item private constructor(
    override val id: ID,
    override val version: Long,
    override val snapshotVersion: SNAPSHOT_VERSION,
    val title: String,
    val price: Double,
) : Base() {

    companion object {
        // will be generated in future
        context(ChangeContext<Shop>)
        fun of(title: String, price: Double): Item {
            return Item(database.getNextId(), 0, nextSnapshotVersion, title, price).persist()
        }
    }

    // will be generated in future
    context(ChangeContext<Shop>)
    fun change(title: String = this.title, price: Double = this.price): Item {
        this.checkIsCurrent()
        if (title != this.title || price != this.price) {
            return Item(id, version + 1, nextSnapshotVersion, title, price).persist()
        }
        return this
    }

    // there is no changeIntern method because there are no external references

    // will be generated in future
    context(SnapshotContext<Shop>)
    fun itemGroup(): ItemGroup {
        this.checkIsCurrent()
        return this.getReferencedBy(0).first() as ItemGroup
    }

    // will be generated in future
    override fun toString(): String {
        return "Item(id=$id, version=$version, snapShotVersion=$snapshotVersion, title='$title', price=$price)"
    }

    // will be generated in future
    override val referencedIds: ImmutableMap<EXT_REF_IDX, ImmutableSet<ID>> get() = persistentMapOf()

}