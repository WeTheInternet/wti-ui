# ROADMAP

Author: James X. Nelson and AI Assistant (both chatGPT and claude engines)
Created: 2025-10-10
Last updated: 2026-04-09

This roadmap defines the LifeQuest data model, daily windowing model, key splaying scheme, materialization/rollover behavior, indexing/querying approach, and an implementation plan with checklists. It is intended to be a living document; update, annotate, and check items as progress is made.

Current focus
- Phase 3C (Definition-driven loading + ACL + user namespace materialization): in active implementation

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
- Keep active set small; write Completed/Failed (and Canceled/Skipped) for history; keep terminal LiveQuest persisted but filtered by default.
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
    - status: enum QuestStatus
        - render order is enum order (OVERDUE first, then ACTIVE, ...; see QuestStatus)
        - TodayView does not render PARKED or ARCHIVED
    - alarmDuration: ModelDuration (optional)
    - estimatedDuration: ModelDuration (optional; used for urgency/planning)
    - gracePeriodDuration: ModelDuration (optional; used for overdue/fail boundary)
    - snoozeUntil (absolute, optional) [MVP: snooze results in updated deadline elsewhere; view just sees updated deadline]
    - createdAt, updatedAt, startedAt, finishedAt
    - effectivePriority
    - tags: copied from definition on creation; updates propagate to live instances
    - skip: boolean (true when day is off via schedule template or ad-hoc)
    - scheduleTemplateKey (for filtering)

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
        - Persist LiveQuest terminal state (status=FINISHED, finishedAt/updatedAt timestamps).
        - Write QuestCompleted (snapshot fields).
        - Views/loaders filter FINISHED LiveQuest by default.
        - For rules: if immediate re-materialization is required (e.g., multi-times/day), compute and materialize again (later feature).
    - On explicit cancel:
        - Write QuestCanceled; persist terminal state.
    - On explicit skip:
        - Write QuestSkipped; persist terminal state or keep LiveQuest with skip=true (policy TBD).

- Rollover job (post-rollover tick)
    - For each LiveQuest:
        - If deadlineMillis > 0 and now > (deadlineMillis + gracePeriodDuration) and skip==false:
            - Write QuestFailed record; remove LiveQuest.
        - If skip==true: do not fail.
        - If now > deadlineMillis and now <= deadlineMillis + gracePeriodDuration:
            - Status is OVERDUE (rendered above ACTIVE in TodayView)
    - Materialize for the new DayIndex (autoMaterialize rules).

- Placeholders
    - Past/future views can render synthetic local-only "syn" placeholders to preview expected items.
    - Placeholders are inert and cannot be completed; they are not persisted.

---

## Views and filtering

Single-day view family (Phase 3)

- TodayView (new)
    - LiveQuest-based “what should I work on right now?”
    - Opinionated and noise-reduced:
        - prioritize actionable items, de-emphasize routine chores
        - intended to be usable on mobile (low noise) while taking advantage of desktop space when available
    - Key behaviors (MVP)
        - ranking priority: “deadlines within next hour” first
        - then blended score (deadline optional) + effectivePriority (+ later: estimatedDuration contribution)
        - status order: QuestStatus enum order (OVERDUE first)
        - do not render PARKED / ARCHIVED
        - skip==true: reduced to a note area (planned restore affordance)

- DayPlanView (new / renamed)
    - Generic planner-style “view a given day” baseline day viewer (used by TomorrowView later)
    - Shows explicit timed quests along a day axis, with hour bucketing and empty-range collapse
    - Rollover display semantics: Policy C (see decision below)
    - Future: will gain day-by-day scrolling to browse adjacent days (TomorrowView concept)

Later views (deferred; naming can stay simple until implemented)
- TomorrowView: browsing shell over DayPlanView (day-by-day forward/back)
- WeekView / MonthView: calendar overviews (de-prioritize routine chores)
- FutureView: long-term/unbounded quests only

