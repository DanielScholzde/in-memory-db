import com.google.common.collect.MultimapBuilder
import com.google.common.collect.SetMultimap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicLong


object Database {

    @Volatile
    var snapShot: SnapShot = SnapShot(root = Shop(""))

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

    fun Long.resolve() = change?.changed?.get(this) ?: snapShot.allEntries[this] ?: throw Exception()

    fun Base.getReferencedBy(): List<Base> {
        val ids = snapShot.backReferences[this.id]
        if (change == null) {
            return ids.map { it.resolve() }
        }
        change!!.changed.map { it.value.getReferencedIds() }
        val result = mutableSetOf<Long>()
        ids.forEach {

        }
    }
}

@Serializable
class SnapShot(
    val version: Long = 0,
    internal val root: Shop,
    internal val allEntries: Map<Long, Base> = mapOf(),
) {

    @Transient
    internal val backReferences: SetMultimap<Long, Long> = MultimapBuilder.hashKeys().hashSetValues().build()

    init {
        allEntries.values.forEach {
            it.getReferencedIds().forEach { refId ->
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
        root: Shop = this.root,
        changedEntries: Collection<Base>
    ): SnapShot {
        return SnapShot(version + 1, root, allEntries.addOrReplace(changedEntries.toList()))
    }

//    @Transient
//    val meta = Meta(root)

}

class Change {

    internal val changed = mutableMapOf<Long, Base>()

    context(MyContext)
    internal fun <T : Base> T.persist(): T {
        val existing = snapShot.allEntries[this.id]
        if (existing == this) return this
        changed += this.id to this
        return this
    }

}

@Serializable
private class Diff(val changed: List<Base>)


@Serializable
sealed class Base {

    companion object {
        private val idGen = AtomicLong()
        fun getNextId() = idGen.incrementAndGet()
    }

    abstract val id: Long

    abstract val version: Long

    abstract fun getReferencedIds(): Set<Long>


    final override fun equals(other: Any?) = this === other

    final override fun hashCode() = id.hashCode()
}
