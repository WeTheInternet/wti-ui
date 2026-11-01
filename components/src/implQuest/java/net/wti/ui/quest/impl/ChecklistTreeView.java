package net.wti.ui.quest.impl;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import net.wti.quest.api.ChecklistSnapshot;
import net.wti.quest.api.QuestStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Stable goal cards with direct completion checkboxes. Expansion is local view state only.
 * Callers supply acknowledged snapshots; a click never optimistically checks a task. */
public final class ChecklistTreeView extends Table {
    private final Map<String, Goal> goals = new LinkedHashMap<>();
    private final Consumer<String> complete;
    private boolean loading;

    public ChecklistTreeView(final Skin skin, final Consumer<String> complete) {
        super(skin);
        this.complete = complete;
        top().left();
    }

    /** Initial/refresh reads must settle before the first visible goal can accept an intent. */
    public void setLoading(final boolean value) {
        loading = value;
        for (final Goal goal : goals.values()) goal.render();
    }

    public void setGoal(final String identity, final ChecklistSnapshot snapshot) {
        Goal goal = goals.get(identity);
        if (goal == null) {
            goal = new Goal(); goals.put(identity, goal);
            add(goal).growX().padBottom(16).row();
        }
        goal.snapshot = snapshot;
        goal.render();
    }

    public void setPending(final String identity, final boolean pending) {
        final Goal goal = goals.get(identity);
        if (goal != null) { goal.pending = pending; goal.render(); }
    }

    public void setError(final String identity, final String error) {
        final Goal goal = goals.get(identity);
        if (goal != null) { goal.error = error; goal.render(); }
    }

    public boolean hasGoal(final String identity) { return goals.containsKey(identity); }

    public boolean isExpanded(final String identity) { return goals.get(identity).expanded; }

    private final class Goal extends Table {
        ChecklistSnapshot snapshot;
        boolean expanded = true, pending;
        String error;
        final Table body = new Table(getSkin());
        final Label status = new Label("", getSkin());
        Goal() { super(ChecklistTreeView.this.getSkin()); top().left(); }
        void render() {
            clearChildren(); body.clearChildren();
            final ChecklistSnapshot.Entry root = snapshot.getRoot();
            final boolean leaf = snapshot.getChildren().isEmpty();
            if (leaf) add(check(root)).growX().minWidth(0).minHeight(48).row();
            else {
                final TextButton toggle = new TextButton((expanded ? "-  " : "+  ") + root.getTitle(), getSkin());
                toggle.getLabel().setWrap(true);
                toggle.setName("goal:" + root.getDefinitionKey());
                toggle.addListener(new ChangeListener() {
                    @Override public void changed(final ChangeEvent event, final Actor actor) {
                        expanded = !expanded; render();
                    }
                });
                add(toggle).growX().minWidth(0).minHeight(48);
                int done = 0;
                for (final ChecklistSnapshot.Entry entry : snapshot.getChildren()) if (entry.isComplete()) done++;
                add(new Label(done + "/" + snapshot.getChildren().size(), getSkin())).padLeft(12).row();
                if (expanded) {
                    for (final ChecklistSnapshot.Entry entry : snapshot.getChildren()) {
                        body.add(check(entry)).growX().minWidth(0).minHeight(48).padTop(4).row();
                    }
                    add(body).colspan(2).growX().minWidth(0).padLeft(12).row();
                }
            }
            if (!leaf && root.canComplete() && !root.isComplete()) {
                // Recovery for a reopened/partially acknowledged ALL_OF root, not an extra normal-path step.
                final TextButton retry = new TextButton("Finish goal", getSkin());
                retry.setDisabled(loading || pending || complete == null);
                retry.addListener(new ChangeListener() {
                    @Override public void changed(final ChangeEvent event, final Actor actor) {
                        if (retry.isDisabled()) return;
                        pending = true; render(); complete.accept(root.getLiveKey());
                    }
                });
                add(retry).colspan(2).minHeight(48).minWidth(48).left().row();
            }
            // Reserve one short status line so pending/ack updates do not shift the board.
            status.setText(error != null ? error : pending ? "Saving..." : "");
            status.setWrap(true);
            add(status).colspan(2).growX().minWidth(0).minHeight(24).padLeft(12).row();
        }
        CheckBox check(final ChecklistSnapshot.Entry entry) {
            final String quantity = entry.getQuantity() > 1 ? " x " + entry.getQuantity() : "";
            final CheckBox box = new CheckBox(entry.getTitle() + quantity, getSkin());
            box.setName("task:" + entry.getDefinitionKey());
            box.getLabel().setWrap(true);
            box.getLabelCell().growX().minWidth(0).padLeft(12);
            box.left();
            box.setProgrammaticChangeEvents(false);
            box.setChecked(entry.isComplete());
            final boolean active = entry.getStatus() == null || entry.getStatus() == QuestStatus.ACTIVE;
            box.setDisabled(loading || complete == null || pending || entry.isComplete()
                    || entry.getLiveKey() == null || !(entry.canComplete() || (snapshot.canStart() && active)));
            box.addListener(new ChangeListener() {
                @Override public void changed(final ChangeEvent event, final Actor actor) {
                    box.setChecked(entry.isComplete()); // Wait for the server acknowledgment.
                    if (box.isDisabled()) return;
                    pending = true; render();
                    complete.accept(entry.getLiveKey());
                }
            });
            return box;
        }
    }
}
