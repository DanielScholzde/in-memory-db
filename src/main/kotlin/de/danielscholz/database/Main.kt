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
            val item = root.itemGroups().first { it.title == "Deo" }.itemsSorted().first()
            val changed = item.change(price = 3.99)
            changed.getVersionBefore()!!.perform {
                println("Price before: ${it.price}")
            }
        }
    }

    database.perform {
        update {
            val item = root.itemGroups().first { it.title == "Deo" }.itemsSorted().first()
            assert(item === item.change(price = 3.99)) // no change
        }
    }

    database.perform {
        val itemGroup = root.itemGroups().first { it.title == "Deo" }
        println(itemGroup.itemIds)
        itemGroup.getVersionBefore()?.perform { itemGroupHist1 ->
            println(itemGroupHist1.itemIds)
        }

        val item = itemGroup.itemsSorted().first()
        println("SnapShot.version: ${snapShot.version}")
        println(item.price)
        println(root.title)
        item.getVersionBefore()?.perform { itemHist1 ->
            println("SnapShot.version: ${snapShot.version}")
            println(itemHist1.price)
            itemHist1.getVersionBefore()?.perform { itemHist2 ->
                println("SnapShot.version: ${snapShot.version}")
                println(itemHist2.price)
                println(itemHist2.getItemGroup().itemIds)
                println(root.title)
            }
        }
    }
}

fun assert(assertion: Boolean) {
    if (!assertion) throw Exception()
}