Filtering (early)
- Query active live quests for a day: dy/{DayNum}/lv/*
- Client-side filter in early phases:
  - tags
  - scheduleTemplate
  - skip/status suppression

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
(Evidence: `wti-ui/src/implQuest/java/net/wti/quest/impl/TodayPlannerService.java`, `wti-ui/src/implQuest/java/net/wti/quest/impl/PlannerService.java`, `wti-ui/src/implQuest/java/net/wti/quest/impl/RolloverService.java`; tests in `wti-ui/src/implQuestTest/groovy/net/wti/quest/impl/*Spec.groovy`)
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

Goals
- Move from legacy task views to LifeQuest-specific single-day views.
- Build new views (do not reuse legacy task schedule widgets) to avoid repeating prior architectural coupling.
- Keep UI modular:
  - containers render lists/days of quests
  - quest views render a single quest (UI component)
- Keep views “pure” with respect to data loading:
  - view classes do not perform persistence calls
  - view classes do not own async subscriptions (binders/controllers can)

Phase 3A — Core UI building blocks ✅ MOSTLY COMPLETE
- [x] Base view abstractions exist (`IsView`, `BaseViewTable`)
- [x] GDX-view abstraction exists (`IsGdxView`) for views that expose an Actor
- [x] Quest container contracts exist (names stabilized)
  - [x] `IsQuestContainer extends IsView` (owns `setLiveQuests(Iterable<LiveQuest>)`) (Implemented in `components/src/quest/java/net/wti/ui/quest/api/IsQuestContainer.java`)
  - [x] `QuestDayView extends IsQuestContainer` (day-level container contract) (Implemented in `components/src/quest/java/net/wti/ui/quest/api/QuestDayView.java`)
- [x] Quest component contract exists
  - [x] `QuestView` (single-quest component base; will extend IsGdxView) (Currently minimal: `components/src/quest/java/net/wti/ui/quest/api/QuestView.java`)
  - [ ] `IsViewState` exists (UI-only state; collapsed/expanded etc.) (No evidence found in repo; not present under `components/` or `wti-ui/`)
- [x] Planner-style day container exists
  - [x] `DayPlanView` (planner/day viewer that buckets by hour and collapses empty ranges) (Implemented in `components/src/implQuest/java/net/wti/ui/quest/impl/DayPlanView.java`)
  - [ ] Time formatting selection (12h vs 24h) not yet wired to shared settings (Current behavior is 24h labels; see `DayPlanView.formatHour(...)` and `DefaultLiveQuestRowFactory.formatTime(...)`)
  - [ ] Relative labels (Yesterday/Tomorrow) not yet wired to DayIndexService/AppContext (Explicitly avoided in `DayPlanView.dayTitle(...)`)
- [x] Basic per-row rendering exists (temporary)
  - [x] `LiveQuestRowFactory` + `DefaultLiveQuestRowFactory` (returns Table) (Implemented in `components/src/quest/java/net/wti/ui/quest/api/LiveQuestRowFactory.java` and `components/src/implQuest/java/net/wti/ui/quest/impl/DefaultLiveQuestRowFactory.java`)
  - Note: this will be replaced by QuestView-based rendering (QuestViewFactory) once `QuestPlanView` exists
- [x] Desktop test harness exists for GL-backed Scene2D tests (`GdxDesktopTestHarness`, LWJGL3)

Phase 3B — Adoption (wiring + confidence) ⏳ IN PROGRESS
- [ ] Implement quest-level views (single quest components)
    - [ ] `QuestPlanView` (QuestView implementation used by DayPlanView)
      - more informative, less “do the task now”
    - [ ] `ActiveQuestView` (QuestView implementation used by TodayView)
      - concise “doing the task” focused rendering (low noise)
    - [ ] QuestView v1 contract confirmed:
      - [ ] `getQuest()` / `setQuest(LiveQuest)`
      - [ ] `asActor()` (via IsGdxView)
      - [ ] `getViewState()` returning `IsViewState` with `isCollapsed()` at minimum

- [ ] Implement the new TodayView (NEW container; no legacy reuse)
    - [ ] Accepts ModelDay + Iterable<LiveQuest> (provided by caller)
    - [ ] Defines “actionable now” ordering (MVP)
      - [ ] “deadlines within next hour” first
      - [ ] then blended score: optional deadline + effectivePriority + (later) estimatedDuration
      - [ ] status ordering: enum order (OVERDUE first)
      - [ ] do not render PARKED/ARCHIVED
      - [ ] skip==true summarized at bottom with planned restore affordance
    - [ ] Provide a “jump to now” control (explicit; do not auto-scroll on refresh)
    - [ ] Later: expand/contract, edit/reschedule, and time tracking controls

- [ ] Update / replace sample app(s)
    - [x] Keep current sample as a day-plan demo (DayPlanView) (Implemented in `components/src/sampleQuest/java/net/wti/ui/quest/sample/LiveQuestDemoApp.java` using `DayPlanView`)
    - [ ] Add a minimal TodayView sample:
      - seeds a ModelDay + sample LiveQuest
      - shows TodayView
      - provides manual refresh and jump-to-now

- [ ] Tests
    - Headless (fast):
      - [ ] DayPlanView construction + refresh is stable (empty/simple)
      - [ ] Sorting and bucketing logic correctness (deadline, priority, ties)
      - [ ] Rollover Policy C bucketing logic correctness
    - Desktop harness (GL-backed; only when needed):
      - [ ] rebuild stability with real Skin/theme assets
      - [ ] interaction smoke tests (construction, expand/collapse, etc.)

- [ ] Navigation wiring
    - [ ] Integrate TodayView into main navigation (replacing legacy OldTodayView) (Legacy exists at `demo/src/main/java/net/wti/ui/demo/ui/view/OldTodayView.java`; no new TodayView found in repo)
    - [ ] Link to day-plan browsing (TomorrowView shell) from TodayView footer

### Phase 3C — Definition-driven loading and namespace materialization ⏳ NEW

Goals
- Replace demo-time direct `LiveQuest` fixture loading with `QuestDefinition` loading from `.xapi`.
- Materialize per-user `LiveQuest` instances from definitions/rules into user namespace(s).
- ACL and visibility are namespace-derived (single source of truth), not duplicated on definitions.
- Provide a JRE-side model service extension to register manifests for required model types up front.

Completed / in-progress groundwork
- [x] Classpath loader abstraction exists for loading model resources from `META-INF/{suffix}`.
- [x] Quest loader path has moved to model-backed resource folders for sample/demo use.
- [x] AST visitor-based quest parsing path is wired (attribute + child traversal).
- [x] Sample/demo now uses classpath-driven loading instead of hardcoded in-method demo items.
- [x] Seed quest data file created and iterated with realistic task content.
- [x] Design decision: namespace is authoritative ACL source; no allow/deny lists on QuestDefinition.
- [x] Design decision: `QuestDefinition.auto()` defaults true when not set.

Planned implementation (next)
- [x] Add `QuestDefinitionLoaderImpl` that reads `.xapi` quest definition resources from classpath.
- [x] Define source resource convention for quest definitions (`META-INF/models/qdef`).
- [x] Add `QuestDefinitionStore` abstraction for namespaced definition access.
- [x] Add `UserGroupStore` abstraction in user module for preloaded account/group membership.
- [x] Add SPI contracts for `WtiUserLoader` and `WtiGroupLoader`.
- [x] Add async `NamespacedQuestDefinitionSource` (priority order: user > groups > root with coalescing).
- [ ] Wire materializer to consume async definition stream and create `LiveQuest` for `auto()==true`.
- [ ] Move demo bootstrap to users/groups first, then definitions, then materialization for `DEFAULT_USER="dad"`.

Testing plan additions
- [ ] Loader tests for definition resources:
  - [ ] per-file structure assertions for loaded `QuestDefinition`
  - [ ] nested composition parsing assertions
- [ ] Namespace + membership tests:
  - [ ] user namespace overrides group/root for same definition id
  - [ ] group namespace overrides root
  - [ ] root-only fallthrough works
- [ ] Materialization tests:
  - [ ] definition/rule/day uniqueness for generated `LiveQuest`
  - [ ] `auto()==true` default behavior when attribute omitted
  - [ ] stable regeneration/idempotency behavior
- [ ] JRE model service extension tests:
  - [ ] manifest registration coverage for required model classes
  - [ ] boot-time availability of manifests without ad hoc registration in app code

Decision settled (ties to tests): Rollover-hour display semantics for planner/day-plan views
- [x] Policy C:
  - When an item’s deadline is inside ModelDay but local hour `< rolloverHour`,
    it appears in the current day, rendered at end-of-day in extended buckets:
      - bucketHour = 24 + hourLocal
- [ ] Labeling/format decision (needs shared settings):
  - In 12-hour mode: label extended buckets as natural “1am” while ordering them after 11pm
  - In 24-hour mode: label extended buckets as “25:00” etc.

Decision deferred (documented; not blocking MVP)
- [ ] Relative day labels (“Yesterday” / “Tomorrow”) in day-plan views:
  - Needs DayIndexService and a stable “now” source.
  - Likely solved via injection (AppContext / services bundle).
  - For now, day title can be “Today” when day.contains(now) else explicit date.

Phase 4 — Completion flows and history
- [ ] Finish flow -> write dn; persist FINISHED.
- [ ] Cancel flow -> write cncl; persist terminal status.
- [ ] Skip flow -> write skp; persist terminal status (or keep with skip=true, choose one consistent policy).
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

Phase 3 UI tests ⏳ IN PROGRESS
- [ ] DayPlanView.refresh() is stable (no throws) across empty/simple/large inputs.
- [ ] Sorting correctness tests (deadline / priority / tie-breaks).
- [ ] Bucketing correctness tests (zone + window boundaries).
- [ ] Rollover Policy C semantics test (enforce via tests).
- [ ] TodayView ranking tests (deadlines next hour, blended scoring, status ordering).
- [ ] No snapshot/pixel tests until view hierarchy is stable.

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

New open questions (Phase 3C)
- ACL schema location:
  - Namespace is the sole source of truth (no allow/deny fields on `QuestDefinition`).
- Namespace identity format:
  - user key string is the namespace for user definitions,
  - group key string is the namespace for group definitions,
  - root namespace is global/shared definitions.
- Merge precedence:
  - user namespace > group namespace > root namespace for same definition id.
- Materialization ownership:
  - projection service consumes async namespaced definition stream and materializes `LiveQuest` for `auto()==true`.

## Open questions and decisions

- Settings location and ownership:
  - Need shared settings for:
    - time format selection (12h vs 24h)
    - timezone defaults (already exist in demo ModelSettings; may move to core)
  - Candidate approach:
    - AppContext / services bundle for safe shared services (DayIndexService, formatting policy, etc.)

- QuestView factory migration:
  - Replace LiveQuestRowFactory (Table rows) with a QuestViewFactory producing QuestView components.
  - DayPlanView uses QuestPlanView; TodayView uses ActiveQuestView.

- Index subscriptions:
  - Views remain pure and accept current snapshot via setLiveQuests().
  - Binder/controller can subscribe to an index/store and call setLiveQuests()+refresh() as data arrives.

- TomorrowView naming:
  - TomorrowView is a browsing shell over DayPlanView; it can scroll forward/back by day.
  - Week/Month/Year browsing to be decided after TodayView is stable.

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

## Next Task

Objective
- Make the Today view able to display LiveQuest items and complete them (write `dn` record + remove `lv`).

Relevant files / modules
- Today materialization + rollover (already exists):
  - `wti-ui/src/implQuest/java/net/wti/quest/impl/TodayPlannerService.java`
  - `wti-ui/src/implQuest/java/net/wti/quest/impl/PlannerService.java`
- History record models (already exist):
  - `wti-ui/src/quest/java/net/wti/quest/api/QuestCompleted.java`
  - `wti-ui/src/quest/java/net/wti/quest/api/QuestHistoryRecord.java`
- Current day-plan UI building blocks (already exist):
  - `components/src/implQuest/java/net/wti/ui/quest/impl/DayPlanView.java`
  - `components/src/quest/java/net/wti/ui/quest/api/QuestView.java` (currently empty)
  - `components/src/quest/java/net/wti/ui/quest/api/QuestDayView.java`
  - `components/src/quest/java/net/wti/ui/quest/api/LiveQuestRowFactory.java`
  - `components/src/implQuest/java/net/wti/ui/quest/impl/DefaultLiveQuestRowFactory.java`
- Legacy navigation target to replace:
  - `demo/src/main/java/net/wti/ui/demo/ui/view/OldTodayView.java`
- Move demo loading from direct LiveQuest fixtures to QuestDefinition-driven materialization per user namespace with ACL filtering.
Implementation checklist
- [ ] Add `QuestDefinitionLoaderImpl` with classpath `.xapi` support.
- [ ] Add ACL fields to `QuestDefinition` + parser mapping.
- [ ] Add `DefinitionMaterializer` (or similarly named service) to project definitions into `LiveQuest`.
- [ ] Add namespace-aware filtering for root + user definition sets.
- [ ] Add JRE model service extension to register manifests at startup.
- [ ] Switch demo app to: load definitions → filter ACL → materialize today → render.

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