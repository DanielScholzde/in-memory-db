fun main() {

    with(Database.snapShot.asContext) {
        Database.update {
            root.change(title = "test title")
                .addOrReplaceItemGroups(
                    listOf(
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
            root.itemGroups()[0].addOrReplaceItem(
                Item(title = "Milk", price = 1.29)
            )
        }
    }

    with(Database.snapShot.asContext) {
        Database.update {
            with(root.itemGroups()[0]) {
                addOrReplaceItem(items()[0].change(price = 2.99))
            }
        }
    }

    with(Database.snapShot.asContext) {
        Database.update {
            root.itemGroups()[1].items()[0].change(price = 3.99)
        }
    }

    with(Database.snapShot.asContext) {
        Database.update {
            root.itemGroups()[1].items()[0].change(price = 3.99) // no change
        }
    }
}