@file:UseSerializers(PersistentSetSerializer::class)
@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package de.danielscholz.database.demo

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.EXT_REF_IDX
import de.danielscholz.database.core.ID
import de.danielscholz.database.core.SNAPSHOT_VERSION
import de.danielscholz.database.core.context.ChangeContext
import de.danielscholz.database.core.context.ChangeContextImpl
import de.danielscholz.database.core.context.SnapshotContext
import de.danielscholz.database.serializer.PersistentSetSerializer
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers


@Serializable
@SerialName("ItemGroup")
class ItemGroup private constructor(
    override val id: ID,
    override val version: Long,
    override val snapshotVersion: SNAPSHOT_VERSION,
    val title: String,
    val itemIds: PersistentSet<ID>,
) : Base() {

    companion object {
        // will be generated in the future
        context(ChangeContext<Shop>)
        fun of(title: String): ItemGroup {
            return ItemGroup(database.getNextId(), 0, nextSnapshotVersion, title, persistentSetOf()).persist()
        }
    }

    // will be generated in the future
    // public change method must not contain itemIds
    context(ChangeContext<Shop>)
    fun change(title: String = this.title): ItemGroup {
        return changeIntern(title = title)
    }

    // will be generated in the future
    // private changeIntern method contains itemIds (in general: all external references)
    context(ChangeContext<Shop>)
    private fun changeIntern(title: String = this.title, itemIds: PersistentSet<ID> = this.itemIds): ItemGroup {
        this.checkIsCurrent()
        if (title != this.title || itemIds != this.itemIds) {
            (context as ChangeContextImpl).changedReferences(id, 0, this.itemIds, itemIds)
            return ItemGroup(id, version + 1, nextSnapshotVersion, title, itemIds).persist()
        }
        return this
    }

    // will be generated in the future
    context(SnapshotContext<Shop>)
    fun shop(): Shop {
        this.checkIsCurrent()
        return this.getReferencedBy(0).first() as Shop
    }

    // will be generated in the future
    context(SnapshotContext<Shop>)
    fun items(): Collection<Item> {
        this.checkIsCurrent()
        return itemIds.map { it.resolve() as Item }
    }

    context(SnapshotContext<Shop>)
    fun itemsSorted(): List<Item> = items().sortedBy { it.id }


    // will be generated in the future
    context(ChangeContext<Shop>)
    fun removeItem(item: Item): ItemGroup {
        return changeIntern(itemIds = itemIds.remove(item.id))
    }

    // will be generated in the future
    context(ChangeContext<Shop>)
    fun addItem(item: Item): ItemGroup {
        return changeIntern(itemIds = itemIds.add(item.id))
    }

    // will be generated in the future
    context(ChangeContext<Shop>)
    fun addItems(items: Set<Item>): ItemGroup {
        return changeIntern(itemIds = itemIds.addAll(items.map { it.id }))
    }

    // will be generated in the future
    override fun toString(): String {
        return "ItemGroup(id=$id, version=$version, snapShotVersion=$snapshotVersion, title='$title', itemIds=$itemIds)"
    }

    // will be generated in the future
    override val referencedIds: ImmutableMap<EXT_REF_IDX, ImmutableSet<ID>> get() = persistentMapOf(0.toByte() to itemIds)

}