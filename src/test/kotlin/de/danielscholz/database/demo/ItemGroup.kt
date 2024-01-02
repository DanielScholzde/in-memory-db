@file:UseSerializers(PersistentSetSerializer::class)

package de.danielscholz.database.demo

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.ID
import de.danielscholz.database.core.SNAPSHOT_VERSION
import de.danielscholz.database.core.context.ChangeContext
import de.danielscholz.database.core.context.SnapShotContext
import de.danielscholz.database.serializer.PersistentSetSerializer
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers


@Serializable
@SerialName("ItemGroup")
class ItemGroup private constructor(
    override val id: ID,
    override val version: Long,
    override val snapShotVersion: SNAPSHOT_VERSION,
    val title: String,
    val itemIds: PersistentSet<ID>,
) : Base() {

    companion object {
        // generated
        context(ChangeContext<Shop>)
        fun of(title: String): ItemGroup {
            return ItemGroup(database.getNextId(), 0, nextSnapShotVersion, title, persistentSetOf()).persist()
        }
    }

    // generated
    context(ChangeContext<Shop>)
    private fun changeIntern(title: String = this.title, itemIds: PersistentSet<ID> = this.itemIds): ItemGroup {
        this.checkIsCurrent()
        if (title != this.title || itemIds != this.itemIds) {
            return ItemGroup(id, version + 1, nextSnapShotVersion, title, itemIds).persist()
        }
        return this
    }

    // generated
    context(ChangeContext<Shop>)
    fun change(title: String = this.title): ItemGroup {
        return changeIntern(title = title)
    }

    // generated
    context(SnapShotContext<Shop>)
    fun getShop(): Shop {
        return this.getReferencedBy().filterIsInstance<Shop>().first()
    }

    // generated
    context(SnapShotContext<Shop>)
    fun items(): Collection<Item> {
        return itemIds.map { it.resolve() as Item }
    }

    context(SnapShotContext<Shop>)
    fun itemsSorted(): List<Item> = items().sortedBy { it.id }


    // generated
    context(ChangeContext<Shop>)
    fun removeItem(item: Item): ItemGroup {
        return changeIntern(itemIds = itemIds.remove(item.id))
    }

    // generated
    context(ChangeContext<Shop>)
    fun addItem(item: Item): ItemGroup {
        return changeIntern(itemIds = itemIds.add(item.id))
    }

    // generated
    context(ChangeContext<Shop>)
    fun addItems(items: Set<Item>): ItemGroup {
        return changeIntern(itemIds = itemIds.addAll(items.map { it.id }))
    }

    // generated
    override fun toString(): String {
        return "ItemGroup(id=$id, version=$version, snapShotVersion=$snapShotVersion, title='$title', itemIds=$itemIds)"
    }

    // generated
    override val referencedIds get() = itemIds

}