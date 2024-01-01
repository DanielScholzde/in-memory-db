package de.danielscholz.database


fun main() {

//    val database = Database()
//
//    database.perform {
//        update {
//            root.change(title = "test title")
//                .addItemGroups(
//                    setOf(
//                        ItemGroup(title = "Deo")
//                            .addItem(Item(title = "Soap", price = 1.79)),
//                        ItemGroup(title = "Test")
//                            .addItem(Item(title = "Melon", price = 0.99))
//                    )
//                )
//        }
//    }
//
//    database.perform {
//        update {
//            val item = Item(title = "Milk", price = 1.29)
//            val updated = root.itemGroups().first { it.title == "Deo" }.addItem(item)
//            //assert(item.getItemGroup() == updated)
//        }
//
//    }
//
//    database.perform {
//        update {
//            root.change(title = "My Shop")
//            root.itemGroups().first { it.title == "Deo" }.itemsSorted().first().change(price = 2.99)
//            assert(root.title == "My Shop")
//        }
//        assert(root.title == "My Shop")
//    }
//
//    database.perform {
//        update {
//            val item = root.itemGroups().first { it.title == "Deo" }.itemsSorted().first()
//            val changed = item.change(price = 3.99)
//            changed.getVersionBefore()!!.perform {
//                println("Price before: ${it.price}")
//            }
//        }
//    }
//
//    database.perform {
//        update {
//            val item = root.itemGroups().first { it.title == "Deo" }.itemsSorted().first()
//            assert(item === item.change(price = 3.99)) // no change
//        }
//    }
//
//    database.perform {
//        val itemGroup = root.itemGroups().first { it.title == "Deo" }
//        assert(itemGroup.itemIds.size==2)
//        itemGroup.getVersionBefore()?.perform { itemGroupHist1 ->
//            assert(itemGroupHist1.itemIds.size==2)
//        }
//
//        val item = itemGroup.itemsSorted().first()
//        println("SnapShot.version: ${snapShot.version}")
//        assert(item.price == 3.99)
//        assert(root.title == "My Shop")
//        item.getVersionBefore()?.perform { itemHist1 ->
//            println("SnapShot.version: ${snapShot.version}")
//            assert(itemHist1.price == 2.99)
//            itemHist1.getVersionBefore()?.perform { itemHist2 ->
//                println("SnapShot.version: ${snapShot.version}")
//                assert(itemHist2.price == 1.79)
//                assert(itemHist2.getItemGroup().itemIds.size==1)
//                assert(root.title == "test title")
//            }
//        }
//    }
}

//fun assert(assertion: Boolean) {
//    if (!assertion) throw Exception()
//}