# wti-ui / LifeQuest UI

This repository contains the LifeQuest UI and supporting model/service modules. The inspected codebase combines:

- XAPI model interfaces and loaders for users, groups, quests, inventory, and time.
- LifeQuest planning services that materialize `LiveQuest` instances from definitions and recurrence rules.
- libGDX Scene2D UI components and samples for day-planning views.
- Spock/Groovy tests and libGDX test utilities.

## Start here

For humans and LLM agents:

1. Read `REQUIREMENTS.md` and `STYLE-GUIDE.md` before changing code.
2. Read `ROADMAP.md` for LifeQuest domain semantics, especially DayIndex, key splaying, materialization, and history.
3. Read `docs/ARCHITECTURE.md` for module/source-set layout and LifeQuest layering.
4. Read `docs/TESTING.md` before choosing verification scope.
5. Read `docs/DEBUGGING.md` when builds, dependency resolution, libGDX threading, or model semantics fail.
6. LLM agents should also read `AGENTS.md`.

## Repository map

The root topology is declared in `schema.xapi`. It defines virtual projects:

| Area | Purpose |
| --- | --- |
| `model/` | XAPI model interfaces, stores, loaders, and model tests for LifeQuest data. |
| `wti-ui/` | Core UI contracts, time/quest API bridges, and service implementations. |
| `components/` | libGDX UI components, quest views, and sample quest app code. |
| `demo/` | Demo app and legacy task-index API/tests used by current samples. |
| `gdx-themes/` | libGDX theme modules and Raeleus theme variants. |
| `test-tools/` | Shared test support for headless and desktop libGDX tests. |

Key docs/config:

- `schema.xapi` — root XAPI project declaration.
- `model/model.xapi`, `wti-ui/wti-ui.xapi`, `components/components.xapi`, `demo/demo.xapi`, `gdx-themes/gdx-themes.xapi`, `test-tools/test-tools.xapi` — per-project module/platform declarations.
- `settings.gradle` — applies the `xapi-settings` plugin and configures the XAPI plugin repository.
- `gradle/wti-ui-versions.toml` — version catalog for XAPI, libGDX, Spock, and related dependencies.
- Generated `*.gradle` files under project/source-set directories — derived Gradle topology. Useful for diagnostics, not the primary source of truth.

## XAPI schemas, generated Gradle, and source sets

This repo uses `.xapi` schema files to describe logical modules and platform variants. The XAPI settings plugin generates Gradle projects/source sets from those schemas. For example:

- `wti-ui/wti-ui.xapi` declares modules such as `api`, `time`, `quest`, `view`, `implQuest`, and `implTime`.
- `components/components.xapi` declares UI/quest/inventory components and `sampleQuest`.
- `test-tools/test-tools.xapi` declares `headless` and `desktop` libGDX test-support variants.

Generated Gradle files show the concrete outcome. For example, the inspected generated file `wti-ui/src/implQuest/wti-uiImplQuest.gradle` maps `:wti-ui-implQuest` to:

- `wti-ui/src/implQuest/java`
- `wti-ui/src/implQuestTest/java`
- `wti-ui/src/implQuestTest/groovy`
- dependencies including `:model-implQuest`, `:wti-ui-quest`, `:wti-ui-implTime`, Spock, and XAPI model JRE.

Treat `.xapi` as the authoritative topology. Treat generated Gradle files as diagnostic output when you need the exact Gradle project path, source directories, or dependency expansion.

## `build/xindex`

`build/xindex` is a generated machine-oriented index. Prefer targeted lookup when the module is known; do not broad-search the whole tree first.

- Path-side lookup pattern: `build/xindex/path/_<project>/<module-or-platform-module>/...`
- Concrete inspected example: `build/xindex/path/_components/inventory/sources`
- That `sources` file contained `/opt/wti-ui/components/src/inventory`.
- Coordinate-side lookup pattern, when present: `build/xindex/coord/<group>/<project-module>/...`; `build/xindex/coord/net.wti/components-inventory` was discoverable here, but no child files were visible through workspace tools.

Use `build/xindex` for fast generated lookup, `.xapi` files for authoritative topology, and generated Gradle files for concrete Gradle project/source-set/dependency output. See `docs/ARCHITECTURE.md` and `AGENTS.md` for the fuller workflow.

## Development constraints

From `REQUIREMENTS.md` and `STYLE-GUIDE.md`:

- Java code must remain Java 8 compatible.
- Tests should be Spock + Groovy unless explicitly requested otherwise.
- Views must remain pure with respect to persistence and data loading: no direct store/network calls inside view classes.
- Scene2D mutation, Skin/atlas/font loading, and layout must happen on the libGDX render thread for real backends.
- Prefer headless tests for logic; use desktop LWJGL3 tests only when a real GL context is required.
- Keep changes minimal and localized; avoid drive-by refactors.

## Local setup notes

Observed configuration:

- Gradle wrapper version is configured as `8.11.1` in `build.gradle`.
- Java toolchain is generated as Java 8 in inspected generated Gradle files.
- `settings.gradle` applies `xapi-settings` version `0.5.1` and looks for an XAPI local Maven repository.
- If `xapiRepo` is not provided, `settings.gradle` defaults to `$rootDir.parent/xapi/repo` and logs that fallback.
- `gradle.properties` sets `xapiVersion=0.5.1`, `gdxVersion=2.13.2-SNAPSHOT`, Quarkus plugin `3.6.8`, `org.gradle.jvmargs=-Xms256m -Xmx3g`, and `forceRegen=true`.
- The version catalog defines Spock `2.3-groovy-4.0` and libGDX dependencies.

## Suggested verification commands

These are suggestions only; they were not run while creating these docs.

```bash
./gradlew :wti-ui-implTime:test
./gradlew :wti-ui-implQuest:test
./gradlew :components-implQuest:test
./gradlew :demo-api:test
./gradlew :test-tools-headless:test
./gradlew :test-tools-desktop:test
```

Choose the smallest command that covers your change. See `docs/TESTING.md` for guidance.
