package de.danielscholz.database

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.Database
import de.danielscholz.database.core.context.Reference
import de.danielscholz.database.core.context.SnapShotContext
import de.danielscholz.database.demo.Item
import de.danielscholz.database.demo.ItemGroup
import de.danielscholz.database.demo.Shop
import io.kotest.matchers.shouldBe
import kotlinx.serialization.modules.subclass
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.measureTime


class Test1 {

    private lateinit var database: Database<Shop>

    private lateinit var soap: Reference<Shop, Item>


    @BeforeEach
    fun init() {
        database = Database("Shop", Shop.empty()).apply {
            addSerializationClasses {
                subclass(Shop::class)
                subclass(ItemGroup::class)
                subclass(Item::class)
            }
            writeToFile = false
            update {
                val soap = Item.of(title = "Soap", price = 1.79)
                root.change(title = "Shop 1")
                    .addItemGroups(
                        setOf(
                            ItemGroup.of(title = "Group1")
                                .addItem(soap),
                            ItemGroup.of(title = "Group2")
                                .addItem(Item.of(title = "Melon", price = 0.99))
                        )
                    )
                this@Test1.soap = soap.asRef()
            }
        }
    }

    context(SnapShotContext<Shop>)
    private fun Shop.getItemGroup1(): ItemGroup {
        return itemGroups().first { it.title == "Group1" }
    }


    @Test
    fun test11() {
        database.perform {
            update {
                val item = Item.of(title = "Milk", price = 1.29)
                val updated = root.getItemGroup1().addItem(item)
                item.getItemGroup() shouldBe updated
            }
            root.getItemGroup1().items().first { it.title == "Milk" }.getItemGroup() shouldBe root.getItemGroup1()
        }
    }

    @Test
    fun test12() {
        database.perform {
            root.title shouldBe "Shop 1"
            update {
                root.change(title = "My Shop")
                root.title shouldBe "My Shop"
                soap.resolve().change(price = 2.99)
                soap.resolve().price shouldBe 2.99
            }
            root.title shouldBe "My Shop"
            soap.resolve().price shouldBe 2.99
        }
    }

    @Test
    fun test13() {
        database.perform {
            soap.resolve().price shouldBe 1.79
            update {
                val item = soap.resolve()
                val changed = item.change(price = 3.99)
                changed.getVersionBefore()!!.perform {
                    it.price shouldBe 1.79
                }
            }
            soap.resolve().getVersionBefore()!!.perform {
                it.price shouldBe 1.79
            }
        }
    }

    @Test
    fun test14() {
        database.perform {
            val soap_ = soap.resolve()
            soap_.price shouldBe 1.79
            update {
                val item = soap.resolve()
                item shouldBe item.change(price = 1.79) // no change
            }
            soap_ shouldBe soap.resolve()
        }
    }

    @Test
    fun test15() {
        database.update {
            val milk = Item.of(title = "Milk", price = 1.29)
            root.getItemGroup1().addItem(milk)
        }
        database.update {
            root.change(title = "My Shop")
            soap.resolve().change(price = 2.99)
        }
        database.update {
            soap.resolve().change(price = 3.99)
        }
        database.update {
            soap.resolve().change(price = 3.99) // no change
        }

        database.perform {
            val itemGroup = root.getItemGroup1()
            itemGroup.itemIds.size shouldBe 2
            itemGroup.getVersionBefore()!!.perform { itemGroupHist1 ->
                itemGroupHist1.print("1: ").snapShotVersion shouldBe 1
                itemGroupHist1.itemIds.size shouldBe 1
            }

            println("SnapShot.version: ${snapShot.version}")
            soap.resolve().print().price shouldBe 3.99
            root.print().title shouldBe "My Shop"

            soap.resolve().getVersionBefore()!!.perform { itemHist1 ->
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

    @Test
    fun performanceTest() {
        database.writeToFile = false
        try {
            measureTime {
                database.update {
                    root.getItemGroup1().addItems(
                        (1..10_000).map { Item.of("Test $it", it.toDouble()) }.toSet()
                    )
                }
            }.let { duration ->
                println("Insert 10.000 Items: $duration")
            }

            measureTime {
                database.update {
                    root.getItemGroup1().addItem(Item.of("Test ABC", 1.99))
                }
            }.let { duration ->
                println("Insert 1 Item: $duration")
            }

            (1..100).forEach {
                measureTime {
                    database.perform {
                        root.getItemGroup1().items()
                    }
                }.let { duration ->
                    if (it == 100) {
                        println("get all Items: $duration")
                    }
                }
            }
        } finally {
            database.writeToFile = true
        }
    }

    private fun <T : Base> T.print(prefix: String? = null) = apply {
        println(prefix?.let { prefix + this } ?: this.toString())
    }
}