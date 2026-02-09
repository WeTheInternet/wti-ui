# REQUIREMENTS

This document captures procedural and implementation constraints for the LifeQuest UI work.
It exists separately from ROADMAP.md so the roadmap can focus on product/domain goals and sequencing.

Created: 2026-02-09

## Scope

Applies to:
- Java helper/library code in this repo
- libGDX (LWJGL3) UI components and views
- Spock (Groovy) test code

## Language and compatibility

- Java code must be **Java 8 compatible**
    - no `var`, records, text blocks, sealed types
    - avoid APIs requiring >8
- Tests are **Spock + Groovy**
    - prefer readable fixtures
    - JUnit tests only if explicitly requested

## Time and date APIs

- Prefer `X_Time` / `TimeComponents` / `TimeZoneInfo` for time breakdown and display preparation.
- Avoid `java.time` in UI component rendering and formatting:
    - no `Instant`, `LocalTime`, `ZoneId`, `DateTimeFormatter` inside view/widget rendering code
    - exceptions should be explicitly justified and documented

## View purity and layering

- Views are **pure** with respect to persistence and data loading:
    - no persistence calls inside view classes
    - no direct model store/network calls inside view classes
- Views should not own async subscriptions:
    - subscription/binding belongs in a binder/controller/presenter layer
    - views accept snapshots via setters (e.g., `setLiveQuests(...)`) and rebuild via `refresh()`

## libGDX / Scene2D threading

- Any Scene2D mutations, Skin/atlas/font loading, and layout work must happen on the **libGDX render thread** when running in a real backend.
- Use `Gdx.app.postRunnable(...)` (or the provided harness helpers) to bridge threads.

## Testing approach

- Prefer headless tests for logic and stability where possible.
- Use real GL-backed LWJGL3 tests only when needed (Skin/assets, GL context requirements):
    - use `net.wti.ui.test.desktop.GdxDesktopTestHarness`
- No pixel/snapshot tests yet:
    - focus on correctness, stability, and iteration speed

## UI composition patterns

- Components should encapsulate:
    - rendering
    - user interaction
    - UI-only state (e.g., collapsed/expanded)
- Expose **intent-level events** (domain/user intent) rather than low-level UI event details:
    - consumers care about “start quest”, “reschedule requested”, “restore skipped”, etc.
    - low-level drag/focus/mouse listeners should be internal implementation details unless explicitly part of a public API

## Naming and legacy constraints

- `OldTodayView` and legacy task-based widgets remain for reference/migration only.
- New views should not reuse legacy day/schedule/task widgets unless explicitly approved.

## Formatting and style

- Use `///` doc comments for:
    - class/interface headers
    - public/protected methods
    - non-trivial private methods where behavior is subtle
- Avoid drive-by refactors:
    - keep changes minimal and localized unless requested