# Immutable, snapshot-based In-Memory Database

## Advantages:

- Database isolation level is always 'Serializable' (highest possible)
- Immutable datastructures
    - get a new snapshot on every change
    - you also get a full history/audit for free with nearly no additional costs
- Content of database is stored as diff files to file system
    - full database content is written asynchronous to filesystem with a customizable interval
- No SQL, just work with database content and its entity relations as normal objects
- Whole implementation is very small, everyone can understand what is going on

### Technical details:

- IDs are globally unique (ID sequence generator is shared across all classes)
- Two instances/objects are equal if their reference is equals (tey are the identical objects)
    - this is also a huge performance advantage (no nested deep recursion equals checks are necessary)
- For writing changes to filesystem, kotlinx serialisation is used