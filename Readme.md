# Snapshot-based in-memory database with immutable data structures

## Advantages:

- No SQL and no ER mapper, no database connections, just work with database content and its entity relations as normal objects
    - Direct use of Kotlin's powerful collections/streaming API (map/reduce)
    - Parallel reading with many threads is no problem at all
- Isolation level is always 'Serializable' (highest possible)
- Database reports may run over hours with no disadvantages (no blocking of other reads/writes to database)
    - Reports are consistent because of 'serializable' isolation level (whole report has exactly one consistent snapshot as source)
- Circular dependencies are no problem (even self-references are permitted)
    - Bidirectional mapping is supplied out of the box
- Immutable datastructures
    - A new database snapshot is created on every change
    - You also get a full history/audit for free with nearly no additional costs!
        - History can be disabled to save memory
- Content of database is stored as diff files (format: json) to file system
    - Full database content is written asynchronous to filesystem with a customizable interval (tbd.)
    - All diff files will be appended to data store (no changes are made after write)
- The entire implementation is very small (about 500 LOC), everyone can understand what is going on in a short time
- Support for all (immutable) data types which can be serialized via kotlinx serialization are permitted (custom serializer can be used)
- Database migrations are supported through Kotlin's support of default values and kotlinx serializations @SerialName annotation. Further changes
  (structural data changes) can be done via a Kotlin migration script which reads all data of the old database format into memory and transform it to
  the new database format. Enhanced support will be available in future releases.
- The design follows these rules:
    - Little, but understandable code
    - No surprises during use
    - There is usually exactly one way to complete a task
        - Incorrect use should result in an exception

## Disadvantages:

- The entire database is kept in memory; currently no lazy loading of data possible
- JSON write speed to disc is currently not yet as good as expected
- The database is currently designed for a small to medium-sized database with few updates but many reads
    - write to disc is a limiting factor
    - It is planned to configure a write cache and combine several database diffs into one to improve the updates per second (both have the
      disadvantage that they collide with an 'atomic' update)

### Technical details:

- A database snapshot is a directed graph of immutable Entities having exactly one root/entry node
- Each node/entity has an ID and a version
- IDs are unique across whole database (ID sequence generator is shared across all entity classes)
- Entities have 0..n properties and 0..n (named) references to other Entities
    - Change of a property or reference creates a new instance with an incremented version of that entity (id stays the same)
- Entities are loosely coupled to each other (lookup to other entities is done via foreign id)
- Two instances/objects are equal if their reference is equals
    - this is also a huge performance advantage (no nested deep recursion equals checks are necessary; note: deep recursion may result in reading all
      database entries!)
- Database object has a synchronized update method to perform changes to the database
    - all actions done within this update should be as fast as possible
        - all data retrieval should/must be done before
    - this speciality (synchronized update method) is taken into account to prevent further complex optimistic locking code
        - pro: no optimistic locking exception can occur
        - pro: code is simple and comprehensive
        - con: update method must be fast
- For writing changes to filesystem, kotlinx-serialization is used
- For immutable collections, kotlinx-collections-immutable is used

## Still to be done:

- Code generation of methods within database model (entity classes)
- Tests with a larger database model
- Create more performance tests
- Design and create customizable (immutable) indices

## Example:

    @Serializable
    @SerialName("Shop")
    class Shop(
        val title: String,
        val itemGroupIds: PersistentSet<ID>,
    ) : Base() {
      // generated methods are not shown here
    }

    @Serializable
    @SerialName("ItemGroup")
    class ItemGroup(
        val title: String,
        val itemIds: PersistentSet<ID>,
    ) : Base() {
      // generated methods are not shown here
    }

    @Serializable
    @SerialName("Item")
    class Item(
        val title: String,
        val price: Double,
    ) : Base() {
      // generated methods are not shown here
    }

    // setup/init database:
    val database = Database("ShopDB", Shop.empty()).apply {
        addSerializationClasses {
            subclass(Shop::class) // entity classes must be registered to kotlinx serialization
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
        root.itemGroups().first { it.title == "Group1" }.addItem(milk)
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
            // other database contents from this historized snapshot can also be easily retrieved here:
            milkHist.itemGroup().items().forEach {
                println("${it.name}: ${it.price}")
            }
        }
    }