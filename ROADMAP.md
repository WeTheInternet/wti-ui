# ROADMAP

Author: James X. Nelson and AI Assistant (both chatGPT and claude engines)
Created: 2025-10-10
Last updated: 2025-12-08

This roadmap defines the LifeQuest data model, daily windowing model, key splaying scheme, materialization/rollover behavior, indexing/querying approach, and an implementation plan with checklists. It is intended to be a living document; update, annotate, and check items as progress is made.

Contents
- Goals and principles
- Time model (DayIndex, ModelDay)
- Keys and splaying
- Domain model
- Materialization and lifecycle
- Views and filtering
- Indexing and querying
- Caching, local-first, and IndexWriter
- Invariants
- Migration plan (phased with checkboxes)
- Test plan
- Open questions and decisions
- Glossary

---

## Goals and principles

- Separate definition from live execution:
    - QuestDefinition: what it is and how it repeats (relative, anchor-based).
    - LiveQuest: concrete, per-day instance with absolute deadline.
- One live instance per (Definition × Rule × DayIndex).
- Daily windows with user-configurable rolloverHour (default 4am).
- Absolute timestamps only on live instances; relative only on definitions/rules.
- Keep active set small; write Completed/Failed (and Canceled/Skipped) for history.
- Composite quests at the definition level; project to instances per day as needed.
- Powerful filtering via schedule templates and tags (inheritable).
- Predictable on-disk splaying, cheap daily queries, inert placeholders for past/future.
- Client-first operation; servers/backends and advanced query engines can be added later.

---

## Time model

- App Epoch
    - Fixed "app epoch" calendar date: 2025-10-10 (inclusive).
    - DayIndex (aka DayNum) = number of days since app epoch, computed per-user with that user's rolloverHour.
    - DayIndex is a signed long (to support far past/future).

- ModelDay
    - A logical model keyed by the DayIndex (may be persisted or synthesized).
    - Stores derived components for a given DayIndex in a user's zone and rollover:
        - dayName (localized), dayOfWeek (0–6), dayOfMonth (1–31), dayOfYear (1–366),
        - startTimestamp, endTimestamp (DST-safe; may be >24h span),
        - timezoneId, rolloverHour used to compute this window.
    - Created lazily (create-if-missing semantics) and can be computed-only (no persistence required).
    - Used as the parent of LiveQuest for a day for natural on-disk splaying.

- Zones and rollover
    - Zone is system-configurable via a JVM/system property; user can override their zone.
    - DayIndex and ModelDay computations use the user's zone and rolloverHour only.
    - No other entities have independent zones unless justified later.

---

## Keys and splaying

- Minimal key type names (to reduce serialized payload size).
- Parent-child splaying:
    - All live and historical records are splayed under the ModelDay parent by DayIndex.

Key formats
- Live quests (active instances)
    - dy/{DayNum}/lv/{LiveKey}

- Completed ("done")
    - dy/{DayNum}/dn/{LiveKey}

- Failed
    - dy/{DayNum}/fld/{LiveKey}

- Canceled
    - dy/{DayNum}/cncl/{LiveKey}

- Skipped
    - dy/{DayNum}/skp/{LiveKey}

- Synthetic (local-only placeholders)
    - dy/{DayNum}/syn/{LiveKey}

- Future (optional, likely unnecessary; synthetic preferred)
    - dy/{DayNum}/ftr/{LiveKey}
      LiveKey
      A LiveKey uniquely identifies a quest instance within a given day by combining:

1. The DefinitionKey (ID of the original QuestDefinition)
2. An optional RuleKey that specifies which RecurrenceRule created this instance (defaults to "default")

Format: {DefinitionKey}[/{RuleKey}]

Examples:

- "dailyStandup" (uses default rule)
- "weeklyReport/monday" (specific rule)

Keys should be kept short and compact since they appear in paths.
The parent ModelDay key (dy/{DayNum}) provides the temporal context.

---

## Domain model

High-level types (names are conceptual; actual interface/class names may vary):

- QuestDefinition
    - key, name, description, priority
    - tags: Set<TagKey>
    - scheduleTemplateKey: which schedule policy it uses ("workday", "day off", "holiday", or custom)
    - rules: List<RecurrenceRule> (relative)
    - composition: List<ChildRef> (structural sub-quests)
    - defaults: defaultAlarmMinutes, defaultGracePeriod (optional), visibility policy
    - active: boolean

