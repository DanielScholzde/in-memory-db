package de.danielscholz.database


fun main() {

    with(Database.snapShot) {
        update {
            root.change(title = "test title")
                .addItemGroups(
                    setOf(
                        ItemGroup(title = "Deo")
                            .addItem(Item(title = "Soap", price = 1.79)),
                        ItemGroup(title = "Test")
                            .addItem(Item(title = "Melon", price = 0.99))
                    )
                )
        }
    }

    with(Database.snapShot) {
        update {
            val item = Item(title = "Milk", price = 1.29)
            val updated = root.itemGroups().first { it.title == "Deo" }.addItem(item)
            assert(item.getItemGroup() == updated)
        }
    }

    with(Database.snapShot) {
        update {
            root.itemGroups().first { it.title == "Deo" }.itemsSorted().first().change(price = 2.99)
        }
    }

    with(Database.snapShot) {
        update {
            root.itemGroups().first { it.title == "Deo" }.itemsSorted().first().change(price = 3.99)
        }
    }

    with(Database.snapShot) {
        update {
            root.itemGroups().first { it.title == "Deo" }.itemsSorted().first().change(price = 3.99) // no change
        }
    }
}

fun assert(assertion: Boolean) {
    if (!assertion) throw Exception()
}