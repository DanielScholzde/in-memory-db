package de.danielscholz.database

import com.google.common.collect.MultimapBuilder
import com.google.common.collect.SetMultimap
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicLong

typealias ID = Long


class Database {

    @Volatile
    internal var _snapShot: SnapShot = SnapShot(root = Shop(""), changed = persistentSetOf(), parent = null)


    fun perform(block: SnapShotContext.() -> Unit) {
        val context = SnapShotContextImpl(this, _snapShot)
        context.block()
    }


    internal fun writeDiff() = true


    internal val json = Json {
        encodeDefaults = true
        prettyPrint = true
    }

}


interface SnapShotContext {

    val database: Database

    val snapShot: SnapShot

    val root: Shop

    fun ID.resolve(): Base

    fun Base.getReferencedBy(): Collection<Base>

    fun update(update: ChangeContext.() -> Unit)

}

interface ChangeContext : SnapShotContext {

    context(SnapShotContext)
    fun <T : Base> T.persist(): T

}


class ChangeContextImpl(override val database: Database, override val snapShot: SnapShot) : ChangeContext {

    internal val changed: MutableMap<ID, Base> = mutableMapOf()

    override val root: Shop get() = changed[snapShot.root.id]?.let { it as Shop } ?: snapShot.root

    override fun ID.resolve() = changed[this] ?: snapShot.allEntries[this] ?: throw Exception()

    context(SnapShotContext)
    override fun <T : Base> T.persist(): T {
        val existing = snapShot.allEntries[this.id]
        if (existing == this) return this
        changed[this.id] = this
        return this
    }

    override fun Base.getReferencedBy(): Collection<Base> {
        val referencedByObjectIds = snapShot.backReferences[this.id]
        val x = changed.values.flatMap { it.referencedIds }
        val result = mutableSetOf<ID>()
        referencedByObjectIds.forEach {
            if (it !in changed.keys) {
                result.add(it)
            } else {
                val referencedIds = changed[it]!!.referencedIds
                if (it in referencedIds) result += it
            }
        }
        x.forEach {

        }
        return result.map { it.resolve() }
    }

    override fun update(update: ChangeContext.() -> Unit) {
        throw Exception()
    }

}

class SnapShotContextImpl(override val database: Database, snapShot: SnapShot) : SnapShotContext {

    @Volatile
    private var _snapShot: SnapShot = snapShot

    override val snapShot: SnapShot
        get() = _snapShot

    override val root: Shop get() = snapShot.root

    override fun ID.resolve() = snapShot.allEntries[this] ?: throw Exception()

    override fun Base.getReferencedBy(): Collection<Base> {
        val referencedByObjectIds = snapShot.backReferences[this.id]
        return referencedByObjectIds.map { it.resolve() }
    }

    @Synchronized
    override fun update(update: ChangeContext.() -> Unit) {
        val change = ChangeContextImpl(database, snapShot)

        change.update()

        if (change.changed.isNotEmpty()) {

            val changedRoot = change.changed[snapShot.root.id]?.let { it as Shop }
            val changedSnapShot = snapShot.copyIntern(changedRoot ?: snapShot.root, change.changed.values)

            if (database.writeDiff()) {
                val file = File("database_v${changedSnapShot.version}_diff.json")
                Files.writeString(file.toPath(), database.json.encodeToString(Diff(change.changed.values)))
                println(file.name)
            } else {
                val file = File("database_v${changedSnapShot.version}_full.json")
                Files.writeString(file.toPath(), database.json.encodeToString(changedSnapShot))
                println(file.name)
            }

            database._snapShot = changedSnapShot
            _snapShot = changedSnapShot
        }
    }
}


@Serializable
class SnapShot(
    val version: Long = 0,
    internal val root: Shop,
    internal val allEntries: PersistentMap<ID, Base> = persistentMapOf(),
    val changed: PersistentSet<Base>,
    val parent: SnapShot?,
) {

    @Transient
    internal val backReferences: SetMultimap<ID, ID> = MultimapBuilder.hashKeys().hashSetValues().build()

    init {
        allEntries.values.forEach {
            it.referencedIds.forEach { refId ->
                backReferences.put(refId, it.id)
            }
        }
    }


    internal fun copyIntern(
        root: Shop,
        changedEntries: Collection<Base>
    ): SnapShot {
        return SnapShot(version + 1, root, allEntries.addOrReplace(changedEntries.toList()), changedEntries.toPersistentSet(), this)
    }

}


@Serializable
private class Diff(val changed: Collection<Base>)


@Serializable
sealed class Base {

    companion object {
        private val idGen = AtomicLong()
        fun getNextId() = idGen.incrementAndGet()
    }

    abstract val id: ID

    abstract val version: Long

    abstract val referencedIds: Set<ID>


    final override fun equals(other: Any?) = this === other

    final override fun hashCode() = id.hashCode()
}
