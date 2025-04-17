package net.wti.ui.demo.common;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL31;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import net.wti.gdx.theme.raeleus.sgx.GdxThemeSgx;
import net.wti.gdx.theme.raeleus.sgx.TabbedPane;
import net.wti.ui.demo.api.CompletionStatus;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.api.ModelTaskCompletion;
import net.wti.ui.demo.ui.SettingsPanel;
import net.wti.ui.demo.ui.TaskTable;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.demo.ui.controller.TaskRegistry;
import net.wti.ui.demo.view.api.IsTaskView;
import xapi.model.X_Model;

/// DemoApp
///
/// Entry point for the UI.
/// Initializes and wires up theme, UI layers, tabs, task lists and controllers.
///
/// 『 Roadmap Checklist 』
///
/// 『 ✓ 』   1. ⚙️ Initialization and Setup
/// 『 ✓ 』      Set up `TaskRegistry` and `TaskController`
/// 『 ✓ 』      Initialize Active and Done tabs
///
/// 『 ✓ 』   2. 📋 Task Flow
/// 『 ✓ 』      Hook `markAsDone()` to move ONCE tasks to done
/// 『 ✓ 』      Reschedule recurring tasks
/// 『   』      Handle deferrals and cancellations
/// 『 ✓ 』      Toggle expanded/collapsed TaskView with click
/// 『 ✓ 』      Show expand/collapse icon on hover
/// 『 ✓ 』      Show more recurrence data in expanded view
/// 『 ✓ 』      Style expanded view with spacing and labels
/// 『 ✓ 』      Toggle expanded/collapsed for completed tasks (TaskCompletionView)
///
/// 『 ✓ 』   3. 📦 Persistence
/// 『 ✓ 』      Persist new tasks
/// 『   』      Load saved state
///
/// 『   』   4. ✅ UX Polish
/// 『   』      Animate task movement
/// 『   』      Undo option after task completion
/// 『   』      Improve style and layout of ACTIVE list
///
/// 『   』   5. ⚖️ Tests (via Spock/Groovy)
/// 『   』      Task lifecycle test coverage
/// 『   』      UI state transitions
/// 『   』      Recurrence handling logic
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 2025-04-16 @ 21:41 CST
public class DemoApp extends ApplicationAdapter {

    private Skin skin;
    private Stage stage;
    private TaskRegistry registry;
    private TaskController controller;
    private TaskTable active; // Table to show active tasks
    private TaskTable done;   // Table to show completed tasks

    @Override
    public void create() {
        // Setup theme and skin
        final GdxThemeSgx theme = new GdxThemeSgx();
        skin = theme.getSkin();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Load background image
        Texture texture = new Texture(Gdx.files.internal(theme.getAssetPath() + "/background.png"));
        Image background = new Image(texture);
        background.setFillParent(true);
        stage.addActor(background);

        // Hook the task registry callbacks to the views
        registry = new TaskRegistry(
                task -> done.addTask(task),      // move completed tasks
                task -> active.addTask(task)     // reinsert active recurring tasks
        );
        controller = new TaskController(registry);

        // Create task tables for active and done views
        active = new TaskTable(theme, controller);
        done = new TaskTable(theme, controller);

        active.setHeader("Active");
        done.setHeader("Done");

        // Tabbed layout root
        TabbedPane root = new TabbedPane(skin);
        root.setFillParent(true);
        if (stage.getWidth() > 400) {
            root.pad(10, 100, 10, 100);
        }

        // Layout for the active tab contents
        Table activePane = new Table(skin);
        activePane.setFillParent(true);
        activePane.add(active).expand().fill();

        // Hook up tabs
        root.addTab("Active", activePane);
        root.addTab("Done", done);
        root.addTab("Settings", new SettingsPanel(theme));

        stage.addActor(root);
        stage.setScrollFocus(active);

        // Inject demo task objects (generated from the checklist)
        injectTask("Implement Task Deferral", "Handle deferred scheduling and UI state update");
        injectTask("Implement Task Cancellation", "Allow tasks to be canceled and removed from active list");
        injectTask("Implement Task Save/Load", "Persist and reload tasks between app sessions");
        injectTask("Animate Task Movement", "Polish UI transitions when tasks move between lists");
        injectTask("Implement Undo for Completion", "Add undo option immediately after task is finished");
        injectTask("Add Unit Tests", "Full task lifecycle unit testing with Groovy/Spock");
        injectTask("Test Recurrence Logic", "Ensure weekly/biweekly/etc. recurrences work");
        injectTask("Toggle Expand/Collapse Tasks", "Make ACTIVE items clickable to show full details and description");
        injectTask("Show Recurrence Info", "Display recurrence details like day/time range in expanded view");
        injectTask("Hover Expand Button", "Indicate clickable expand/collapse affordance on hover");
        injectTask("Style Expanded TaskView", "Improve layout of TaskViewExpandable with clear spacing and labels");

        injectCompletion("Mark Task Done", "move ONCE task to done list");
        injectCompletion("Reschedule Recurring", "reinsert repeating task with updated deadline");
        injectCompletion("Persist New Task", "uses X_Model.persist");
        injectCompletion("Create Task UI Views", "TaskViewExpandable + DeadlineView setup");
        injectCompletion("Click Expand TaskView", "Make TaskViewExpandable respond to user click");
        injectCompletion("Show Expanded Recurrence Info", "Include recurrence data in expanded task view");
        injectCompletion("Style Expanded View", "Spaced layout, visible deadlines, and readable rows");
        injectCompletion("Toggle Completed View", "Click to expand/collapse additional info for completed tasks");
    }

    private void injectTask(String name, String desc) {
        ModelTask task = TaskFactory.create(name, desc);
        controller.save(task);
        IsTaskView view = active.addTask(task);
    }

    private void injectCompletion(String name, String desc) {
        ModelTaskCompletion done = X_Model.create(ModelTaskCompletion.class);
        done.setName(name);
        done.setDescription(desc);
        done.setCompleted(System.currentTimeMillis());
        done.setStatus(CompletionStatus.COMPLETED);
        controller.save(done);
        done(done);
    }

    private void done(ModelTaskCompletion done) {
        IsTaskView view = this.done.addTask(done);
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(1, 0, 0, 1);
        Gdx.gl.glClear(GL31.GL_COLOR_BUFFER_BIT);

        stage.act();
        stage.draw();

        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            dispose();
            create();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        skin.dispose();
        stage.dispose();
    }
}
