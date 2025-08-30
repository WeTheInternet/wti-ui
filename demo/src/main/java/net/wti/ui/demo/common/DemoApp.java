package net.wti.ui.demo.common;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL31;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import net.wti.gdx.theme.raeleus.sgx.TabbedPane;
import net.wti.tasks.event.RefreshFinishedEvent;
import net.wti.tasks.index.TaskIndex;
import net.wti.ui.demo.theme.TaskUiTheme;
import net.wti.ui.demo.ui.SettingsPanel;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.demo.ui.controller.TaskRegistry;
import net.wti.ui.demo.ui.view.TaskTableActive;
import net.wti.ui.demo.ui.view.TaskTableComplete;
import net.wti.ui.demo.ui.view.TaskTableDefinitions;
import net.wti.ui.demo.ui.view.TodayView;
import xapi.fu.Do;
import xapi.fu.Pointer;
import xapi.fu.log.Log;

/// DemoApp
///
/// ——— Prefix Legend ———
/// - 🏗 **APP** : Application‑wide features (DemoApp)
/// - 🧠 **CTL** : Controller logic (TaskController)
/// - 🎨 **UI**  : View & widget improvements (TaskView, TaskTable…)
/// - 🔎 **SORT**: Sorting & filtering concerns
/// - 🌐 **I18N**: Internationalisation tasks
/// - 🧪 **TEST**: Automated tests
/// - ♿ **ACC** : Accessibility enhancements
/// - 💾 **SYS** : Persistence, preferences, performance
/// - ⚙️ **CMD** : Command‑line options & parsing
///
/// 『 Roadmap Checklist 』
///
/// 🔥 **High Priority**
/// - 『 ✓ 』 APP‑1 Handle deferral & cancellation flow
/// - 『 ○ 』 CTL‑1 Implement cancel() in TaskController
/// - 『 ○ 』 CTL‑2 Implement defer() in TaskController
/// - 『 ○ 』 UI‑1 Refactor TaskView to TaskActionBar + TaskSummaryPane
/// - 『 ○ 』 SORT‑1 Sort Active by nearest deadline
/// - 『 ○ 』 SORT‑2 Weighted sort (deadline × priority)
/// - 『 ○ 』 SORT‑3 Filter Active by priority
///
/// 📈 **Medium Priority**
/// - 『 ○ 』 I18N‑1 Integrate xapi‑i18n for strings & dates
/// - 『 ○ 』 TEST‑1 Unit‑test task lifecycle (Spock)
/// - 『 ○ 』 TEST‑2 Recurrence‑handling tests
/// - 『 ○ 』 UI‑2 Improve ACTIVE list styling/layout
/// - 『 ○ 』 UI‑3 Improve DONE list styling/layout
/// - 『 ○ 』 UI‑4 Inline editing in TaskView
/// - 『 ○ 』 UI‑5 Long‑press tool‑tips on buttons
/// - 『 ○ 』 CTL‑3 Hook recurrence editing logic
/// - 『 ○ 』 SORT‑4 Sorting controls UI
/// - 『 ○ 』 SORT‑5 Remember last sort (smart default)
/// - 『 ○ 』 UI‑10 Library tab for all tasks
/// - 『 ○ 』 CTL‑6 Snooze logic persistence
///
/// 💤 **Low Priority**
/// - 『 ○ 』 APP‑2 Load saved state
/// - 『 ○ 』 APP‑3 Animate task movement between tabs
/// - 『 ○ 』 TEST‑3 UI state‑transition tests
/// - 『 ○ 』 CTL‑4 Undo for recent completions
/// - 『 ○ 』 UI‑6 Editable recurrence control
/// - 『 ○ 』 UI‑7 Expand/collapse animations
/// - 『 ○ 』 UI‑8 Search/quick‑find box
/// - 『 ○ 』 ACC‑1 Screen‑reader labels for controls
/// - 『 ○ 』 SYS‑1 User preferences (depends on loops port)
/// - 『 ○ 』 UI‑11 Sorting & filtering of Library tab
///
/// 🔮 **Future**
/// - 『 ○ 』 APP‑4 Keyboard‑shortcut help overlay
/// - 『 ○ 』 CTL‑5 Persist‑finished notification hooks
/// - 『 ○ 』 UI‑9 Elegant empty‑state handling
/// - 『 ○ 』 SYS‑2 Performance audit for large lists
/// - 『 ○ 』 CMD‑1 `--demo=false` flag support
/// - 『 ○ 』 CMD‑2 `--headless=true` flag support
///
/// Entry point for the libGDX **task‑tracking demo**.
public final class DemoApp extends ApplicationAdapter {

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Mutable state
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private Skin skin;
    private Stage stage;
    private TaskRegistry registry;
    private TaskController controller;
    private TodayView today; // main interface for completing tasks
    private TaskTableDefinitions library; // tasks definitions
    private TaskTableActive active; // active tasks
    private TaskTableComplete complete;   // completed tasks
    private TabbedPane tabs;
    private boolean doInvalidate;
    private TaskUiTheme theme;
    private Do cleanup = Do.NOTHING;

