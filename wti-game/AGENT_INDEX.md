# wti-game Agent Index

See `../AGENTS.md` for repository-wide guidance and `README.md` in this directory for the
public contract, model roles, queue semantics, coordinates, and verification commands.

## Boundary

This module owns a reusable, libGDX-free durable game-data marker, command/result
envelopes, authority-side SPI hooks, and deterministic queued local delivery. It must
remain independent of Kukunochi, LifeQuest, RPG inventory/equipment policy, Scene2D,
simulation clocks, sockets, and persistence backends.

## Authoritative topology

`wti-game.xapi` declares four modules:

- `api` — `GameDataModel`, `GameCommand`, `GameCommandResult`, `GameSession`, and stable
  boundary rejection codes;
- `spi` — typed command context, authorization, mutation handler, and rejection factory;
- `impl` — `QueuedLocalGameSession`;
- `main` — transitive convenience artifact.

Generated project paths are `:wti-game-api`, `:wti-game-spi`, `:wti-game-impl`, and
`:wti-game-main`. Generated Gradle files are diagnostic output, not edit targets.

## Key invariants

- `GameDataModel` marks a durable-data schema, not instance authority. Concrete data
  interfaces own `@IsModel` and persistence configuration.
- A server-owned keyed instance may be authoritative; a client instance using the same
  schema is a replica and gains no authority from the marker.
- Canonical data may be multiple keyed aggregates and nested values; do not require a
  single root aggregate.
- `GameCommand` requests authority action and `GameCommandResult` reports its outcome;
  both remain direct `Model` siblings outside the `GameDataModel` hierarchy. Separate
  audit/replay retention does not make them canonical state.
- Concrete games extend the common XApi message envelopes with typed properties; do not
  introduce a heterogeneous base `ModelList` or serialized payload blob.
- `submit` never invokes authorization, handlers, or listeners.
- One pump processes the queue size captured at pump entry; callback submissions wait.
- Retained duplicate IDs republish the original result without re-running mutation.
- Result retention is positive, bounded, insertion ordered, and FIFO-evicted.
- Authorization is explicit and receives session plus requested actor context.
- Client sequence and expected revision are envelope data interpreted by the game handler.
- Keep all production code Java 8 compatible and free of thread/clock/libGDX assumptions.

## Focused tests

`src/implTest/groovy/net/wti/game/impl/QueuedLocalGameSessionSpec.groovy` covers queue
boundaries, FIFO notification, deduplication, authorization, eviction, and XApi JRE
manifest/serialization behavior using concrete message and durable-data models in the
neighboring Java test source directory. It also proves command and result roles are not
assignable to `GameDataModel`.

Run:

```bash
./gradlew :wti-game-impl:test
```
