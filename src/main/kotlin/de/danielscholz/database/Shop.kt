@file:UseSerializers(PersistentSetSerializer::class)

package de.danielscholz.database

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.ChangeContext
import de.danielscholz.database.core.ID
import de.danielscholz.database.core.SNAPSHOT_VERSION
import de.danielscholz.database.core.SnapShotContext
import de.danielscholz.database.serializer.PersistentSetSerializer
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers


@Serializable
@SerialName("Shop")
class Shop private constructor(
    override val id: ID,
    override val version: Long,
    override val snapShotVersion: SNAPSHOT_VERSION,
    val title: String,
    val itemGroupIds: PersistentSet<ID>,
) : Base() {

    constructor(
        title: String,
    ) : this(getNextId(), 0, -1, title, persistentSetOf())

    override fun setSnapShotVersion(snapShotVersion: SNAPSHOT_VERSION): Shop {
        if (this.snapShotVersion != -1L) throw Exception()
        return Shop(id, version, snapShotVersion, title, itemGroupIds)
    }

    // generated
    context(ChangeContext<Shop>)
    fun change(title: String = this.title): Shop {
        return changeIntern(title = title)
    }

    // generated
    context(ChangeContext<Shop>)
    private fun changeIntern(title: String = this.title, itemGroupIds: PersistentSet<ID> = this.itemGroupIds): Shop {
        if (title != this.title || itemGroupIds != this.itemGroupIds) {
            return Shop(id, version + 1, nextSnapShotVersion, title, itemGroupIds).persist()
        }
        return this
    }

    // generated
    context(SnapShotContext<Shop>)
    fun itemGroups(): Collection<ItemGroup> {
        return itemGroupIds.map { it.resolve() as ItemGroup }
    }

    context(SnapShotContext<Shop>)
    fun itemGroupsSorted(): List<ItemGroup> = itemGroups().sortedBy { it.id }


    // generated
    context(ChangeContext<Shop>)
    fun addItemGroup(itemGroup: ItemGroup): Shop {
        return changeIntern(itemGroupIds = itemGroupIds.add(itemGroup.persist().id))
    }

    // generated
    context(ChangeContext<Shop>)
    fun addItemGroups(itemGroups: Set<ItemGroup>): Shop {
        return changeIntern(itemGroupIds = itemGroupIds.addAll(itemGroups.map { it.persist().id }))
    }

    // generated
    context(ChangeContext<Shop>)
    fun removeItemGroup(itemGroup: ItemGroup): Shop {
        return changeIntern(itemGroupIds = itemGroupIds.remove(itemGroup.id))
    }

    // generated
    context(ChangeContext<Shop>)
    fun removeItemGroups(itemGroups: Set<ItemGroup>): Shop {
        return changeIntern(itemGroupIds = itemGroupIds.removeAll(itemGroups.map { it.id }))
    }

    // generated
    override fun toString(): String {
        return "Shop(id=$id, version=$version, snapShotVersion=$snapShotVersion, title='$title', itemGroupIds=$itemGroupIds)"
    }

    // generated
    override val referencedIds get() = itemGroupIds


}


@Serializable
@SerialName("ItemGroup")
class ItemGroup private constructor(
    override val id: ID,
    override val version: Long,
    override val snapShotVersion: SNAPSHOT_VERSION,
    val title: String,
    val itemIds: PersistentSet<ID>,
) : Base() {

    constructor(
        title: String,
    ) : this(getNextId(), 0, -1, title, persistentSetOf())

    override fun setSnapShotVersion(snapShotVersion: SNAPSHOT_VERSION): ItemGroup {
        if (this.snapShotVersion != -1L) throw Exception()
        return ItemGroup(id, version, snapShotVersion, title, itemIds)
    }

    // generated
    context(ChangeContext<Shop>)
    fun change(title: String = this.title): ItemGroup {
        return changeIntern(title = title)
    }

    // generated
    context(ChangeContext<Shop>)
    private fun changeIntern(title: String = this.title, itemIds: PersistentSet<ID> = this.itemIds): ItemGroup {
        if (title != this.title || itemIds != this.itemIds) {
            return ItemGroup(id, version + 1, nextSnapShotVersion, title, itemIds).persist()
        }
        return this
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
        return changeIntern(itemIds = itemIds.add(item.persist().id))
    }

    // generated
    override fun toString(): String {
        return "ItemGroup(id=$id, version=$version, snapShotVersion=$snapShotVersion, title='$title', itemIds=$itemIds)"
    }

    // generated
    override val referencedIds get() = itemIds

}


@Serializable
@SerialName("Item")
class Item private constructor(
    override val id: ID,
    override val version: Long,
    override val snapShotVersion: SNAPSHOT_VERSION,
    val title: String,
    val price: Double,
) : Base() {

    constructor(
        title: String,
        price: Double,
    ) : this(getNextId(), 0, -1, title, price)

    override fun setSnapShotVersion(snapShotVersion: SNAPSHOT_VERSION): Item {
        if (this.snapShotVersion != -1L) throw Exception()
        return Item(id, version, snapShotVersion, title, price)
    }

    // generated
    context(SnapShotContext<Shop>)
    fun getItemGroup(): ItemGroup {
        return this.getReferencedBy().filterIsInstance<ItemGroup>().first()
    }

    // generated
    context(ChangeContext<Shop>)
    fun change(title: String = this.title, price: Double = this.price): Item {
        if (title != this.title || price != this.price) {
            return Item(id, version + 1, nextSnapShotVersion, title, price).persist()
        }
        return this
    }

    // generated
    override fun toString(): String {
        return "Item(id=$id, version=$version, snapShotVersion=$snapShotVersion, title='$title', price=$price)"
    }

    // generated
    override val referencedIds get() = setOf<ID>()

}

