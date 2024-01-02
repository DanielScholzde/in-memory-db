package de.danielscholz.database

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.Database
import de.danielscholz.database.core.context.SnapShotContext
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class Test1 {

    private lateinit var database: Database<Shop>

    @BeforeEach
    fun init() {
        database = Database(Shop.empty()).apply {
            update {
                root.change(title = "Shop 1")
                    .addItemGroups(
                        setOf(
                            ItemGroup.of(title = "Group1")
                                .addItem(Item.of(title = "Soap", price = 1.79)),
                            ItemGroup.of(title = "Group2")
                                .addItem(Item.of(title = "Melon", price = 0.99))
                        )
                    )
            }
        }
    }

    context(SnapShotContext<Shop>)
    private fun Shop.getItemGroup1(): ItemGroup {
        return itemGroups().first { it.title == "Group1" }
    }

    context(SnapShotContext<Shop>)
    private fun Shop.getItemSoap(): Item {
        return getItemGroup1().items().first { it.title == "Soap" }
    }

    context(SnapShotContext<Shop>)
    private fun Shop.getItemMilk(): Item {
        return getItemGroup1().items().first { it.title == "Milk" }
    }

    @Test
    fun test11() {
        database.perform {
            update {
                val item = Item.of(title = "Milk", price = 1.29)
                val updated = root.getItemGroup1().addItem(item)
                item.getItemGroup() shouldBe updated
            }
            root.getItemMilk().getItemGroup() shouldBe root.getItemGroup1()
        }
    }

    @Test
    fun test12() {
        database.perform {
            root.title shouldBe "Shop 1"
            update {
                root.change(title = "My Shop")
                root.title shouldBe "My Shop"
                root.getItemSoap().change(price = 2.99)
                root.getItemSoap().price shouldBe 2.99
            }
            root.title shouldBe "My Shop"
            root.getItemSoap().price shouldBe 2.99
        }
    }

    @Test
    fun test13() {
        database.perform {
            root.getItemSoap().price shouldBe 1.79
            update {
                val item = root.getItemSoap()
                val changed = item.change(price = 3.99)
                changed.getVersionBefore()!!.perform {
                    it.price shouldBe 1.79
                }
            }
            root.getItemSoap().getVersionBefore()!!.perform {
                it.price shouldBe 1.79
            }
        }
    }

    @Test
    fun test14() {
        database.perform {
            val soap = root.getItemSoap()
            soap.price shouldBe 1.79
            update {
                val item = root.getItemSoap()
                item shouldBe item.change(price = 1.79) // no change
            }
            soap shouldBe root.getItemSoap()
        }
    }

    @Test
    fun test15() {

        database.update {
            root.getItemGroup1().addItem(Item.of(title = "Milk", price = 1.29))
        }

        database.update {
            root.change(title = "My Shop")
            root.getItemSoap().change(price = 2.99)
        }

        database.update {
            root.getItemSoap().change(price = 3.99)
        }


        database.update {
            root.getItemSoap().change(price = 3.99) // no change
        }

        database.perform {
            val itemGroup = root.getItemGroup1()
            itemGroup.itemIds.size shouldBe 2
            itemGroup.getVersionBefore()!!.perform { itemGroupHist1 ->
                itemGroupHist1.print("1: ").snapShotVersion shouldBe 1
                itemGroupHist1.itemIds.size shouldBe 1
            }

            val item = root.getItemSoap()
            println("SnapShot.version: ${snapShot.version}")
            item.print().price shouldBe 3.99
            root.print().title shouldBe "My Shop"

            item.getVersionBefore()!!.perform { itemHist1 ->
                println("SnapShot.version: ${snapShot.version}")
                itemHist1.print().price shouldBe 2.99

                itemHist1.getVersionBefore()!!.perform { itemHist2 ->
                    println("SnapShot.version: ${snapShot.version}")
                    itemHist2.print().price shouldBe 1.79
                    itemHist2.getItemGroup().print().itemIds.size shouldBe 1
                    root.print().title shouldBe "Shop 1"
                }
            }
        }
    }


    private fun <T : Base> T.print(prefix: String? = null): T {
        println(prefix?.let { prefix + this } ?: this.toString())
        return this
    }
}