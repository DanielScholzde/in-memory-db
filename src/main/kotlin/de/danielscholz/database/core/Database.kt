package de.danielscholz.database.core

import de.danielscholz.database.core.context.ChangeContext
import de.danielscholz.database.core.context.Diff
import de.danielscholz.database.core.context.SnapShotContext
import de.danielscholz.database.core.context.SnapShotContextImpl
import de.danielscholz.database.core.context.toDiffSerialization
import de.danielscholz.database.core.context.toFullSerialization
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicLong
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

typealias ID = Long
typealias SNAPSHOT_VERSION = Long


class Database<ROOT : Base>(val name: String, private val root: ROOT) {

    private val idGen = AtomicLong(root.id)

    fun getNextId() = idGen.incrementAndGet()

    @Volatile
    internal var snapShot: SnapShot<ROOT> = SnapShot.init(root)
        private set


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

    internal class MakeChangeResult<ROOT : Base, T>(
        val changedSnapShot: SnapShot<ROOT>?,
        val otherResult: T,
        val performFullWriteOfSnapShot: Boolean = false
    )

    @Synchronized
    internal fun <T> makeChange(block: () -> MakeChangeResult<ROOT, T>): T {
        val makeChangeResult = block()
        makeChangeResult.changedSnapShot?.let {
            if (config.writeToFile) {
                if (makeChangeResult.performFullWriteOfSnapShot || !config.writeDiff(it.version)) {
                    writeFullSnapShot(it)
                } else {
                    writeDiffSnapShot(it)
                }
            }
            snapShot = it
        }
        return makeChangeResult.otherResult
    }

    fun clearHistory() {
        makeChange {
            MakeChangeResult(snapShot.clearHistory(), Unit, true)
        }
    }

    fun readFromFileSystem() {
        val snapShotRead = File(".").listFiles()!!.asSequence()
            .filter { it.name.startsWith("database_${name}_v") && it.name.endsWith("_diff.json") }
            .map { it to it.name.removePrefix("database_${name}_v").removeSuffix("_diff.json").toLong() }
            .sortedBy { it.second }
            .fold(SnapShot.init(root)) { snapShot1, (file, version) ->
                val diff = json.decodeFromString<Diff>(Files.readString(file.toPath()))

                val allEntries = snapShot1.allEntries.putAll(diff.changed.associateBy { it.id })

                SnapShot(
                    version,
                    snapShot1.time,
                    snapShot1.rootId,
                    allEntries,
                    diff.changed.toPersistentSet(),
                    snapShot1.snapShotHistory.put(version - 1, snapShot1)
                )
            }

        idGen.set(snapShotRead.allEntries.maxOf { it.value.id })
        snapShot = snapShotRead
    }

    fun writeFullSnapShot(snapShot: SnapShot<ROOT> = this.snapShot) {
        val snapShot1 = snapShot
        val file = File("database_${name}_v${snapShot1.version}_full.json")
        Files.writeString(file.toPath(), json.encodeToString(snapShot1.toFullSerialization())) // TODO encodeToStream
        println(file.name)
    }

    private fun writeDiffSnapShot(changedSnapShot: SnapShot<ROOT>) {
        val file = File("database_${name}_v${changedSnapShot.version}_diff.json")
        Files.writeString(file.toPath(), json.encodeToString(changedSnapShot.toDiffSerialization())) // TODO encodeToStream
        println(file.name)
    }


    @Volatile
    var config = Config()

    @Volatile
    internal var json = Json {
        initJson()
    }

    private fun JsonBuilder.initJson() {
        encodeDefaults = true
        prettyPrint = config.jsonPrettyPrint
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
