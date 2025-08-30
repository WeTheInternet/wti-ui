package net.wti.ui.demo.ui.view;


import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import net.wti.ui.components.SymbolButton;
import net.wti.ui.demo.api.BasicModelTask;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.api.ModelTaskDescription;
import net.wti.ui.demo.common.DemoConstants;
import net.wti.ui.demo.i18n.Messages;
import net.wti.ui.demo.ui.controller.TaskController;
import xapi.model.X_Model;

/// TaskViewDescription
///
/// Row renderer for `ModelTaskDescription` objects displayed in the **Library**
/// tab.  Presents three glyph buttons:
/// ▶ Start • ✏ Edit • 🗑 Delete
///
/// ### 『 Status Checklist 』
/// - 『 ✓ 』 Start button copies definition → ModelTask & persists
/// - 『 ○ 』 Edit button opens inline editor *(UI‑4)*
/// - 『 ✓ 』 Delete button removes definition & view
/// - 『 ○ 』 Keyboard shortcuts for quick actions
public final class TaskViewDescription extends AbstractTaskView<ModelTaskDescription> {

    private Cell<TaskViewDescription> cell;

    public TaskViewDescription(ModelTaskDescription d, Skin skin, TaskController ctl) {
        this(d,
                skin.has("taskview", TaskViewStyle.class)
                        ? skin.get("taskview", TaskViewStyle.class)
                        : new TaskViewStyle(),
                skin, ctl);
    }
    private TaskViewDescription(ModelTaskDescription d, TaskViewStyle st, Skin sk, TaskController ctl) {
        super(d, st, sk, ctl);
        rebuild();
    }

    @Override protected void buildCollapsed() {
        add(label(model.getName(), style.nameStyle)).left().expandX();
        add(buttonBar()).right();
    }
    @Override protected void buildExpanded() { buildCollapsed(); }

    /* ---------------------------------------------------------------- */
    private Table buttonBar() {
        Table t = new Table(skin);
        Messages msg = DemoConstants.MESSAGES;
        t.add(icon(DemoConstants.GLYPH_START, this::startTask, msg.buttonStart())).padRight(4);
        t.add(icon(DemoConstants.GLYPH_EDIT, this::editTask, msg.buttonEdit())).padRight(4);
        t.add(icon(DemoConstants.GLYPH_CANCEL, this::deleteTask, msg.buttonDelete()));
        return t;
    }

    private void startTask() {
        ModelTask task = X_Model.create(ModelTask.class);
        BasicModelTask.copyModel(model, task);
        task.setTaskSource(model.getKey());
        controller.save(task);
    }
    private void editTask(){ /* TODO UI‑4 */ }
    private void deleteTask(){
        controller.deleteTaskDescription(model);
        remove();
        if (cell != null) {
            cell.spaceBottom(0);
            cell = null;
        }
    }

    /* helpers */
    private SymbolButton icon(final String glyph, final Runnable r, final String tooltip) {
        SymbolButton b = new SymbolButton(glyph, SymbolButton.STYLE_NORMAL, skin, tooltip);
        b.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                r.run();
            }
        });
        return b;
    }
    private Label label(String s, Label.LabelStyle ls){
        return new Label(s, ls!=null?ls:skin.get(Label.LabelStyle.class));
    }

    public void setCell(final Cell<TaskViewDescription> cell) {
        this.cell = cell;
    }

    public Cell<TaskViewDescription> getCell() {
        return cell;
    }
}
