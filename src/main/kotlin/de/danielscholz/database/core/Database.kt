package de.danielscholz.database.core

import de.danielscholz.database.core.context.ChangeContext
import de.danielscholz.database.core.context.SnapShotContext
import de.danielscholz.database.core.context.SnapShotContextImpl
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import java.io.File
import java.nio.file.Files
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

typealias ID = Long
typealias SNAPSHOT_VERSION = Long


class Database<ROOT : Base>(val name: String, private val init: ROOT) {

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

    fun readFromFileSystem() {
        val snapShot1 = File(".").listFiles()!!.asSequence()
            .filter { it.name.startsWith("database_${name}_v") && it.name.endsWith("_diff.json") }
            .map { it to it.name.removePrefix("database_${name}_v").removeSuffix("_diff.json").toLong() }
            .sortedBy { it.second }
            .fold(SnapShot.init(init)) { snapShot1, (file, version) ->
                val diff = json.decodeFromString<SnapShotContextImpl.Diff>(Files.readString(file.toPath()))

                val allEntries = snapShot1.allEntries.putAll(diff.changed.associateBy { it.id })

                SnapShot(
                    version,
                    snapShot1.rootId,
                    allEntries,
                    diff.changed.toPersistentSet(),
                    snapShot1.snapShotHistory.put(version - 1, snapShot1)
                )
            }

        Base.setMaxAssignedId(snapShot1.allEntries.maxOf { it.value.id })
        snapShot = snapShot1
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
