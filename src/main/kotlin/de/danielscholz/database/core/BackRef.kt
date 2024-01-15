package de.danielscholz.database.core


internal data class BackRef(
    val targetId: ID, // ID of target entry
    val sourceExtRefIdx: EXT_REF_IDX, // Index/number of external reference of source entry
)