    // -------------------------------------------------------------------
    // Life‑cycle overrides
    // -------------------------------------------------------------------

    /// libGDX init callback: constructs UI and demo data.
    @Override
    public void create() {
        // Theme & Skin
        theme = new TaskUiTheme();
        skin = theme.getSkin();
        theme.applyTooltipDefaults();

        // Stage
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Background
        Texture tex = new Texture(Gdx.files.internal(theme.getAssetPath() + "/background.png"));
        Image bg = new Image(tex);
        bg.setFillParent(true);
        stage.addActor(bg);

        // Registry / Controller
        registry = new TaskRegistry(
                (task, completion) -> {
                    active.removeTask(task);
                    complete.addTask(completion);
                },   // move ONCE tasks → Done
                t -> active.addTask(t)  // reschedule recurring tasks
        );
        final TaskIndex index = new TaskIndex();
        Pointer<Do> undo = Pointer.pointer();
        undo.in(index.subscribeEvents(e->{
            undo.out1().done();
            if (index.getAll().isEmpty()) {
                Log.tryLog(DemoApp.class, this, "No index data yet; seeding new data");
                // 7 — Add seed data (chatgpt will regen these from the checklist in this class's javadoc)
                SeedDataGenerator.seed(controller, library, active, complete);
            }
        }, RefreshFinishedEvent.class));
        controller = new TaskController(registry, index);
        cleanup = cleanup.doAfter(index.startAutoRefresh(5));

        // Task views

        today = new TodayView(theme, controller);
        library = new TaskTableDefinitions(theme, controller);
        active = new TaskTableActive(theme, controller);
        complete = new TaskTableComplete(theme, controller);
        library.setHeader("All");
        active.setHeader("Active");
        complete.setHeader("Complete");

        // Tabs
        tabs = new TabbedPane(skin, Align.center);
        tabs.setFillParent(true);
        stage.addActor(tabs);
        tabs.addTab("Today",   today);
        tabs.addTab("Active",   active);
        tabs.addTab("Complete", complete);
        tabs.addTab("All",   library);
        tabs.addTab("Settings", new SettingsPanel(theme));
        stage.setScrollFocus(today);

        // Reset outer tab panel padding / trigger invalidation + layout
        updatePad();
    }

    /// Render loop – clears screen, delegates to Stage, supports F5 hot‑reload.
    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL31.GL_COLOR_BUFFER_BIT);
        /// update logic
        stage.act();
        /// trigger any invalidations
        if (doInvalidate) {
            doInvalidate = false;
            tabs.refreshLayout();
        }
        /// perform drawing after children have had a chance to redraw/remeasure themselves
        stage.draw();

        /// Allow triggering a full redraw w/ the F5 key.
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            dispose();
            create();
        }
    }

    /// Updates viewport on window resize.
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        stage.getViewport().update(width, height, true);
        updatePad();
    }

    /// Cleanup resources.
    @Override
    public void dispose() {
        cleanup.done();
        cleanup = Do.NOTHING;
        skin.dispose();
        stage.dispose();
    }

    private void updatePad() {
        final float totalWidth = stage.getWidth();
        final float totalHeight = stage.getHeight();
        // Use nearly full width, leaving only a small horizontal gutter.
        // This replaces the previous MAX_WIDTH clamp that centered a narrow column.
        final float gutter = Math.min(16f, totalWidth * 0.02f); // tweak if you want more/less margin
        tabs.pad(0, gutter, 0, gutter);

        // Determine portrait vs landscape based on actual aspect ratio
        theme.setLandscape(totalWidth >= totalHeight);

        Log.tryLog(DemoApp.class, this,
                "Updating for screen size", totalWidth, totalHeight
                , "Tabs Info:", tabs.getInfo()
        );
        // Trigger a re‑layout after the size/padding change is applied
        doInvalidate = true;

    }
}