package de.danielscholz.database.core

import de.danielscholz.database.core.context.ChangeContext
import de.danielscholz.database.core.context.SnapShotContext
import de.danielscholz.database.core.context.SnapShotContextImpl
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

typealias ID = Long
typealias SNAPSHOT_VERSION = Long


class Database<ROOT : Base>(val name: String, init: ROOT) {

    @Volatile
    internal var snapShot: SnapShot<ROOT> = SnapShot.init(init) // TODO private set


    @OptIn(ExperimentalContracts::class)
    fun <T> perform(block: SnapShotContext<ROOT>.() -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        val context = SnapShotContextImpl(this, snapShot)
        return context.block()
    }

    fun <T> update(block: ChangeContext<ROOT>.() -> T): T {
        return perform {
            update(block)
        }
    }

    fun clearHistory() {
        makeChange {
            snapShot = snapShot.clearHistory()
        }
    }

    @Synchronized
    internal fun <T> makeChange(block: () -> T): T {
        return block()
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
