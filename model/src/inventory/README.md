# Generic Inventory Model

This source set owns the smallest XApi model contract shared by inventory consumers. It
contains no libGDX types and no assumptions about a game world.

## Contracts

- `BasicStack extends Model` adds only integer `count`. The inherited `ModelKey` is stable
  stack identity.
- `BasicInventory<T extends BasicStack> extends Model` stores the typed `ModelList<T>` and
  an optional maximum number of occupied stack entries.
- Capacity less than one means unlimited. `setCapacity` canonicalizes negative inputs to
  zero; `isUnlimited()` and `isBounded()` keep consumers from repeating sentinel checks.
- Concrete stack models add item-definition identity, slot/layout metadata, durability,
  or other consumer-specific state.
- Capacity derivation and enforcement, bags, equipment, hotbars, ticking, sessions,
  recipes, and combat do not belong in these base contracts.

This lets a non-game consumer such as LifeQuest use an unlimited inventory while an RPG
layer can declare a bounded stack-slot count and compose its own derivation, enforcement,
and equipment policies around the same basic shape.

## Required concrete-model pattern

XApi's JRE manifest builder visits inherited abstract generic methods before concrete
redeclared methods and erases unresolved `T` to `BasicStack`. `BasicInventory` therefore
implements its generic accessors as default property bridges, which manifest inspection
skips.

Every concrete `@IsModel` inventory must redeclare both accessors with the exact type:

```java
@IsModel(modelType = "exampleInventory")
interface ExampleInventory extends BasicInventory<ExampleStack> {
    @Override ModelList<ExampleStack> getStacks();
    @Override void setStacks(ModelList<ExampleStack> stacks);

    default ModelList<ExampleStack> stacks() {
        return getOrCreateModelList(
                ExampleStack.class,
                this::getStacks,
                this::setStacks
        );
    }
}
```

Do not make `setStacks` fluent in the concrete model. A covariant return makes javac emit
a synthetic default bridge method, and the XApi JRE proxy can dispatch that bridge instead
of the manifest property setter.

The generic base cannot supply `stacks()` because it has no reliable `Class<T>` literal.
The concrete helper is intentional, small boilerplate.

## Verification

`model/src/implInventoryTest` supplies concrete test-only stack and inventory models. Its
Spock specification asserts the concrete manifest component type, verifies unlimited and
positive bounded capacity behavior, and round-trips capacity, the typed list, model keys,
count, and a subtype-only item ID through XApi serialization.

Run:

```bash
./gradlew :model-implInventory:test
```

The current proof covers the JRE model service. Add target-specific coverage before
assuming the same behavior on another generated/mobile model service.

## Publication

The generated Maven coordinate is `net.wti:model-inventory:<schema-version>`. The
inventory artifact has transitive `net.wti:model-api` and `net.wti:model-spi` dependencies
at the same version, so publish the three together:

```bash
./gradlew \
  :model-api:xapiPublish \
  :model-spi:xapiPublish \
  :model-inventory:xapiPublish
```

The XApi settings plugin generates the active root script as `wti-ui.gradle` and propagates
the version declared in `schema.xapi` to generated projects. If a publication reports
`unspecified`, refresh the locally published settings plugin and regenerate; do not add a
second version source to `src/main/gradle/body` or hand-edit generated Gradle files.
