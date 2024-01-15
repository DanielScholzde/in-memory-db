@file:UseSerializers(PersistentSetSerializer::class)

package de.danielscholz.database.demo

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.EXT_REF_IDX
import de.danielscholz.database.core.ID
import de.danielscholz.database.core.SNAPSHOT_VERSION
import de.danielscholz.database.core.context.ChangeContext
import de.danielscholz.database.core.context.ChangeContextImpl
import de.danielscholz.database.core.context.SnapShotContext
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
@SerialName("Shop")
class Shop private constructor(
    override val id: ID,
    override val version: Long,
    override val snapShotVersion: SNAPSHOT_VERSION,
    val title: String,
    val itemGroupIds: PersistentSet<ID>,
) : Base() {

    companion object {
        fun empty(): Shop {
            return Shop(1, 0, 0, "", persistentSetOf())
        }
    }

    // will be generated in future
    context(ChangeContext<Shop>)
    fun change(title: String = this.title): Shop {
        return changeIntern(title = title)
    }

    // will be generated in future
    // private changeIntern method contains itemGroupIds (in general: all external references)
    context(ChangeContext<Shop>)
    private fun changeIntern(title: String = this.title, itemGroupIds: PersistentSet<ID> = this.itemGroupIds): Shop {
        this.checkIsCurrent()
        if (title != this.title || itemGroupIds != this.itemGroupIds) {
            (context as ChangeContextImpl).changedReferences(id, 0, this.itemGroupIds, itemGroupIds)
            return Shop(id, version + 1, nextSnapShotVersion, title, itemGroupIds).persist()
        }
        return this
    }

    // will be generated in future
    context(SnapShotContext<Shop>)
    fun itemGroups(): Collection<ItemGroup> {
        this.checkIsCurrent()
        return itemGroupIds.map { it.resolve() as ItemGroup }
    }

    context(SnapShotContext<Shop>)
    fun itemGroupsSorted(): List<ItemGroup> = itemGroups().sortedBy { it.id }


    // will be generated in future
    context(ChangeContext<Shop>)
    fun addItemGroup(itemGroup: ItemGroup): Shop {
        return changeIntern(itemGroupIds = itemGroupIds.add(itemGroup.id))
    }

    // will be generated in future
    context(ChangeContext<Shop>)
    fun addItemGroups(itemGroups: Set<ItemGroup>): Shop {
        return changeIntern(itemGroupIds = itemGroupIds.addAll(itemGroups.map { it.id }))
    }

    // will be generated in future
    context(ChangeContext<Shop>)
    fun removeItemGroup(itemGroup: ItemGroup): Shop {
        return changeIntern(itemGroupIds = itemGroupIds.remove(itemGroup.id))
    }

    // will be generated in future
    context(ChangeContext<Shop>)
    fun removeItemGroups(itemGroups: Set<ItemGroup>): Shop {
        return changeIntern(itemGroupIds = itemGroupIds.removeAll(itemGroups.map { it.id }))
    }

    // will be generated in future
    override fun toString(): String {
        return "Shop(id=$id, version=$version, snapShotVersion=$snapShotVersion, title='$title', itemGroupIds=$itemGroupIds)"
    }

    // will be generated in future
    override val referencedIds: ImmutableMap<EXT_REF_IDX, ImmutableSet<ID>> get() = persistentMapOf(0.toByte() to itemGroupIds)

}
