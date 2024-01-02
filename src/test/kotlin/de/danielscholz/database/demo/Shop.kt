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
@SerialName("Shop")
class Shop private constructor(
    override val id: ID,
    override val version: Long,
    override val snapShotVersion: SNAPSHOT_VERSION,
    val title: String,
    val itemGroupIds: PersistentSet<ID>,
) : Base() {

    companion object {
        // generated
        fun empty(): Shop {
            return Shop(getNextId(), 0, 0, "", persistentSetOf())
        }
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
