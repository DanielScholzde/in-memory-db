package de.danielscholz.database

import de.danielscholz.database.core.Base
import de.danielscholz.database.core.Database
import de.danielscholz.database.core.context.Reference
import de.danielscholz.database.core.util.performEach
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
    private lateinit var group1: Reference<Shop, ItemGroup>


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
                val group1 = ItemGroup.of(title = "Group1")
                root.change(title = "Shop 1")
                    .addItemGroups(
                        setOf(
                            group1
                                .addItem(soap),
                            ItemGroup.of(title = "Group2")
                                .addItem(Item.of(title = "Melon", price = 0.99))
                        )
                    )
                this@Test1.soap = soap.asRef()
                this@Test1.group1 = group1.asRef()
            }
        }
    }


    @Test
    fun test11() {
        database.perform {
            update {
                val item = Item.of(title = "Milk", price = 1.29)
                val updated = group1.get().addItem(item)
                item.getItemGroup() shouldBe updated
            }
            group1.get().items().first { it.title == "Milk" }.getItemGroup() shouldBe group1.get()
        }
    }

    @Test
    fun test12() {
        database.perform {
            root.title shouldBe "Shop 1"
            update {
                root.change(title = "My Shop")
                root.title shouldBe "My Shop"
                soap.get().change(price = 2.99)
                soap.get().price shouldBe 2.99
            }
            root.title shouldBe "My Shop"
            soap.get().price shouldBe 2.99
        }
    }

    @Test
    fun test13() {
        database.perform {
            soap.get().price shouldBe 1.79
            update {
                val item = soap.get()
                val changed = item.change(price = 3.99)
                changed.getVersionBefore()!!.perform {
                    it.price shouldBe 1.79
                }
            }
            soap.get().getVersionBefore()!!.perform {
                it.price shouldBe 1.79
            }
        }
    }

    @Test
    fun test14() {
        database.perform {
            val soap_ = soap.get()
            soap_.price shouldBe 1.79
            update {
                val item = soap.get()
                item shouldBe item.change(price = 1.79) // no change
            }
            soap_ shouldBe soap.get()
        }
    }

    @Test
    fun test15() {
        database.update {
            val milk = Item.of(title = "Milk", price = 1.29)
            group1.get().addItem(milk)
        }
        database.update {
            root.change(title = "My Shop")
            soap.get().change(price = 2.99)
        }
        database.update {
            soap.get().change(price = 3.99)
        }
        database.update {
            soap.get().change(price = 3.99) // no change
        }

        database.perform {
            val itemGroup = group1.get()
            itemGroup.itemIds.size shouldBe 2
            itemGroup.getVersionBefore()!!.perform { itemGroupHist1 ->
                itemGroupHist1.print("1: ").snapShotVersion shouldBe 1
                itemGroupHist1.itemIds.size shouldBe 1
            }

            println("SnapShot.version: ${snapShot.version}")
            soap.get().print().price shouldBe 3.99
            root.print().title shouldBe "My Shop"

            soap.get().getVersionBefore()!!.perform { itemHist1 ->
                println("SnapShot.version: ${snapShot.version}")
                itemHist1.print().price shouldBe 2.99

                itemHist1.getVersionBefore()!!.perform { itemHist2 ->
                    println("SnapShot.version: ${snapShot.version}")
                    itemHist2.print().price shouldBe 1.79
                    itemHist2.getItemGroup().print().itemIds.size shouldBe 1
                    root.print().title shouldBe "Shop 1"
                }
            }

            soap.get().getVersionsBefore().performEach {
                println(it.price)
                println(it.getItemGroup().getShop().title)
            }
        }
    }

    @Test
    fun performanceTest() {
        database.writeToFile = false
        try {
            measureTime {
                database.update {
                    group1.get().addItems(
                        (1..10_000).map { Item.of("Test $it", it.toDouble()) }.toSet()
                    )
                }
            }.let { duration ->
                println("Insert 10.000 Items: $duration")
            }

            measureTime {
                database.update {
                    group1.get().addItem(Item.of("Test ABC", 1.99))
                }
            }.let { duration ->
                println("Insert 1 Item: $duration")
            }

            (1..100).forEach {
                measureTime {
                    database.perform {
                        group1.get().items()
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