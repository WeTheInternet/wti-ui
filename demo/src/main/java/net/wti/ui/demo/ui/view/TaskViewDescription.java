package net.wti.ui.demo.ui.view;


import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import net.wti.ui.demo.api.BasicModelTask;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.api.ModelTaskDescription;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.view.DeadlineView;
import xapi.model.X_Model;
import xapi.util.api.SuccessHandler;

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

    /* ---------------------------------------------------------------- */
    @Override protected DeadlineView createDeadlineView(ModelTaskDescription m){ return null; }

    @Override protected void buildCollapsed() {
        add(label(model.getName(), style.nameStyle)).left().expandX();
        add(buttonBar()).right();
    }
    @Override protected void buildExpanded() { buildCollapsed(); }

    /* ---------------------------------------------------------------- */
    private Table buttonBar() {
        Table t = new Table(skin);
        t.add(icon("▶", this::startTask)).padRight(4);
        t.add(icon("✏", this::editTask)).padRight(4);
        t.add(icon("🗑", this::deleteTask));
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
        X_Model.delete(model.getKey(), SuccessHandler.noop());
        remove();
    }

    /* helpers */
    private TextButton icon(String glyph, Runnable r){
        TextButton b = new TextButton(glyph, style.buttonStyle);
        b.addListener(new ClickListener(){@Override public void clicked(InputEvent e, float x, float y){r.run();}});
        return b;
    }
    private Label label(String s, Label.LabelStyle ls){
        return new Label(s, ls!=null?ls:skin.get(Label.LabelStyle.class));
    }
}
