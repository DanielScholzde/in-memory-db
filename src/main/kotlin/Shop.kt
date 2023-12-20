import kotlinx.serialization.Serializable


@Serializable
class Shop private constructor(
    override val id: Long,
    override val version: Long,
    val title: String,
    val itemGroupIds: List<Long>,
) : Base() {

    constructor(
        title: String,
    ) : this(getNextId(), 0, title, listOf())


    // generated
    context(Change, MyContext)
    fun change(title: String = this.title): Shop {
        return changeIntern(title = title)
    }

    // generated
    context(Change, MyContext)
    private fun changeIntern(title: String = this.title, itemGroupIds: List<Long> = this.itemGroupIds): Shop {
        if (title != this.title || itemGroupIds != this.itemGroupIds) {
            return Shop(id, version + 1, title, itemGroupIds).persist()
        }
        return this
    }

    // generated
    context(MyContext)
    fun itemGroups(): List<ItemGroup> {
        return itemGroupIds.map { it.resolve() as ItemGroup }
    }

    // generated
    context(Change, MyContext)
    fun addOrReplaceItemGroup(itemGroup: ItemGroup): Shop {
        return changeIntern(itemGroupIds = itemGroupIds.addOrReplaceEntry(itemGroup.persist().id))
    }

    // generated
    context(Change, MyContext)
    fun addOrReplaceItemGroups(itemGroups: List<ItemGroup>): Shop {
        return changeIntern(itemGroupIds = itemGroupIds.addOrReplaceEntries(itemGroups.map { it.persist().id }))
    }

    // generated
    override fun getReferencedIds(): Set<Long> {
        return itemGroupIds.toSet()
    }

}

@Serializable
class ItemGroup private constructor(
    override val id: Long,
    override val version: Long,
    val title: String,
    val itemIds: List<Long>,
) : Base() {

    constructor(
        title: String,
    ) : this(getNextId(), 0, title, listOf())


    // generated
    context(Change, MyContext)
    fun change(title: String = this.title): ItemGroup {
        return changeIntern(title = title)
    }

    // generated
    context(Change, MyContext)
    private fun changeIntern(title: String = this.title, itemIds: List<Long> = this.itemIds): ItemGroup {
        if (title != this.title || itemIds != this.itemIds) {
            return ItemGroup(id, version + 1, title, itemIds).persist()
        }
        return this
    }

    // generated
    context(MyContext)
    fun items(): List<Item> {
        return itemIds.map { it.resolve() as Item }
    }

    // generated
    context(Change, MyContext)
    fun addOrReplaceItem(item: Item): ItemGroup {
        return changeIntern(itemIds = itemIds.addOrReplaceEntry(item.persist().id))
    }

    // generated
    override fun getReferencedIds(): Set<Long> {
        return itemIds.toSet()
    }
}

@Serializable
class Item private constructor(
    override val id: Long,
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
        return change?.changed?.values?.filterIsInstance<ItemGroup>()?.firstOrNull { this.id in it.itemIds }
            ?: snapShot.allEntries.values.filterIsInstance<ItemGroup>().first { this.id in it.itemIds }
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
    override fun getReferencedIds(): Set<Long> {
        return setOf()
    }
}

class Meta(root: Shop) {


}


