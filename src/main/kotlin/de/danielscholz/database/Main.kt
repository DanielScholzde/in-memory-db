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
            //assert(item.getItemGroup() == updated)
        }

    }

    database.perform {
        update {
            root.change(title = "My Shop")
            root.itemGroups().first { it.title == "Deo" }.itemsSorted().first().change(price = 2.99)
            assert(root.title == "My Shop")
        }
        assert(root.title == "My Shop")
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

    database.perform {
        val itemGroup = root.itemGroups().first { it.title == "Deo" }
        println(itemGroup.itemIds)
        itemGroup.getVersionBefore()?.perform { itemGroupBefore ->
            println(itemGroupBefore.itemIds)
        }

        val item = itemGroup.itemsSorted().first()
        println("SnapShot.version: ${snapShot.version}")
        println(item.price)
        println(root.title)
        item.getVersionBefore()?.perform {
            println("SnapShot.version: ${snapShot.version}")
            println(it.price)
            it.getVersionBefore()?.perform {
                println("SnapShot.version: ${snapShot.version}")
                println(it.price)
                println(it.getItemGroup().itemIds)
                println(root.title)
            }
        }
    }
}

fun assert(assertion: Boolean) {
    if (!assertion) throw Exception()
}