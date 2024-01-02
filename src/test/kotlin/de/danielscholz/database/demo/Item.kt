package de.danielscholz.database.demo

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.ID
import de.danielscholz.database.core.SNAPSHOT_VERSION
import de.danielscholz.database.core.context.ChangeContext
import de.danielscholz.database.core.context.SnapShotContext
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("Item")
class Item private constructor(
    override val id: ID,
    override val version: Long,
    override val snapShotVersion: SNAPSHOT_VERSION,
    val title: String,
    val price: Double,
) : Base() {

    companion object {
        // generated
        context(ChangeContext<Shop>)
        fun of(title: String, price: Double): Item {
            return Item(getNextId(), 0, nextSnapShotVersion, title, price)
        }
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
    override val referencedIds get() = persistentSetOf<ID>()

}