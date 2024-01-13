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
import org.junit.jupiter.api.assertThrows
import kotlin.time.measureTime


class Test1 {

    private lateinit var database: Database<Shop>

    private lateinit var soapRef: Reference<Shop, Item>
    private lateinit var group1Ref: Reference<Shop, ItemGroup>


    @BeforeEach
    fun init() {
        database = Database("ShopDB", Shop.empty()).apply {
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
                soapRef = soap.asRef()
                group1Ref = group1.asRef()
            }
        }
    }


    @Test
    fun test11() {
        database.perform {
            update {
                val item = Item.of(title = "Milk", price = 1.29)
                val updated = group1Ref.get().addItem(item)
                item.getItemGroup() shouldBe updated
            }
            group1Ref.get().items().first { it.title == "Milk" }.getItemGroup() shouldBe group1Ref.get()
        }
    }

    @Test
    fun test12() {
        database.perform {
            root.title shouldBe "Shop 1"
            update {
                root.change(title = "My Shop")
                root.title shouldBe "My Shop"
                soapRef.get().change(price = 2.99)
                soapRef.get().price shouldBe 2.99
            }
            root.title shouldBe "My Shop"
            soapRef.get().price shouldBe 2.99
        }
    }

    @Test
    fun test13() {
        database.perform {
            soapRef.get().price shouldBe 1.79
            update {
                val item = soapRef.get()
                val changed = item.change(price = 3.99)
                changed.getVersionBefore()!!.perform {
                    it.price shouldBe 1.79
                }
            }
            soapRef.get().getVersionBefore()!!.perform {
                it.price shouldBe 1.79
            }
        }
    }

    @Test
    fun test14() {
        database.perform {
            val soap_ = soapRef.get()
            soap_.price shouldBe 1.79
            update {
                val item = soapRef.get()
                item shouldBe item.change(price = 1.79) // no change
            }
            soap_ shouldBe soapRef.get()
        }
    }

    @Test
    fun test15() {
        database.update {
            val milk = Item.of(title = "Milk", price = 1.29)
            group1Ref.get().addItem(milk)
        }
        database.update {
            root.change(title = "My Shop")
            soapRef.get().change(price = 2.99)
        }
        database.update {
            soapRef.get().change(price = 3.99)
        }
        database.update {
            soapRef.get().change(price = 3.99) // no change
        }

        database.perform {
            val itemGroup = group1Ref.get()
            itemGroup.itemIds.size shouldBe 2
            itemGroup.getVersionBefore()!!.perform { itemGroupHist1 ->
                itemGroupHist1.print("1: ").snapShotVersion shouldBe 1
                itemGroupHist1.itemIds.size shouldBe 1
            }

            println("SnapShot.version: ${snapShot.version}")
            soapRef.get().print().price shouldBe 3.99
            root.print().title shouldBe "My Shop"

            soapRef.get().getVersionBefore()!!.perform { itemHist1 ->
                println("SnapShot.version: ${snapShot.version}")
                itemHist1.print().price shouldBe 2.99

                itemHist1.getVersionBefore()!!.perform { itemHist2 ->
                    println("SnapShot.version: ${snapShot.version}")
                    itemHist2.print().price shouldBe 1.79
                    itemHist2.getItemGroup().print().itemIds.size shouldBe 1
                    root.print().title shouldBe "Shop 1"
                }
            }

            soapRef.get().getVersionsBefore().performEach {
                println(it.price)
                println(it.getItemGroup().getShop().title)
            }
        }
    }

    @Test
    fun test16() {
        var soap: Item
        database.perform {
            soap = soapRef.get()
            update {
                soap.change(price = 1.50)
            }
            update {
                assertThrows<Exception> {
                    soap.change(price = 1.49)
                }
            }
        }
    }

    @Test
    fun test17() {
        var soap: Item
        database.perform {
            soap = soapRef.get()
            update {
                soap.change(price = 1.50)
            }
        }
        database.perform {
            update {
                assertThrows<Exception> {
                    soap.change(price = 1.49)
                }
            }
        }
    }

    @Test
    fun testReadWrite() {
        val database = Database("ShopTest", Shop.empty()).apply {
            addSerializationClasses {
                subclass(Shop::class)
                subclass(ItemGroup::class)
                subclass(Item::class)
            }
        }
        database.update {
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
            soapRef = soap.asRef()
            group1Ref = group1.asRef()
        }
        database.update {
            val milk = Item.of(title = "Milk", price = 1.29)
            group1Ref.get().addItem(milk)
        }
        database.update {
            root.change(title = "My Shop")
            soapRef.get().change(price = 2.99)
        }
        database.update {
            soapRef.get().change(price = 3.99)
        }

        val snapShot = database.snapShot

        database.readFromFileSystem()

        snapShot.version shouldBe database.snapShot.version
        snapShot.allEntries.map { it.key } shouldBe database.snapShot.allEntries.map { it.key }
        snapShot.snapShotHistory.map { it.key to it.value.allEntries.size } shouldBe database.snapShot.snapShotHistory.map { it.key to it.value.allEntries.size }

        database.clearHistory()

    }

    @Test
    fun performanceTest() {
        // jvm warm up
        repeat(100) {
            database.update {
                group1Ref.get().addItems(
                    (1..1_000).map { Item.of("Test $it", it.toDouble()) }.toSet()
                )
            }
            if (it % 10 == 0) init()
        }
        repeat(10_000) {
            database.perform {
                group1Ref.get().items()
            }
        }

        init() // reset DB

        // performance test
        measureTime {
            database.update {
                group1Ref.get().addItems(
                    (1..1_000).map { Item.of("Test $it", it.toDouble()) }.toSet()
                )
            }
        }.let { duration ->
            println("Insert 1.000 Items: $duration")
        }

        measureTime {
            repeat(1_000) {
                database.update {
                    group1Ref.get().addItem(Item.of("Test ABC", 1.99))
                }
            }
        }.let { duration ->
            println("Insert 1 Item: ${duration / 1_000}")
        }

        measureTime {
            repeat(1_000) {
                database.perform {
                    group1Ref.get().items()
                }
            }
        }.let { duration ->
            println("get items of group1: ${duration / 1_000}")
        }
    }

    private fun <T : Base> T.print(prefix: String? = null) = apply {
        println(prefix?.let { prefix + this } ?: this.toString())
    }
}