package de.danielscholz.database

import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.json.Json

typealias ID = Long
typealias SNAPSHOT_VERSION = Long


class Database {

    @Volatile
    internal var snapShot: SnapShot = SnapShot(root = Shop(""), changed = persistentSetOf(), snapShotHistory = persistentMapOf())


    fun perform(block: SnapShotContext.() -> Unit) {
        val context = SnapShotContextImpl(this, snapShot)
        context.block()
    }


    internal fun writeDiff() = true


    internal val json = Json {
        encodeDefaults = true
        prettyPrint = true
    }

}