- RecurrenceRule
    - key, parentDefinitionKey (lineage)
    - cadence: ModelDuration { amount: int, unit: DurationUnit (DAY, WEEK, MONTH, YEAR) }
    - anchor: TimeAnchor (position within window)
        - DAILY: hour:minute
        - WEEKLY: dayOfWeek + hour:minute
        - MONTHLY: dayOfMonth + hour:minute
        - YEARLY: dayOfYear + hour:minute
    - activeRange: optional (start/end day or timestamp)
    - autoMaterialize: boolean (default true)
    - parentDefinitionKey is included so a LiveQuest can point to the specific rule that generated it.

- LiveQuest (active instance)
    - key: dy/{DayNum}/lv/{LiveKey}
    - parentKey: dy/{DayNum}
    - sourceDefinitionKey, sourceRuleKey (nullable for manual)
    - deadlineMillis (absolute; 0 => no deadline)
    - status: active | paused | finished | cancelled | failed | archived
    - alarmMinutes (override), snoozeUntil (absolute, optional)
    - createdAt, updatedAt, startedAt, finishedAt
    - effectivePriority
    - tags: copied from definition on creation; updates propagate to live instances
    - skip: boolean (true when day is off via schedule template or ad-hoc)
    - gracePeriod (optional; falls back to system/user default)

- QuestCompleted
    - key: dy/{DayNum}/dn/{LiveKey}
    - instanceKey, sourceDefinitionKey, sourceRuleKey, dayIndex
    - occurredAt, deadlineAt, durationSpent (optional)
    - notes (e.g., approver/boss message or user note)
    - snapshot: name, description, tags, priority (to render history without chasing changing definitions)
    - completionRequirementsSnapshot (optional, if used by reporting)

- QuestFailed
    - key: dy/{DayNum}/fld/{LiveKey}
    - Same shape as Completed + failureReason and notes.

- QuestCanceled
    - key: dy/{DayNum}/cncl/{LiveKey}
    - Same shape; represents explicit cancellation action.

- QuestSkipped
    - key: dy/{DayNum}/skp/{LiveKey}
    - Represents "intentionally no-op today" (e.g., PTO, holiday).

- ChildRef (definition-level composition)
    - childDefinitionKey
    - role: parallel | sequential | optional | blocking
    - startPolicy: after-parent-start | after-N-siblings-complete | manual
    - completionPolicy (on parent definition): all-of | any-of | weighted | milestone
    - quantity (optional; quotas in future)
    - tags inheritance: child inherits parent tags; can add more.

- ScheduleTemplate
    - key, name (e.g., "Workdays", "Days Off", "Holidays", user custom)
    - rules: weekdays on/off, optional holiday sets
    - skip behavior for off days: present with skip=true (preferred), not absent
    - On template updates: mark-sweep LiveQuest to set skip; do not chase next valid day.

---

## Materialization and lifecycle

- One live instance per (Definition × Rule × DayIndex).
- Today's materialization
    - For each active QuestDefinition + RecurrenceRule:
        - If autoMaterialize is true, ensure dy/{DayNum}/lv exists (create-if-missing).
        - deadlineMillis computed from rule anchor within the current ModelDay window.
        - Apply schedule template: set skip=true when day is off (weekends/holidays/PTO).
    - User "Start" for a definition/rule creates/activates the LiveQuest if missing.

- Completion
    - On success:
        - Write QuestCompleted (snapshot fields).
        - Remove LiveQuest.
        - For rules: if immediate re-materialization is required (e.g., multi-times/day), compute and materialize again (later feature).
    - On explicit cancel:
        - Write QuestCanceled; remove LiveQuest.
    - On explicit skip:
        - Write QuestSkipped; remove or keep LiveQuest with skip=true (policy: write record and remove to keep active set minimal).

- Rollover job (post-rollover tick)
    - For each LiveQuest:
        - If deadlineMillis > 0 and now > (deadlineMillis + gracePeriod) and skip==false:
            - Write QuestFailed record; remove LiveQuest.
        - If skip==true: do not fail.
    - Materialize for the new DayIndex (autoMaterialize rules).

- Placeholders
    - Past/future views can render synthetic local-only "syn" placeholders to preview expected items.
    - Placeholders are inert and cannot be completed; they are not persisted.

---

## Views and filtering

