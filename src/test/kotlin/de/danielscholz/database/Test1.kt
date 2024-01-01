package de.danielscholz.database

import de.danielscholz.database.core.Database
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test


class Test1 {

    private val database = Database().apply {
        perform {
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
    }


    @Test
    fun test1() {

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
                root.title shouldBe "My Shop"
            }
            root.title shouldBe "My Shop"
        }

        database.perform {
            update {
                val item = root.itemGroups().first { it.title == "Deo" }.itemsSorted().first()
                val changed = item.change(price = 3.99)
                changed.getVersionBefore()!!.perform {
                    it.price shouldBe 2.99
                }
            }
        }

        database.perform {
            update {
                val item = root.itemGroups().first { it.title == "Deo" }.itemsSorted().first()
                item shouldBe item.change(price = 3.99) // no change
            }
        }

        database.perform {
            val itemGroup = root.itemGroups().first { it.title == "Deo" }
            itemGroup.itemIds.size shouldBe 2
            itemGroup.getVersionBefore()?.perform { itemGroupHist1 ->
                itemGroupHist1.itemIds.size shouldBe 2
            }

            val item = itemGroup.itemsSorted().first()
            println("SnapShot.version: ${snapShot.version}")
            item.price shouldBe 3.99
            root.title shouldBe "My Shop"
            item.getVersionBefore()?.perform { itemHist1 ->
                println("SnapShot.version: ${snapShot.version}")
                itemHist1.price shouldBe 2.99
                itemHist1.getVersionBefore()?.perform { itemHist2 ->
                    println("SnapShot.version: ${snapShot.version}")
                    itemHist2.price shouldBe 1.79
                    itemHist2.getItemGroup().itemIds.size shouldBe 1
                    root.title shouldBe "test title"
                }
            }
        }
    }

}