# Immutable, snapshot-based In-Memory Database

## Advantages:

- Database isolation level is always 'Serializable' (highest possible)
- Immutable datastructures
  - a new snapshot is created on every change
    - you also get a full history/audit for free with nearly no additional costs
- Content of database is stored as diff files to file system
    - full database content is written asynchronous to filesystem with a customizable interval
- No SQL, just work with database content and its entity relations as normal objects
- Whole implementation is very small, everyone can understand what is going on

### Technical details:

- IDs are globally unique (ID sequence generator is shared across all classes)
- Two instances/objects are equal if their reference is equals (they are the identical objects)
    - this is also a huge performance advantage (no nested deep recursion equals checks are necessary)
- For writing changes to filesystem, kotlinx serialisation is used

### Example:

    @Serializable
    @SerialName("Shop")
    class Shop(
        val title: String,
        val itemGroupIds: PersistentSet<ID>,
    ) : Base()

    @Serializable
    @SerialName("ItemGroup")
    class ItemGroup(
        val title: String,
        val itemIds: PersistentSet<ID>,
    ) : Base()

    @Serializable
    @SerialName("Item")
    class Item(
        val title: String,
        val price: Double,
    ) : Base()

    // setup/init:
    val database = Database("Shop", Shop.empty()).apply {
        addSerializationClasses {
            subclass(Shop::class)
            subclass(ItemGroup::class)
            subclass(Item::class)
        }
        writeToFile = false
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

    // add Item:
    val milk = database.update {
        val milk = Item.of(title = "Milk", price = 1.29)
        root.getItemGroup1().addItem(milk)
        milk.asRef() // return reference to milk
    }

    // ...possible other changes to milk...

    // change price of milk:
    database.update {
        milk.resolve().change(price = 3.99)
    }

    // get history of milk:
    database.perform {
        milk.resolve().getVersionBefore()?.perform { milkHist ->
            println(milkHist.price) // 1.29
        }
    }