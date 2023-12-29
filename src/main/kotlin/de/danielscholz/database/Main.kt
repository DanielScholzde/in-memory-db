package de.danielscholz.database


fun main() {

    with(Database.snapShot.asContext) {
        Database.update {
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

    with(Database.snapShot.asContext) {
        Database.update {
            root.itemGroups().first { it.title == "Deo" }.addItem(
                Item(title = "Milk", price = 1.29)
            )
        }
    }

    with(Database.snapShot.asContext) {
        Database.update {
            root.itemGroups().first { it.title == "Deo" }.itemsSorted().first().change(price = 2.99)
        }
    }

    with(Database.snapShot.asContext) {
        Database.update {
            root.itemGroups().first { it.title == "Deo" }.itemsSorted().first().change(price = 3.99)
        }
    }

    with(Database.snapShot.asContext) {
        Database.update {
            root.itemGroups().first { it.title == "Deo" }.itemsSorted().first().change(price = 3.99) // no change
        }
    }
}