- Single-day view (first target)
    - Query: dy/{DayNum}/lv/* (active), optionally include skipped=on/off.
    - Sort: deadlineMillis (non-zero first, soonest first) then priority.
    - Show tags and schedule template; user toggles to filter quickly.

- Weekly/Monthly/Yearly views (later)
    - Range queries over DayIndex.
    - Combine lv/dn/fld/cncl/skp types as needed (one request per type).
    - Fill gaps with synthetic placeholders if desired.

- Filters
    - Schedule template (each quest belongs to exactly one template).
    - Tags: typeahead, multi-select. Definition tags copied to instances; updates push to live.
    - Context-aware defaults (work hours vs off hours/weekends), overridable at runtime.

---

## Indexing and querying

Query shapes
- Day range: up to 31 DayNum (start..end inclusive).
- Type selection: one of {lv, dn, fld, cncl, skp}. Views that show all types will make N requests (one per type).
- RuleKey list: filter by RuleKey set (empty => all rules).
- Client-side filter: tags, templates, status, etc., are performed after retrieval in early phases.

First phase (client-side index)
- Maintain a client-side index that is kept up-to-date by the app:
    - For each requested DayIndex and type, load dy/{DayNum}/{type}/* once and cache.
    - Local queries filter/sort directly on cached data.
    - Subscriptions/eventing can be added later.

Future roadmap for the query engine
- Teach X_Model.query to handle splayed queries:
    - Accept parent key prefixes (dy/{DayNum}/{type}/) and optional suffix filters.
    - Use server-side cache to serve warm results and minimize disk I/O.
    - Stream to client cache (read-through) to avoid redundant network hits.
- Consider integrating a columnar/time-series engine (e.g., Deephaven) behind ModelService for scalable aggregation and live queries.

---

## Caching, local-first, and IndexWriter

- Local-first
    - The client app maintains a local cache and serves most queries out of it.
    - A thin sync loop keeps local cache fresh; server can be added later.

- IndexWriter (optional, later)
    - Maintain precomputed indexes for common queries (e.g., tags/schedules) for faster filtering:
        - On-demand index: if no hits found, compute, return results, then persist the index.
        - Only usable once fully written (atomic swap).
    - Index granularity: per user, per DayRange chunk, per Type.

---

## Invariants

- Uniqueness:
    - Exactly one LiveQuest per (Definition × Rule × DayIndex) under dy/{DayNum}/lv/*.
- Absolute times:
    - LiveQuest.deadlineMillis is always absolute (0 = no deadline).
- Rollover:
    - Computed per user (zone + rolloverHour).
    - Overdue → failed after (deadline + gracePeriod), except skip==true.
- History immutability:
    - dn/fld/cncl/skp are append-only records.
- Tag propagation:
    - Definition tag changes update live instances (push); history retains snapshots unchanged.
- Skip policy:
    - Off days produce either a LiveQuest with skip=true or a Skipped record; skipped items do not fail.

---

## Migration plan

Phase 0 — Foundations ✅ COMPLETE
- [x] Lock app epoch (2025-10-10) and document DayIndex formula.
- [x] Implement DayIndex math with rolloverHour and user zone.
- [x] Implement ModelDay derivation utilities (start/end, components, DST-safe).
- [x] Unit tests for time math and DST boundaries.
    - [x] DayIndex value type with epoch constant
    - [x] DayIndexService with timezone and rollover support
    - [x] DayService with caching for ModelDay instances
    - [x] Comprehensive test coverage (DayIndexServiceSpec, DayServiceSpec)
    - [x] DST handling validated
    - [x] Timezone offset handling validated
    - [x] Rollover boundary edge cases validated

Phase 1 — Models and keys ✅ COMPLETE
- [x] Define ModelDuration and TimeAnchor types.
    - Implemented as `ModelDuration` + `DurationUnit` and `TimeAnchor` + `TimeAnchorKind` under `net.wti.time.api`.
    - Utility: `ModelDurationUtil` for applying durations to `DayIndex`.
- [x] Define QuestDefinition, RecurrenceRule.
    - Implemented as `QuestDefinition` and `RecurrenceRule` under `net.wti.quest.api`.
    - `RecurrenceRule` uses `ModelDuration` (cadence) and `TimeAnchor` (anchor within a `ModelDay` window).
- [x] Define LiveQuest with key dy/{DayNum}/lv/{LiveKey}.
    - Implemented as `LiveQuest` under `net.wti.quest.api`.
    - Keys:
        - Parent: `ModelDay.newKey(dayNum)` (model type `"day"`, id = `DayIndex.dayNum`).
        - Child: `"lv"` model type with id = LiveKey string `{definitionId}[/{ruleId}]`.
- [x] Define QuestCompleted, QuestFailed, QuestCanceled, QuestSkipped with respective dy/{DayNum}/{type}/{LiveKey}.
    - Implemented as `QuestCompleted` (`"dn"`), `QuestFailed` (`"fld"`), `QuestCanceled` (`"cncl"`), `QuestSkipped` (`"skp"`).
    - All extend `QuestHistoryRecord` and carry a `QuestSnapshot` for immutable rendering.
    - All keys are parented under `ModelDay` to preserve the `dy/{DayNum}/{type}/{LiveKey}` splaying scheme.
- [x] Define ScheduleTemplate and ChildRef (MVP fields).
    - Implemented as `ScheduleTemplate` (with `OffDaySkipBehavior`) and `ChildRef` (with `ChildRole`, `ChildStartPolicy`, `ParentCompletionPolicy`).

Phase 2 — Materialization & rollover (MVP) ✅ COMPLETE (MVP)
- [x] DayService: compute DayIndex, getOrCreateModelDay (compute-only ok).
    - Implemented via `DayIndexService` and `ModelDayService`.
    - `ModelDay` encapsulates start/end timestamps, duration, day-of-week/month/year, localized day name, and per-day zone/rollover configuration.
- [x] Planner.ensureToday(user):
    - Implemented as:
        - `PlannerService.ensureLiveQuestForDay(ModelDay, QuestDefinition, RecurrenceRule, skipFlag)`.
        - `TodayPlannerService.ensureToday(userKey)` / `ensureDay(userKey, ModelDay)`.
    - Uses:
        - `QuestDefinitionSource` to enumerate relevant `QuestDefinition` instances for a user.
        - `ScheduleTemplateService.shouldSkip(day, definition, rule)` to compute `skip` flags.
        - `LiveQuestStore` abstraction to find and create `LiveQuest` instances.
    - Behavior:
        - For each active `(QuestDefinition × RecurrenceRule)` with `autoMaterialize == true`:
            - Build LiveKey and ensure a `LiveQuest` exists under `ModelDay`.
            - Compute `deadlineMillis` from `RecurrenceRule.anchor` + `ModelDay` via `TimeAnchorUtil`.
            - Apply `skip` flag from schedule templates.
        - Idempotent: repeated calls do not create duplicates.
- [x] RolloverJob:
    - Implemented as `RolloverService` with:
        - `runRollover(userKey, fromDay, nowMillis)` and `runRolloverForYesterday(userKey, nowMillis)`.
    - Uses `RolloverStore` abstraction:
        - `findActiveLiveQuests(ModelDay)` to enumerate active `LiveQuest` instances.
        - `createFailureRecord(LiveQuest, RolloverContext, reason)` to append `QuestFailed` history.
        - `deleteLiveQuest(LiveQuest)` to shrink the active set.
    - Behavior:
        - For each `LiveQuest` with `deadlineMillis > 0` and `skip == false`:
            - Compute grace period in millis from `LiveQuest.gracePeriodMinutes` (0 if null/≤0).
            - If `nowMillis > deadlineMillis + graceMillis`:
                - Write a `QuestFailed` record under the `ModelDay` parent for `fromDay`.
                - Delete the `LiveQuest` instance.
        - `skip == true` and `deadlineMillis == 0` are treated as non-failing at rollover.
        - After processing `fromDay`, always calls `TodayPlannerService.ensureDay(userKey, toDay)` to materialize the new day (`toDay = fromDay.DayIndex + 1`).

Phase 3 — Single-day view switch (MVP UI)

Goals:
- Move from legacy task views to a LifeQuest-specific single-day view based on `LiveQuest` under `dy/{DayNum}/lv/*`.
- Keep UI code modular and reusable across demos, future apps, and platforms using LibGDX + wti-ui abstractions.

Modules:
- `wti-ui-view` — core view abstractions (package `net.wti.view`).
    - Shared base classes for LibGDX-based views (composition helpers, refresh lifecycle, common styles).
- `quest-view` — quest-specific view implementations (package `net.wti.quest.view`).
    - `LiveQuestView`: per-day LiveQuest visualization (replacement for older `DayView` usage).
    - Additional quest-focused components (filters, status badges, etc.) layered on top of core view primitives.
- `quest-view-sample` — simple launcher/main to preview individual components.
    - A tiny demo app that:
        - Seeds in-memory data (e.g. sample `ModelDay` + `LiveQuest` instances).
        - Creates a `LiveQuestView` for “today” and optionally adjacent days.
        - Allows manual refresh to validate layout/behavior during development.
- `quest-view-test` — minimal automated tests around view behavior.
    - Focus initially on:
        - Basic construction and non-crashing refresh cycles.
        - Simple state transitions (e.g. toggling skip flag, status changes).
    - Defer pixel-perfect or snapshot-based validation to a future “UI integration test” phase once the view hierarchy is stable.

Planned work items:
- [ ] Establish `wti-ui-view` core abstractions:
    - [ ] Define `net.wti.view.IsView` or reuse existing equivalent with a clear `refresh()` contract.
    - [ ] Provide simple layout helpers and base classes for table-based views (`AbstractViewTable`-style) that integrate with LibGDX `Skin`.
    - [ ] Document conventions for:
        - Refresh lifecycle (when to call `refresh()` vs. incremental updates).
        - Dependency boundaries (no direct persistence; views consume already-loaded models).
- [ ] Implement `quest-view` for LifeQuest:
    - [ ] Define `LiveQuestView` in `net.wti.quest.view`:
        - [ ] Accepts a `ModelDay` (or `DayIndex`) and a collection of `LiveQuest` instances (already loaded from storage).
        - [ ] Renders items grouped or sorted by:
            - `deadlineMillis` (non-zero first, earliest first).
            - Then `effectivePriority`.
        - [ ] Shows:
            - Name/title (from `QuestDefinition` snapshot or live definition, as available).
            - Deadline time (formatted using `ModelDay`’s zone and rollover).
            - Status (active/paused/etc.) and `skip` flag.
            - Tags and schedule template key for simple visual filtering.
        - [ ] Avoid binding to legacy `DayView` or `Schedule` types; work directly with `LiveQuest` and time APIs.
    - [ ] Add simple filter controls (for later wiring into controllers):
        - [ ] Filter by `skip` (show/hide skipped).
        - [ ] Filter by tags and schedule template key (MVP: basic multiselect/UI affordances; filtering logic can be client-side).
    - [ ] Provide hooks for actions (to be wired in Phase 4):
        - [ ] “Start” / “Activate” a quest instance.
        - [ ] “Finish”, “Cancel”, “Skip” actions (will later call completion flows that write `dn/cncl/skp` records).
- [ ] `quest-view-sample` demo:
    - [ ] Minimal launcher that:
        - [ ] Boots LibGDX with a simple stage/skin.
        - [ ] Uses fake or seeded `ModelDay` + `LiveQuest` data (no real backend dependency).
        - [ ] Places a `LiveQuestView` on screen with Today/Yesterday/Tomorrow toggles.
    - [ ] Ensure the sample is easy to run from Gradle/IDE for rapid UI iteration.
- [ ] `quest-view-test`:
    - [ ] Add very lightweight tests verifying:
        - [ ] `LiveQuestView` construction and `refresh()` do not throw with empty or simple data.
        - [ ] Sorting by `deadlineMillis` and `effectivePriority` behaves as expected for a small set of sample `LiveQuest` instances.
    - [ ] Defer “GUI is good” tests:
        - [ ] No snapshot or pixel-diff tests until the view hierarchy is closer to stable.
        - [ ] Track this as a future enhancement (UI integration tests that render and validate behavior and layout).

Phase 4 — Completion flows and history
- [ ] Finish flow -> write dn; remove lv.
- [ ] Cancel flow -> write cncl; remove lv.
- [ ] Skip flow -> write skp; remove lv (or keep with skip=true, choose one consistent policy).
- [ ] History view (basic).

Phase 5 — Tags and templates
- [ ] Tag inheritance & propagation (definition -> live).
- [ ] ScheduleTemplate management UI and mark-sweep apply; do not chase "next valid day."
- [ ] Context-aware default filters (work hours vs off hours).

Phase 6 — Composition (MVP)
- [ ] ChildRef with default parent policy (all-of) and start policy (parallel).
- [ ] On parent start, project children as LiveQuest for today as needed.
- [ ] Nested rendering (expand/collapse).

Phase 7 — Query engine and IndexWriter (optional)
- [ ] Client-side IndexWriter to precompute tag/template indexes.
- [ ] Extend ModelQuery to handle splayed dy/{DayNum}/{type} range queries efficiently.
- [ ] Add warm-cache server support or alternative backend.

---

## Test plan

Time and DayIndex ✅ COMPLETE
- [x] DayIndex calculations around rollover boundaries (3:59 -> 4:00).
- [x] DST transitions: ensure start/end timestamps and DayIndex roll correctly.
- [x] Timezone handling (UTC, EST, PST, Tokyo).
- [x] Edge cases: negative indices, far past/future, leap seconds.
- [x] Concurrent access safety.
- [x] Cache behavior and consistency.

Materialization ✅ MVP COMPLETE
- [x] One LiveQuest per (def, rule, day) uniqueness.
    - Enforced by `LiveQuestStore.findByDayAndLiveKey(day, liveKey)` + `PlannerService.ensureLiveQuestForDay(...)`.
- [x] AutoMaterialize on today; manual start creates/activates lv (manual start still to be wired from UI).
- [x] Deadline computed from TimeAnchor within ModelDay bounds for DAILY anchors.
    - WEEKLY/MONTHLY/YEARLY anchors are structurally defined but currently throw `UnsupportedOperationException` until semantics are finalized.

Rollover ✅ MVP COMPLETE
- [x] Overdue + grace -> `fld` record; `lv` removed.
- [x] `skip==true` -> never fails; can write `skp` on explicit skip action (planned in Phase 4).

History
- [ ] `dn`/`fld`/`cncl`/`skp` records with correct snapshots; immutable.
    - `QuestFailed` path is implemented via `RolloverService` and `RolloverStore`.
    - `QuestCompleted`, `QuestCanceled`, `QuestSkipped` flows will be wired from UI actions in Phase 4.
- [ ] Removal of `lv` on terminal transitions.

Tags and templates
- [ ] Definition tag changes propagate to live; history snapshots unaffected.
- [ ] Template toggle sets skip flags correctly via ScheduleTemplateService; no attempt to chase next valid day.

Composition
- [ ] Parent all-of completion computed from children; start policy parallel for MVP.
- [ ] Projected children appear for today only (bounded expansion).

Querying
- [ ] Range queries for up to 31 DayNum per type.
- [ ] Combined views issue one request per type and merge locally.
- [ ] RuleKey filters applied client-side (empty => all).

Concurrency and idempotency
- [x] Planner is safe under repeated executions; uniqueness enforced by (day, LiveKey) lookup in `LiveQuestStore`.
- [x] Rollover is safe to retry within a process; overdue detection is purely time-based (`deadline + grace` vs `now`).
    - Cross-process idempotency will depend on underlying storage semantics (X_Model/backend) and may require additional guards once wired to multi-node deployments.

---

## Open questions and decisions

- Keep Skipped as a record (dy/{DayNum}/skp/{LiveKey}) vs. leaving only skip=true on LiveQuest?
    - Current plan: write skp on explicit skip, remove lv to minimize active set; implicit "off day" = lv skip=true without skp record.
- Snapshot payload for history:
    - Minimum: name, description, tags, priority; add more only if needed.
- Multi-zone change by user at runtime:
    - Recommendation: LiveQuest binds to user zone at creation-time. New instances use updated zone.
- Multiple occurrences per day for the same rule:
    - Future feature: quantity or multiple anchors per rule.
- Future/past placeholders:
    - Keep synthetic-only; never persisted; unify under dy/{DayNum}/syn/{LiveKey} for internal consistency.

---

## Glossary

- App Epoch: Calendar date that defines DayIndex=0 (2025-10-10).
- DayIndex (DayNum): Number of days since app epoch, aligned to user's rolloverHour.
- ModelDay: Derived daily window (start/end + components); parent for live/history keys.
- QuestDefinition: Canonical description and structure of a quest.
- RecurrenceRule: Relative anchor-based schedule used to materialize live instances.
- LiveQuest: The active instance for a specific DayIndex.
- QuestCompleted/Failed/Canceled/Skipped: Immutable history records.
- ScheduleTemplate: Work/off/holiday policy that sets skip behavior.
- TimeAnchor: Anchor position inside a window (daily/weekly/monthly/yearly).
- ModelDuration: amount + unit used by recurrence rules.

---