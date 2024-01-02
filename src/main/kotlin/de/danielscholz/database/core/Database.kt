package de.danielscholz.database.core

import de.danielscholz.database.Item
import de.danielscholz.database.ItemGroup
import de.danielscholz.database.Shop
import de.danielscholz.database.core.context.SnapShotContext
import de.danielscholz.database.core.context.SnapShotContextImpl
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

typealias ID = Long
typealias SNAPSHOT_VERSION = Long


class Database<ROOT : Base>(init: ROOT) {

    @Volatile
    internal var snapShot: SnapShot<ROOT> = SnapShot.init(init)


    fun perform(block: SnapShotContext<ROOT>.() -> Unit) {
        val context = SnapShotContextImpl(this, snapShot)
        context.block()
    }


    internal fun writeDiff() = true // TODO

    val serializersModule = SerializersModule {
        polymorphic(Base::class) {
            subclass(Shop::class) // TODO
            subclass(ItemGroup::class)
            subclass(Item::class)
        }
    }

    internal val json = Json {
        serializersModule = this@Database.serializersModule
        encodeDefaults = true
        prettyPrint = true
    }

}
