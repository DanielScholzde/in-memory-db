package de.danielscholz.database.core

import de.danielscholz.database.core.context.ChangeContext
import de.danielscholz.database.core.context.ChangeContextImpl
import de.danielscholz.database.core.context.Diff
import de.danielscholz.database.core.context.SnapShotContext
import de.danielscholz.database.core.context.SnapShotContextImpl
import de.danielscholz.database.core.context.toDiffSerialization
import de.danielscholz.database.core.context.toFullSerialization
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentMap
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
typealias EXT_REF_IDX = Byte


class Database<ROOT : Base>(val name: String, root: ROOT) {

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
                if (snapShot.version == 0L) {
                    writeDiffSnapShot(snapShot)
                }
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

    /**
     * Reads database content from file system.
     */
    fun readFromFileSystem() {
        // TODO reads only diffs currently
        val snapShotRead = File(".").listFiles()!!.asSequence()
            .filter { it.name.startsWith("database_${name}_v") && it.name.endsWith("_diff.json") }
            .map { it to it.name.removePrefix("database_${name}_v").removeSuffix("_diff.json").toLong() }
            .sortedBy { it.second }
            .fold(null) { snapShot1: SnapShot<ROOT>?, (file, version) ->

                val diff = json.decodeFromString<Diff>(Files.readString(file.toPath()))

                val allEntries =
                    snapShot1?.allEntries?.putAll(diff.changed.associateBy { it.id }) ?: diff.changed.associateBy { it.id }.toPersistentMap()

                var backReferences = snapShot1?.backReferences ?: persistentMapOf()

                diff.changed.forEach { entryAfter ->
                    val entryBefore = snapShot1?.allEntries?.get(entryAfter.id)
                    val referencedIdsBefore = entryBefore?.referencedIds
                    entryAfter.referencedIds.forEach {
                        val idsBefore = referencedIdsBefore?.get(it.key) ?: persistentSetOf()
                        val idsAfter = it.value
                        backReferences = ChangeContextImpl.changedReferences(backReferences, entryAfter.id, it.key, idsBefore, idsAfter)
                    }
                }

                SnapShot(
                    version,
                    diff.time,
                    diff.rootId,
                    allEntries,
                    diff.changed.toPersistentSet(),
                    snapShot1?.snapShotHistory?.put(version - 1, snapShot1) ?: persistentMapOf(),
                    backReferences
                )
            }

        if (snapShotRead != null) {
            idGen.set(snapShotRead.allEntries.maxOf { it.value.id })
            snapShot = snapShotRead
        }
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
