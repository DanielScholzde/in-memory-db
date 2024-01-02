package de.danielscholz.database.core

import de.danielscholz.database.core.context.ChangeContext
import de.danielscholz.database.core.context.SnapShotContext
import de.danielscholz.database.core.context.SnapShotContextImpl
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

typealias ID = Long
typealias SNAPSHOT_VERSION = Long


class Database<ROOT : Base>(val name: String, init: ROOT) {

    @Volatile
    internal var snapShot: SnapShot<ROOT> = SnapShot.init(init)


    fun perform(block: SnapShotContext<ROOT>.() -> Unit) {
        val context = SnapShotContextImpl(this, snapShot)
        context.block()
    }

    fun update(block: ChangeContext<ROOT>.() -> Unit) {
        perform {
            update(block)
        }
    }


    @Volatile
    var writeToFile: Boolean = true

    @Volatile
    var writeDiff: (SNAPSHOT_VERSION) -> Boolean = { true } // TODO


    @Volatile
    internal var json = Json {
        initJson()
    }

    private fun JsonBuilder.initJson() {
        encodeDefaults = true
        //prettyPrint = true
    }

    fun addSerializationClasses(block: PolymorphicModuleBuilder<Base>.() -> Unit) {
        val serializersModule = SerializersModule {
            polymorphic(Base::class) {
                block()
            }
        }
        json = Json {
            this.serializersModule = serializersModule
            initJson()
        }
    }

}
