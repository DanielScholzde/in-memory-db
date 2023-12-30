package de.danielscholz.database


fun main() {

    val database = Database()

    database.perform {
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

    database.perform {
        update {
            val item = Item(title = "Milk", price = 1.29)
            val updated = root.itemGroups().first { it.title == "Deo" }.addItem(item)
            assert(item.getItemGroup() == updated)
        }
    }

    database.perform {
        update {
            root.itemGroups().first { it.title == "Deo" }.itemsSorted().first().change(price = 2.99)
        }
    }

    database.perform {
        update {
            root.itemGroups().first { it.title == "Deo" }.itemsSorted().first().change(price = 3.99)
        }
    }

    database.perform {
        update {
            root.itemGroups().first { it.title == "Deo" }.itemsSorted().first().change(price = 3.99) // no change
        }
    }
}

fun assert(assertion: Boolean) {
    if (!assertion) throw Exception()
}