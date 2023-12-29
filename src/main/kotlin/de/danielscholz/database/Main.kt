package de.danielscholz.database

import kotlinx.collections.immutable.persistentSetOf

fun main() {

    with(Database.snapShot.asContext) {
        Database.update {
            root.change(title = "test title")
                .addOrReplaceItemGroups(
                    persistentSetOf(
                        ItemGroup(title = "Deo")
                            .addOrReplaceItem(Item(title = "Soap", price = 1.79)),
                        ItemGroup(title = "Test")
                            .addOrReplaceItem(Item(title = "Melon", price = 0.99))
                    )
                )
        }
    }

    with(Database.snapShot.asContext) {
        Database.update {
            root.itemGroupsSorted().first().addOrReplaceItem(
                Item(title = "Milk", price = 1.29)
            )
        }
    }

    with(Database.snapShot.asContext) {
        Database.update {
            with(root.itemGroupsSorted().first()) {
                addOrReplaceItem(itemsSorted().first().change(price = 2.99))
            }
        }
    }

    with(Database.snapShot.asContext) {
        Database.update {
            root.itemGroupsSorted().first().itemsSorted().first().change(price = 3.99)
        }
    }

    with(Database.snapShot.asContext) {
        Database.update {
            root.itemGroupsSorted().first().itemsSorted().first().change(price = 3.99) // no change
        }
    }
}