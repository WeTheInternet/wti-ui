package net.wti.ui.demo.ui.view;

import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.ui.TaskActionBar;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.view.DeadlineView;

/// TaskViewActive
///
/// Visualises a *live* `ModelTask` in both collapsed and expanded modes.
///
/// ### 『 Status Checklist 』
///
/// ## 📦 Compact View
/// - 『 ✓ 』 Render task name using prominent font
/// - 『 ✓ 』 Display truncated description preview
/// - 『 ✓ 』 Show deadline preview (TaskSummaryPane)
/// - 『 ✓ 』 TaskActionBar left‑aligned
///
/// ## 🔄 Expandable Layout
/// - 『 ✓ 』 Toggle expanded state
/// - 『 ✓ 』 Show full description & recurrence
/// - 『 ✓ 』 Integrate DeadlineView + labels
///
/// ## 🧠 Controller Integration
/// - 『 ✓ 』 Buttons route to TaskController
/// - 『 ○ 』 Inline editing support  *(UI‑4)*
///
/// ## ✨ Polish
/// - 『 ✓ 』 Hover highlight
/// - 『 ○ 』 Animations (UI‑7)
/// - 『 ○ 』 Recurrence edit control (UI‑6)
///
///  Created by ChatGPT 3o & James X. Nelson (James@WeTheInter.net) on 18/04/2025 @ 20:00
public final class TaskViewActive extends AbstractTaskView<ModelTask> {

    private final TaskActionBar actionBar;
    private final TaskSummaryPane summaryPane;
    private final Label nameLabel;
    private final Label descriptionLabel;
    private final Cell<Label> descriptionCell;

    public TaskViewActive(ModelTask task, Skin skin, TaskController ctl) {
        this(task,
                skin.has("taskview", TaskViewStyle.class)
                        ? skin.get("taskview", TaskViewStyle.class)
                        : new TaskViewStyle(),
                skin, ctl);
    }
    private TaskViewActive(ModelTask t, TaskViewStyle st, Skin sk, TaskController ctl) {
        super(t, st, sk, ctl);
        actionBar = new TaskActionBar(this, controller, style);
        summaryPane = new TaskSummaryPane(skin, model);
        nameLabel = label(model.getName(), style.nameStyle);
        nameLabel.setWrap(true);
        nameLabel.setAlignment(Align.left);

        defaults().space(4).pad(0);

        descriptionLabel = label(getDescriptionShort(), style.previewStyle);
        descriptionLabel.setAlignment(Align.left);

        add(nameLabel).growX().colspan(2);
        add(actionBar).colspan(1).row();
        descriptionCell = add(descriptionLabel);
        descriptionCell.growX().center().colspan(3).row();

        add(summaryPane).center().colspan(3).row();

        rebuild();
    }

    private String getDescriptionShort() {
        String desc = model.getDescription() == null ? "" :
                model.getDescription().replace('\n',' ').trim();
        if (desc.length() > 80) {
            // TODO: record that we are truncating
            desc = desc.substring(0,77)+"...";
        }
        return desc;
    }

    /* ---------------------------------------------------------------- */
    @Override protected DeadlineView createDeadlineView(ModelTask t) {
        return new DeadlineView(t.getDeadline(), skin, t.getAlarmMinutes());
    }

    /* ---------------------------------------------------------------- */
    @Override protected void buildCollapsed() {
        nameLabel.setText(model.getName());
        descriptionLabel.setText(getDescriptionShort());
        descriptionLabel.setWrap(false);

        summaryPane.collapse();
    }

    @Override protected void buildExpanded() {
        nameLabel.setText(model.getName());
        descriptionLabel.setText(model.getDescription());
        descriptionLabel.setWrap(true);

        summaryPane.expand();
    }

    @Override
    protected void rebuild() {
        if (isExpanded()) {
            buildExpanded();
        } else {
            buildCollapsed();
        }
    }

    /* ---------------------------------------------------------------- */
    private Label label(String txt, Label.LabelStyle ls) {
        return new Label(txt, ls!=null?ls:skin.get(Label.LabelStyle.class));
    }
    private boolean notEmpty(String s){return s!=null && !s.isEmpty();}
}
