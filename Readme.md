# Immutable, snapshot-based In-Memory Database

## Advantages:

- Database isolation level is always 'Serializable' (highest possible)
- Immutable datastructures
  - a new database snapshot is created on every change
    - you also get a full history/audit for free with nearly no additional costs
- Content of database is stored as diff files (format: json) to file system
    - full database content is written asynchronous to filesystem with a customizable interval
- No SQL, just work with database content and its entity relations as normal objects
  - use directly Kotlin collections/streaming API (map/reduce)
- Circular dependencies are no problem
  - bidirectional mapping is supplied out of the box
- Whole implementation is very small, everyone can understand in short amount of time what is going on

### Technical details:

- A database SnapShot is a graph of immutable Entities having exactly one root/entry node
- Each Entity has an ID and a version
- IDs are unique across whole database (ID sequence generator is shared across all classes)
- Entities have 0..n properties and 0..n references to other Entities
  - Change of a property or reference creates a new instance with an incremented version of that entity (id stays the same)
- Entities are loosely coupled to each other (lookup to other entities is done via foreign id)
- Two instances/objects are equal if their reference is equals
    - this is also a huge performance advantage (no nested deep recursion equals checks are necessary)
- Database object has a synchronized update method to perform changes to the database
  - all actions done within this update should be a fast implementation
  - this speciality (synchronized update method) is taken into account to prevent further complex optimistic locking code
    - pro: no optimistic locking exception can occur
    - pro: code is simple and comprehensive
    - con: update method must be fast
- For writing changes to filesystem, kotlinx serialization is used

## Example:

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

    // setup/init database:
    val database = Database("Shop", Shop.empty()).apply {
        addSerializationClasses {
            subclass(Shop::class) // Enity classes must be registered to kotlinx serialization
            subclass(ItemGroup::class)
            subclass(Item::class)
        }
    }

    // make first change
    database.update {
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

    // add Item:
    val milkRef = database.update {
        val milk = Item.of(title = "Milk", price = 1.29)
        root.getItemGroups().first { it.title == "Group1" }.addItem(milk)
        milk.asRef() // return reference to milk
    }

    // ...possible other changes to milk...

    // change price of milk:
    database.update {
        milkRef.get().change(price = 3.99)
    }

    // get price of milk before last change:
    database.perform {
        milkRef.get().getVersionBefore()?.perform { milkHist ->
            println(milkHist.price) // 1.29
        }
    }

    // get all milk history changes:
    database.perform {
        milkRef.get().getVersionsBefore().performEach { milkHist ->
            println(milkHist.price)
        }
    }