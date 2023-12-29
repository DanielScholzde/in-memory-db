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


object Database {

    @Volatile
    var snapShot: SnapShot = SnapShot(root = Shop(""), changed = persistentSetOf(), parent = null)

    fun writeDiff() = true

    @Synchronized
    fun update(update: Change.() -> Unit) {
        val change = Change()

        change.update()

        if (change.changed.isNotEmpty()) {

            val changedRoot = change.changed[snapShot.root.id]?.let { it as Shop }
            val changedSnapShot = snapShot.copyIntern(changedRoot ?: snapShot.root, change.changed.values)

            if (writeDiff()) {
                val file = File("database_v${changedSnapShot.version}_diff.json")
                Files.writeString(file.toPath(), json.encodeToString(Diff(change.changed.values.toList())))
                println(file.name)
            } else {
                val file = File("database_v${changedSnapShot.version}_full.json")
                Files.writeString(file.toPath(), json.encodeToString(changedSnapShot))
                println(file.name)
            }

            snapShot = changedSnapShot
        }
    }

    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
    }

}


interface MyContext {
    val snapShot: SnapShot
    val change: Change?

    val root: Shop get() = change?.changed?.get(snapShot.root.id)?.let { it as Shop } ?: snapShot.root

    fun ID.resolve() = change?.changed?.get(this) ?: snapShot.allEntries[this] ?: throw Exception()

    fun Base.getReferencedBy(): Collection<Base> {
        val referencedByObjectIds = snapShot.backReferences[this.id]
        if (change == null) {
            return referencedByObjectIds.map { it.resolve() }
        }
        val x = change!!.changed.values.map { it.referencedIds }
        val result = mutableSetOf<ID>()
        referencedByObjectIds.forEach {
            if (it !in change!!.changed.keys) {
                result.add(it)
            } else {
                val referencedIds = change!!.changed[it]!!.referencedIds
                if (it in referencedIds) result += it
            }
        }
        return result.map { it.resolve() }
    }
}


class Change {

    internal val changed = mutableMapOf<ID, Base>()

    context(MyContext)
    internal fun <T : Base> T.persist(): T {
        val existing = snapShot.allEntries[this.id]
        if (existing == this) return this
        changed += this.id to this
        return this
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

    val asContext: MyContext
        get() = object : MyContext {
            override val snapShot: SnapShot = this@SnapShot
            override val change: Change? = null
        }


    internal fun copyIntern(
        root: Shop,
        changedEntries: Collection<Base>
    ): SnapShot {
        return SnapShot(version + 1, root, allEntries.addOrReplace(changedEntries.toList()), changedEntries.toPersistentSet(), this)
    }

//    @Transient
//    val meta = Meta(root)

}


@Serializable
private class Diff(val changed: List<Base>)


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
