# wti-ui modules

`wti-ui` contains reusable Java-8-compatible libGDX UI contracts and LifeQuest service
layers. Module topology is authoritative in `wti-ui.xapi`; generated Gradle scripts are
diagnostic output.

## Responsive view composition

The `view` module owns generic Scene2D composition, including responsive panels made of
titled sections and leading-content rows. Its durable rule is layout-first: callers set
the outer actor bounds and supply content/styles, while the actor hierarchy owns
preferred height, wrapping, spacing, responsive columns, invalidation, rendering bounds,
and hit bounds. Do not maintain a second baseline or rectangle model beside Scene2D.

```java
ResponsivePanelStyle spacing = new ResponsivePanelStyle();
spacing.minimumColumnWidth = 480f;
spacing.panelPadTop = spacing.panelPadBottom = 18f;
spacing.panelPadLeft = spacing.panelPadRight = 20f;
spacing.columnGap = 24f;
spacing.sectionGap = 18f;
spacing.headingGap = 8f;
spacing.rowGap = 6f;
spacing.leadingBodyGap = 10f;

ResponsivePanel panel = new ResponsivePanel(spacing);
ResponsiveSection section = panel.addSection(headingActor);
section.addWrappedLabelRow(leadingActor, description, bodyLabelStyle);

ResponsiveScrollPane viewport = new ResponsiveScrollPane(panel, scrollPaneStyle);
viewport.setBounds(x, y, width, height);
```

For arbitrary Scene2D content, use `ResponsiveContentScrollPane`; it retains the same
vertical-scroll, hover-focus, clipping, and caller-owned-bounds policy without requiring a
`ResponsivePanel` wrapper.

### Responsive accordions

`ResponsiveAccordion` generalizes the legacy theme `AccordionPane` pattern while keeping
section geometry in one `Table`. Supply stable keys plus caller-owned header and body
actors. Sections are multi-open by default; enable exclusive mode for legacy
`OldTodayView` behavior. A trailing action is a separate actor and does not toggle the
header.

```java
ResponsiveAccordion accordion = new ResponsiveAccordion();
accordion.addSection("deadlines", deadlinesHeader, deadlinesBody, detachAction);
accordion.addSection("goals", goalsHeader, goalsBody);
accordion.expand("deadlines");
accordion.toggle("goals");
// accordion.setExclusive(true); // optional one-open-at-a-time mode
```

Collapsed bodies remain attached for actor identity, but are invisible, touch-disabled,
and contribute no effective Table height. Accordion section collapse is independent from
`FloatingPanel.setMinimized(...)`.

At validation time the panel uses two columns only when both meet
`minimumColumnWidth`; otherwise it reflows to one. Wrapping body labels receive the
remaining row width, grow the row's preferred height, and move following rows through
normal `Table` layout.

Run the focused tests with:

```bash
./gradlew :wti-ui-view:test
```

Publish the focused artifact with:

```bash
./gradlew :wti-ui-view:xapiPublish
```

Consumer coordinate: `net.wti:wti-ui-view:0.51`.

## Generic form controls

The `implForm` module provides typed Scene2D form elements backed by getter/setter
bindings. `WtiFormFieldBoolean` is intentionally a standard libGDX `CheckBox`, so
generic forms retain a conventional form appearance. Consumers may provide their own
skin/style or wrapper when a game-specific visual treatment is required.

Form controls only update the supplied binding. Settings persistence, network requests,
domain commands, localization, and other application policy belong to the consuming
game/controller layer.

## Floating editor panels

The `net.wti.ui.view.responsive` package also provides reusable Scene2D floating-panel
infrastructure. `FloatingPanel` keeps a caller-owned title actor and content actor in one
Table hierarchy and places the content inside a `ResponsiveContentScrollPane`; only the title
actor receives the drag handle. `FloatingPanelLayer` owns local panel placement and
clamping while remaining touchable only through its children, so unused layer space does
not block an actor beneath it.

```java
FloatingPanelStyle panelStyle = new FloatingPanelStyle();
panelStyle.edgePadding = 8f;
panelStyle.cascadeGap = 18f;
panelStyle.minimumWidth = 240f;
panelStyle.minimumHeight = 120f;

FloatingPanelLayer layer = new FloatingPanelLayer(panelStyle);
FloatingPanel panel = new FloatingPanel(titleActor, existingContent, scrollPaneStyle, panelStyle);
panel.setRedockAction(redockAction);
panel.setResizable(true);
panel.setLockedInsideParent(true);
layer.addPanel(panel);
editorStack.addActor(layer);
```

The consumer supplies the layer's bounds through normal Scene2D layout; for stage locking,
make the layer cover the editor's stage/viewport bounds. Panels are locked inside that
parent by default. `setLockedInsideParent(false)` permits intentional free-floating
positions. `setResizable(true)` exposes the bottom-right resize handle, and
`setMinimized(true)` collapses the viewport while preserving the expanded size for later
restoration. The consumer may persist only `panel.getX()`/`panel.getY()` and its minimized
state in its own preference model, restoring values after the layer has real bounds and
then calling `layer.clampPanels()`. Removing a panel never recreates or clones its content
actor. This module does not provide docking policy, window persistence, map behavior, or
domain requests.

The resize handle is caller-owned visual chrome: `setResizable(true)` enables its touch
region, while `setResizeHandleBackground(drawable)` supplies its visible styling. A demo or
game skin should style the handle explicitly; the generic view module does not assume a
particular skin or resize glyph.

The handle is the panel’s bottom-right corner in Scene2D coordinates: vertical drag
direction is interpreted from the parent-local delta, so dragging down grows the panel and
keeps its top edge anchored while dragging up shrinks it toward the configured minimum.

## Runnable demos

From the repository root, run the existing desktop showcases with:

```bash
./gradlew :gdx-themes-raeleus-shadeSample:runShadeSample
./gradlew :gdx-themes-raeleus-sgxSample:runSgxSample
./gradlew :demo-jre:runDemoApp
./gradlew :components-sampleQuest:runLiveQuestDemo
```

The Shade sample is the control gallery; it includes standard and switch checkboxes,
radio buttons, sliders, progress bars, select boxes, text fields, and windows. The WTI
demo includes the form/settings surface. The LifeQuest demo focuses on the day-plan UI.
These launch desktop windows and require an available X11/Wayland display.
