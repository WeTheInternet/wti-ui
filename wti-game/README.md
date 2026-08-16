# wti-game

`wti-game` is a Java 8, libGDX-free command/session boundary for offline-first and hosted
games. It supplies a durable game-data model role, typed XApi message envelopes, and
deterministic queued in-process delivery. Games own their concrete data schemas, commands,
results/events, authorization policy, and authoritative mutation handlers.

## Published modules

| Coordinate | Purpose |
| --- | --- |
| `net.wti:wti-game-api:0.51` | Durable `GameDataModel` schema role plus command/result envelopes and session contract. |
| `net.wti:wti-game-spi:0.51` | Typed authority context, authorization, handler, and rejection factory hooks. |
| `net.wti:wti-game-impl:0.51` | Deterministic `QueuedLocalGameSession`. |
| `net.wti:wti-game:0.51` | Convenience artifact exporting the complete chain. |

An XApi consumer can declare the complete local-session implementation with:

```xapi
@transitive
external : "net.wti:wti-game:0.51"
```

Depend on `wti-game-api` instead when only the transport-neutral model/session contracts
belong on that module's public surface.

## Model roles and authority

`GameDataModel extends Model` marks an interface as a schema for durable game data. The
marker is deliberately method-free and unannotated: concrete interfaces carry `@IsModel`
and any `@Persistent` configuration. Durable state may comprise several keyed aggregates
and nested value models; it does not require one giant root.

The schema role does not confer authority on every instance:

| Role | Meaning |
| --- | --- |
| Model-interface schema | Defines the typed shape shared by instances. |
| Authoritative instance | A server-owned keyed instance accepted as canonical state. |
| Client replica | Uses the same schema but does not gain mutation authority from it. |
| Command | A client-to-authority request, modeled by `GameCommand`, outside the durable-state hierarchy. |
| Result/event or delta | An authority-to-client receipt or update, modeled by `GameCommandResult`, also outside that hierarchy. |

Commands and results may be retained separately for audit or replay without becoming
members of the canonical state graph. XApi `Model` supplies creation, manifests, and
serialization, while `@Persistent` configures storage; neither by itself identifies an
authoritative instance.

## Message envelopes

`GameCommand extends Model` carries:

- stable command and session IDs;
- the acting actor ID;
- monotonic client sequence;
- expected aggregate revision.

`GameCommandResult extends Model` carries:

- the originating command ID;
- accepted/rejected state and a stable reason code;
- first authoritative server order;
- resulting aggregate revision.

Concrete `@IsModel` interfaces extend these sibling message envelopes and add typed payload properties.
They do not need a polymorphic `ModelList`, serialized blob, or reflection-based payload.
The focused JRE test proves inherited envelope fields and subtype fields survive manifest
construction and serialization.

## Queued local delivery

`QueuedLocalGameSession` has no threads, executor, socket, filesystem, libGDX, or
simulation-clock dependency. It is a caller-owned deterministic queue, not a thread-safe
concurrent collection; submit and pump it from the session owner's chosen execution
context.

- `submit(command)` only enqueues. It never calls authorization, mutation, or listeners.
- `pump()` snapshots the current queue size and processes exactly that many commands FIFO.
- Commands submitted by a handler or result listener wait for the next pump.
- Reentrant pumping is rejected.
- Result listeners run in registration order for each command result.
- Authorization is an explicit hook receiving the actual session, requested actor,
  server order, and concrete command. Session connection does not imply actor ownership.
- A retained duplicate command ID republishes the original result object without running
  authorization or mutation again.

First-seen accepted and rejected results are cached in insertion order up to the positive
retention limit supplied to the constructor. Eviction is FIFO. Once an ID is evicted it is
treated as new, so size the limit for the consumer's retry horizon. Published results must
be treated as immutable.

The generic session does not interpret client sequence or expected revision. The owning
game's authorization/handler composition validates those aggregate-specific policies and
creates its typed accepted result. The result factory creates typed boundary rejections
such as `invalid-command-id`, `session-mismatch`, and `unauthorized`.

## Verification and publication

Run the implementation and XApi model proof:

```bash
./gradlew :wti-game-impl:test
```

Publish the complete chain to the configured XApi local repository:

```bash
./gradlew \
  :wti-game-api:xapiPublish \
  :wti-game-spi:xapiPublish \
  :wti-game-impl:xapiPublish \
  :wti-game-main:xapiPublish
```

The authoritative topology is `wti-game.xapi`. The generated Gradle files under
`wti-game/src/*` are diagnostic/publication output and must not be hand-edited.
