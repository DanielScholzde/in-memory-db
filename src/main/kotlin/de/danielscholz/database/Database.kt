package de.danielscholz.database

import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

typealias ID = Long
typealias SNAPSHOT_VERSION = Long


class Database {

    @Volatile
    internal var snapShot: SnapShot = SnapShot(root = Shop(""), changed = persistentSetOf(), snapShotHistory = persistentMapOf())


    fun perform(block: SnapShotContext.() -> Unit) {
        val context = SnapShotContextImpl(this, snapShot)
        context.block()
    }


    internal fun writeDiff() = true

    val serializersModule = SerializersModule {
        polymorphic(Base::class) {
            subclass(Shop::class)
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
