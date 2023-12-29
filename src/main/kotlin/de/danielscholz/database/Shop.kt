@file:UseSerializers(PersistentSetSerializer::class)

package de.danielscholz.database

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
    val title: String,
    val itemGroupIds: PersistentSet<ID>,
) : Base() {

    constructor(
        title: String,
    ) : this(getNextId(), 0, title, persistentSetOf())


    // generated
    context(Change, MyContext)
    fun change(title: String = this.title): Shop {
        return changeIntern(title = title)
    }

    // generated
    context(Change, MyContext)
    private fun changeIntern(title: String = this.title, itemGroupIds: PersistentSet<ID> = this.itemGroupIds): Shop {
        if (title != this.title || itemGroupIds != this.itemGroupIds) {
            return Shop(id, version + 1, title, itemGroupIds).persist()
        }
        return this
    }

    // generated
    context(MyContext)
    fun itemGroups(): Collection<ItemGroup> {
        return itemGroupIds.map { it.resolve() as ItemGroup }
    }

    context(MyContext)
    fun itemGroupsSorted(): List<ItemGroup> = itemGroups().sortedBy { it.id }


    // generated
    context(Change, MyContext)
    fun addItemGroup(itemGroup: ItemGroup): Shop {
        return changeIntern(itemGroupIds = itemGroupIds.add(itemGroup.persist().id))
    }

    // generated
    context(Change, MyContext)
    fun addItemGroups(itemGroups: Set<ItemGroup>): Shop {
        return changeIntern(itemGroupIds = itemGroupIds.addAll(itemGroups.map { it.persist().id }))
    }

    // generated
    context(Change, MyContext)
    fun removeItemGroup(itemGroup: ItemGroup): Shop {
        return changeIntern(itemGroupIds = itemGroupIds.remove(itemGroup.id))
    }

    // generated
    context(Change, MyContext)
    fun removeItemGroups(itemGroups: Set<ItemGroup>): Shop {
        return changeIntern(itemGroupIds = itemGroupIds.removeAll(itemGroups.map { it.id }))
    }

    // generated
    override val referencedIds get() = itemGroupIds

}


@Serializable
@SerialName("ItemGroup")
class ItemGroup private constructor(
    override val id: ID,
    override val version: Long,
    val title: String,
    val itemIds: PersistentSet<ID>,
) : Base() {

    constructor(
        title: String,
    ) : this(getNextId(), 0, title, persistentSetOf())


    // generated
    context(Change, MyContext)
    fun change(title: String = this.title): ItemGroup {
        return changeIntern(title = title)
    }

    // generated
    context(Change, MyContext)
    private fun changeIntern(title: String = this.title, itemIds: PersistentSet<ID> = this.itemIds): ItemGroup {
        if (title != this.title || itemIds != this.itemIds) {
            return ItemGroup(id, version + 1, title, itemIds).persist()
        }
        return this
    }

    // generated
    context(MyContext)
    fun items(): Collection<Item> {
        return itemIds.map { it.resolve() as Item }
    }

    context(MyContext)
    fun itemsSorted(): List<Item> = items().sortedBy { it.id }


    // generated
    context(Change, MyContext)
    fun removeItem(item: Item): ItemGroup {
        return changeIntern(itemIds = itemIds.remove(item.id))
    }

    // generated
    context(Change, MyContext)
    fun addItem(item: Item): ItemGroup {
        return changeIntern(itemIds = itemIds.add(item.persist().id))
    }

    // generated
    override val referencedIds get() = itemIds

}


@Serializable
@SerialName("Item")
class Item private constructor(
    override val id: ID,
    override val version: Long,
    val title: String,
    val price: Double,
) : Base() {

    constructor(
        title: String,
        price: Double,
    ) : this(getNextId(), 0, title, price)

    // generated
    context(MyContext)
    fun getItemGroup(): ItemGroup {
        return this.getReferencedBy().filterIsInstance<ItemGroup>().first()
    }

    // generated
    context(Change, MyContext)
    fun change(title: String = this.title, price: Double = this.price): Item {
        if (title != this.title || price != this.price) {
            return Item(id, version + 1, title, price).persist()
        }
        return this
    }

    // generated
    override val referencedIds get() = setOf<ID>()

}

