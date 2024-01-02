package de.danielscholz.database.core

import de.danielscholz.database.core.context.ChangeContext
import de.danielscholz.database.core.context.SnapShotContext
import de.danielscholz.database.core.context.SnapShotContextImpl
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

typealias ID = Long
typealias SNAPSHOT_VERSION = Long


class Database<ROOT : Base>(init: ROOT) {

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


    internal fun writeDiff() = true // TODO


    @Volatile
    internal var json = Json {
        encodeDefaults = true
        prettyPrint = true
    }

    fun addSerializationClasses(block: PolymorphicModuleBuilder<Base>.() -> Unit) {
        val serializers = SerializersModule {
            polymorphic(Base::class) {
                block()
            }
        }
        json = Json {
            serializersModule = serializers
            encodeDefaults = true
            prettyPrint = true
        }
    }

}
