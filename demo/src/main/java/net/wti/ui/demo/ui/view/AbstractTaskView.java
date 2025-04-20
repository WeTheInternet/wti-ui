package net.wti.ui.demo.ui.view;


import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import net.wti.ui.api.IsDeadlineView;
import net.wti.ui.demo.api.BasicModelTask;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.demo.view.api.IsTaskView;
import net.wti.ui.view.DeadlineView;

/// AbstractTaskView
///
/// Base class for any Scene2D widget that visualises a `BasicModelTask`.
/// Handles hover background, expand/collapse plumbing, and exposes
/// theming via `TaskViewStyle`.
///
/// ### 『 Status Checklist 』
/// - 『 ✓ 』 Shared hover/expanded behaviour
/// - 『 ✓ 』 `TaskViewStyle` extracted for JSON theming
/// - 『 ✓ 』 DeadlineView helper
/// - 『 ○ 』 Animations for expand/collapse
/// - 『 ○ 』 Keyboard accessibility & a11y labels
///
/// Created 2025‑04‑18 by ChatGPT‑4o & James X. Nelson (James@WeTheInter.net)
public abstract class AbstractTaskView<M extends BasicModelTask<M>>
        extends Table implements IsTaskView<M> {

    // ------------------------------------------------------------------ //
    // Style bundle (all optional; supplied via skin JSON)                 //
    // ------------------------------------------------------------------ //
    public static class TaskViewStyle {
        public Drawable background;
        public Drawable hoveredBackground;
        public Label.LabelStyle nameStyle;
        public Label.LabelStyle descStyle;
        public Label.LabelStyle previewStyle;
        public Label.LabelStyle recurrenceLabelStyle;
        public Label.LabelStyle recurrenceValueStyle;
        public TextButton.TextButtonStyle buttonStyle;
        public TextButton.TextButtonStyle editButtonStyle;
        public TextButton.TextButtonStyle toggleButtonStyle;
    }

    // ------------------------------------------------------------------ //
    // Fields                                                              //
    // ------------------------------------------------------------------ //
    protected final M model;
    protected final TaskController controller;
    protected final Skin skin;
    protected final TaskViewStyle style;
    protected final DeadlineView deadlineView;

    private boolean expanded;
    private boolean hovered;

    // ------------------------------------------------------------------ //
    // Construction                                                        //
    // ------------------------------------------------------------------ //
    protected AbstractTaskView(
            M model,
            TaskViewStyle style,
            Skin skin,
            TaskController controller
    ) {
        super(skin);
        this.model      = model;
        this.skin       = skin;
        this.style      = style;
        this.controller = controller;
        this.deadlineView = createDeadlineView(model);

        if (style.background != null) setBackground(style.background);
        addHoverListener();
    }

    // ------------------------------------------------------------------ //
    // Abstract hooks implemented by subclasses                            //
    // ------------------------------------------------------------------ //
    protected abstract DeadlineView createDeadlineView(M model);
    protected abstract void buildCollapsed();
    protected abstract void buildExpanded();

    // ------------------------------------------------------------------ //
    // IsTaskView implementation                                           //
    // ------------------------------------------------------------------ //
    @Override public void expand()   { if (!expanded) toggleExpanded(); }
    @Override public void collapse() { if (expanded)  toggleExpanded(); }
    @Override public boolean isExpanded() { return expanded; }
    @Override public M getTask() { return model; }
    @Override public IsDeadlineView getDeadlineView() { return deadlineView; }
    @Override public void rerender() { rebuild(); }

    // ------------------------------------------------------------------ //
    // Helpers                                                             //
    // ------------------------------------------------------------------ //
    public void toggleExpanded() {
        expanded = !expanded;
        rebuild();
    }

    protected void rebuild() {
        clear();
        pad(8).top().left();
        if (expanded) buildExpanded(); else buildCollapsed();
    }

    private void addHoverListener() {
        addListener(new InputListener() {
            @Override public void enter(InputEvent e,float x,float y,int p,Actor fr) {
                hovered = true;  updateBg();
            }
            @Override public void exit(InputEvent e,float x,float y,int p,Actor to) {
                hovered = false; updateBg();
            }
        });
    }
    private void updateBg() {
        Drawable bg = style.background;
        if (hovered && style.hoveredBackground != null) bg = style.hoveredBackground;
        setBackground(bg);
    }
}