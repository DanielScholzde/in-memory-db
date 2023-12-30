package de.danielscholz.database


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

    override fun <T : Base> T.getVersionBefore(): HistoryEntryContext<T>? {
        if (changed[this.id] != null) {
            snapShot.allEntries[this.id]?.let {
                @Suppress("UNCHECKED_CAST")
                return HistoryEntryContext(SnapShotContextImpl(database, snapShot), it as T)
            }
            return null // entry is new
        }
        // copy from SnapShotContextImpl:
        val snapShot1 = snapShot.snapShotHistory[this.snapShotVersion - 1]
        snapShot1?.allEntries?.get(this.id)?.let {
            @Suppress("UNCHECKED_CAST")
            return HistoryEntryContext(SnapShotContextImpl(database, snapShot1), it as T)
        }
        return null
    }

    override fun update(update: ChangeContext.() -> Unit) {
        throw Exception()
    }

}